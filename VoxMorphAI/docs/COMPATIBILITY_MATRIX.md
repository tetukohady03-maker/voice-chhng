# FASE 7 — Matriks Kompatibilitas Aplikasi Pihak Ketiga

Matriks ini secara jujur memisahkan **4 aspek independen** sesuai batasan OS Android ([Sharing Audio Input](https://developer.android.com/media/platform/sharing-audio-input); [AOSP Concurrent Capture](https://source.android.com/docs/core/audio/concurrent)).

| Aplikasi Target | Mode Implementasi | Floating Overlay Berjalan | Mikrofon Diakses VOXMORPH | Pemrosesan DSP Lokal Berjalan | Aplikasi Tujuan Menerima Suara Hasil Perubahan |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Mobile Legends** | APK Standar (Non-Root) | Ya (`SYSTEM_ALERT_WINDOW`) | Terbatas saat mic ML aktif (konflik concurrent capture) | Ya (saat mic dipegang VOXMORPH) | **Tidak langsung** (butuh hardware loopback / USB interface) |
| **WhatsApp** | APK Standar (Non-Root) | Ya | Terbatas saat panggilan aktif (`VOICE_COMMUNICATION` eksklusif) | Ya (sebelum/di luar panggilan) | **Tidak langsung** (butuh hardware loopback / device terpisah) |
| **Discord** | APK Standar (Non-Root) | Ya | Terbatas saat voice channel aktif | Ya (saat mic dipegang VOXMORPH) | **Tidak langsung** (butuh USB audio loopback) |
| **Game Voice Chat Lain** | APK Standar (Non-Root) | Ya | Bergantung kebijakan OEM/Android 10+ | Ya | **Tidak langsung** tanpa loopback atau Virtual Audio HAL |
| **Semua Aplikasi di Atas** | **Hardware Loopback (Non-Root)** | Ya | Ya (mic utama → VOXMORPH) | Ya (Real-time Oboe DSP) | **Ya** (via kabel TRRS splitter / USB interface ke mic input tujuan) |
| **Semua Aplikasi di Atas** | **System/Privileged App / Custom ROM** | Ya | Ya (AudioPolicy privileged capture) | Ya | **Ya** (melalui Virtual Audio HAL / `AudioPolicy` dynamic mix) |
