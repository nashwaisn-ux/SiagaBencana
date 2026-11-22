# SIAGABENCANA
Sistem Pelaporan dan Koordinasi Penanganan Bencana Lingkungan Berbasis Android

## Cara Menjalankan

### BACKEND (Laravel)
```bash
cd backend
composer install
copy .env.example .env      # (atau cp .env.example .env)
php artisan key:generate
# (Buat database MySQL "siagabencana", user root tanpa password, atau sesuaikan DB_* di .env)
php artisan migrate --seed
php artisan storage:link
php artisan serve --host=0.0.0.0 --port=8000
```

### ANDROID
- Emulator Android Studio: `BASE_URL = http://10.0.2.2:8000/api/`
- HP fisik (satu WiFi dengan komputer): `BASE_URL = http://IP_LAPTOP:8000/api/` dan izinkan port 8000 di Windows Firewall.
- Atur `BASE_URL` di `gradle.properties` (root proyek), contoh:
  `BASE_URL=http://10.0.2.2:8000/api/`
  lalu Sync Gradle.

### AKUN DEMO (dari seeder, password: `password`)
- Petugas: `petugas@demo.test`
- Relawan: `relawan@demo.test`
- Masyarakat: `masyarakat@demo.test`
- Admin Utama: `admin.petugas@siagabencana.id` / `password123`
