#include <jni.h>
#include <memory>
#include "engine/OboeAudioEngine.h"

namespace {
std::unique_ptr<voxmorph::OboeAudioEngine> gEngine;

voxmorph::OboeAudioEngine* getEngine() {
    if (!gEngine) {
        gEngine = std::make_unique<voxmorph::OboeAudioEngine>();
    }
    return gEngine.get();
}
} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeStart(
    JNIEnv* /*env*/, jobject /*thiz*/, jint sampleRate, jint framesPerBurst) {
    return getEngine()->start(sampleRate, framesPerBurst) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeStop(JNIEnv* /*env*/, jobject /*thiz*/) {
    if (gEngine) gEngine->stop();
}

JNIEXPORT jboolean JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeIsRunning(JNIEnv* /*env*/, jobject /*thiz*/) {
    return (gEngine && gEngine->isRunning()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeUpdateParams(
    JNIEnv* env, jobject /*thiz*/,
    jfloat pitchSemitones, jfloat formantShift, jfloat bassDb, jfloat trebleDb,
    jfloat resonance, jfloat brightness, jfloat voiceDepth, jfloat modulationIntensity,
    jfloat noiseSuppression, jfloat reverbMix, jfloat distortionDrive, jfloat robotModMix,
    jfloat monitoringVolume, jint vocalFilterMode, jint processingMode,
    jboolean muted, jboolean bypassed, jboolean spectralEnabled, jfloatArray spectralBands) {

    voxmorph::DspParameters p;
    p.pitchSemitones = pitchSemitones;
    p.formantShift = formantShift;
    p.bassDb = bassDb;
    p.trebleDb = trebleDb;
    p.resonance = resonance;
    p.brightness = brightness;
    p.voiceDepth = voiceDepth;
    p.modulationIntensity = modulationIntensity;
    p.noiseSuppression = noiseSuppression;
    p.reverbMix = reverbMix;
    p.distortionDrive = distortionDrive;
    p.robotModMix = robotModMix;
    p.monitoringVolume = monitoringVolume;
    p.vocalFilterMode = vocalFilterMode;
    p.processingMode = processingMode;
    p.muted = (muted == JNI_TRUE);
    p.bypassed = (bypassed == JNI_TRUE);
    p.spectralProfileEnabled = (spectralEnabled == JNI_TRUE);

    if (spectralBands != nullptr) {
        jsize len = env->GetArrayLength(spectralBands);
        jfloat* elems = env->GetFloatArrayElements(spectralBands, nullptr);
        if (elems != nullptr) {
            int copyCount = std::min(static_cast<int>(len), 16);
            for (int i = 0; i < copyCount; ++i) {
                p.spectralEnvelopeDb[i] = elems[i];
            }
            env->ReleaseFloatArrayElements(spectralBands, elems, JNI_ABORT);
        }
    }

    getEngine()->updateParameters(p);
}

JNIEXPORT jfloat JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeGetInputRms(JNIEnv*, jobject) {
    return gEngine ? gEngine->getInputRms() : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeGetOutputRms(JNIEnv*, jobject) {
    return gEngine ? gEngine->getOutputRms() : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeGetLatencyMs(JNIEnv*, jobject) {
    return gEngine ? gEngine->getEstimatedLatencyMs() : 0.0f;
}

JNIEXPORT jint JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeGetUnderrunCount(JNIEnv*, jobject) {
    return gEngine ? gEngine->getUnderrunCount() : 0;
}

JNIEXPORT void JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeGetWaveform(JNIEnv* env, jobject, jfloatArray outArr) {
    if (!gEngine || outArr == nullptr) return;
    jsize len = env->GetArrayLength(outArr);
    jfloat* elems = env->GetFloatArrayElements(outArr, nullptr);
    if (elems != nullptr) {
        gEngine->copyWaveform(elems, static_cast<int32_t>(len));
        env->ReleaseFloatArrayElements(outArr, elems, 0);
    }
}

JNIEXPORT jfloatArray JNICALL
Java_com_voxmorph_ai_audio_NativeAudioBridge_nativeProcessOffline(JNIEnv* env, jobject, jfloatArray inputArr) {
    if (inputArr == nullptr) return nullptr;
    jsize len = env->GetArrayLength(inputArr);
    jfloat* inElems = env->GetFloatArrayElements(inputArr, nullptr);
    jfloatArray outArr = env->NewFloatArray(len);
    jfloat* outElems = env->GetFloatArrayElements(outArr, nullptr);

    if (inElems && outElems) {
        getEngine()->processOfflineBuffer(inElems, outElems, static_cast<int32_t>(len));
    }
    if (inElems) env->ReleaseFloatArrayElements(inputArr, inElems, JNI_ABORT);
    if (outElems) env->ReleaseFloatArrayElements(outArr, outElems, 0);
    return outArr;
}

} // extern "C"
