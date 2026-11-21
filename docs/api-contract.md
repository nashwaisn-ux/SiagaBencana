# Kontrak API SIAGABENCANA (v1.0)

Dokumen ini adalah **kesepakatan antara Backend (Anggota 2, 3) dan Android (Anggota 4, 5)**.
Perubahan nama endpoint, field, atau format respons **wajib diumumkan di grup** dan diperbarui di dokumen ini.

- **Base URL (lokal):** `http://<IP-LAPTOP>:8000/api` (emulator: `http://10.0.2.2:8000/api`)
- **Penanggung jawab:** A2 = Anggota 2, A3 = Anggota 3

---

## 1. Aturan Umum

### 1.1 Header

Semua request:

```
Accept: application/json
Authorization: Bearer <token>        (kecuali register & login)
Content-Type: application/json       (kecuali upload file: multipart/form-data)
```

### 1.2 Format respons sukses

```json
{
  "success": true,
  "message": "Pesan singkat",
  "data": { }
}
```

`data` bisa berupa objek, array, atau `null`.

### 1.3 Format respons gagal

```json
{
  "success": false,
  "message": "Pesan singkat yang ramah pengguna",
  "errors": { "email": ["Email sudah terdaftar."] }
}
```

`errors` hanya ada pada validasi gagal (422).

| Kode | Arti | Kapan |
|---|---|---|
| 200 | OK | Berhasil (GET, PUT, PATCH, DELETE, login) |
| 201 | Created | Berhasil membuat data baru (POST) |
| 401 | Unauthenticated | Token tidak ada / salah / kedaluwarsa |
| 403 | Forbidden | Login benar, tapi role tidak berhak |
| 404 | Not Found | Data tidak ditemukan |
| 422 | Unprocessable | Validasi input gagal |
| 500 | Server Error | Kesalahan server |

### 1.4 Format daftar berhalaman (pagination)

Semua endpoint daftar yang berhalaman mengembalikan:

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [ ],
    "meta": { "current_page": 1, "last_page": 3, "per_page": 10, "total": 27 }
  }
}
```

Query umum: `page` (default 1), `per_page` (default 10, maks 50).

### 1.5 Tipe data & nilai tetap (enum)

| Field | Nilai |
|---|---|
| `role` | `masyarakat`, `relawan`, `petugas` |
| `urgensi` | `rendah`, `sedang`, `tinggi`, `kritis` |
| `status` (laporan) | `baru`, `diverifikasi`, `ditangani`, `selesai` |
| `status_tugas` (penugasan) | `ditugaskan`, `diterima`, `selesai` |

Aturan tipe:
- `latitude` dan `longitude` dikirim sebagai **angka (number)**, bukan string. Di Laravel, ubah dengan `(float)` atau cast `float` di model, karena kolom `decimal` secara bawaan terbaca sebagai string.
- Tanggal memakai **ISO 8601** (`2026-11-06T09:30:00+07:00`). Atur `'timezone' => 'Asia/Jakarta'` di `config/app.php`.
- URL foto selalu **lengkap** (`http://IP:8000/storage/laporan/abc.jpg`), bukan path relatif. Nilainya `null` jika tidak ada foto.
- Field tanpa nilai dikirim sebagai `null`, bukan dihapus dari JSON.

### 1.6 Objek yang dipakai berulang

**User ringkas**
```json
{ "id": 1, "nama": "Budi Santoso" }
```

**Laporan (untuk daftar)**
```json
{
  "id": 12,
  "judul": "Banjir setinggi 1 meter di Jalan Melati",
  "deskripsi": "Air masuk ke rumah warga sejak subuh.",
  "foto_url": "http://192.168.1.10:8000/storage/laporan/abc123.jpg",
  "latitude": 0.5070677,
  "longitude": 101.4477930,
  "jumlah_terdampak": 25,
  "urgensi": "tinggi",
  "status": "baru",
  "kategori": { "id": 1, "nama": "Banjir" },
  "pelapor": { "id": 3, "nama": "Siti Aminah" },
  "created_at": "2026-11-06T09:30:00+07:00",
  "updated_at": "2026-11-06T09:30:00+07:00"
}
```

**Laporan (detail)** = objek di atas ditambah:
```json
{
  "penugasan": [
    {
      "id": 4,
      "relawan": { "id": 7, "nama": "Andi Pratama" },
      "petugas": { "id": 2, "nama": "Pak Joko" },
      "catatan": "Bawa perahu karet.",
      "status_tugas": "diterima",
      "foto_bukti_url": null,
      "created_at": "2026-11-06T10:00:00+07:00"
    }
  ],
  "riwayat": [
    {
      "id": 21,
      "status_lama": null,
      "status_baru": "baru",
      "keterangan": "Laporan dibuat",
      "user": { "id": 3, "nama": "Siti Aminah" },
      "created_at": "2026-11-06T09:30:00+07:00"
    }
  ]
}
```

---

## 2. Hak Akses per Role

| Endpoint | Masyarakat | Relawan | Petugas |
|---|:-:|:-:|:-:|
| Register / Login / Logout / Profil | ya | ya | ya |
| Lihat kategori | ya | ya | ya |
| Daftar & detail laporan | milik sendiri | yang ditugaskan padanya | semua |
| Buat laporan | ya | tidak | ya |
| Ubah / hapus laporan | milik sendiri, **hanya saat status `baru`** | tidak | semua |
| Hapus massal | milik sendiri (status `baru`) | tidak | semua |
| Ubah urgensi | tidak | tidak | ya |
| Tugaskan relawan, daftar relawan | tidak | tidak | ya |
| Ubah status laporan | tidak | laporan yang ditugaskan padanya | semua |
| Daftar penugasan | tidak | miliknya | semua |
| Ubah status tugas, unggah bukti | tidak | tugas miliknya | tidak |
| Riwayat | laporan miliknya | laporan yang ditugaskan padanya | semua |
| Dashboard | statistik miliknya | statistik tugasnya | statistik semua |
| Peta | laporan miliknya | laporan ditugaskan padanya | semua |

Pelanggaran hak akses menghasilkan **403**.

---

## 3. Daftar Endpoint (ringkasan)

| No | Method | Endpoint | Fungsi | PJ |
|---|---|---|---|---|
| 1 | POST | `/register` | Daftar akun masyarakat | A2 |
| 2 | POST | `/login` | Masuk, dapat token | A2 |
| 3 | POST | `/logout` | Keluar | A2 |
| 4 | GET | `/me` | Profil user login | A2 |
| 5 | GET | `/kategori` | Daftar kategori bencana | A2 |
| 6 | GET | `/laporan` | Daftar laporan (cari, filter, paginasi) | A2 |
| 7 | GET | `/laporan/{id}` | Detail laporan | A2 |
| 8 | POST | `/laporan` | Buat laporan (multipart) | A2 |
| 9 | PUT | `/laporan/{id}` | Ubah laporan | A2 |
| 10 | DELETE | `/laporan/{id}` | Hapus satu laporan | A2 |
| 11 | POST | `/laporan/bulk-delete` | Hapus banyak laporan | A2 |
| 12 | GET | `/relawan` | Daftar relawan | A3 |
| 13 | POST | `/laporan/{id}/tugaskan` | Tugaskan relawan | A3 |
| 14 | PATCH | `/laporan/{id}/status` | Ubah status laporan | A3 |
| 15 | PATCH | `/laporan/{id}/urgensi` | Ubah urgensi | A3 |
| 16 | GET | `/penugasan` | Daftar penugasan | A3 |
| 17 | PATCH | `/penugasan/{id}/status` | Ubah status tugas | A3 |
| 18 | POST | `/penugasan/{id}/bukti` | Unggah foto bukti (multipart) | A3 |
| 19 | GET | `/riwayat` | Riwayat aktivitas/status | A3 |
| 20 | GET | `/dashboard` | Statistik ringkas | A3 |
| 21 | GET | `/peta` | Titik sebaran laporan | A3 |

---

## 4. Detail Endpoint

### 4.1 Autentikasi

#### POST `/register` (publik)

Selalu membuat akun **masyarakat**. Akun relawan dan petugas dibuat lewat seeder.

Request:
```json
{
  "nama": "Siti Aminah",
  "email": "siti@mail.com",
  "password": "rahasia123",
  "password_confirmation": "rahasia123",
  "no_hp": "081234567890"
}
```
Validasi: `nama` wajib maks 100; `email` wajib, valid, unik; `password` wajib min 8, konfirmasi cocok; `no_hp` opsional.

Respons 201:
```json
{
  "success": true,
  "message": "Registrasi berhasil",
  "data": {
    "token": "1|abcdef123456...",
    "user": { "id": 3, "nama": "Siti Aminah", "email": "siti@mail.com", "role": "masyarakat", "no_hp": "081234567890" }
  }
}
```

#### POST `/login` (publik)

Request:
```json
{ "email": "petugas@demo.test", "password": "password" }
```
Respons 200:
```json
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "token": "2|xyz789...",
    "user": { "id": 2, "nama": "Pak Joko", "email": "petugas@demo.test", "role": "petugas", "no_hp": null }
  }
}
```
Gagal (email/password salah) → **401**, `"message": "Email atau password salah"`.

#### POST `/logout`

Tanpa body. Menghapus token yang sedang dipakai.
```json
{ "success": true, "message": "Logout berhasil", "data": null }
```

#### GET `/me`

```json
{
  "success": true,
  "message": "OK",
  "data": { "id": 2, "nama": "Pak Joko", "email": "petugas@demo.test", "role": "petugas", "no_hp": null }
}
```

### 4.2 Referensi

#### GET `/kategori`

Tanpa paginasi (jumlahnya sedikit). Dipakai untuk dropdown di form laporan.
```json
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": 1, "nama": "Banjir" },
    { "id": 2, "nama": "Kebakaran" },
    { "id": 3, "nama": "Tanah Longsor" },
    { "id": 4, "nama": "Pencemaran Lingkungan" },
    { "id": 5, "nama": "Kebakaran Hutan dan Lahan" },
    { "id": 6, "nama": "Lainnya" }
  ]
}
```

#### GET `/relawan` (petugas)

Query opsional: `search` (nama). Tanpa paginasi.
```json
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": 7, "nama": "Andi Pratama", "no_hp": "081200000001", "tugas_aktif": 1 },
    { "id": 8, "nama": "Rina Wulandari", "no_hp": "081200000002", "tugas_aktif": 0 }
  ]
}
```
`tugas_aktif` = jumlah penugasan berstatus `ditugaskan` atau `diterima`.

### 4.3 Laporan

#### GET `/laporan`

Query (semua opsional):

| Parameter | Contoh | Keterangan |
|---|---|---|
| `search` | `banjir` | Cari di judul dan deskripsi |
| `status` | `baru` | Filter status |
| `urgensi` | `tinggi` | Filter urgensi |
| `kategori_id` | `1` | Filter kategori |
| `page`, `per_page` | `1`, `10` | Paginasi |

Urutan: terbaru dulu. Respons memakai format paginasi (bagian 1.4) dengan `items` berisi **Laporan (daftar)**.

Data kosong tetap 200 dengan `"items": []` dan `"total": 0` (Android menampilkan tampilan "data kosong").

#### GET `/laporan/{id}`

Respons 200: objek **Laporan (detail)**. Tidak ditemukan atau bukan haknya → 404 / 403.

#### POST `/laporan` (multipart/form-data)

| Field | Tipe | Wajib | Aturan |
|---|---|---|---|
| `judul` | text | ya | maks 120 |
| `deskripsi` | text | ya | |
| `kategori_id` | number | ya | harus ada di tabel kategori |
| `latitude` | number | ya | -90 sampai 90 |
| `longitude` | number | ya | -180 sampai 180 |
| `jumlah_terdampak` | number | tidak | min 0, default 0 |
| `foto` | file | tidak | gambar jpg/png, maks 4 MB |

Server otomatis mengisi: `user_id` (dari token), `status = baru`, `urgensi` (dari skor otomatis), dan menulis satu baris riwayat "Laporan dibuat".

Respons 201:
```json
{ "success": true, "message": "Laporan berhasil dibuat", "data": { "...Laporan (detail)..." } }
```

#### PUT `/laporan/{id}`

Mengubah `judul`, `deskripsi`, `kategori_id`, `latitude`, `longitude`, `jumlah_terdampak`, dan opsional `foto`.

Karena PHP tidak membaca file pada request PUT, **Android mengirim sebagai `POST /laporan/{id}` dengan field tambahan `_method=PUT`** (multipart). Laravel akan memperlakukannya sebagai PUT. Jika tanpa foto, `PUT` JSON biasa juga diterima.

Respons 200: `data` = Laporan (detail) terbaru. Masyarakat hanya boleh saat status `baru`, jika tidak → 403 dengan pesan "Laporan sudah diproses dan tidak dapat diubah".

#### DELETE `/laporan/{id}`

```json
{ "success": true, "message": "Laporan dihapus", "data": null }
```
Aturan hak akses sesuai bagian 2.

#### POST `/laporan/bulk-delete`

Request:
```json
{ "ids": [12, 13, 15] }
```
Validasi: `ids` wajib array, minimal 1, maksimal 50, tiap id angka.

Respons 200:
```json
{
  "success": true,
  "message": "2 laporan dihapus",
  "data": { "deleted": 2, "skipped": [15] }
}
```
`skipped` berisi id yang tidak boleh atau tidak bisa dihapus (bukan haknya, sudah diproses, atau tidak ditemukan). Penghapusan berjalan dalam satu transaksi.

### 4.4 Penugasan dan status

#### POST `/laporan/{id}/tugaskan` (petugas)

Request:
```json
{ "relawan_id": 7, "catatan": "Bawa perahu karet." }
```
Efek: membuat baris penugasan (`status_tugas = ditugaskan`); jika status laporan masih `baru`, otomatis menjadi `diverifikasi` dan tercatat di riwayat.

Respons 201:
```json
{
  "success": true,
  "message": "Relawan berhasil ditugaskan",
  "data": {
    "id": 4,
    "laporan_id": 12,
    "relawan": { "id": 7, "nama": "Andi Pratama" },
    "petugas": { "id": 2, "nama": "Pak Joko" },
    "catatan": "Bawa perahu karet.",
    "status_tugas": "ditugaskan",
    "foto_bukti_url": null,
    "created_at": "2026-11-06T10:00:00+07:00"
  }
}
```
Error: `relawan_id` bukan user ber-role relawan → 422.

#### PATCH `/laporan/{id}/status` (petugas, relawan yang ditugaskan)

Request:
```json
{ "status": "ditangani", "keterangan": "Tim sudah di lokasi." }
```
Validasi: `status` salah satu enum; `keterangan` opsional.
Relawan hanya boleh mengubah ke `ditangani` atau `selesai`.
Efek: menulis satu baris `riwayat_status` (status lama, baru, user, keterangan).

Respons 200: `data` = Laporan (detail) terbaru.

#### PATCH `/laporan/{id}/urgensi` (petugas)

Request:
```json
{ "urgensi": "kritis", "keterangan": "Ada korban terjebak." }
```
Respons 200: `data` = Laporan (detail) terbaru.

#### GET `/penugasan`

Query: `status_tugas` (opsional), `page`, `per_page`. Relawan hanya melihat miliknya; petugas melihat semua.

`items` berisi:
```json
{
  "id": 4,
  "status_tugas": "diterima",
  "catatan": "Bawa perahu karet.",
  "foto_bukti_url": null,
  "created_at": "2026-11-06T10:00:00+07:00",
  "relawan": { "id": 7, "nama": "Andi Pratama" },
  "laporan": { "id": 12, "judul": "Banjir setinggi 1 meter di Jalan Melati", "urgensi": "tinggi", "status": "diverifikasi" }
}
```

#### PATCH `/penugasan/{id}/status` (relawan pemilik tugas)

Request:
```json
{ "status_tugas": "diterima" }
```
Nilai yang diizinkan: `diterima`, `selesai`. Respons 200: `data` = objek penugasan seperti di atas.

#### POST `/penugasan/{id}/bukti` (relawan pemilik tugas, multipart/form-data)

| Field | Tipe | Wajib | Aturan |
|---|---|---|---|
| `foto_bukti` | file | ya | gambar jpg/png, maks 4 MB |
| `catatan` | text | tidak | |

Respons 200: `data` = objek penugasan dengan `foto_bukti_url` terisi.

### 4.5 Riwayat, dashboard, peta

#### GET `/riwayat`

Query: `laporan_id` (opsional, untuk riwayat satu laporan), `page`, `per_page`. Urutan: terbaru dulu. Format paginasi; `items`:
```json
{
  "id": 25,
  "laporan": { "id": 12, "judul": "Banjir setinggi 1 meter di Jalan Melati" },
  "status_lama": "diverifikasi",
  "status_baru": "ditangani",
  "keterangan": "Tim sudah di lokasi.",
  "user": { "id": 7, "nama": "Andi Pratama" },
  "created_at": "2026-11-06T13:15:00+07:00"
}
```

#### GET `/dashboard`

Isi menyesuaikan role (lihat bagian 2).
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "total_laporan": 27,
    "per_status": { "baru": 8, "diverifikasi": 6, "ditangani": 9, "selesai": 4 },
    "per_urgensi": { "rendah": 5, "sedang": 10, "tinggi": 9, "kritis": 3 },
    "per_kategori": [
      { "kategori": "Banjir", "total": 12 },
      { "kategori": "Kebakaran", "total": 7 }
    ],
    "laporan_terbaru": [ { "...Laporan (daftar)..." } ],
    "tugas_aktif": 2
  }
}
```
Semua kunci `per_status` dan `per_urgensi` selalu ada (nilai 0 jika kosong). `tugas_aktif` hanya terisi untuk relawan, untuk role lain `null`. `laporan_terbaru` berisi maksimal 5 data.

#### GET `/peta`

Query opsional: `status`, `urgensi`, `kategori_id`. Tanpa paginasi (ringan untuk marker).
```json
{
  "success": true,
  "message": "OK",
  "data": [
    { "id": 12, "judul": "Banjir setinggi 1 meter di Jalan Melati", "latitude": 0.5070677, "longitude": 101.4477930, "urgensi": "tinggi", "status": "diverifikasi", "kategori": "Banjir" }
  ]
}
```
Warna marker disarankan: rendah = hijau, sedang = kuning, tinggi = oranye, kritis = merah.

---

## 5. Aturan Skor Urgensi Otomatis (Fitur Unggulan, A3)

Dihitung di server saat laporan dibuat, tersimpan di kolom `urgensi`:

```
skor = bobot_urgensi(kategori)
     + 0  jika jumlah_terdampak 0-4
     + 1  jika 5-19
     + 2  jika 20-49
     + 3  jika 50 ke atas
     + 2  jika deskripsi memuat kata kunci: korban, terjebak, meluas, evakuasi, meninggal
```

| Skor | Urgensi |
|---|---|
| 0-2 | rendah |
| 3-4 | sedang |
| 5-6 | tinggi |
| 7 ke atas | kritis |

Petugas boleh mengubah hasilnya lewat `PATCH /laporan/{id}/urgensi`.
Angka bobot dan ambang di atas adalah usulan awal dan boleh disesuaikan A3, asalkan nilai enum tetap sama.

---

## 6. Akun Demo (dari seeder)

| Role | Email | Password |
|---|---|---|
| petugas | `petugas@demo.test` | `password` |
| relawan | `relawan@demo.test` | `password` |
| relawan | `relawan2@demo.test` | `password` |
| masyarakat | `masyarakat@demo.test` | `password` |

---

## 7. Catatan untuk Pengembang

**Backend**
- Semua respons lewat satu helper agar bentuk `{success, message, data}` seragam. Tangani pengecualian di `bootstrap/app.php` agar 401, 403, 404, 422 juga berformat sama.
- Pasang route di `routes/api.php`; grup `auth:sanctum` untuk semua selain register dan login.
- Jalankan `php artisan storage:link` dan kembalikan URL foto lengkap.

**Android**
- Satu pembungkus generik: `data class ApiResponse<T>(val success: Boolean, val message: String, val data: T?, val errors: Map<String, List<String>>?)`.
- Sebelum API siap, gunakan data dummy dengan **bentuk JSON persis seperti contoh di dokumen ini**, sehingga saat berpindah ke API nyata hanya base URL yang berubah.
- Tampilkan `message` dari server untuk error 403 dan 422 (tampilkan isi `errors` di bawah kolom yang bersangkutan).

**Perubahan dokumen**
- Ubah dokumen ini hanya lewat Pull Request atau setelah disepakati di grup. Tulis perubahan di bagian bawah:

| Tanggal | Perubahan | Oleh |
|---|---|---|
| 6 Nov 2026 | Versi awal | Anggota 1 |
