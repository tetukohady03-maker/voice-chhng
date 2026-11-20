#include "OboeAudioEngine.h"
#include <android/log.h>
#include <algorithm>
#include <cstring>

#define LOG_TAG "VoxMorphOboe"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace voxmorph {

OboeAudioEngine::OboeAudioEngine() {
    inputScratch_.resize(4096, 0.0f);
}

OboeAudioEngine::~OboeAudioEngine() {
    stop();
}

bool OboeAudioEngine::start(int32_t sampleRate, int32_t framesPerBurst) {
    if (running_.load(std::memory_order_acquire)) {
        return true;
    }
    sampleRate_ = (sampleRate > 0) ? sampleRate : 48000;
    framesPerBurst_ = (framesPerBurst > 0) ? framesPerBurst : 192;
    processor_.configure(static_cast<float>(sampleRate_));
    underrunCount_.store(0, std::memory_order_relaxed);

    if (!openStreams()) {
        closeStreams();
        return false;
    }
    running_.store(true, std::memory_order_release);
    return true;
}

void OboeAudioEngine::stop() {
    running_.store(false, std::memory_order_release);
    closeStreams();
}

bool OboeAudioEngine::openStreams() {
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setSampleRate(sampleRate_)
        ->setUsage(oboe::Usage::VoiceCommunication)
        ->setContentType(oboe::ContentType::Speech)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    oboe::Result outRes = outBuilder.openStream(outputStream_);
    if (outRes != oboe::Result::OK) {
        LOGI("Exclusive output failed (%s), falling back to Shared", oboe::convertToText(outRes));
        outBuilder.setSharingMode(oboe::SharingMode::Shared);
        outRes = outBuilder.openStream(outputStream_);
        if (outRes != oboe::Result::OK) {
            LOGE("Failed to open output stream: %s", oboe::convertToText(outRes));
            return false;
        }
    }

    sampleRate_ = outputStream_->getSampleRate();
    framesPerBurst_ = outputStream_->getFramesPerBurst();
    processor_.configure(static_cast<float>(sampleRate_));

    // Set buffer size to 2 bursts for optimal low-latency / glitch-free balance
    outputStream_->setBufferSizeInFrames(framesPerBurst_ * 2);

    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setSampleRate(sampleRate_)
        ->setInputPreset(oboe::InputPreset::VoiceCommunication);

    oboe::Result inRes = inBuilder.openStream(inputStream_);
    if (inRes != oboe::Result::OK) {
        inBuilder.setSharingMode(oboe::SharingMode::Shared);
        inRes = inBuilder.openStream(inputStream_);
        if (inRes != oboe::Result::OK) {
            LOGE("Failed to open input stream: %s", oboe::convertToText(inRes));
            return false;
        }
    }

    if (static_cast<int32_t>(inputScratch_.size()) < framesPerBurst_ * 8) {
        inputScratch_.resize(static_cast<size_t>(framesPerBurst_ * 8), 0.0f);
    }

    // Start input first, then output
    if (inputStream_->requestStart() != oboe::Result::OK ||
        outputStream_->requestStart() != oboe::Result::OK) {
        LOGE("Failed to start full-duplex audio streams");
        return false;
    }

    auto outLat = outputStream_->calculateLatencyMillis();
    auto inLat = inputStream_->calculateLatencyMillis();
    float totalMs = 0.0f;
    if (outLat) totalMs += static_cast<float>(outLat.value());
    if (inLat) totalMs += static_cast<float>(inLat.value());
    if (totalMs <= 0.0f) {
        // Fallback estimate from burst size
        totalMs = (static_cast<float>(framesPerBurst_ * 2) / static_cast<float>(sampleRate_)) * 1000.0f;
    }
    latencyMs_.store(totalMs, std::memory_order_relaxed);
    LOGI("Oboe full-duplex started @ %d Hz, burst=%d, estLatency=%.2f ms",
         sampleRate_, framesPerBurst_, totalMs);
    return true;
}

void OboeAudioEngine::closeStreams() {
    if (outputStream_) {
        outputStream_->stop();
        outputStream_->close();
        outputStream_.reset();
    }
    if (inputStream_) {
        inputStream_->stop();
        inputStream_->close();
        inputStream_.reset();
    }
}

void OboeAudioEngine::updateParameters(const DspParameters& params) {
    processor_.updateParameters(params);
}

void OboeAudioEngine::processOfflineBuffer(const float* input, float* output, int32_t numFrames) {
    processor_.processBlock(input, output, numFrames);
}

oboe::DataCallbackResult OboeAudioEngine::onAudioReady(
    oboe::AudioStream* audioStream,
    void* audioData,
    int32_t numFrames) {

    auto* out = static_cast<float*>(audioData);
    if (!running_.load(std::memory_order_relaxed) || !inputStream_) {
        std::memset(out, 0, static_cast<size_t>(numFrames) * sizeof(float));
        return oboe::DataCallbackResult::Continue;
    }

    if (numFrames > static_cast<int32_t>(inputScratch_.size())) {
        std::memset(out, 0, static_cast<size_t>(numFrames) * sizeof(float));
        return oboe::DataCallbackResult::Continue;
    }

    auto readResult = inputStream_->read(inputScratch_.data(), numFrames, 0);
    if (!readResult || readResult.value() <= 0) {
        std::memset(out, 0, static_cast<size_t>(numFrames) * sizeof(float));
        underrunCount_.fetch_add(1, std::memory_order_relaxed);
        return oboe::DataCallbackResult::Continue;
    }

    int32_t framesRead = readResult.value();
    if (framesRead < numFrames) {
        std::memset(inputScratch_.data() + framesRead, 0,
                    static_cast<size_t>(numFrames - framesRead) * sizeof(float));
    }

    processor_.processBlock(inputScratch_.data(), out, numFrames);

    auto xruns = audioStream->getXRunCount();
    if (xruns) {
        underrunCount_.store(xruns.value(), std::memory_order_relaxed);
    }

    return oboe::DataCallbackResult::Continue;
}

void OboeAudioEngine::onErrorAfterClose(oboe::AudioStream* /*audioStream*/, oboe::Result error) {
    if (error == oboe::Result::ErrorDisconnected && running_.load(std::memory_order_acquire)) {
        LOGI("Audio device disconnected (Bluetooth/headphone change); restarting Oboe streams");
        closeStreams();
        openStreams();
    }
}

} // namespace voxmorph
