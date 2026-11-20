package com.example.siagabencana.model

import com.example.siagabencana.data.SessionManager

enum class Urgensi { RENDAH, SEDANG, TINGGI, KRITIS }
enum class StatusLaporan { BARU, DIPROSES, SELESAI }

data class Laporan(
    val id: Int,
    val judul: String,
    val jenis: String,
    val urgensi: Urgensi,
    val status: StatusLaporan,
    val deskripsi: String,
    val lat: Double,
    val lng: Double,
    val waktu: String,
    val fotoUrl: String? = null,
    val milikSaya: Boolean = false,
)

data class DashboardUi(
    val baru: Int,
    val diproses: Int,
    val selesai: Int,
    val relawanAktif: Int,
    val terbaru: List<Laporan>,
    val siagaJudul: String? = null,   // null = sembunyikan banner siaga
    val siagaDesc: String? = null,
)

object DummyData {
    val laporan = listOf(
        Laporan(
            1, "Banjir meluap di permukiman", "Banjir", Urgensi.KRITIS, StatusLaporan.BARU,
            "Air setinggi lutut masuk ke rumah warga sejak subuh. Butuh perahu karet dan dapur umum.",
            -0.5071, 101.4478, "20 menit lalu", milikSaya = true
        ),
        Laporan(
            2, "Kebakaran lahan kosong", "Kebakaran", Urgensi.TINGGI, StatusLaporan.DIPROSES,
            "Api menyebar ke semak kering di dekat permukiman.", -0.5102, 101.4410, "1 jam lalu"
        ),
        Laporan(
            3, "Tanah longsor tutup jalan", "Longsor", Urgensi.TINGGI, StatusLaporan.DIPROSES,
            "Jalan penghubung desa tertutup material longsor.", -0.5200, 101.4600, "3 jam lalu"
        ),
        Laporan(
            4, "Sampah menumpuk di sungai", "Lainnya", Urgensi.RENDAH, StatusLaporan.BARU,
            "Aliran sungai tersumbat sampah rumah tangga.", -0.5000, 101.4300, "5 jam lalu", milikSaya = true
        ),
        Laporan(
            5, "Pohon tumbang halangi jalan", "Lainnya", Urgensi.SEDANG, StatusLaporan.SELESAI,
            "Pohon tumbang sudah dipotong dan dibersihkan relawan.", -0.4950, 101.4550, "1 hari lalu"
        ),
    )

    /** Data yang boleh dilihat tiap role. TODO Anggota 5: server yang memfilter (GET /api/laporan memakai token). */
    fun untuk(role: String): List<Laporan> = when (role) {
        SessionManager.ROLE_MASYARAKAT -> laporan.filter { it.milikSaya }
        SessionManager.ROLE_RELAWAN -> laporan.filter { it.status != StatusLaporan.SELESAI }
        else -> laporan.sortedByDescending { it.urgensi.ordinal }
    }

    fun dashboard(role: String): DashboardUi = when (role) {
        SessionManager.ROLE_MASYARAKAT -> DashboardUi(
            baru = 1, diproses = 0, selesai = 1, relawanAktif = 24,
            terbaru = untuk(role).take(3),
            siagaJudul = "Siaga banjir radius 3 km",
            siagaDesc = "2 laporan kritis dekat lokasi Anda. Buka peta siaga.",
        )
        SessionManager.ROLE_RELAWAN -> DashboardUi(
            baru = 5, diproses = 2, selesai = 14, relawanAktif = 24,
            terbaru = untuk(role).take(3),
            siagaJudul = "3 tugas dalam radius 5 km",
            siagaDesc = "Laporan kritis menunggu relawan. Buka peta untuk rute.",
        )
        else -> DashboardUi(
            baru = 12, diproses = 7, selesai = 38, relawanAktif = 24,
            terbaru = untuk(role).take(3),
            siagaJudul = "2 laporan kritis belum ditangani",
            siagaDesc = "Tugaskan relawan atau ubah status di tab Laporan.",
        )
    }
}