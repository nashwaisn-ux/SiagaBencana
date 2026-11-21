package com.example.siagabencana

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.siagabencana.model.DummyData
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.model.StatusLaporan
import com.example.siagabencana.ui.applyUrgensi
import com.example.siagabencana.ui.toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class DetailLaporanActivity : AppCompatActivity() {

    private val currentRole: String by lazy { bacaRoleAktif() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            when (currentRole) {
                ROLE_PETUGAS -> R.layout.activity_detail_laporan_petugas
                ROLE_RELAWAN -> R.layout.activity_detail_laporan_relawan
                else -> R.layout.activity_detail_laporan_masyarakat
            }
        )

        // TODO Anggota 5: ganti pencarian dummy dengan GET /api/laporan/{id}.
        val id = intent.getIntExtra(EXTRA_ID, -1)
        val l = DummyData.laporan.firstOrNull { it.id == id } ?: DummyData.laporan.first()

        findViewById<TextView>(R.id.tvJudul).text = l.judul
        findViewById<TextView>(R.id.tvDeskripsi).text = l.deskripsi
        findViewById<TextView>(R.id.tvBadgeUrgensi).applyUrgensi(l.urgensi)
        findViewById<TextView>(R.id.tvBadgeJenis).text = l.jenis
        findViewById<TextView>(R.id.tvKoordinat).text = String.format(Locale.US, "%.4f, %.4f", l.lat, l.lng)
        findViewById<TextView>(R.id.tvPelapor).text = "Dilaporkan oleh warga · ${l.waktu}"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        setupFoto()
        setupAksi(l)
    }

    private fun bacaRoleAktif(): String {
        // Sesuaikan jika di SessionManager.kt sudah ada properti/method role yang pasti.
        val prefs = getSharedPreferences("siaga_session", Context.MODE_PRIVATE)
        val rawRole = prefs.getString("role", "")
            ?: prefs.getString("user_role", "")
            ?: ""
        return when (rawRole.trim().lowercase(Locale.ROOT)) {
            "petugas", "role_petugas" -> ROLE_PETUGAS
            "relawan", "role_relawan" -> ROLE_RELAWAN
            else -> ROLE_MASYARAKAT
        }
    }

    private fun setupFoto() {
        // Foto bukti: sementara satu placeholder. Anggota 5 mengisi dari URL foto API.
        val jumlah = 1
        val tvIndex = findViewById<TextView>(R.id.tvIndexFoto)
        val pager = findViewById<ViewPager2>(R.id.vpFoto)
        pager.adapter = PlaceholderPhotoAdapter(jumlah)
        tvIndex.text = "1 / $jumlah foto bukti"
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tvIndex.text = "${position + 1} / $jumlah foto bukti"
            }
        })
    }

    private fun setupAksi(l: Laporan) {
        val bar = findViewById<View>(R.id.bottomBar)
        when (currentRole) {
            ROLE_PETUGAS -> {
                findViewById<View>(R.id.btnStatus).setOnClickListener { pilihStatus(l) }
                findViewById<View>(R.id.btnHapus).setOnClickListener { konfirmasiHapus() }
                hubungkanTombolHubungiJikaAda(l)
            }
            ROLE_RELAWAN -> {
                bar.isVisible = l.status != StatusLaporan.SELESAI
                findViewById<View>(R.id.btnPeta).setOnClickListener {
                    startActivity(Intent(this, PetaSiagaActivity::class.java))
                }
                val ambil = findViewById<MaterialButton>(R.id.btnAmbil)
                ambil.setOnClickListener {
                    // TODO Anggota 5: PATCH /api/laporan/{id}/ambil
                    toast("Tugas diambil (demo)")
                    ambil.isEnabled = false
                    ambil.text = "Tugas diambil"
                }
                hubungkanTombolHubungiJikaAda(l)
            }
            else -> {
                // Masyarakat hanya mengelola laporannya sendiri yang masih berstatus Baru.
                bar.isVisible = l.milikSaya && l.status == StatusLaporan.BARU
                findViewById<View>(R.id.btnEdit).setOnClickListener {
                    startActivity(Intent(this, BuatLaporanActivity::class.java))
                }
                findViewById<View>(R.id.btnHapus).setOnClickListener { konfirmasiHapus() }
            }
        }
    }

    private fun hubungkanTombolHubungiJikaAda(l: Laporan) {
        // Cari ID tombol hubungi secara dinamis agar aman walau ID di XML berbeda atau belum dibuat.
        val candidateIds = listOf("btnHubungi", "btnHubungiRelawan", "btnChat", "btnHubungiTim")
        val tombol = candidateIds
            .mapNotNull { namaId ->
                val resId = resources.getIdentifier(namaId, "id", packageName)
                if (resId != 0) findViewById<View?>(resId) else null
            }
            .firstOrNull()

        tombol?.setOnClickListener {
            val intent = Intent(this, HubungiRelawanActivity::class.java).apply {
                putExtra("EXTRA_ROLE", currentRole)
                putExtra("EXTRA_ID_LAPORAN", l.id)
                putExtra(
                    "EXTRA_NAMA_RELAWAN",
                    if (currentRole == ROLE_RELAWAN) {
                        "Posko / Tim Koordinasi"
                    } else {
                        "Raka • Relawan"
                    }
                )
                putExtra(
                    "EXTRA_STATUS_RELAWAN",
                    "Laporan #${l.id} • ${l.jenis}"
                )
            }
            startActivity(intent)
        }
    }

    private fun pilihStatus(l: Laporan) {
        val label = arrayOf("Baru", "Diproses", "Selesai")
        MaterialAlertDialogBuilder(this)
            .setTitle("Ubah status laporan")
            .setSingleChoiceItems(label, l.status.ordinal, null)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { d, _ ->
                val pilih = (d as AlertDialog).listView.checkedItemPosition
                // TODO Anggota 5: PATCH /api/laporan/{id} body status
                toast("Status diubah menjadi ${label[pilih]} (demo)")
            }
            .show()
    }

    private fun konfirmasiHapus() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus laporan ini?")
            .setMessage("Laporan dan foto buktinya akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                // TODO Anggota 5: DELETE /api/laporan/{id}
                toast("Laporan dihapus (demo)")
                finish()
            }
            .show()
    }

    private class PlaceholderPhotoAdapter(private val count: Int) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = count
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val img = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(ContextCompat.getColor(parent.context, R.color.siaga_thumb))
                contentDescription = "Foto bukti laporan"
            }
            return object : RecyclerView.ViewHolder(img) {}
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    }

    companion object {
        const val EXTRA_ID = "laporan_id"
        const val ROLE_PETUGAS = "petugas"
        const val ROLE_RELAWAN = "relawan"
        const val ROLE_MASYARAKAT = "masyarakat"
    }
}