# Catatan Teknis & Arsitektur Proyek (PROJECT_NOTES.md)
*Dokumen ini dibuat untuk mempermudah pelacakan konteks, debugging, dan transfer pekerjaan ke developer / AI agent berikutnya.*

Terakhir Diperbarui: **November 2026**  
Status: **Production-Ready (v1.1.4)**  
Repositori GitHub: [`Sugem123/CBT-Smapra`](https://github.com/Sugem123/CBT-Smapra)

---

## 1. Ikhtisar & Identitas Proyek
- **Nama Proyek**: CBT SMAN 1 PRAMBON (Secure Exam Browser)
- **Paket Android**:
  - Modern: `com.smapra.exambro`
  - Legacy: `com.smapra.exambro.legacy`
- **User-Agent Khusus**:
  `Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) SEB/3.3.2 SMAPRA-CBT/1.0`
- **PIN Proktor Darurat**: `131313` (Konstanta `PROCTOR_PIN` di `ExamScreen.kt`)
  - Titik Akses: Ketuk **5x cepat** pada kotak status Jam & Tanggal di kiri bawah layar ujian.
- **Server Moodle Target**:
  - URL Produksi: `http://sassman1prambon.sytes.net`
  - IP SSH Manajemen: `100.67.65.42` (user `dhonawid`)
  - Direktori Moodle: `/home/ujian/www/moodle` (PHP 7.4-FPM + PostgreSQL + Redis)
  - Aturan Nginx SEB: `/etc/nginx/includes/seb-guard.conf`

---

## 2. Struktur Modul & Berkas Kunci

```
ExamBro/
├── app/
│   ├── build.gradle.kts                # Build config, flavor (modern/legacy), versioning, signing
│   ├── smapra-release.jks              # Official Keystore RSA 2048-bit (Play Protect Safe)
│   ├── src/main/
│   │   ├── AndroidManifest.xml         # Manifest bersih (tanpa PACKAGE_USAGE_STATS / BIND_DEVICE_ADMIN)
│   │   ├── java/com/smapra/exambro/
│   │   │   ├── ExamBroApp.kt           # Application class (@HiltAndroidApp)
│   │   │   ├── presentation/
│   │   │   │   ├── MainActivity.kt     # FLAG_SECURE, window focus debouncer listener
│   │   │   │   ├── setup/              # Beranda: URL, QR Scanner, Auto-Update Dialog, Dynamic Footer
│   │   │   │   │   ├── SetupScreen.kt
│   │   │   │   │   └── SetupViewModel.kt
│   │   │   │   └── exam/               # Ruang Ujian WebView, Top Drawer, Bottom Bar, Smart Back
│   │   │   │       ├── ExamScreen.kt
│   │   │   │       └── ExamViewModel.kt
│   │   │   ├── security/               # Komponen Pertahanan
│   │   │   │   ├── FloatingAppDetector.kt  # Deteksi izin SYSTEM_ALERT_WINDOW & proses overlay
│   │   │   │   ├── ScreenCaptureGuard.kt   # Hardware FLAG_SECURE
│   │   │   │   ├── KioskModeManager.kt     # App Pinning (startLockTask)
│   │   │   │   └── SEBCompatibility.kt     # Header & URL parser SEB
│   │   │   └── updater/
│   │   │       └── AppUpdater.kt       # GitHub Releases checker, downloader, & package installer
│   │   └── res/xml/
│   │       ├── file_provider_paths.xml # FileProvider untuk APK auto-update
│   │       └── network_security_config.xml # Mengizinkan cleartext HTTP (IP lokal sekolah)
```

---

## 3. Logika & Mekanisme Keamanan Utama

### A. Alur Validasi Akses Kuis (SEB Protocol)
1. Kuis Moodle di-set `requiresafeexambrowser = 4` (`USE_SEB_CLIENT_CONFIG`).
2. Nginx `/etc/nginx/includes/seb-guard.conf` memfilter request `/mod/quiz/*`:
   - Jika User-Agent mengandung `iPhone|iPad|iPod`: **Tolak 403** (Pesan: Perangkat iOS Tidak Didukung).
   - Jika mengandung `SEB/` tapi tidak memiliki `SMAPRA-CBT` dan bukan `Windows|Macintosh`: **Tolak 403** (Blokir Exambro spoofer).
   - Jika mengandung `SMAPRA-CBT`: **Lolos ke Moodle**.
   - Jika desktop Windows/Macintosh: **Lolos ke Moodle** (untuk keperluan lab/pengawas).

### B. Anti Floating Apps (Defense-in-Depth)
1. **`filterTouchesWhenObscured = true`**: Pada level hardware, Android menolak sentuhan pada WebView jika ada jendela floating transparan di atasnya.
2. **Debounced Window Focus Detection (800ms)**: Jika siswa menyentuh jendela floating di luar CBT, `onWindowFocusChanged(false)` dipanggil. Sistem menunggu 800ms (agar keyboard sistem tidak memicu alarm palsu), jika fokus tidak kembali, layar soal ditutup dengan peringatan pelanggaran.
3. **App Pinning**: Memanggil `activity.startLockTask()` saat ujian dimulai, mencegah tombol navigasi fisik/gesture membuka aplikasi lain.

### C. Bebas Google Play Protect
1. Tidak menggunakan izin sensitif yang dilarang untuk APK non-store:
   - ❌ `android.permission.PACKAGE_USAGE_STATS` (dihapus)
   - ❌ `android.permission.BIND_DEVICE_ADMIN` (dihapus)
2. Ditandatangani menggunakan sertifikat resmi:
   - Keystore: `smapra-release.jks` (RSA 2048-bit, berlaku 27 tahun)
   - Alias: `smapra_cbt`
   - Password: `smapra_cbt_pass_2026`

---

## 4. Alur Auto-Update GitHub Releases
1. Saat aplikasi dibuka di halaman beranda (`SetupScreen.kt`), fungsi `checkForUpdate()` berjalan di latar belakang.
2. Membaca endpoint publik: `https://api.github.com/repos/Sugem123/CBT-Smapra/releases?per_page=1`.
3. Membandingkan SemVer (`v1.0.X` vs `BuildConfig.VERSION_NAME`):
   - Jika ada rilis baru: Menampilkan dialog unduh & banner interaktif di halaman beranda.
   - Mengunduh APK yang sesuai dengan OS perangkat (mencari asset yang mengandung `Android8+` untuk Android 8.0+ atau `Android5+` untuk OS lama).
   - Membuka instalasi APK via `FileProvider`.

---

## 5. Riwayat Versi Penting
- **v1.0.0**: Rilis perdana, integrasi SEB, QR Scanner kamera, Kiosk Mode, Smart BackHandler.
- **v1.0.1 - v1.0.2**: Sinkronisasi SemVer perbandingan rilis, tahun copyright dinamis, footer version display.
- **v1.0.3**: Penambahan Mini Status Bar Jam & Baterai pojok kanan atas.
- **v1.0.4**: Redesain UI Ujian: Top Collapsible Drawer (Kecerahan & Volume), Bottom Status Bar (Jam, Baterai, Tanggal) + Tombol Refresh Cepat.
- **v1.0.5**: Play Protect Safe: Pembersihan izin berisiko tinggi dan migrasi ke Official Release Keystore.
- **v1.0.6**: Penguatan Anti-Floating Apps (Touch Obscuration Filter + Window Focus 800ms debouncer).
- **v1.0.7**: Blokir pemanggilan file upload (`onShowFileChooser`), pengunci skala font (`textZoom = 100` + Tombol `A-/A+`), session keep-alive otomatis (4 menit), dan halaman kustom saat koneksi terputus.
- **v1.0.8**: Dual secret exit tap target (Pojok Kanan Atas 64x64 dp & Kotak Status Kiri Bawah, tap 5x untuk PIN Proktor 131313).
- **v1.0.9**: Menghapus input manual URL dan histori, beralih penuh ke scan QR Code langsung masuk (Direct Login), alamat server Moodle disembunyikan total agar siswa tidak mengetahui URL dan wajib scan ulang ke pengawas jika keluar aplikasi.
- **v1.1.0**: Pre-Exam Anti-Floating Threat Gate: Menambahkan izin `QUERY_ALL_PACKAGES` untuk visibilitas paket di Android 11–16, blacklist package by-name (`com.lwi.android.flapps`, dll.), deteksi izin `SYSTEM_ALERT_WINDOW == MODE_ALLOWED` pada aplikasi pihak ketiga, serta penguncian total tombol ujian di beranda dengan Kartu Merah Ancaman dan tombol pembersihan cepat.
- **v1.1.1**: Integrasi Moodle CBT Control Panel: Sinkronisasi dinamis blacklist/whitelist dari server, pembebasan whitelist (Shopee, Musixmatch, Fly GPS, MyTelkomsel, Location Changer), serta pelaporan real-time saat siswa keluar aplikasi ke tabel `mdl_local_bulkquiz_cbt_violation`.
- **v1.1.2**: Hidden Technician Bypass: Ketuk 5x pada Logo Sekolah di beranda memunculkan otentikasi PIN Proktor (`131313`) untuk input URL server manual khusus bagi siswa yang kameranya rusak/error, tanpa membocorkan URL kepada siswa.
- **v1.1.3**: Kompatibilitas Jaringan Lokal Offline (IP lokal dan internet) untuk seluruh sinkronisasi CBT, serta Otorisasi Token Keluar Siswa (Siswa wajib memasukkan token sistem/manual dari pengawas untuk keluar ujian).
- **v1.1.4**: Dukungan Input IP Lokal Langsung tanpa awalan http:// (IPv4 auto-normalize), dan Pengaturan PIN Proktor Dinamis langsung dari Moodle CBT Control Panel.

---

## 6. Catatan Pengembang / Agent Selanjutnya
1. **Jangan mengubah algoritma penamaan output APK** di `app/build.gradle.kts`:
   Pola `CBT-SMAPRA-v{version}-Android{8+|5+}-{buildType}.apk` digunakan oleh logika regex `AppUpdater.kt` untuk mencocokkan download asset di HP siswa.
2. **Jangan menambahkan kembali izin `PACKAGE_USAGE_STATS`** ke `AndroidManifest.xml` agar reputasi Play Protect tidak jatuh kembali ke status *harmful app*.
3. **Penyimpanan Build Lokal**: Salinan build APK siap distribusi selalu disimpan di folder:
   `D:\belajar a.i\exambro-smapra\release\`
