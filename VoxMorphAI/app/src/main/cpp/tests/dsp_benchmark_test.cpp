#include "../dsp/VoiceDspProcessor.h"
#include <iostream>
#include <vector>
#include <cmath>
#include <chrono>
#include <iomanip>
#include <string>
#include <cassert>

using namespace voxmorph;

struct PresetTest {
    std::string name;
    DspParameters params;
};

int main() {
    constexpr float kSampleRate = 48000.0f;
    constexpr int kBlockSize = 192; // 4.0 ms audio burst @ 48kHz
    constexpr int kTotalBlocks = 500; // 2.0 seconds of continuous streaming audio
    constexpr float kPi = 3.14159265358979323846f;

    // Generate synthetic voiced speech-like signal (fundamental 140Hz + harmonics + noise floor)
    std::vector<float> inputSignal(kBlockSize * kTotalBlocks);
    for (size_t i = 0; i < inputSignal.size(); ++i) {
        float t = static_cast<float>(i) / kSampleRate;
        float env = 0.5f + 0.5f * std::sin(2.0f * kPi * 3.0f * t); // syllable modulation
        float voice = 0.35f * std::sin(2.0f * kPi * 140.0f * t)
                    + 0.20f * std::sin(2.0f * kPi * 280.0f * t)
                    + 0.12f * std::sin(2.0f * kPi * 420.0f * t)
                    + 0.08f * std::sin(2.0f * kPi * 1260.0f * t);
        float noise = 0.005f * std::sin(2.0f * kPi * 7919.0f * t);
        inputSignal[i] = env * voice + noise;
    }

    std::vector<PresetTest> tests;
    {
        DspParameters p; p.pitchSemitones = 4.5f; p.formantShift = 0.45f; p.brightness = 0.35f;
        tests.push_back({"Feminine Voice", p});
    }
    {
        DspParameters p; p.pitchSemitones = -4.0f; p.formantShift = -0.35f; p.voiceDepth = 0.55f; p.bassDb = 4.5f;
        tests.push_back({"Deep Voice", p});
    }
    {
        DspParameters p; p.pitchSemitones = 7.0f; p.formantShift = 0.65f; p.brightness = 0.5f;
        tests.push_back({"Anime / Helium", p});
    }
    {
        DspParameters p; p.robotModMix = 0.75f; p.modulationIntensity = 0.5f; p.resonance = 0.6f;
        tests.push_back({"Robot / Cyborg", p});
    }
    {
        DspParameters p; p.pitchSemitones = -7.0f; p.formantShift = -0.6f; p.distortionDrive = 0.55f; p.reverbMix = 0.45f;
        tests.push_back({"Demon / Monster", p});
    }
    {
        DspParameters p; p.vocalFilterMode = static_cast<int32_t>(VocalFilterMode::WALKIE_TALKIE); p.distortionDrive = 0.25f;
        tests.push_back({"Walkie Talkie", p});
    }
    {
        DspParameters p; p.pitchSemitones = 2.5f; p.spectralProfileEnabled = true;
        p.spectralEnvelopeDb = {-2.0f, -1.0f, 0.5f, 2.0f, 3.5f, 2.5f, 1.0f, 0.0f, -1.5f, 1.5f, 3.0f, 2.0f, 1.0f, 0.5f, 2.5f, 1.5f};
        tests.push_back({"Custom AI Profile (16-Band)", p});
    }

    VoiceDspProcessor dsp;
    dsp.configure(kSampleRate);
    std::vector<float> outputBlock(kBlockSize);

    std::cout << "=== VOXMORPH AI NATIVE C++ DSP VERIFICATION & BENCHMARK ===\n";
    std::cout << "Sample Rate: " << kSampleRate << " Hz | Block Size: " << kBlockSize
              << " frames (4.00 ms budget) | Stream Duration: 2.00 s\n\n";
    std::cout << std::left << std::setw(28) << "Preset / Pipeline"
              << std::right << std::setw(12) << "Avg us/blk"
              << std::setw(12) << "Max us/blk"
              << std::setw(11) << "CPU Load"
              << std::setw(10) << "Out Peak"
              << std::setw(10) << "Status" << "\n";
    std::cout << std::string(83, '-') << "\n";

    bool allPassed = true;
    for (const auto& t : tests) {
        dsp.reset();
        dsp.updateParameters(t.params);

        double totalUs = 0.0;
        double maxUs = 0.0;
        float maxPeak = 0.0f;
        int nanCount = 0;
        int clipCount = 0;

        for (int b = 0; b < kTotalBlocks; ++b) {
            const float* inPtr = inputSignal.data() + (b * kBlockSize);
            auto t0 = std::chrono::high_resolution_clock::now();
            dsp.processBlock(inPtr, outputBlock.data(), kBlockSize);
            auto t1 = std::chrono::high_resolution_clock::now();

            double us = std::chrono::duration<double, std::micro>(t1 - t0).count();
            totalUs += us;
            if (us > maxUs) maxUs = us;

            for (float s : outputBlock) {
                if (!std::isfinite(s)) nanCount++;
                float a = std::fabs(s);
                if (a > maxPeak) maxPeak = a;
                if (a > 0.99f) clipCount++;
            }
        }

        double avgUs = totalUs / kTotalBlocks;
        double cpuPct = (avgUs / 4000.0) * 100.0; // 4000 us = 4ms realtime budget
        bool passed = (nanCount == 0 && clipCount == 0 && maxPeak > 0.01f && avgUs < 2000.0);
        if (!passed) allPassed = false;

        std::cout << std::left << std::setw(28) << t.name
                  << std::right << std::fixed << std::setprecision(1)
                  << std::setw(12) << avgUs
                  << std::setw(12) << maxUs
                  << std::setw(10) << cpuPct << "%"
                  << std::setprecision(3) << std::setw(10) << maxPeak
                  << std::setw(10) << (passed ? "PASS" : "FAIL") << "\n";
    }

    // Verify Mute produces exact silence
    DspParameters muteP;
    muteP.muted = true;
    dsp.updateParameters(muteP);
    dsp.processBlock(inputSignal.data(), outputBlock.data(), kBlockSize);
    float muteMax = 0.0f;
    for (float s : outputBlock) muteMax = std::max(muteMax, std::fabs(s));
    assert(muteMax == 0.0f);
    std::cout << "\nMute Isolation Test: PASS (max output = " << muteMax << ")\n";
    std::cout << "Overall Native DSP Suite: " << (allPassed ? "ALL TESTS PASSED" : "FAILED") << "\n";
    return allPassed ? 0 : 1;
}
