package com.example.siagabencana.model

enum class Urgensi { RENDAH, SEDANG, TINGGI, KRITIS }
enum class StatusLaporan { BARU, DIVERIFIKASI, DITANGANI, SELESAI }

data class Laporan(
    val id: Int,
    val judul: String? = null,
    val deskripsi: String? = null,
    val foto_url: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val jumlah_terdampak: Int = 0,
    val urgensi: String? = null,
    val status: String? = null,
    val kategori: Kategori? = null,
    val pelapor: UserSummary? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val penugasan: List<Penugasan>? = null,
    val riwayat: List<RiwayatStatus>? = null
)
