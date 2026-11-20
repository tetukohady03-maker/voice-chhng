# FASE 8 — Laporan Pengujian & Benchmark QA

## 1. Pengujian Native C++ DSP yang Sudah Dijalankan Secara Nyata
Dikompilasi dengan `g++ -std=c++17 -O3 -ffast-math -Wall -Wextra` (0 warning, 0 error) dan dijalankan pada sinyal bicara harmonik sintetis 48 kHz, block size 192 frame (anggaran real-time 4.00 ms), durasi streaming 2.00 detik (500 blok beruntun):

| Pipeline / Preset | Avg µs/blok | Max µs/blok | Estimasi CPU Load (vs 4 ms) | Output Peak | NaN / Clipping | Status |
| :--- | ---: | ---: | ---: | ---: | ---: | :--- |
| Feminine Voice | 12.1 µs | 44.1 µs | 0.3% | 0.440 | 0 / 0 | **PASS** |
| Deep Voice | 12.5 µs | 28.2 µs | 0.3% | 0.546 | 0 / 0 | **PASS** |
| Anime / Helium | 12.0 µs | 24.4 µs | 0.3% | 0.483 | 0 / 0 | **PASS** |
| Robot / Cyborg | 9.4 µs | 25.9 µs | 0.2% | 0.448 | 0 / 0 | **PASS** |
| Demon / Monster | 20.7 µs | 39.0 µs | 0.5% | 0.442 | 0 / 0 | **PASS** |
| Walkie Talkie | 17.1 µs | 31.5 µs | 0.4% | 0.360 | 0 / 0 | **PASS** |
| Custom AI Profile (16-Band) | 18.2 µs | 34.8 µs | 0.5% | 0.483 | 0 / 0 | **PASS** |
| Mute Isolation Test | — | — | — | 0.000 | 0 / 0 | **PASS** |

## 2. Pengujian Perangkat Android Fisik (Belum Dijalankan di Sandbox)
Karena sandbox ini tidak memiliki Android SDK/NDK/Gradle maupun emulator/perangkat fisik, daftar berikut wajib dijalankan setelah Anda membangun `app-debug.apk` di Android Studio:
- [ ] Instalasi APK pada Android 10–15 (arm64-v8a).
- [ ] Uji izin `RECORD_AUDIO`, `POST_NOTIFICATIONS`, dan `SYSTEM_ALERT_WINDOW`.
- [ ] Uji impor file WAV, MP3, M4A, dan AAC di `AI Voice Import`.
- [ ] Uji perpindahan rute headphone kabel ↔ Bluetooth headset ↔ panggilan telepon masuk (`AudioFocus`).
- [ ] Uji stabilitas Foreground Service & Floating Control saat berpindah ke Mobile Legends, WhatsApp, dan Discord.
- [ ] Uji stres 30 menit (memori, suhu perangkat, xrun/underrun, dan konsumsi baterai).

**Status Jujur**: Mesin C++ DSP terverifikasi bersih dan bebas clipping/NaN di pengujian native; aplikasi Android secara keseluruhan **belum** dinyatakan bebas bug sebelum kompilasi APK dan pengujian perangkat fisik dilakukan.
