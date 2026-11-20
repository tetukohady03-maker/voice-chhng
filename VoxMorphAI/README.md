# VOXMORPH AI — Real-Time Android Voice Changer

Proyek Android Studio lengkap (**Kotlin + Jetpack Compose + C++ Oboe/AAudio DSP + Room + ONNX Runtime Mobile + Floating Overlay**) untuk pemrosesan suara real-time dan ekstraksi profil suara lokal.

## Struktur Proyek
- `app/src/main/cpp/dsp/` — Mesin DSP C++17 real-time tanpa alokasi heap di audio callback (`VoiceDspProcessor`, `PitchShifter`, `BiquadFilter`, `DspParameters`).
- `app/src/main/cpp/engine/` — Full-duplex low-latency `OboeAudioEngine` (AAudio/OpenSL ES via [Google Oboe](https://github.com/google/oboe)).
- `app/src/main/cpp/jni/` — Bridge JNI (`VoxMorphJni.cpp`) dengan pembaruan parameter atomik lock-free.
- `app/src/main/cpp/tests/` — Native C++ DSP verification & benchmark harness (`dsp_benchmark_test.cpp`).
- `app/src/main/java/com/voxmorph/ai/ai/` — Dekoder lokal WAV/MP3/M4A/AAC (`AudioDecoder`) dan ekstraktor profil suara lokal (`VoiceProfileAnalyzer` + slot [ONNX Runtime Android](https://mvnrepository.com/artifact/com.microsoft.onnxruntime/onnxruntime-android)).
- `app/src/main/java/com/voxmorph/ai/data/` — Room Database (`VoxMorphDatabase`), `VoicePresetDao`, 20 preset bawaan (`DefaultPresets`), dan `UserSettingsRepository`.
- `app/src/main/java/com/voxmorph/ai/service/` — `VoiceChangerForegroundService` (`FOREGROUND_SERVICE_MICROPHONE`).
- `app/src/main/java/com/voxmorph/ai/overlay/` — `FloatingVoiceControlManager` (`SYSTEM_ALERT_WINDOW`).
- `app/src/main/java/com/voxmorph/ai/ui/` — 7 layar Jetpack Compose bertema Midnight Black + Electric Purple + Neon Blue (`Splash`, `Dashboard`, `Voice Studio`, `AI Voice Import`, `Voice Library`, `Floating Settings`, `Settings`).
- `docs/` — Feasibility Audit, Arsitektur Teknis, Matriks Kompatibilitas, dan Laporan Pengujian QA.

## Cara Build APK (Debug & Release)
1. Buka folder `VoxMorphAI` di **Android Studio Ladybug / Meerkat** atau lebih baru (JDK 17, Android SDK 35, Android NDK r26+ & CMake 3.22.1).
2. Tunggu Gradle Sync selesai (mengunduh Jetpack Compose, Room, Oboe Prefab, dan ONNX Runtime Android).
3. Jalankan build APK Debug:
   ```bash
   ./gradlew assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`
4. Untuk APK Release (setelah menambahkan konfigurasi `signingConfigs` keystore Anda di `app/build.gradle.kts`):
   ```bash
   ./gradlew assembleRelease
   ```
5. (Opsional) Untuk menjalankan benchmark C++ DSP langsung di desktop Linux/macOS:
   ```bash
   g++ -std=c++17 -O3 -ffast-math app/src/main/cpp/dsp/VoiceDspProcessor.cpp app/src/main/cpp/tests/dsp_benchmark_test.cpp -o dsp_test -lm && ./dsp_test
   ```

## Keterbatasan Jujur (Non-Root Android)
- **Routing Mikrofon Lintas Aplikasi**: Android standar tanpa root tidak mengizinkan aplikasi pihak ketiga menyuntikkan mikrofon virtual ke aplikasi lain seperti Mobile Legends, WhatsApp, atau Discord ([Android Developers — Sharing Audio Input](https://developer.android.com/media/platform/sharing-audio-input)). Lihat `docs/FEASIBILITY_AUDIT.md` dan `docs/COMPATIBILITY_MATRIX.md` untuk jalur resmi (hardware loopback non-root vs privileged/system HAL).
- **Build APK di Sandbox**: Lingkungan sandbox tempat proyek ini dibuat tidak memiliki Android SDK/NDK/Gradle atau internet keluar, sehingga APK belum dikompilasi di sini; mesin C++ DSP sudah dikompilasi dan diuji nyata menggunakan `g++ -O3`.
