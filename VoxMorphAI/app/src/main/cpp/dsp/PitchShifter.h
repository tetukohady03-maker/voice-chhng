#pragma once

#include <array>
#include <cmath>
#include <algorithm>
#include <cstdint>

namespace voxmorph {

/**
 * Real-time low-latency dual-tap overlap-add (Hann-windowed) pitch shifter.
 * Zero dynamic heap allocation in audio callback; supports -12 to +12 semitones.
 */
class PitchShifter {
public:
    static constexpr int kBufferSize = 8192; // Power of 2 (>170 ms at 48kHz)
    static constexpr int kBufferMask = kBufferSize - 1;

    void init(float sampleRate) {
        sampleRate_ = sampleRate;
        // ~24ms window gives clean speech pitch shifting with low algorithmic delay
        windowSize_ = std::clamp(static_cast<float>(sampleRate * 0.024f), 256.0f, 2048.0f);
        reset();
    }

    void reset() {
        buffer_.fill(0.0f);
        writePos_ = 0;
        phase_ = 0.0f;
    }

    inline float process(float input, float semitones) {
        buffer_[writePos_] = input;

        if (std::fabs(semitones) < 0.02f) {
            writePos_ = (writePos_ + 1) & kBufferMask;
            return input;
        }

        float ratio = std::pow(2.0f, std::clamp(semitones, -12.0f, 12.0f) / 12.0f);
        float phaseStep = (1.0f - ratio) / windowSize_;

        phase_ += phaseStep;
        if (phase_ >= 1.0f) phase_ -= 1.0f;
        if (phase_ < 0.0f) phase_ += 1.0f;

        float phaseA = phase_;
        float phaseB = phase_ + 0.5f;
        if (phaseB >= 1.0f) phaseB -= 1.0f;

        float delayA = phaseA * windowSize_ + 16.0f;
        float delayB = phaseB * windowSize_ + 16.0f;

        // Raised-cosine (Hann) crossfade windows sum to 1.0 at 0.5 phase offset
        float winA = 0.5f * (1.0f - std::cos(2.0f * kPi * phaseA));
        float winB = 0.5f * (1.0f - std::cos(2.0f * kPi * phaseB));

        float sampleA = readLinear(delayA);
        float sampleB = readLinear(delayB);

        writePos_ = (writePos_ + 1) & kBufferMask;
        return sampleA * winA + sampleB * winB;
    }

private:
    inline float readLinear(float delaySamples) const {
        float readPos = static_cast<float>(writePos_) - delaySamples;
        while (readPos < 0.0f) readPos += static_cast<float>(kBufferSize);
        int idx0 = static_cast<int>(readPos) & kBufferMask;
        int idx1 = (idx0 + 1) & kBufferMask;
        float frac = readPos - std::floor(readPos);
        return buffer_[idx0] * (1.0f - frac) + buffer_[idx1] * frac;
    }

    static constexpr float kPi = 3.14159265358979323846f;
    float sampleRate_ = 48000.0f;
    float windowSize_ = 1152.0f;
    std::array<float, kBufferSize> buffer_{};
    int writePos_ = 0;
    float phase_ = 0.0f;
};

} // namespace voxmorph
