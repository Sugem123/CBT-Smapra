# CBT SMAN 1 PRAMBON — Changelog

Semua perubahan penting pada aplikasi ini didokumentasikan di file ini.

Format mengikuti [Keep a Changelog](https://keepachangelog.com/id-ID/1.0.0/),
dan proyek ini menggunakan [Semantic Versioning](https://semver.org/lang/id/).

---

## [1.1.3] — 2026-11-09

### 🌐 Kompatibilitas Jaringan Lokal Offline & Otorisasi Token Keluar

#### Ditambahkan
- **Dukungan Penuh Jaringan Lokal (Tanpa Internet)**:
  - Komunikasi endpoint kontrol CBT (`api_cbt_config.php` dan `api_cbt_violation.php`) kini secara dinamis mengikuti host server yang sedang diakses (baik IP lokal sekolah misal `http://192.168.x.x` maupun domain internet publik).
  - Pengecekan whitelist, blacklist, dan pelaporan pelanggaran tetap berjalan 100% normal pada jaringan LAN/Wi-Fi lokal sekolah tanpa koneksi internet.
- **Otorisasi Token Keluar Siswa (Wajib Izin Pengawas)**:
  - Tombol keluar dan tombol Back tidak lagi langsung menutup ujian.
  - Muncul dialog: *"Untuk keluar dari mode ujian, masukkan Token Keluar yang diberikan oleh pengawas ruangan"*.
  - Token divalidasi terhadap:
    1. **Token AutoToken Sistem Moodle** yang aktif saat itu.
    2. **Token Manual Pengawas** yang diatur di Moodle CBT Control Panel.
  - Siswa tidak dapat keluar dari ujian jika belum meminta token kepada pengawas.
- **Pengaturan Token Keluar di Moodle CBT Control Panel**:
  - Pilihan mode token: *Token Sistem + Manual*, *Hanya Token Sistem*, atau *Hanya Token Manual*.
  - Menampilkan daftar token aktif yang dapat dibagikan pengawas kepada siswa yang telah selesai ujian.

#### Berkas APK
- `CBT-SMAPRA-v1.1.3-Android8+-release.apk` (32.4 MB)
- `CBT-SMAPRA-v1.1.3-Android5+-release.apk` (20.8 MB)

---

## [1.1.2] — 2026-11-09

### 🔑 Akses Tersembunyi Khusus Teknisi (Bypass Kamera Rusak)

#### Ditambahkan
- **Fitur Hidden Tap Logo Sekolah (5x Cepat)**:
  - Mengatasi kendala beberapa HP siswa yang kameranya rusak/buram/error sehingga tidak bisa memindai QR Code.
  - Pengawas/teknisi cukup mengetuk **Logo Sekolah SMAN 1 Prambon** di bagian atas beranda sebanyak **5x cepat**.
  - Memunculkan dialog autentikasi **PIN Proktor** (`131313`).
  - Setelah verifikasi PIN berhasil, muncul dialog khusus bagi teknisi untuk memasukkan URL server Moodle secara manual.
  - Siswa tetap tidak mengetahui URL server karena input dilakukan oleh teknisi dan tidak pernah disimpan ke riwayat perangkat.

#### Berkas APK
- `CBT-SMAPRA-v1.1.2-Android8+-release.apk` (32.4 MB)
- `CBT-SMAPRA-v1.1.2-Android5+-release.apk` (20.8 MB)

---

## [1.1.1] — 2026-11-09

### 🌐 Integrasi Moodle CBT Control Panel & Whitelist Aplikasi

#### Ditambahkan
- **Whitelist Aplikasi Resmi**:
  - Menambahkan pengecualian (*whitelist*) untuk aplikasi:
    1. **Shopee & ShopeePay** (`com.shopee.id`, `com.shopeepay.id`)
    2. **Musixmatch** (`com.musixmatch.android.lyrify`)
    3. **Fly GPS** (`com.fly.gps`)
    4. **MyTelkomsel** (`com.telkomsel.telkomselcm`)
    5. **Location Changer** (`com.locationchanger`)
  - Aplikasi-aplikasi di atas tidak akan lagi diblokir atau memicu peringatan di beranda.
- **Sinkronisasi Dinamis dari Server Moodle**:
  - Aplikasi secara otomatis mengambil konfigurasi blacklist & whitelist terbaru dari endpoint `/local/bulkquiz/api_cbt_config.php` saat memeriksa ancaman.
- **Pelaporan Real-time Siswa Keluar Ujian (`api_cbt_violation.php`)**:
  - Aplikasi Android secara otomatis mengirim laporan heartbeat pelanggaran setiap kali siswa terdeteksi keluar dari aplikasi, beralih window, atau membuka overlay selama ujian.
  - Tercatat langsung ke tabel database Moodle dan masuk ke counter pelanggaran di layar pengawas.

#### Berkas APK
- `CBT-SMAPRA-v1.1.1-Android8+-release.apk` (32.4 MB)
- `CBT-SMAPRA-v1.1.1-Android5+-release.apk` (20.8 MB)

---

## [1.1.0] — 2026-11-09

### 🛡️ Pre-Exam Anti-Floating Threat Gate (Kunci Total Aplikasi Melayang)

#### Ditambahkan
- **Izin Visibilitas Paket Penuh (`QUERY_ALL_PACKAGES` + `<queries>`)**:
  - Mengatasi pembatasan *Package Visibility* di Android 11 s/d Android 16 (API 30–36) agar sistem dapat memindai 100% seluruh aplikasi pihak ketiga yang terpasang di HP siswa.
- **Audit Izin `SYSTEM_ALERT_WINDOW` Tanpa Pengecualian**:
  - Mendeteksi setiap aplikasi pihak ketiga (selain aplikasi vendor resmi) yang memiliki status izin "Tampilkan di atas aplikasi lain" aktif (`MODE_ALLOWED`).
- **Database Blacklist By-Name & Keyword Scanner Populer**:
  - Memasukkan daftar hitam aplikasi floating populer Play Store: `com.lwi.android.flapps` (Floating Apps), `com.applay.overlay`, `com.leo.floating.browser`, `com.floating.tube`, `free.tube.premium.advanced.tuber`, `com.shy.floatingcalc`, `com.directchat.android`, dll.
  - Memindai nama aplikasi berbasis kata kunci: `"floating"`, `"float"`, `"overlay"`, `"bubble"`, `"popup"`, `"multitask"`, `"pip"`, `"tuber"`, `"split screen"`.
- **Pre-Exam Security Card (Blokir Total di Halaman Depan)**:
  - Jika terdeteksi ada aplikasi floating atau izin overlay aktif di HP siswa:
    - Tombol "Pindai QR Code Server" **dikunci total / dinonaktifkan**.
    - Muncul **Kartu Merah Peringatan Keamanan** yang menampilkan daftar nama aplikasi yang terdeteksi beserta alasannya.
    - Disediakan tombol langsung **"Atur / Buka Pengaturan HP"** untuk mempermudah siswa mencabut izin overlay atau meng-uninstall aplikasi.
    - Tombol **"Pindai Ulang"** & observer otomatis (`ON_RESUME`) yang memulihkan akses begitu aplikasi terlarang telah dibersihkan dari HP siswa.

#### Berkas APK
- `CBT-SMAPRA-v1.1.0-Android8+-release.apk` (32.4 MB)
- `CBT-SMAPRA-v1.1.0-Android5+-release.apk` (20.8 MB)

---

## [1.0.9] — 2026-11-09

### 📷 Mode Masuk Eksklusif Scan QR Code & Sembunyikan URL Server

#### Diubah / Ditingkatkan
- **Penghapusan Kolom Input URL Manual**:
  - Kolom teks input URL dan tombol mulai manual telah dihapus dari antarmuka beranda.
  - Alamat server CBT Moodle disembunyikan total agar siswa tidak mengetahui alamat IP maupun domain server sekolah.
- **Penghapusan Riwayat Tersimpan (Saved URLs)**:
  - Kartu riwayat URL dan preferensi lokal dibersihkan total saat aplikasi dibuka.
  - Mencegah siswa memilih kembali alamat yang pernah dimasukkan sebelumnya.
- **Alur Masuk Otomatis (Scan & Direct Login)**:
  - Akses ujian kini **wajib melalui pemindaian QR Code resmi dari pengawas di depan kelas**.
  - Begitu kamera mendeteksi QR Code server, aplikasi langsung memvalidasi dan memuat ruang ujian seketika tanpa jeda atau konfirmasi tambahan.
- **Pencegahan Siswa Keluar-Masuk Aplikasi Seenaknya**:
  - Jika siswa keluar dari ujian (misalnya setelah selesai atau proktor membuka kunci), aplikasi kembali ke beranda dalam kondisi bersih tanpa teks URL.
  - Siswa yang keluar wajib maju ke meja pengawas di depan kelas untuk memindai ulang QR Code jika ingin masuk kembali.

#### Berkas APK
- `CBT-SMAPRA-v1.0.9-Android8+-release.apk` (32.4 MB)
- `CBT-SMAPRA-v1.0.9-Android5+-release.apk` (20.8 MB)

---

## [1.0.8] — 2026-11-08

### 🔓 Pemulihan Titik Ketuk Rahasia Proktor Pojok Kanan Atas

#### Diperbaiki
- **Dual Secret Exit Tap Target**:
  - Menempatkan kembali invisible tap area (64x64 dp) di **Pojok Kanan Atas** layar ujian.
  - Mempertahankan tap target alternatif pada **Kotak Status Jam & Tanggal di Kiri Bawah**.
  - Pengawas/teknisi kini dapat mengetuk **5x cepat** pada **pojok kanan atas** ATAU **kotak status kiri bawah** untuk memunculkan dialog PIN Proktor (`131313`).

#### Berkas APK
- `CBT-SMAPRA-v1.0.8-Android8+-release.apk` (32.3 MB)
- `CBT-SMAPRA-v1.0.8-Android5+-release.apk` (20.7 MB)

---

## [1.0.7] — 2026-11-08

### 🔒 Hardening Keamanan, Skala Huruf, & Stabilitas Sesi Ujian

#### Ditambahkan
- **Blokir Total File Chooser / Dokumen Upload (`onShowFileChooser`)**:
  - `WebChromeClient` secara otomatis membatalkan pemanggilan pemilihan file (`filePathCallback.onReceiveValue(null)` dan me-return `true`).
  - Mencegah celah siswa membuka file manager, galeri foto, atau dokumen contekan dari dalam kuis Moodle.
- **Pengunci Skala Teks & Kontrol Ukuran Huruf Soal (`textZoom = 100` + Tombol `A- / A+`)**:
  - Kunci skala dasar teks webview ke `100%` agar tidak terpengaruh font ukuran raksasa dari pengaturan sistem Android siswa yang sering merusak layout tabel soal.
  - Tersedia kontrol zoom manual di Top Drawer (`A-` dan `A+`) dengan rentang 80% s/d 140% untuk mempermudah siswa yang membutuhkan ukuran tulisan lebih besar tanpa merusak struktur halaman.
- **Session Keep-Alive Otomatis (Anti-Session Expired Moodle)**:
  - Background coroutine menyuntikkan heartbeat fetch ke endpoint session Moodle setiap 4 menit.
  - Mencegah siswa logout otomatis atau kehilangan progres jawaban saat membaca soal esai panjang di satu halaman lebih dari 20-30 menit.
- **Custom Offline Error Page**:
  - Menggantikan halaman dinosaurus / error putih mentah browser Android dengan kartu gelap ramah jika Wi-Fi sekolah terputus: *"Koneksi Terputus. Pastikan terhubung ke Wi-Fi sekolah lalu tekan Refresh."*
- **Smart BackHandler di Lembar Kuis (`attempt.php` & `summary.php`)**:
  - Menekan tombol Back bawaan HP saat sedang mengerjakan soal akan memunculkan dialog konfirmasi keluar kuis alih-alih navigasi mundur yang berisiko mereset jawaban siswa.

#### Berkas APK
- `CBT-SMAPRA-v1.0.7-Android8+-release.apk` (32.3 MB)
- `CBT-SMAPRA-v1.0.7-Android5+-release.apk` (20.7 MB)

---

## [1.0.6] — 2026-11-08

### 🛡️ Penguatan Anti-Floating Apps (Zero-Overhead)

#### Ditambahkan
- **Hardware-Level Touch Obscuration Filter (`filterTouchesWhenObscured = true`)**:
  - Menolak event sentuhan pada WebView jika terdapat jendela mengambang (floating window) atau overlay transparan di atas layar ujian. Mencegah teknik kecurangan *tapjacking*.
- **Debounced Window Focus Detection (800ms)**:
  - Mendeteksi hilangnya fokus jendela aplikasi (`onWindowFocusChanged(false)`) saat siswa berinteraksi dengan floating app luar.
  - Toleransi pintar 800ms memastikan dialog sistem atau keyboard bawaan HP tidak memicu alarm palsu (*false positive*).
  - Layar soal otomatis tertutup layar peringatan pelanggaran keamanan jika fokus hilang lebih dari 800ms.

#### Berkas APK
- `CBT-SMAPRA-v1.0.6-Android8+-release.apk`
- `CBT-SMAPRA-v1.0.6-Android5+-release.apk`

---

## [1.0.5] — 2026-11-08

### 🔑 Play Protect Safe & Official Keystore Signing

#### Diubah / Diperbaiki
- **Pembersihan Izin Berisiko Tinggi (False-Positive Elimination)**:
  - Dihapus: `android.permission.PACKAGE_USAGE_STATS`.
  - Dihapus: `ExamDeviceAdminReceiver` dan izin `android.permission.BIND_DEVICE_ADMIN`.
  - Sistem penguncian mode kiosk beralih murni ke `startLockTask()` (App Pinning bawaan Android) yang resmi didukung tanpa memicu peringatan aplikasi berbahaya dari Google Play Protect.
- **Tanda Tangan Resmi SMAN 1 Prambon (`smapra-release.jks`)**:
  - Pembuatan Keystore RSA 2048-bit (SHA384withRSA) dengan masa berlaku 27 tahun: `CN=SMAN 1 Prambon, OU=IT CBT, O=SMAN 1 Prambon, L=Nganjuk, ST=Jawa Timur, C=ID`.
  - Menggantikan Android Debug Keystore sehingga instalasi di HP siswa lancar dan bebas peringatan merah.

#### Berkas APK
- `CBT-SMAPRA-v1.0.5-Android8+-release.apk`
- `CBT-SMAPRA-v1.0.5-Android5+-release.apk`

---

## [1.0.4] — 2026-11-08

### 🎨 Desain Ruang Ujian Baru: Top Quick Drawer & Bottom Bar

#### Ditambahkan
- **Top Collapsible Quick Settings Drawer**:
  - Tombol melayang `⚙ Pengaturan ▾` di tengah atas dengan animasi buka-tutup halus.
  - **Slider Kecerahan Layar**: Mengatur terang/redupnya layar langsung di dalam ujian tanpa perlu membuka status bar Android yang terkunci.
  - **Slider Volume Media**: Mengatur volume soal listening bahasa asing.
- **Bottom Status Bar (2 Tingkat)**:
  - Tingkat 1: Jam Digital real-time (`HH:mm`) dan Indikator Baterai adaptif (`⚡ / %`).
  - Tingkat 2: Tanggal lengkap Bahasa Indonesia (`Min, 08 Nov 2026`).
  - **Secret Exit PIN**: Mengetuk kotak status jam & tanggal sebanyak **5x cepat** akan memunculkan dialog PIN darurat pengawas (`131313`).
- **Tombol Refresh Cepat**:
  - Tombol `[ 🔄 Refresh ]` di kanan bawah untuk memuat ulang halaman saat koneksi Wi-Fi tersendat.

#### Berkas APK
- `CBT-SMAPRA-v1.0.4-Android8+-release.apk`
- `CBT-SMAPRA-v1.0.4-Android5+-release.apk`

---

## [1.0.3] — 2026-11-08

### ⏱️ Mini Status Bar (Waktu & Baterai)

#### Ditambahkan
- Kapsul status mini jam digital real-time dan indikator persentase baterai di pojok layar ujian.
- Integrasi BroadcastReceiver untuk status charging dan kapasitas baterai.

---

## [1.0.2] — 2026-11-08

### 📅 Tahun Hak Cipta Dinamis & Informasi Build

#### Ditambahkan
- Tampilan versi build aplikasi pada footer halaman beranda (`vX.X.X • Android 8+`).
- Tahun copyright dinamis mengikuti kalender sistem berjalan (`Calendar.YEAR`).

---

## [1.0.1] — 2026-11-08

### 🔄 Penyempurnaan Auto-Update GitHub Releases

#### Diperbaiki
- Sinkronisasi algoritma pembanding versi SemVer (`compareSemVer`) antara versi lokal dan rilis tag GitHub (`v1.0.X`).
- Pilihan cerdas arsitektur file APK: otomatis mengunduh varian `Android8+` untuk perangkat Android 8.0 ke atas, dan `Android5+` untuk perangkat versi sebelumnya.
- Penyelesaian masalah notifikasi update berulang setelah instalasi berhasil.

---

## [1.0.0] — 2026-11-07

### 🎉 Rilis Perdana

- Implementasi protokol Safe Exam Browser (SEB) dengan User-Agent `SMAPRA-CBT/1.0`.
- Integrasi keamanan Nginx Moodle (`seb-guard.conf`).
- Pemindai QR Code Server berbasis kamera dengan tombol aksi eksplisit di halaman utama.
- Fitur Kiosk Mode & Anti-Screenshot (`FLAG_SECURE`).
- Dukungan Multi-Flavor: `modern` (minSdk 26) dan `legacy` (minSdk 21).
- Otomatisasi pemeriksaan dan instalasi pembaruan dari GitHub Releases (`Sugem123/CBT-Smapra`).
