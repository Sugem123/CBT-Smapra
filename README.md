# CBT SMAN 1 PRAMBON (Secure Exam Browser)

[![Release](https://img.shields.io/github/v/release/Sugem123/CBT-Smapra?color=blue&style=flat-square)](https://github.com/Sugem123/CBT-Smapra/releases)
[![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat-square)](https://developer.android.com)
[![Moodle](https://img.shields.io/badge/Integration-Moodle%20SEB-orange?style=flat-square)](https://moodle.org)
[![Security](https://img.shields.io/badge/Security-Play%20Protect%20Safe-brightgreen?style=flat-square)](https://developers.google.com)

**CBT SMAN 1 PRAMBON** adalah aplikasi *Secure Exam Browser* (Kiosk Browser) berbasis Android yang dirancang khusus untuk pelaksanaan Computer-Based Test (CBT) berbasis Moodle di lingkungan **SMAN 1 Prambon, Nganjuk**.

Aplikasi ini mengisolasi lingkungan perangkat siswa selama ujian, mencegah kecurangan multi-aplikasi, menyelaraskan protokol Safe Exam Browser (SEB), serta memberikan antarmuka pengerjaan yang nyaman dan stabil.

---

## 🌟 Fitur Utama

### 1. Keamanan & Integritas Ujian (Anti-Cheat)
- **Safe Exam Browser (SEB) Protocol**: Dilengkapi token otentikasi User-Agent `SMAPRA-CBT/1.0` yang terintegrasi langsung dengan proteksi Nginx server Moodle (`seb-guard.conf`). Menolak browser biasa, peramban bawaan, maupun aplikasi exambro spoofing pihak ketiga.
- **Anti Screenshot & Screen Recording**: Dilindungi flag hardware `FLAG_SECURE` untuk memblokir tangkapan layar, perekaman layar, dan mirroring via kabel/nirkabel.
- **Hardware-Level Touch Obscuration Filter (`filterTouchesWhenObscured`)**: Menolak sentuhan layar WebView jika terdapat jendela mengambang (floating window) di atasnya untuk mencegah teknik *tapjacking*.
- **Debounced Window Focus Detection (800ms)**: Menutup layar soal dan mengunci ujian jika fokus aplikasi direbut oleh aplikasi floating luar atau split-screen.
- **Anti File Chooser Injection**: Memblokir total pemanggilan file manager / gallery selector (`onShowFileChooser`) pada WebView agar siswa tidak dapat membuka dokumen contekan.
- **Smart Back Navigation**: Mencegah tombol navigasi Back HP mengulang lembar pengerjaan kuis (`attempt.php`) atau keluar tanpa konfirmasi.

### 2. Kenyamanan Siswa di Ruang Ujian
- **Top Collapsible Quick Settings Drawer**:
  - **Slider Kecerahan Layar**: Mengatur terang/redupnya layar langsung di dalam ujian tanpa perlu membuka status bar Android yang terkunci.
  - **Slider Volume Media**: Mengatur volume soal listening bahasa asing.
  - **Kontrol Ukuran Teks Soal (`A-` / `A+`)**: Menyesuaikan skala teks soal (80% - 140%) dengan font terkunci (`textZoom = 100`) agar layout tabel soal tidak pecah.
- **Bottom Status Bar (2 Tingkat)**:
  - Kotak atas: Jam Digital real-time (`HH:mm`) & Status Baterai adaptif (`⚡ / %`).
  - Kotak bawah: Tanggal lengkap Bahasa Indonesia.
  - Kanan: Tombol **Refresh** cepat dengan animasi reload jika Wi-Fi sekolah sempat tersendat.
- **Anti-Timeout Session Keep-Alive**: Mengirim sinyal heartbeat setiap 4 menit ke Moodle agar sesi login tidak kedaluwarsa saat siswa membaca soal panjang.
- **Custom Offline Error Screen**: Menampilkan kartu pemberitahuan ramah jika koneksi Wi-Fi terputus tanpa memunculkan halaman browser rusak mentah.

### 3. Kemudahan Pengawas & Teknisi
- **Secret Exit PIN**: Pengawas/teknisi cukup mengetuk kotak jam/tanggal di kiri bawah sebanyak **5x cepat** untuk membuka dialog PIN darurat (`131313`).
- **Scan QR Code Server Eksplisit**: Tombol scan QR kamera yang jelas di halaman awal untuk pengisian URL server Moodle instan.
- **Auto-Update dari GitHub Releases**: Aplikasi secara otomatis mendeteksi rilis baru dari GitHub, mengunduh APK yang cocok dengan versi Android perangkat, dan memicu instalasi dalam satu ketukan.
- **Play Protect Safe**: Ditandatangani secara resmi dengan sertifikat Keystore RSA 2048-bit SMAN 1 Prambon tanpa izin berisiko tinggi (`PACKAGE_USAGE_STATS`), sehingga instalasi bebas peringatan berbahaya.

---

## 📱 Varian Rilis (Flavor)

Aplikasi dibangun ke dalam dua varian arsitektur untuk memastikan kompatibilitas penuh:

| Varian | Minimum OS | Target Perangkat | Fitur Kunci |
| :--- | :--- | :--- | :--- |
| **Modern (`modernRelease`)** | Android 8.0 (API 26) ke atas | HP Siswa Standar / Terbaru | WindowInsets edge-to-edge, App Pinning DPC, Adaptive Icon |
| **Legacy (`legacyRelease`)** | Android 5.0 (API 21) s/d 7.1 | HP / Tablet Lama | Runtime graceful degradation, kompatibilitas WebKit lawas |

---

## 🛠️ Panduan Build

### Prasyarat
- JDK 17 atau Temurin 21
- Android SDK (API 35, Build-Tools 34.0.0+)
- Gradle Wrapper (termasuk di dalam repositori)

### Perintah Kompilasi

```bash
# Clone repositori
git clone https://github.com/Sugem123/CBT-Smapra.git
cd CBT-Smapra

# Build Varian Modern (Release)
./gradlew assembleModernRelease

# Build Varian Legacy (Release)
./gradlew assembleLegacyRelease

# Build Semua Varian Sekaligus
./gradlew assembleRelease
```

File output APK akan berada di folder:
- `app/build/outputs/apk/modern/release/CBT-SMAPRA-v{version}-Android8+-release.apk`
- `app/build/outputs/apk/legacy/release/CBT-SMAPRA-v{version}-Android5+-release.apk`

---

## 🔒 Konfigurasi Server (Nginx SEB Guard)

Untuk memastikan kuis Moodle hanya dapat diakses melalui aplikasi resmi ini, server Nginx dikonfigurasikan dengan aturan filter User-Agent berikut pada `/etc/nginx/includes/seb-guard.conf`:

```nginx
location ~ ^/mod/quiz/(attempt|startattempt|processattempt|view)\.php(/|$) {
    # 1. Tolak perangkat iOS (iPhone / iPad) sementara
    if ($http_user_agent ~* "iPhone|iPad|iPod") {
        return 403 "...Perangkat iOS Tidak Diizinkan...";
    }

    # 2. Tolak peramban luar / Exambro spoofer tanpa token resmi
    set $block_spoofer 0;
    if ($http_user_agent ~* "SEB/") {
        set $block_spoofer 1;
    }
    if ($http_user_agent ~* "SMAPRA-CBT") {
        set $block_spoofer 0;
    }
    if ($http_user_agent ~* "Windows|Macintosh") {
        set $block_spoofer 0; # Tetap izinkan SEB Desktop di Lab
    }

    if ($block_spoofer = 1) {
        return 403 "...Aplikasi Tidak Diizinkan...";
    }

    # Teruskan ke pool PHP-FPM
    fastcgi_pass unix:/var/run/php/php7.4-fpm-frontend.sock;
    include fastcgi_params;
    fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
}
```

---

## 📄 Lisensi & Hak Cipta
Hak Cipta © 2026 **SMAN 1 Prambon, Nganjuk**.  
Dikelola oleh Tim IT & Panitia CBT SMAN 1 Prambon.
