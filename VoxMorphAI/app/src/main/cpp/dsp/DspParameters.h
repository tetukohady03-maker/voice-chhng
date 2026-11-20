#pragma once

#include <array>
#include <atomic>
#include <cstdint>

namespace voxmorph {

enum class VocalFilterMode : int32_t {
    NONE = 0,
    TELEPHONE = 1,
    RADIO = 2,
    WALKIE_TALKIE = 3,
    MEGAPHONE = 4
};

enum class ProcessingMode : int32_t {
    PERFORMANCE = 0, // Lowest latency, lighter reverb/envelope
    BALANCED = 1,    // Default full DSP chain
    QUALITY = 2      // High-precision formant & spectral matching
};

struct DspParameters {
    float pitchSemitones = 0.0f;       // -12.0 .. +12.0
    float formantShift = 0.0f;         // -1.0 .. +1.0
    float bassDb = 0.0f;               // -12.0 .. +12.0
    float trebleDb = 0.0f;             // -12.0 .. +12.0
    float resonance = 0.0f;            // 0.0 .. 1.0
    float brightness = 0.0f;           // -1.0 .. +1.0
    float voiceDepth = 0.0f;           // -1.0 .. +1.0
    float modulationIntensity = 0.0f;  // 0.0 .. 1.0
    float noiseSuppression = 0.25f;    // 0.0 .. 1.0
    float reverbMix = 0.0f;            // 0.0 .. 1.0
    float distortionDrive = 0.0f;      // 0.0 .. 1.0
    float robotModMix = 0.0f;          // 0.0 .. 1.0
    float monitoringVolume = 0.85f;    // 0.0 .. 1.5
    int32_t vocalFilterMode = 0;       // VocalFilterMode
    int32_t processingMode = 1;        // ProcessingMode
    bool muted = false;
    bool bypassed = false;
    bool spectralProfileEnabled = false;
    std::array<float, 16> spectralEnvelopeDb{}; // 16-band Bark-spaced gains (-12..+12 dB)
};

// Lock-free atomic snapshot wrapper for real-time audio thread safety
class AtomicDspParameters {
public:
    void set(const DspParameters& params) {
        int nextIdx = 1 - activeIndex_.load(std::memory_order_relaxed);
        buffers_[nextIdx] = params;
        activeIndex_.store(nextIdx, std::memory_order_release);
    }

    DspParameters get() const {
        int idx = activeIndex_.load(std::memory_order_acquire);
        return buffers_[idx];
    }

private:
    DspParameters buffers_[2]{};
    std::atomic<int> activeIndex_{0};
};

} // namespace voxmorph
