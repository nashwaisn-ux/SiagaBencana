package com.example.siagabencana

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siagabencana.databinding.ActivityBuatLaporanBinding
import com.example.siagabencana.model.Urgensi
import com.example.siagabencana.ui.FotoAdapter
import com.example.siagabencana.ui.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


data class LaporanForm(
    val judul: String,
    val jenis: String,
    val urgensi: Urgensi,
    val deskripsi: String,
    val lat: Double,
    val lng: Double,
    val fotos: List<Uri>,
)

class BuatLaporanActivity : AppCompatActivity() {

    private lateinit var b: ActivityBuatLaporanBinding
    private val fotos = mutableListOf<Uri>()
    private lateinit var fotoAdapter: FotoAdapter
    private var lat: Double? = null
    private var lng: Double? = null

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_FOTO)
    ) { uris ->
        val sisa = MAX_FOTO - fotos.size
        fotos.addAll(uris.take(sisa))
        refreshFoto()
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchLocation() else showLocationUnavailable("Izin lokasi ditolak") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBuatLaporanBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }

        fotoAdapter = FotoAdapter(fotos) { index ->
            fotos.removeAt(index)
            refreshFoto()
        }
        b.rvFoto.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        b.rvFoto.adapter = fotoAdapter
        refreshFoto()

        b.btnTambahFoto.setOnClickListener {
            if (fotos.size >= MAX_FOTO) toast("Maksimal $MAX_FOTO foto") else
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        b.tvUbahLokasi.setOnClickListener { requestLocation() }
        b.btnKirim.setOnClickListener { submit() }

        requestLocation()
    }

    // ---- Lokasi ----

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) fetchLocation() else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val terbaru = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (terbaru != null) {
            lat = terbaru.latitude
            lng = terbaru.longitude
            b.tvKoordinat.text = String.format(Locale.US, "%.4f, %.4f", terbaru.latitude, terbaru.longitude)
            b.tvStatusLokasi.text = "Lokasi GPS terdeteksi"
            b.tvStatusLokasi.setTextColor(ContextCompat.getColor(this, R.color.siaga_text_secondary))
        } else {
            showLocationUnavailable("Lokasi belum tersedia. Nyalakan GPS lalu ketuk Ubah.")
        }
    }

    private fun showLocationUnavailable(message: String) {
        lat = null
        lng = null
        b.tvKoordinat.text = "Lokasi belum diketahui"
        b.tvStatusLokasi.text = message
    }

    // ---- Foto ----

    private fun refreshFoto() {
        fotoAdapter.notifyDataSetChanged()
        b.btnTambahFoto.isVisible = fotos.size < MAX_FOTO
        if (fotos.isNotEmpty()) b.tvErrorFoto.isVisible = false
    }

    // ---- Validasi dan kirim ----

    private fun jenisTerpilih() = when (b.chipGroupJenis.checkedChipId) {
        R.id.chipJenisKebakaran -> "Kebakaran"
        R.id.chipJenisLongsor -> "Longsor"
        R.id.chipJenisLainnya -> "Lainnya"
        else -> "Banjir"
    }

    private fun urgensiTerpilih() = when (b.toggleUrgensi.checkedButtonId) {
        R.id.btnSedang -> Urgensi.SEDANG
        R.id.btnTinggi -> Urgensi.TINGGI
        R.id.btnKritis -> Urgensi.KRITIS
        else -> Urgensi.RENDAH
    }

    private fun validate(): LaporanForm? {
        val judul = b.etJudul.text?.toString().orEmpty().trim()
        val deskripsi = b.etDeskripsi.text?.toString().orEmpty().trim()

        b.tilJudul.error = if (judul.length >= 5) null else "Judul wajib diisi, minimal 5 karakter."
        b.tilDeskripsi.error = if (deskripsi.length >= 10) null else "Deskripsi minimal 10 karakter."
        b.tvErrorFoto.isVisible = fotos.isEmpty()
        val lokasiOk = lat != null && lng != null
        if (!lokasiOk) b.tvStatusLokasi.text = "Lokasi wajib. Nyalakan GPS lalu ketuk Ubah."

        if (b.tilJudul.error != null || b.tilDeskripsi.error != null || fotos.isEmpty() || !lokasiOk) return null
        return LaporanForm(judul, jenisTerpilih(), urgensiTerpilih(), deskripsi, lat!!, lng!!, fotos.toList())
    }

    private fun submit() {
        val form = validate() ?: return
        setLoading(true)

        // TODO Anggota 5: kirim `form` lewat Retrofit multipart POST /api/laporan.
        //   Sukses -> toast("Laporan terkirim"), setResult(RESULT_OK), finish().
        //   Gagal  -> setLoading(false) dan tampilkan pesan error / gagal jaringan.
        lifecycleScope.launch {
            delay(1200)
            toast("Laporan terkirim (demo): ${form.judul}")
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progressKirim.isVisible = loading
        b.btnKirim.isEnabled = !loading
    }

    companion object {
        private const val MAX_FOTO = 3
    }
}
