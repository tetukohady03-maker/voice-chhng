#include "VoiceDspProcessor.h"
#include <cmath>
#include <algorithm>

namespace voxmorph {

namespace {
constexpr float kPi = 3.14159265358979323846f;

inline float softLimit(float x) {
    // Smooth tanh-based limiter guaranteeing output strictly within [-0.98, 0.98]
    if (!std::isfinite(x)) return 0.0f;
    return 0.98f * std::tanh(x / 0.98f);
}

inline bool paramsChanged(const DspParameters& a, const DspParameters& b) {
    if (std::fabs(a.formantShift - b.formantShift) > 1e-4f ||
        std::fabs(a.bassDb - b.bassDb) > 1e-4f ||
        std::fabs(a.trebleDb - b.trebleDb) > 1e-4f ||
        std::fabs(a.resonance - b.resonance) > 1e-4f ||
        std::fabs(a.brightness - b.brightness) > 1e-4f ||
        std::fabs(a.voiceDepth - b.voiceDepth) > 1e-4f ||
        a.vocalFilterMode != b.vocalFilterMode ||
        a.spectralProfileEnabled != b.spectralProfileEnabled) {
        return true;
    }
    if (a.spectralProfileEnabled) {
        for (int i = 0; i < VoiceDspProcessor::kSpectralBands; ++i) {
            if (std::fabs(a.spectralEnvelopeDb[i] - b.spectralEnvelopeDb[i]) > 1e-4f) {
                return true;
            }
        }
    }
    return false;
}
} // namespace

VoiceDspProcessor::VoiceDspProcessor() {
    configure(48000.0f);
}

void VoiceDspProcessor::configure(float sampleRate) {
    sampleRate_ = std::clamp(sampleRate, 16000.0f, 96000.0f);
    pitchShifter_.init(sampleRate_);
    dcBlocker_.setHighPass(sampleRate_, 55.0f, 0.7071f);
    reset();
    coeffsDirty_ = true;
    applyFilterCoefficients(activeParams_);
}

void VoiceDspProcessor::reset() {
    dcBlocker_.reset();
    pitchShifter_.reset();
    formant1_.reset();
    formant2_.reset();
    formant3_.reset();
    depthFilter_.reset();
    brightnessFilter_.reset();
    for (auto& f : spectralBank_) f.reset();
    bassEq_.reset();
    trebleEq_.reset();
    vocalHp_.reset();
    vocalLp_.reset();
    vocalPeak_.reset();
    comb1_.fill(0.0f);
    comb2_.fill(0.0f);
    comb3_.fill(0.0f);
    comb1Idx_ = comb2Idx_ = comb3Idx_ = 0;
    noiseGateEnv_ = 0.0f;
    noiseGateGain_ = 1.0f;
    compEnv_ = 0.0f;
    modPhase_ = 0.0f;
    lfoPhase_ = 0.0f;
}

void VoiceDspProcessor::updateParameters(const DspParameters& params) {
    atomicParams_.set(params);
}

void VoiceDspProcessor::applyFilterCoefficients(const DspParameters& p) {
    // Formant shifting proxy via 3 shifted vocal tract resonances (F1, F2, F3)
    float fShift = std::clamp(p.formantShift, -1.0f, 1.0f);
    float fScale = std::pow(2.0f, fShift * 0.45f); // ~0.73x to 1.37x vocal tract length scaling
    float resQ = 1.6f + std::clamp(p.resonance, 0.0f, 1.0f) * 3.0f;
    float formantGain = (std::fabs(fShift) * 6.5f) + (p.resonance * 4.5f);

    if (formantGain > 0.1f) {
        formant1_.setPeaking(sampleRate_, 550.0f * fScale, resQ, formantGain * 0.75f);
        formant2_.setPeaking(sampleRate_, 1550.0f * fScale, resQ * 1.1f, formantGain);
        formant3_.setPeaking(sampleRate_, 2650.0f * fScale, resQ * 1.2f, formantGain * 0.65f);
    } else {
        formant1_.setBypass();
        formant2_.setBypass();
        formant3_.setBypass();
    }

    // Voice Depth (chest resonance 130-220 Hz) & Brightness (presence 3.5-5.5 kHz)
    depthFilter_.setPeaking(sampleRate_, 175.0f, 1.1f, std::clamp(p.voiceDepth, -1.0f, 1.0f) * 9.0f);
    brightnessFilter_.setPeaking(sampleRate_, 3800.0f, 1.0f, std::clamp(p.brightness, -1.0f, 1.0f) * 8.0f);

    // Bass & Treble EQ
    bassEq_.setLowShelf(sampleRate_, 220.0f, std::clamp(p.bassDb, -12.0f, 12.0f));
    trebleEq_.setHighShelf(sampleRate_, 3200.0f, std::clamp(p.trebleDb, -12.0f, 12.0f));

    // 16-band Spectral Envelope Matching for Imported Custom AI Voice Profiles
    if (p.spectralProfileEnabled) {
        for (int i = 0; i < kSpectralBands; ++i) {
            float g = std::clamp(p.spectralEnvelopeDb[i], -10.0f, 10.0f);
            spectralBank_[i].setPeaking(sampleRate_, kBarkCenterFreqs[i], 2.2f, g);
        }
    } else {
        for (auto& f : spectralBank_) f.setBypass();
    }

    // Vocal Filter Presets (Telephone, Radio, Walkie-Talkie, Megaphone)
    switch (static_cast<VocalFilterMode>(p.vocalFilterMode)) {
        case VocalFilterMode::TELEPHONE:
            vocalHp_.setHighPass(sampleRate_, 340.0f, 0.9f);
            vocalLp_.setLowPass(sampleRate_, 3300.0f, 0.9f);
            vocalPeak_.setPeaking(sampleRate_, 1400.0f, 1.4f, 4.0f);
            break;
        case VocalFilterMode::RADIO:
            vocalHp_.setHighPass(sampleRate_, 420.0f, 1.1f);
            vocalLp_.setLowPass(sampleRate_, 2800.0f, 1.1f);
            vocalPeak_.setPeaking(sampleRate_, 1800.0f, 2.0f, 6.0f);
            break;
        case VocalFilterMode::WALKIE_TALKIE:
            vocalHp_.setHighPass(sampleRate_, 520.0f, 1.4f);
            vocalLp_.setLowPass(sampleRate_, 2300.0f, 1.4f);
            vocalPeak_.setPeaking(sampleRate_, 1650.0f, 2.5f, 7.5f);
            break;
        case VocalFilterMode::MEGAPHONE:
            vocalHp_.setHighPass(sampleRate_, 600.0f, 1.2f);
            vocalLp_.setLowPass(sampleRate_, 3600.0f, 1.0f);
            vocalPeak_.setPeaking(sampleRate_, 2100.0f, 2.2f, 9.0f);
            break;
        case VocalFilterMode::NONE:
        default:
            vocalHp_.setBypass();
            vocalLp_.setBypass();
            vocalPeak_.setBypass();
            break;
    }

    coeffsDirty_ = false;
}

float VoiceDspProcessor::processReverb(float in, float mix) {
    if (mix <= 0.005f) return in;
    float feedback = 0.52f + mix * 0.25f;

    float y1 = comb1_[comb1Idx_];
    comb1_[comb1Idx_] = in + y1 * feedback;
    comb1Idx_ = (comb1Idx_ + 1) % kComb1Len;

    float y2 = comb2_[comb2Idx_];
    comb2_[comb2Idx_] = in + y2 * (feedback * 0.95f);
    comb2Idx_ = (comb2Idx_ + 1) % kComb2Len;

    float y3 = comb3_[comb3Idx_];
    comb3_[comb3Idx_] = in + y3 * (feedback * 0.90f);
    comb3Idx_ = (comb3Idx_ + 1) % kComb3Len;

    float wet = (y1 + y2 + y3) * 0.3333f;
    return in * (1.0f - mix * 0.5f) + wet * mix;
}

void VoiceDspProcessor::processBlock(const float* input, float* output, int32_t numFrames) {
    if (!input || !output || numFrames <= 0) return;

    DspParameters latest = atomicParams_.get();
    if (coeffsDirty_ || paramsChanged(latest, activeParams_)) {
        activeParams_ = latest;
        applyFilterCoefficients(activeParams_);
    } else {
        activeParams_ = latest;
    }

    const bool isPerfMode = (activeParams_.processingMode == static_cast<int32_t>(ProcessingMode::PERFORMANCE));
    const float gateThreshold = 0.002f + std::clamp(activeParams_.noiseSuppression, 0.0f, 1.0f) * 0.035f;
    const float modFreqHz = 45.0f + activeParams_.modulationIntensity * 95.0f;
    const float modInc = 2.0f * kPi * modFreqHz / sampleRate_;
    const float lfoInc = 2.0f * kPi * (2.5f + activeParams_.modulationIntensity * 5.0f) / sampleRate_;

    double sumInSq = 0.0;
    double sumOutSq = 0.0;
    float maxPeak = 0.0f;

    for (int32_t i = 0; i < numFrames; ++i) {
        float x = input[i];
        if (!std::isfinite(x)) x = 0.0f;
        sumInSq += static_cast<double>(x) * static_cast<double>(x);

        if (activeParams_.muted) {
            output[i] = 0.0f;
            continue;
        }

        if (activeParams_.bypassed) {
            float y = softLimit(x * activeParams_.monitoringVolume);
            output[i] = y;
            sumOutSq += static_cast<double>(y) * static_cast<double>(y);
            maxPeak = std::max(maxPeak, std::fabs(y));
            continue;
        }

        // 1. DC Blocker & Adaptive Noise Gate / Suppression
        float s = dcBlocker_.process(x);
        float absS = std::fabs(s);
        noiseGateEnv_ = 0.992f * noiseGateEnv_ + 0.008f * absS;
        float targetGate = (noiseGateEnv_ >= gateThreshold || activeParams_.noiseSuppression < 0.02f)
                               ? 1.0f
                               : (noiseGateEnv_ / (gateThreshold + 1e-6f));
        targetGate = targetGate * targetGate; // Smooth quadratic attenuation
        noiseGateGain_ = 0.985f * noiseGateGain_ + 0.015f * targetGate;
        s *= noiseGateGain_;

        // 2. Real-Time Pitch Shifting (-12 to +12 semitones)
        s = pitchShifter_.process(s, activeParams_.pitchSemitones);

        // 3. Formant, Depth, Brightness & Timbre Shaping
        s = formant1_.process(s);
        s = formant2_.process(s);
        s = formant3_.process(s);
        s = depthFilter_.process(s);
        s = brightnessFilter_.process(s);

        // 4. Custom AI Voice Profile 16-Band Spectral Envelope Matching
        if (activeParams_.spectralProfileEnabled) {
            int step = isPerfMode ? 2 : 1; // Half-band optimization in Performance Mode
            for (int b = 0; b < kSpectralBands; b += step) {
                s = spectralBank_[b].process(s);
            }
        }

        // 5. Bass / Treble EQ & Vocal Filter
        s = bassEq_.process(s);
        s = trebleEq_.process(s);
        if (activeParams_.vocalFilterMode != static_cast<int32_t>(VocalFilterMode::NONE)) {
            s = vocalHp_.process(s);
            s = vocalLp_.process(s);
            s = vocalPeak_.process(s);
        }

        // 6. Robot / Ring Modulation & Vibrato/Alien Modulation
        if (activeParams_.robotModMix > 0.01f || activeParams_.modulationIntensity > 0.01f) {
            float carrier = std::sin(modPhase_);
            modPhase_ += modInc;
            if (modPhase_ > 2.0f * kPi) modPhase_ -= 2.0f * kPi;

            float lfo = 0.5f + 0.5f * std::sin(lfoPhase_);
            lfoPhase_ += lfoInc;
            if (lfoPhase_ > 2.0f * kPi) lfoPhase_ -= 2.0f * kPi;

            if (activeParams_.robotModMix > 0.01f) {
                float ring = s * carrier;
                s = s * (1.0f - activeParams_.robotModMix) + ring * activeParams_.robotModMix;
            }
            if (activeParams_.modulationIntensity > 0.01f) {
                float trem = s * (1.0f - activeParams_.modulationIntensity * 0.45f * lfo);
                s = trem;
            }
        }

        // 7. Distortion / Overdrive (Demon, Monster, Megaphone)
        if (activeParams_.distortionDrive > 0.01f) {
            float drive = 1.0f + activeParams_.distortionDrive * 9.0f;
            float driven = std::tanh(s * drive) / std::tanh(drive);
            s = s * (1.0f - activeParams_.distortionDrive) + driven * activeParams_.distortionDrive;
        }

        // 8. Dynamic Range Compressor & Voice Enhancement
        float absPost = std::fabs(s);
        compEnv_ = (absPost > compEnv_) ? (0.95f * compEnv_ + 0.05f * absPost)
                                        : (0.998f * compEnv_ + 0.002f * absPost);
        constexpr float compThresh = 0.28f;
        float compGain = 1.0f;
        if (compEnv_ > compThresh) {
            float excess = compEnv_ / compThresh;
            compGain = std::pow(excess, -0.55f); // ~2.2:1 vocal compression
        }
        s *= compGain * 1.15f; // Gentle makeup gain

        // 9. Reverb / Echo
        if (!isPerfMode && activeParams_.reverbMix > 0.01f) {
            s = processReverb(s, activeParams_.reverbMix);
        }

        // 10. Output Volume & Anti-Clipping Soft Limiter
        float y = softLimit(s * activeParams_.monitoringVolume);
        output[i] = y;

        sumOutSq += static_cast<double>(y) * static_cast<double>(y);
        maxPeak = std::max(maxPeak, std::fabs(y));
    }

    float inRms = static_cast<float>(std::sqrt(sumInSq / static_cast<double>(numFrames)));
    float outRms = static_cast<float>(std::sqrt(sumOutSq / static_cast<double>(numFrames)));
    inputRms_.store(inRms, std::memory_order_relaxed);
    outputRms_.store(outRms, std::memory_order_relaxed);
    peakLevel_.store(maxPeak, std::memory_order_relaxed);

    // Update lock-free UI waveform ring buffer (1 sample per block)
    waveform_[waveformCursor_].store(std::clamp(outRms * 3.0f, 0.0f, 1.0f), std::memory_order_relaxed);
    waveformCursor_ = (waveformCursor_ + 1) % kWaveformPoints;
}

void VoiceDspProcessor::copyWaveform(float* dest, int32_t count) const {
    int32_t n = std::min(count, kWaveformPoints);
    int start = waveformCursor_;
    for (int32_t i = 0; i < n; ++i) {
        dest[i] = waveform_[(start + i) % kWaveformPoints].load(std::memory_order_relaxed);
    }
}

} // namespace voxmorph
