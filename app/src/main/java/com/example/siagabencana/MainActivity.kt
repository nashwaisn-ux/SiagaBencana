package com.example.siagabencana

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView

    private val red = Color.rgb(198, 40, 40)
    private val navy = Color.rgb(23, 50, 77)
    private val gray = Color.rgb(100, 110, 120)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.container)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> showHome()
                R.id.nav_laporan -> showReports()
                R.id.nav_relawan -> showVolunteers()
                R.id.nav_riwayat -> showHistory()
                R.id.nav_profil -> showProfile()
                else -> false
            }
        }

        showHome()
    }

    // =========================
    // KOMPONEN TAMPILAN
    // =========================

    private fun clearPage() {
        container.removeAllViews()
    }

    private fun addText(
        text: String,
        size: Float = 14f,
        color: Int = navy,
        bold: Boolean = false
    ) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
            setPadding(0, 8, 0, 8)
        }

        container.addView(textView)
    }

    private fun addTitle(text: String) {
        addText(text, 24f, navy, true)
    }

    private fun addSubtitle(text: String) {
        addText(text, 14f, gray)
    }

    private fun addCard(title: String, description: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.rgb(245, 247, 250))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(navy)
            setTypeface(null, Typeface.BOLD)
        }

        val descriptionView = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(gray)
            setPadding(0, 6, 0, 0)
        }

        card.addView(titleView)
        card.addView(descriptionView)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 8, 0, 8)
        }

        container.addView(card, params)
    }

    private fun addButton(text: String, action: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            setTextColor(white)
            setBackgroundColor(red)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 8, 0, 8)
        }

        container.addView(button, params)
    }

    // =========================
    // HALAMAN BERANDA
    // =========================

    private fun showHome(): Boolean {
        clearPage()

        addTitle("Halo, Rina!")
        addSubtitle("Selamat datang di SIAGABENCANA.")
        addSubtitle("Lapor cepat, tanggap bersama.")

        addText("Ringkasan Laporan", 18f, navy, true)

        addCard("12 Laporan", "Total laporan bencana yang tercatat.")
        addCard("5 Laporan Diproses", "Laporan sedang ditangani petugas.")
        addCard("7 Laporan Selesai", "Laporan yang telah ditindaklanjuti.")

        addText("Akses Cepat", 18f, navy, true)

        addButton("Buat Laporan") {
            showCreateReport()
        }

        addButton("Lihat Peta Bencana") {
            showMap()
        }

        addText("Laporan Terbaru", 18f, navy, true)

        addCard(
            "Banjir - Kelurahan A",
            "Status: Sedang ditangani"
        )

        addCard(
            "Pohon Tumbang - Kelurahan B",
            "Status: Menunggu petugas"
        )

        return true
    }

    // =========================
    // HALAMAN LAPORAN
    // =========================

    private fun showReports(): Boolean {
        clearPage()

        addTitle("Laporan Bencana")
        addSubtitle("Daftar laporan bencana masyarakat.")

        addButton("Buat Laporan Baru") {
            showCreateReport()
        }

        addCard(
            "Banjir di Kelurahan A",
            "Kategori: Banjir\nPrioritas: Tinggi\nStatus: Diproses"
        )

        addCard(
            "Kebakaran di Kelurahan B",
            "Kategori: Kebakaran\nPrioritas: Darurat\nStatus: Menunggu"
        )

        addCard(
            "Tanah Longsor di Kelurahan C",
            "Kategori: Longsor\nPrioritas: Sedang\nStatus: Selesai"
        )

        return true
    }

    // =========================
    // FORM BUAT LAPORAN
    // =========================

    private fun showCreateReport() {
        clearPage()

        addTitle("Buat Laporan")
        addSubtitle("Lengkapi informasi kejadian bencana.")

        addText("Judul Laporan", 14f, navy, true)
        addInput("Masukkan judul laporan")

        addText("Jenis Bencana", 14f, navy, true)
        addInput("Contoh: Banjir, Kebakaran, Longsor")

        addText("Tingkat Urgensi", 14f, navy, true)
        addInput("Contoh: Rendah, Sedang, Tinggi")

        addText("Deskripsi Kejadian", 14f, navy, true)
        addInput("Jelaskan kondisi kejadian")

        addText("Lokasi Kejadian", 14f, navy, true)
        addInput("Masukkan lokasi kejadian")

        addButton("Ambil Lokasi GPS") {
            Toast.makeText(
                this,
                "Simulasi pengambilan lokasi GPS",
                Toast.LENGTH_SHORT
            ).show()
        }

        addButton("Pilih Foto") {
            Toast.makeText(
                this,
                "Simulasi memilih foto",
                Toast.LENGTH_SHORT
            ).show()
        }

        addButton("Kirim Laporan") {
            Toast.makeText(
                this,
                "Laporan berhasil disimulasikan!",
                Toast.LENGTH_SHORT
            ).show()

            bottomNavigation.selectedItemId = R.id.nav_laporan
        }

        addButton("Kembali") {
            showReports()
        }
    }

    private fun addInput(hintText: String) {
        val input = android.widget.EditText(this).apply {
            hint = hintText
            textSize = 14f
            setSingleLine(false)
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(245, 247, 250))
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 4, 0, 12)
        }

        container.addView(input, params)
    }

    // =========================
    // HALAMAN PETA
    // =========================

    private fun showMap() {
        clearPage()

        addTitle("Peta Bencana")
        addSubtitle("Informasi lokasi kejadian bencana.")

        addCard(
            "Peta Lokasi",
            "Tampilan peta akan ditampilkan pada bagian ini."
        )

        addCard(
            "Radius Peringatan",
            "Jangkauan informasi bencana: 3 km"
        )

        addButton("SOS / Bantuan Darurat") {
            Toast.makeText(
                this,
                "Simulasi permintaan bantuan darurat",
                Toast.LENGTH_SHORT
            ).show()
        }

        addButton("Kembali ke Beranda") {
            bottomNavigation.selectedItemId = R.id.nav_beranda
        }
    }

    // =========================
    // HALAMAN RELAWAN
    // =========================

    private fun showVolunteers(): Boolean {
        clearPage()

        addTitle("Relawan")
        addSubtitle("Informasi relawan dan penugasan.")

        addCard(
            "Andi Saputra",
            "Status: Aktif\nTugas: Penanganan banjir"
        )

        addCard(
            "Budi Santoso",
            "Status: Aktif\nTugas: Evakuasi warga"
        )

        addCard(
            "Siti Rahma",
            "Status: Tidak bertugas"
        )

        return true
    }

    // =========================
    // HALAMAN RIWAYAT
    // =========================

    private fun showHistory(): Boolean {
        clearPage()

        addTitle("Riwayat Aktivitas")
        addSubtitle("Catatan aktivitas dan laporan sebelumnya.")

        addCard(
            "Laporan Banjir",
            "08 November 2026\nStatus: Selesai"
        )

        addCard(
            "Laporan Kebakaran",
            "06 November 2026\nStatus: Diproses"
        )

        addCard(
            "Laporan Tanah Longsor",
            "02 November 2026\nStatus: Selesai"
        )

        return true
    }

    // =========================
    // HALAMAN PROFIL
    // =========================

    private fun showProfile(): Boolean {
        clearPage()

        addTitle("Profil Pengguna")

        addCard(
            "Rina Anggraini",
            "Masyarakat\nrina@email.com"
        )

        addText("Informasi Akun", 18f, navy, true)

        addCard("Nama Lengkap", "Rina Anggraini")
        addCard("Email", "rina@email.com")
        addCard("Peran", "Masyarakat")

        addButton("Edit Profil") {
            Toast.makeText(
                this,
                "Simulasi edit profil",
                Toast.LENGTH_SHORT
            ).show()
        }

        addButton("Keluar") {
            Toast.makeText(
                this,
                "Simulasi keluar akun",
                Toast.LENGTH_SHORT
            ).show()
        }

        return true
    }
}