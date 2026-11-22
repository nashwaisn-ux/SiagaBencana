package com.example.siagabencana

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityHubungiRelawanBinding
import com.example.siagabencana.model.KirimPesanRequest
import com.example.siagabencana.model.PesanItem
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.ui.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HubungiRelawanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHubungiRelawanBinding
    private lateinit var adapter: ChatRelawanAdapter
    private val daftarPesan = mutableListOf<PesanRelawan>()

    private val session by lazy { SessionManager(this) }
    private var rolePengguna: String = ""
    private var idLaporan: Int = -1
    private var namaLawanBicara: String = "Relawan"
    private var lastPesanId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHubungiRelawanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ambilDataIntent()
        if (idLaporan == -1) {
            toast("Laporan tidak ditemukan")
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupQuickReply()
        setupActions()

        mulaiPollingPesan()
    }

    private fun ambilDataIntent() {
        rolePengguna = intent.getStringExtra("EXTRA_ROLE")
            ?.takeIf { it.isNotBlank() }
            ?: session.role
        idLaporan = intent.getIntExtra("EXTRA_ID_LAPORAN", -1)

        val namaExtra = intent.getStringExtra("EXTRA_NAMA_RELAWAN")
        val statusExtra = intent.getStringExtra("EXTRA_STATUS_RELAWAN")

        if (!namaExtra.isNullOrBlank()) {
            namaLawanBicara = namaExtra
            binding.tvNamaRelawan.text = namaExtra
            binding.tvInisialRelawan.text = buatInisial(namaExtra)
        } else {
            binding.tvNamaRelawan.text = if (rolePengguna == SessionManager.ROLE_PETUGAS) "Relawan" else "Petugas"
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
            kirimPesanKeApi("Butuh evakuasi segera")
        }
        binding.chipLokasiAman.setOnClickListener {
            kirimPesanKeApi("Posisi di titik aman")
        }
        binding.chipLogistik.setOnClickListener {
            kirimPesanKeApi("Butuh air & P3K")
        }
    }

    private fun setupActions() {
        binding.btnKirim.setOnClickListener {
            val teks = binding.etPesan.text?.toString()?.trim().orEmpty()
            if (teks.isEmpty()) {
                toast("Silakan ketik pesan terlebih dahulu")
                return@setOnClickListener
            }
            kirimPesanKeApi(teks)
            binding.etPesan.text?.clear()
        }

        binding.btnKirimLokasi.setOnClickListener {
            val infoLaporan = if (idLaporan != -1) " (Laporan #$idLaporan)" else ""
            kirimPesanKeApi("Titik lokasi saya$infoLaporan: sertakan patokan jalan terdekat")
        }
    }

    private fun mulaiPollingPesan() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    muatPesanDariApi()
                    delay(4000)
                }
            }
        }
    }

    private suspend fun muatPesanDariApi() {
        try {
            val api = RetrofitClient.getApiService(this@HubungiRelawanActivity)
            val response = api.getPesan(idLaporan, afterId = lastPesanId)
            if (response.isSuccessful && response.body()?.success == true) {
                val listBaru = response.body()?.data.orEmpty()
                if (listBaru.isNotEmpty()) {
                    val myId = session.getUserId()
                    var adaBaru = false
                    listBaru.forEach { item ->
                        if (lastPesanId == null || item.id > (lastPesanId ?: 0)) {
                            lastPesanId = item.id
                        }
                        val isSaya = item.pengirim?.id == myId
                        val namaPengirim = if (isSaya) "Saya" else (item.pengirim?.nama ?: namaLawanBicara)
                        val jamText = formatJam(item.created_at)

                        daftarPesan.add(
                            PesanRelawan(
                                id = item.id,
                                pengirim = namaPengirim,
                                isi = item.isi,
                                waktu = jamText,
                                isDariSaya = isSaya
                            )
                        )
                        adaBaru = true
                    }
                    if (adaBaru) {
                        adapter.notifyDataSetChanged()
                        gulirKePesanTerbaru()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore network drop during background polling
        }
    }

    private fun kirimPesanKeApi(isiPesan: String) {
        binding.btnKirim.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@HubungiRelawanActivity)
                val response = api.kirimPesan(idLaporan, KirimPesanRequest(isiPesan))
                if (response.isSuccessful && response.body()?.success == true) {
                    val item = response.body()?.data
                    if (item != null) {
                        if (lastPesanId == null || item.id > (lastPesanId ?: 0)) {
                            lastPesanId = item.id
                        }
                        val jamText = formatJam(item.created_at)
                        adapter.tambahPesan(
                            PesanRelawan(
                                id = item.id,
                                pengirim = "Saya",
                                isi = item.isi,
                                waktu = jamText,
                                isDariSaya = true
                            )
                        )
                        gulirKePesanTerbaru()
                    }
                } else {
                    toast(response.body()?.message ?: "Gagal mengirim pesan")
                }
            } catch (e: Exception) {
                toast("Gagal terhubung ke server")
            } finally {
                binding.btnKirim.isEnabled = true
            }
        }
    }

    private fun formatJam(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoDate)
            if (date != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
            }
        } catch (_: Exception) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        }
    }

    private fun gulirKePesanTerbaru() {
        if (daftarPesan.isNotEmpty()) {
            binding.rvChat.scrollToPosition(daftarPesan.size - 1)
        }
    }
}
