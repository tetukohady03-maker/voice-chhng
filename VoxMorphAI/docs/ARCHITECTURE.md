# FASE 2 — Arsitektur Teknis Modular VOXMORPH AI

## Lapisan Modul
1. **UI Layer (Jetpack Compose — `ui/`)**:
   - Single-activity Compose (`MainActivity`) dengan 7 layar penuh (`Splash`, `Dashboard`, `Voice Studio`, `AI Voice Import`, `Voice Library`, `Floating Settings`, `Settings`) dan `VoxMorphViewModel` berbasis `StateFlow`.
2. **Audio & Native DSP Layer (`audio/` + `cpp/`)**:
   - `OboeAudioEngine` mengelola stream full-duplex AAudio/Oboe dan menangani perpindahan perangkat (headphone/Bluetooth) lewat `onErrorAfterClose`.
   - `AtomicDspParameters` menggunakan double-buffered atomic swap sehingga perubahan slider di UI terdengar seketika tanpa mutex/blocking di audio callback.
   - `VoiceDspProcessor` menjalankan rantai 10 tahap: DC Blocker + Noise Gate → Dual-Tap Hann Pitch Shifter (-12..+12 st) → 3-Formant + Depth + Brightness → 16-Band Spectral Envelope Bank → Bass/Treble EQ → Vocal Filter (Telephone/Radio/Walkie-Talkie/Megaphone) → Ring/AM Robot Modulation → Soft-Clip Distortion → Vocal Compressor → Schroeder Reverb/Echo → Soft Limiter (`tanh`, anti-clipping).
3. **AI Voice Import Layer (`ai/`)**:
   - `AudioDecoder` (MediaExtractor + MediaCodec untuk WAV/MP3/M4A/AAC).
   - `VoiceProfileAnalyzer` mengekstrak F0 median/spread, SNR, 16-band spectral envelope, skor kualitas (0–100), dan rekomendasi sampel tambahan.
4. **Persistence Layer (`data/`)**:
   - Room Database (`VoxMorphDatabase`, `VoicePresetDao`) menyimpan 20 preset bawaan dan seluruh profil kustom/AI dengan dukungan Rename, Duplicate, Delete, Favorite, dan Reset Default.
5. **Background & Floating Layer (`service/` + `overlay/`)**:
   - `VoiceChangerForegroundService` dengan tipe `microphone` dan notifikasi transparan permanen saat mikrofon aktif.
   - `FloatingVoiceControlManager` menggunakan `WindowManager` (`TYPE_APPLICATION_OVERLAY`) yang dapat digeser, diminimalkan, diperluas, diatur transparansinya, dan disembunyikan sementara.
