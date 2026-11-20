#pragma once

#include "dsp/VoiceDspProcessor.h"
#include <oboe/Oboe.h>
#include <atomic>
#include <memory>
#include <vector>

namespace voxmorph {

/**
 * Full-duplex Oboe/AAudio real-time engine with lock-free SPSC ring buffer.
 * Uses LowLatency + Exclusive sharing (with automatic Shared fallback) and handles
 * headset/Bluetooth disconnection restarts cleanly via onErrorAfterClose.
 */
class OboeAudioEngine : public oboe::AudioStreamDataCallback,
                        public oboe::AudioStreamErrorCallback {
public:
    OboeAudioEngine();
    ~OboeAudioEngine() override;

    bool start(int32_t sampleRate, int32_t framesPerBurst);
    void stop();
    bool isRunning() const { return running_.load(std::memory_order_acquire); }

    void updateParameters(const DspParameters& params);

    float getInputRms() const { return processor_.getInputRms(); }
    float getOutputRms() const { return processor_.getOutputRms(); }
    float getPeakLevel() const { return processor_.getPeakLevel(); }
    float getEstimatedLatencyMs() const { return latencyMs_.load(std::memory_order_relaxed); }
    int32_t getUnderrunCount() const { return underrunCount_.load(std::memory_order_relaxed); }
    int32_t getSampleRate() const { return sampleRate_; }
    void copyWaveform(float* dest, int32_t count) const { processor_.copyWaveform(dest, count); }

    // Offline processing helper for AI sample preview / verification
    void processOfflineBuffer(const float* input, float* output, int32_t numFrames);

    // oboe::AudioStreamDataCallback
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override;

    // oboe::AudioStreamErrorCallback
    void onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result error) override;

private:
    bool openStreams();
    void closeStreams();

    std::shared_ptr<oboe::AudioStream> inputStream_;
    std::shared_ptr<oboe::AudioStream> outputStream_;
    VoiceDspProcessor processor_;

    std::atomic<bool> running_{false};
    std::atomic<float> latencyMs_{0.0f};
    std::atomic<int32_t> underrunCount_{0};
    int32_t sampleRate_ = 48000;
    int32_t framesPerBurst_ = 192;

    std::vector<float> inputScratch_;
};

} // namespace voxmorph
