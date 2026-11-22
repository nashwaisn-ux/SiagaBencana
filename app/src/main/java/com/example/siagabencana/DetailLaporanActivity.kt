package com.example.siagabencana

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.model.TugaskanRequest
import com.example.siagabencana.model.UpdatePenugasanStatusRequest
import com.example.siagabencana.model.UpdateStatusRequest
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.ui.applyUrgensi
import com.example.siagabencana.ui.formatTanggal
import com.example.siagabencana.ui.toUrgensi
import com.example.siagabencana.ui.toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class DetailLaporanActivity : AppCompatActivity() {

    private val session by lazy { SessionManager(this) }
    private val role by lazy { session.role }
    private var laporanId: Int = -1
    private var currentLaporan: Laporan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            when (role) {
                SessionManager.ROLE_PETUGAS -> R.layout.activity_detail_laporan_petugas
                SessionManager.ROLE_RELAWAN -> R.layout.activity_detail_laporan_relawan
                else -> R.layout.activity_detail_laporan_masyarakat
            }
        )

        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        laporanId = intent.getIntExtra(EXTRA_ID, -1)
        if (laporanId == -1) {
            toast("Laporan tidak ditemukan")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (laporanId != -1) {
            loadDetail()
        }
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).getDetailLaporan(laporanId)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val l = resp.body()?.data
                    if (l != null) {
                        currentLaporan = l
                        bindLaporanData(l)
                    } else {
                        toast("Data laporan tidak ditemukan")
                        finish()
                    }
                } else {
                    toast(resp.body()?.message ?: "Gagal memuat detail laporan")
                    finish()
                }
            } catch (_: Exception) {
                toast("Tidak dapat terhubung ke server")
                finish()
            }
        }
    }

    private fun bindLaporanData(l: Laporan) {
        findViewById<TextView>(R.id.tvJudul)?.text = l.judul ?: "Bencana"
        findViewById<TextView>(R.id.tvDeskripsi)?.text = l.deskripsi ?: "-"
        findViewById<TextView>(R.id.tvBadgeUrgensi)?.applyUrgensi(l.urgensi.toUrgensi())
        findViewById<TextView>(R.id.tvBadgeJenis)?.text = l.kategori?.nama ?: "Bencana"
        findViewById<TextView>(R.id.tvKoordinat)?.text = String.format(Locale.US, "%.4f, %.4f", l.latitude, l.longitude)

        val pelaporNama = l.pelapor?.nama ?: "Warga"
        findViewById<TextView>(R.id.tvPelapor)?.text = "Dilaporkan oleh $pelaporNama · ${formatTanggal(l.created_at)}"

        // Map Click Listener
        findViewById<View>(R.id.tvBukaPeta)?.setOnClickListener {
            val intent = Intent(this, PetaSiagaActivity::class.java).apply {
                putExtra(PetaSiagaActivity.EXTRA_LAT, l.latitude)
                putExtra(PetaSiagaActivity.EXTRA_LNG, l.longitude)
                putExtra(PetaSiagaActivity.EXTRA_JUDUL, l.judul ?: "Bencana")
            }
            startActivity(intent)
        }

        bindTimeline(l)
        bindRelawanCard(l)
        setupFoto(l)
        setupAksi(l)
    }

    private fun bindTimeline(l: Laporan) {
        val tvWaktuDiterima = findViewById<TextView>(R.id.tvWaktuDiterima)
        val ivStep2 = findViewById<ImageView>(R.id.ivStep2)
        val tvStep2Judul = findViewById<TextView>(R.id.tvStep2Judul)
        val tvKeteranganProses = findViewById<TextView>(R.id.tvKeteranganProses)
        val ivStep3 = findViewById<ImageView>(R.id.ivStep3)
        val tvStep3Judul = findViewById<TextView>(R.id.tvStep3Judul)

        tvWaktuDiterima?.text = formatTanggal(l.created_at)

        val st = l.status?.lowercase().orEmpty()
        when (st) {
            "diverifikasi" -> {
                tvStep2Judul?.text = "Diverifikasi petugas"
                tvStep2Judul?.setTextColor(ContextCompat.getColor(this, R.color.badge_high_text))
                ivStep2?.setImageResource(R.drawable.ic_check)
                ivStep2?.setBackgroundResource(R.drawable.bg_circle_orange)
            }
            "ditangani" -> {
                tvStep2Judul?.text = "Sedang ditangani"
                tvStep2Judul?.setTextColor(ContextCompat.getColor(this, R.color.badge_high_text))
                ivStep2?.setImageResource(R.drawable.ic_check)
                ivStep2?.setBackgroundResource(R.drawable.bg_circle_orange)
            }
            "selesai" -> {
                tvStep2Judul?.text = "Penanganan dilakukan"
                tvStep2Judul?.setTextColor(ContextCompat.getColor(this, R.color.badge_done_text))
                ivStep2?.setImageResource(R.drawable.ic_check)
                ivStep2?.setBackgroundResource(R.drawable.bg_circle_green)
            }
            else -> { // "baru"
                tvStep2Judul?.text = "Menunggu verifikasi"
                tvStep2Judul?.setTextColor(ContextCompat.getColor(this, R.color.siaga_text_secondary))
                ivStep2?.setImageDrawable(null)
                ivStep2?.setBackgroundResource(R.drawable.bg_circle_outline)
            }
        }

        val latestRiwayat = l.riwayat?.lastOrNull { it.status_baru == l.status }
        val ket = latestRiwayat?.keterangan
        if (!ket.isNullOrBlank()) {
            tvKeteranganProses?.text = ket
            tvKeteranganProses?.isVisible = true
        } else {
            tvKeteranganProses?.isVisible = false
        }

        if (st == "selesai") {
            ivStep3?.setImageResource(R.drawable.ic_check)
            ivStep3?.setBackgroundResource(R.drawable.bg_circle_green)
            tvStep3Judul?.setTextColor(ContextCompat.getColor(this, R.color.badge_done_text))
        } else {
            ivStep3?.setImageDrawable(null)
            ivStep3?.setBackgroundResource(R.drawable.bg_circle_outline)
            tvStep3Judul?.setTextColor(ContextCompat.getColor(this, R.color.siaga_text_secondary))
        }
    }

    private fun bindRelawanCard(l: Laporan) {
        val tvNamaRelawan = findViewById<TextView>(R.id.tvNamaRelawan)
        val tvInisialRelawan = findViewById<TextView>(R.id.tvInisialRelawan)
        val tvInfoRelawan = findViewById<TextView>(R.id.tvInfoRelawan)

        val latestPenugasan = l.penugasan?.lastOrNull()
        if (latestPenugasan != null && latestPenugasan.relawan != null) {
            val rNama = latestPenugasan.relawan.nama
            tvNamaRelawan?.text = rNama
            tvInfoRelawan?.text = "Status tugas: ${latestPenugasan.status_tugas}"

            val inisial = rNama.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.let { parts ->
                when {
                    parts.isEmpty() -> "RL"
                    parts.size == 1 -> parts[0].take(2).uppercase()
                    else -> "${parts.first().first()}${parts.last().first()}".uppercase()
                }
            }
            tvInisialRelawan?.text = inisial
        } else {
            tvNamaRelawan?.text = "Belum ada relawan ditugaskan"
            tvInisialRelawan?.text = "-"
            tvInfoRelawan?.text = "Status tugas: - "
        }
    }

    private fun setupFoto(l: Laporan) {
        val photos = mutableListOf<String>()
        if (!l.foto_url.isNullOrBlank()) {
            photos.add(l.foto_url)
        }

        val tvIndex = findViewById<TextView>(R.id.tvIndexFoto)
        val pager = findViewById<ViewPager2>(R.id.vpFoto)

        if (photos.isEmpty()) {
            tvIndex?.text = "0 / 0 foto bukti"
            pager?.adapter = PhotoAdapter(emptyList())
        } else {
            pager?.adapter = PhotoAdapter(photos)
            tvIndex?.text = "1 / ${photos.size} foto bukti"
            pager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    tvIndex?.text = "${position + 1} / ${photos.size} foto bukti"
                }
            })
        }
    }

    private fun setupAksi(l: Laporan) {
        val bar = findViewById<View>(R.id.bottomBar)
        val btnTugaskan = findViewById<MaterialButton>(R.id.btnTugaskan)

        when (role) {
            SessionManager.ROLE_PETUGAS -> {
                bar?.isVisible = true
                btnTugaskan?.isVisible = l.status != "selesai"

                findViewById<View>(R.id.btnStatus)?.setOnClickListener { pilihStatusPetugas(l) }
                findViewById<View>(R.id.btnHapus)?.setOnClickListener { konfirmasiHapusPetugas() }
                btnTugaskan?.setOnClickListener { tugaskanRelawanDialog() }
            }
            SessionManager.ROLE_RELAWAN -> {
                bar?.isVisible = l.status != "selesai"
                findViewById<View>(R.id.btnPeta)?.setOnClickListener {
                    val intent = Intent(this, PetaSiagaActivity::class.java).apply {
                        putExtra(PetaSiagaActivity.EXTRA_LAT, l.latitude)
                        putExtra(PetaSiagaActivity.EXTRA_LNG, l.longitude)
                        putExtra(PetaSiagaActivity.EXTRA_JUDUL, l.judul ?: "Bencana")
                    }
                    startActivity(intent)
                }
                val btnAmbil = findViewById<MaterialButton>(R.id.btnAmbil)
                val myPenugasan = l.penugasan?.firstOrNull { it.relawan?.id == session.getUserId() }

                if (myPenugasan != null) {
                    if (myPenugasan.status_tugas == "ditugaskan") {
                        btnAmbil?.text = "Terima Tugas"
                        btnAmbil?.setOnClickListener { terimaTugasRelawan(myPenugasan.id) }
                    } else if (myPenugasan.status_tugas == "diterima") {
                        btnAmbil?.text = "Selesaikan Tugas"
                        btnAmbil?.setOnClickListener { selesaikanTugasRelawan(myPenugasan.id) }
                    } else {
                        btnAmbil?.text = "Tugas Selesai"
                        btnAmbil?.isEnabled = false
                    }
                } else {
                    btnAmbil?.text = "Bukan Tugas Anda"
                    btnAmbil?.isEnabled = false
                }
            }
            else -> {
                // Masyarakat: hanya mengelola miliknya jika berstatus 'baru'
                val isMyReport = l.pelapor?.id == session.getUserId()
                val isBaru = l.status == "baru" || l.status == "BARU"
                bar?.isVisible = isMyReport && isBaru

                findViewById<View>(R.id.btnEdit)?.setOnClickListener {
                    val intent = Intent(this, BuatLaporanActivity::class.java).apply {
                        putExtra(EXTRA_ID, l.id)
                    }
                    startActivity(intent)
                }
                findViewById<View>(R.id.btnHapus)?.setOnClickListener { konfirmasiHapusMasyarakat() }
            }
        }
    }

    private fun pilihStatusPetugas(l: Laporan) {
        val options = arrayOf("baru", "diverifikasi", "ditangani", "selesai")
        val labels = arrayOf("Baru", "Diverifikasi", "Ditangani", "Selesai")
        val currentIdx = options.indexOf(l.status?.lowercase() ?: "baru").coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("Ubah status laporan")
            .setSingleChoiceItems(labels, currentIdx, null)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { dialog, _ ->
                val selected = (dialog as AlertDialog).listView.checkedItemPosition
                val newStatus = options[selected]
                updateStatusApi(newStatus)
            }
            .show()
    }

    private fun updateStatusApi(status: String) {
        lifecycleScope.launch {
            try {
                val req = UpdateStatusRequest(status = status)
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).updateStatusLaporan(laporanId, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    toast("Status berhasil diubah menjadi $status")
                    setResult(RESULT_OK)
                    loadDetail()
                } else {
                    toast(resp.body()?.message ?: "Gagal mengubah status")
                }
            } catch (_: Exception) {
                toast("Gagal terhubung ke server")
            }
        }
    }

    private fun konfirmasiHapusPetugas() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus laporan ini?")
            .setMessage("Laporan akan dihapus permanen dari sistem.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                deleteLaporanApi()
            }
            .show()
    }

    private fun konfirmasiHapusMasyarakat() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus laporan Anda?")
            .setMessage("Laporan Anda akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                deleteLaporanApi()
            }
            .show()
    }

    private fun deleteLaporanApi() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).deleteLaporan(laporanId)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    toast("Laporan berhasil dihapus")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    toast(resp.body()?.message ?: "Gagal menghapus laporan")
                }
            } catch (_: Exception) {
                toast("Gagal terhubung ke server")
            }
        }
    }

    private fun tugaskanRelawanDialog() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).getRelawan()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    // Sort relawan by tugas_aktif ascending (terkecil di atas)
                    val relawans = resp.body()?.data.orEmpty().sortedBy { it.tugas_aktif }
                    if (relawans.isEmpty()) {
                        toast("Belum ada relawan terdaftar")
                        return@launch
                    }

                    val names = relawans.map { "${it.nama} (tugas aktif: ${it.tugas_aktif})" }.toTypedArray()
                    MaterialAlertDialogBuilder(this@DetailLaporanActivity)
                        .setTitle("Tugaskan Relawan")
                        .setItems(names) { _, which ->
                            val selectedRelawan = relawans[which]
                            submitPenugasan(selectedRelawan.id)
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                } else {
                    toast("Gagal memuat daftar relawan")
                }
            } catch (_: Exception) {
                toast("Gagal terhubung ke server")
            }
        }
    }

    private fun submitPenugasan(relawanId: Int) {
        lifecycleScope.launch {
            try {
                val req = TugaskanRequest(relawan_id = relawanId, catatan = "Penugasan lapangan")
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).tugaskanRelawan(laporanId, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    toast("Relawan berhasil ditugaskan")
                    setResult(RESULT_OK)
                    loadDetail()
                } else {
                    toast(resp.body()?.message ?: "Relawan sudah ditugaskan pada laporan ini")
                }
            } catch (_: Exception) {
                toast("Gagal terhubung ke server")
            }
        }
    }

    private fun terimaTugasRelawan(penugasanId: Int) {
        lifecycleScope.launch {
            try {
                val req = UpdatePenugasanStatusRequest(status_tugas = "diterima")
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).updateStatusPenugasan(penugasanId, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    toast("Tugas diterima")
                    updateStatusApi("ditangani")
                } else {
                    toast(resp.body()?.message ?: "Gagal menerima tugas")
                }
            } catch (_: Exception) {
                toast("Gagal terhubung ke server")
            }
        }
    }

    private fun selesaikanTugasRelawan(penugasanId: Int) {
        lifecycleScope.launch {
            try {
                val req = UpdatePenugasanStatusRequest(status_tugas = "selesai")
                val resp = RetrofitClient.getApiService(this@DetailLaporanActivity).updateStatusPenugasan(penugasanId, req)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    toast("Tugas selesai")
                    updateStatusApi("selesai")
                } else {
                    toast(resp.body()?.message ?: "Gagal menyelesaikan tugas")
                }
            } catch (_: Exception) {
                toast("Gagal terhubung ke server")
            }
        }
    }

    private class PhotoAdapter(private val photos: List<String>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = if (photos.isEmpty()) 1 else photos.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val img = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(ContextCompat.getColor(parent.context, R.color.siaga_thumb))
            }
            return object : RecyclerView.ViewHolder(img) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val img = holder.itemView as ImageView
            if (photos.isNotEmpty()) {
                Glide.with(img.context)
                    .load(photos[position])
                    .placeholder(R.color.siaga_thumb)
                    .into(img)
            } else {
                img.setImageResource(R.drawable.bg_card)
            }
        }
    }

    companion object {
        const val EXTRA_ID = "laporan_id"
    }
}
