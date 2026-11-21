package com.example.siagabencana.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Map<String, List<String>>? = null
)

data class PaginatedData<T>(
    val items: List<T>,
    val meta: PaginationMeta
)

data class PaginationMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int
)

data class User(
    val id: Int,
    val nama: String,
    val email: String? = null,
    val role: String? = null,
    val no_hp: String? = null
)

data class AuthData(
    val token: String,
    val user: User
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val nama: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
    val no_hp: String? = null
)

data class Kategori(
    val id: Int,
    val nama: String
)

data class UserSummary(
    val id: Int,
    val nama: String
)

data class Relawan(
    val id: Int,
    val nama: String,
    val no_hp: String? = null,
    val tugas_aktif: Int = 0
)

data class Laporan(
    val id: Int,
    val judul: String,
    val deskripsi: String,
    val foto_url: String? = null,
    val latitude: Double,
    val longitude: Double,
    val jumlah_terdampak: Int = 0,
    val urgensi: String,
    val status: String,
    val kategori: Kategori? = null,
    val pelapor: UserSummary? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val penugasan: List<Penugasan>? = null,
    val riwayat: List<RiwayatStatus>? = null
)

data class Penugasan(
    val id: Int,
    val status_tugas: String,
    val catatan: String? = null,
    val foto_bukti_url: String? = null,
    val created_at: String? = null,
    val relawan: UserSummary? = null,
    val petugas: UserSummary? = null,
    val laporan: LaporanSummary? = null
)

data class LaporanSummary(
    val id: Int,
    val judul: String,
    val urgensi: String? = null,
    val status: String? = null
)

data class RiwayatStatus(
    val id: Int,
    val laporan: LaporanSummary? = null,
    val status_lama: String? = null,
    val status_baru: String? = null,
    val keterangan: String? = null,
    val user: UserSummary? = null,
    val created_at: String? = null
)

data class DashboardData(
    val total_laporan: Int,
    val per_status: Map<String, Int>,
    val per_urgensi: Map<String, Int>,
    val per_kategori: List<KategoriStat>,
    val laporan_terbaru: List<Laporan>,
    val tugas_aktif: Int? = null
)

data class KategoriStat(
    val kategori: String,
    val total: Int
)

data class MapPoint(
    val id: Int,
    val judul: String,
    val latitude: Double,
    val longitude: Double,
    val urgensi: String,
    val status: String,
    val kategori: String? = null
)

data class TugaskanRequest(
    val relawan_id: Int,
    val catatan: String? = null
)

data class UpdateStatusRequest(
    val status: String,
    val keterangan: String? = null
)

data class UpdateUrgensiRequest(
    val urgensi: String,
    val keterangan: String? = null
)

data class BulkDeleteRequest(
    val ids: List<Int>
)

data class UpdatePenugasanStatusRequest(
    val status_tugas: String
)
