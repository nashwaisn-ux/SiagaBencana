package com.example.siagabencana

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siagabencana.databinding.ActivityHubungiRelawanBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HubungiRelawanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHubungiRelawanBinding
    private lateinit var adapter: ChatRelawanAdapter
    private val daftarPesan = mutableListOf<PesanRelawan>()

    private var rolePengguna: String = ""
    private var idLaporan: Int = -1
    private var namaLawanBicara: String = "Raka • Relawan"
    private var labelPengirimSaya: String = "Saya"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHubungiRelawanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ambilDataIntent()
        setupToolbar()
        setupRecyclerView()
        setupQuickReply()
        setupActions()
        muatPesanAwal()
    }

    private fun bacaRoleAktif(): String {
        val prefs = getSharedPreferences("siaga_session", Context.MODE_PRIVATE)
        val rawRole = prefs.getString("role", "")
            ?: prefs.getString("user_role", "")
            ?: ""
        return when (rawRole.trim().lowercase(Locale.ROOT)) {
            "petugas", "role_petugas" -> ROLE_PETUGAS
            "relawan", "role_relawan" -> ROLE_RELAWAN
            else -> ""
        }
    }

    private fun ambilDataIntent() {
        rolePengguna = intent.getStringExtra("EXTRA_ROLE")
            ?.takeIf { it.isNotBlank() }
            ?: bacaRoleAktif()
        idLaporan = intent.getIntExtra("EXTRA_ID_LAPORAN", -1)

        labelPengirimSaya = when (rolePengguna.lowercase(Locale.ROOT)) {
            ROLE_PETUGAS -> "Petugas"
            ROLE_RELAWAN -> "Relawan"
            else -> "Saya"
        }

        val namaExtra = intent.getStringExtra("EXTRA_NAMA_RELAWAN")
        val statusExtra = intent.getStringExtra("EXTRA_STATUS_RELAWAN")

        if (!namaExtra.isNullOrBlank()) {
            namaLawanBicara = namaExtra
            binding.tvNamaRelawan.text = namaExtra
            binding.tvInisialRelawan.text = buatInisial(namaExtra)
        }

        if (!statusExtra.isNullOrBlank()) {
            binding.tvStatusRelawan.text = statusExtra
        }

        if (idLaporan != -1) {
            binding.tvInfoKoordinasi.text =
                "Koordinasi Laporan #$idLaporan — sertakan patokan jalan atau titik lokasi agar respons lebih cepat."
        }
    }

    private fun buatInisial(nama: String): String {
        val bersih = nama.substringBefore("•").trim()
        val bagian = bersih.split(" ").filter { it.isNotBlank() }
        return when {
            bagian.size >= 2 -> "${bagian[0].first().uppercaseChar()}${bagian[1].first().uppercaseChar()}"
            bagian.size == 1 -> bagian[0].take(2).uppercase()
            else -> "RL"
        }
    }

    private fun setupToolbar() {
        binding.toolbarRelawan.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatRelawanAdapter(daftarPesan)
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@HubungiRelawanActivity).apply {
                stackFromEnd = true
            }
            adapter = this@HubungiRelawanActivity.adapter
        }
    }

    private fun setupQuickReply() {
        binding.chipEvakuasi.setOnClickListener {
            kirimPesan("Butuh evakuasi segera")
        }
        binding.chipLokasiAman.setOnClickListener {
            kirimPesan("Posisi di titik aman")
        }
        binding.chipLogistik.setOnClickListener {
            kirimPesan("Butuh air & P3K")
        }
    }

    private fun setupActions() {
        binding.btnKirim.setOnClickListener {
            val teks = binding.etPesan.text?.toString()?.trim().orEmpty()
            if (teks.isEmpty()) {
                Toast.makeText(this, "Silakan ketik pesan terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            kirimPesan(teks)
            binding.etPesan.text?.clear()
        }

        binding.btnKirimLokasi.setOnClickListener {
            val infoLaporan = if (idLaporan != -1) " (Laporan #$idLaporan)" else ""
            kirimPesan("Titik lokasi saya$infoLaporan: sertakan patokan jalan terdekat")
        }
    }

    private fun muatPesanAwal() {
        daftarPesan.clear()
        daftarPesan.addAll(
            listOf(
                PesanRelawan(
                    pengirim = namaLawanBicara,
                    isi = "Halo, siap berkoordinasi untuk penanganan laporan ini. Bagaimana kondisi terkini di lapangan?",
                    waktu = "15:45",
                    isDariSaya = false
                ),
                PesanRelawan(
                    pengirim = labelPengirimSaya,
                    isi = "Air mulai naik di sekitar jalan utama, mohon pantau titik evakuasi.",
                    waktu = "15:46",
                    isDariSaya = true
                )
            )
        )
        adapter.notifyDataSetChanged()
        gulirKePesanTerbaru()
    }

    private fun kirimPesan(isiPesan: String) {
        val waktuSekarang = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        adapter.tambahPesan(
            PesanRelawan(
                pengirim = labelPengirimSaya,
                isi = isiPesan,
                waktu = waktuSekarang,
                isDariSaya = true
            )
        )
        gulirKePesanTerbaru()
    }

    private fun gulirKePesanTerbaru() {
        if (daftarPesan.isNotEmpty()) {
            binding.rvChat.scrollToPosition(daftarPesan.size - 1)
        }
    }

    companion object {
        private const val ROLE_PETUGAS = "petugas"
        private const val ROLE_RELAWAN = "relawan"
    }
}