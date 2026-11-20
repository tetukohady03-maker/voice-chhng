# FASE 1 — Android Feasibility Audit (VOXMORPH AI)

## 1. Pemrosesan Audio Real-Time (DSP Lokal)
- **Status**: **LAYAK PENUH (Non-Root Android 8.0+ / API 26+)**.
- **Mekanisme**: Full-duplex C++ menggunakan [Google Oboe](https://github.com/google/oboe) (AAudio Exclusive/Shared LowLatency) dengan `AudioRecord`/`InputPreset::VoiceCommunication` dan `AudioTrack`/`Usage::VoiceCommunication`.
- **Target Latensi**: Pipeline DSP C++ lock-free membutuhkan ~10–21 µs per blok 192 frame (4 ms), sehingga total round-trip latency pada perangkat kelas menengah bergantung pada driver HAL AAudio perangkat (umumnya 18–40 ms dengan headphone kabel, lebih tinggi pada Bluetooth SCO).

## 2. AI Voice Conversion & Custom Voice Profile
- **Status**: **LAYAK DENGAN BATASAN PERANGKAT**.
- **Ekstraksi Profil Lokal**: Dekode WAV/MP3/M4A/AAC (`MediaExtractor` + `MediaCodec`), estimasi distribusi F0 (autokorelasi), ekstraksi 16-band Bark spectral envelope (Goertzel), brightness, depth, dan skor SNR berjalan 100% lokal tanpa server.
- **Konversi Suara Real-Time**:
  - Jalur default (latensi rendah ~18–35 ms): Pitch shifter + 3-formant shift + 16-band spectral envelope matching. Meniru karakter timbre/pitch/formant secara real-time, tetapi **bukan kloning identik 100%**.
  - Jalur neural opsional: Slot [ONNX Runtime Android](https://mvnrepository.com/artifact/com.microsoft.onnxruntime/onnxruntime-android) (`filesDir/models/voice_conversion_quant.onnx`) tersedia untuk model neural VC terkuantisasi (INT8) dengan estimasi latensi 65–120 ms pada perangkat kelas menengah.

## 3. Penggantian Mikrofon Lintas Aplikasi (Mobile Legends, WhatsApp, Discord)
- **Status pada APK Standar Non-Root**: **TIDAK DIDUKUNG SECARA UNIVERSAL OLEH OS ANDROID**.
- **Akar Masalah Teknis**:
  1. Kebijakan [Sharing Audio Input](https://developer.android.com/media/platform/sharing-audio-input) dan [AOSP Concurrent Capture](https://source.android.com/docs/core/audio/concurrent) hanya mengizinkan aplikasi sistem/privileged tertentu untuk menangkap audio secara bersamaan dengan aplikasi reguler. Ketika game/VoIP membuka `VOICE_COMMUNICATION`, aplikasi reguler lain di background akan dibungkam (menerima buffer nol) atau kehilangan akses mikrofon.
  2. Android tidak menyediakan API publik bagi aplikasi non-root untuk mendaftarkan Virtual Microphone HAL yang bisa dipilih oleh aplikasi pihak ketiga.
- **Jalur Teknis yang Benar & Realistis**:
  1. **Non-Root APK Standar (Diimplementasikan)**: Real-time DSP monitoring + Floating Control di atas aplikasi lain + dukungan **Hardware Loopback** (USB Audio Interface / TRRS audio splitter yang menghubungkan output hasil DSP ke input mic perangkat/PC kedua).
  2. **Privileged / System App (`priv-app`) atau Custom ROM**: Memerlukan tanda tangan platform/OEM (`CAPTURE_AUDIO_OUTPUT`, `MODIFY_AUDIO_ROUTING`) dan modul Virtual Audio HAL (`audio_policy_configuration.xml` dynamic mix) agar output `VoiceDspProcessor` diarahkan sebagai sumber input sistem.
  3. **Rooted Device (Opsional, di luar ruang lingkup non-root)**: Hook pada Audio HAL / `AudioFlinger` melalui modul Magisk/LSPosed.
