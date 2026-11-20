#pragma once

#include "DspParameters.h"
#include "BiquadFilter.h"
#include "PitchShifter.h"
#include <array>
#include <atomic>
#include <cstddef>

namespace voxmorph {

class VoiceDspProcessor {
public:
    static constexpr int kWaveformPoints = 64;
    static constexpr int kSpectralBands = 16;

    VoiceDspProcessor();

    void configure(float sampleRate);
    void reset();
    void updateParameters(const DspParameters& params);

    // Real-time lock-free audio block processing (mono float PCM in [-1, 1])
    void processBlock(const float* input, float* output, int32_t numFrames);

    // Telemetry getters (safe to read from UI thread)
    float getInputRms() const { return inputRms_.load(std::memory_order_relaxed); }
    float getOutputRms() const { return outputRms_.load(std::memory_order_relaxed); }
    float getPeakLevel() const { return peakLevel_.load(std::memory_order_relaxed); }
    void copyWaveform(float* dest, int32_t count) const;

private:
    void applyFilterCoefficients(const DspParameters& p);
    float processReverb(float in, float mix);

    float sampleRate_ = 48000.0f;
    AtomicDspParameters atomicParams_;
    DspParameters activeParams_;
    bool coeffsDirty_ = true;

    // 1. Pre-cleaning
    BiquadFilter dcBlocker_;
    float noiseGateEnv_ = 0.0f;
    float noiseGateGain_ = 1.0f;

    // 2. Pitch Shifter
    PitchShifter pitchShifter_;

    // 3. Formant & Timbre shaping
    BiquadFilter formant1_;
    BiquadFilter formant2_;
    BiquadFilter formant3_;
    BiquadFilter depthFilter_;
    BiquadFilter brightnessFilter_;
    std::array<BiquadFilter, kSpectralBands> spectralBank_;

    // 4. EQ & Vocal Filter
    BiquadFilter bassEq_;
    BiquadFilter trebleEq_;
    BiquadFilter vocalHp_;
    BiquadFilter vocalLp_;
    BiquadFilter vocalPeak_;

    // 5. Modulation (Robot / Alien / Cyborg)
    float modPhase_ = 0.0f;
    float lfoPhase_ = 0.0f;

    // 6. Compressor
    float compEnv_ = 0.0f;

    // 7. Compact Schroeder Reverb / Echo
    static constexpr int kComb1Len = 1116;
    static constexpr int kComb2Len = 1356;
    static constexpr int kComb3Len = 1557;
    std::array<float, kComb1Len> comb1_{};
    std::array<float, kComb2Len> comb2_{};
    std::array<float, kComb3Len> comb3_{};
    int comb1Idx_ = 0, comb2Idx_ = 0, comb3Idx_ = 0;

    // Telemetry
    std::atomic<float> inputRms_{0.0f};
    std::atomic<float> outputRms_{0.0f};
    std::atomic<float> peakLevel_{0.0f};
    std::array<std::atomic<float>, kWaveformPoints> waveform_{};
    int waveformCursor_ = 0;

    static constexpr std::array<float, kSpectralBands> kBarkCenterFreqs = {
        150.0f, 250.0f, 350.0f, 450.0f, 570.0f, 700.0f, 840.0f, 1000.0f,
        1170.0f, 1370.0f, 1600.0f, 1850.0f, 2150.0f, 2500.0f, 3150.0f, 4400.0f
    };
};

} // namespace voxmorph
