#pragma once

#include <cmath>
#include <algorithm>

namespace voxmorph {

class BiquadFilter {
public:
    void reset() {
        z1_ = 0.0f;
        z2_ = 0.0f;
    }

    inline float process(float in) {
        float out = in * b0_ + z1_;
        z1_ = in * b1_ + z2_ - a1_ * out;
        z2_ = in * b2_ - a2_ * out;
        // Denormal protection
        if (std::fabs(z1_) < 1e-20f) z1_ = 0.0f;
        if (std::fabs(z2_) < 1e-20f) z2_ = 0.0f;
        return out;
    }

    void setBypass() {
        b0_ = 1.0f; b1_ = 0.0f; b2_ = 0.0f;
        a1_ = 0.0f; a2_ = 0.0f;
    }

    void setHighPass(float sampleRate, float freq, float q = 0.7071f) {
        freq = std::clamp(freq, 20.0f, sampleRate * 0.45f);
        float w0 = 2.0f * kPi * freq / sampleRate;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0_ = ((1.0f + cosW0) * 0.5f) / a0;
        b1_ = (-(1.0f + cosW0)) / a0;
        b2_ = ((1.0f + cosW0) * 0.5f) / a0;
        a1_ = (-2.0f * cosW0) / a0;
        a2_ = (1.0f - alpha) / a0;
    }

    void setLowPass(float sampleRate, float freq, float q = 0.7071f) {
        freq = std::clamp(freq, 20.0f, sampleRate * 0.45f);
        float w0 = 2.0f * kPi * freq / sampleRate;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0_ = ((1.0f - cosW0) * 0.5f) / a0;
        b1_ = (1.0f - cosW0) / a0;
        b2_ = ((1.0f - cosW0) * 0.5f) / a0;
        a1_ = (-2.0f * cosW0) / a0;
        a2_ = (1.0f - alpha) / a0;
    }

    void setBandPass(float sampleRate, float freq, float q = 1.0f) {
        freq = std::clamp(freq, 40.0f, sampleRate * 0.45f);
        float w0 = 2.0f * kPi * freq / sampleRate;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) / (2.0f * q);
        float a0 = 1.0f + alpha;
        b0_ = alpha / a0;
        b1_ = 0.0f;
        b2_ = -alpha / a0;
        a1_ = (-2.0f * cosW0) / a0;
        a2_ = (1.0f - alpha) / a0;
    }

    void setLowShelf(float sampleRate, float freq, float gainDb, float slope = 1.0f) {
        if (std::fabs(gainDb) < 0.05f) { setBypass(); return; }
        freq = std::clamp(freq, 30.0f, sampleRate * 0.45f);
        float A = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * kPi * freq / sampleRate;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) * 0.5f * std::sqrt((A + 1.0f / A) * (1.0f / slope - 1.0f) + 2.0f);
        float twoSqrtAAlpha = 2.0f * std::sqrt(A) * alpha;
        float a0 = (A + 1.0f) + (A - 1.0f) * cosW0 + twoSqrtAAlpha;
        b0_ = (A * ((A + 1.0f) - (A - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0;
        b1_ = (2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosW0)) / a0;
        b2_ = (A * ((A + 1.0f) - (A - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0;
        a1_ = (-2.0f * ((A - 1.0f) + (A + 1.0f) * cosW0)) / a0;
        a2_ = ((A + 1.0f) + (A - 1.0f) * cosW0 - twoSqrtAAlpha) / a0;
    }

    void setHighShelf(float sampleRate, float freq, float gainDb, float slope = 1.0f) {
        if (std::fabs(gainDb) < 0.05f) { setBypass(); return; }
        freq = std::clamp(freq, 100.0f, sampleRate * 0.45f);
        float A = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * kPi * freq / sampleRate;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) * 0.5f * std::sqrt((A + 1.0f / A) * (1.0f / slope - 1.0f) + 2.0f);
        float twoSqrtAAlpha = 2.0f * std::sqrt(A) * alpha;
        float a0 = (A + 1.0f) - (A - 1.0f) * cosW0 + twoSqrtAAlpha;
        b0_ = (A * ((A + 1.0f) + (A - 1.0f) * cosW0 + twoSqrtAAlpha)) / a0;
        b1_ = (-2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosW0)) / a0;
        b2_ = (A * ((A + 1.0f) + (A - 1.0f) * cosW0 - twoSqrtAAlpha)) / a0;
        a1_ = (2.0f * ((A - 1.0f) - (A + 1.0f) * cosW0)) / a0;
        a2_ = ((A + 1.0f) - (A - 1.0f) * cosW0 - twoSqrtAAlpha) / a0;
    }

    void setPeaking(float sampleRate, float freq, float q, float gainDb) {
        if (std::fabs(gainDb) < 0.05f) { setBypass(); return; }
        freq = std::clamp(freq, 40.0f, sampleRate * 0.45f);
        q = std::max(0.2f, q);
        float A = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * kPi * freq / sampleRate;
        float cosW0 = std::cos(w0);
        float alpha = std::sin(w0) / (2.0f * q);
        float a0 = 1.0f + alpha / A;
        b0_ = (1.0f + alpha * A) / a0;
        b1_ = (-2.0f * cosW0) / a0;
        b2_ = (1.0f - alpha * A) / a0;
        a1_ = (-2.0f * cosW0) / a0;
        a2_ = (1.0f - alpha / A) / a0;
    }

private:
    static constexpr float kPi = 3.14159265358979323846f;
    float b0_ = 1.0f, b1_ = 0.0f, b2_ = 0.0f;
    float a1_ = 0.0f, a2_ = 0.0f;
    float z1_ = 0.0f, z2_ = 0.0f;
};

} // namespace voxmorph
