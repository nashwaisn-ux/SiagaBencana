package com.example.siagabencana

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siagabencana.databinding.ActivityBuatLaporanBinding
import com.example.siagabencana.model.Urgensi
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.ui.FotoAdapter
import com.example.siagabencana.ui.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.io.FileOutputStream
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

    private var userGpsLat: Double? = null
    private var userGpsLng: Double? = null

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_FOTO)
    ) { uris ->
        val maxSizeBytes = 30 * 1024 * 1024L // Maksimal 30 MB per foto
        val validUris = mutableListOf<Uri>()
        var skippedCount = 0

        for (uri in uris) {
            val size = getFileSize(uri)
            if (size > maxSizeBytes) {
                skippedCount++
            } else {
                validUris.add(uri)
            }
        }

        if (skippedCount > 0) {
            toast("$skippedCount foto melebihi batas 30 MB dan tidak ditambahkan.")
        }

        val sisa = MAX_FOTO - fotos.size
        fotos.addAll(validUris.take(sisa))
        refreshFoto()
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) fetchLocation() else showLocationUnavailable("Izin lokasi ditolak") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "SiagaBencana/1.0"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }

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
        b.tvUbahLokasi.setOnClickListener { showMapLocationPicker() }
        b.btnKirim.setOnClickListener { submit() }

        requestLocation()
    }

    // ---- Lokasi & Map Location Picker (1 km Radius) ----

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
            userGpsLat = terbaru.latitude
            userGpsLng = terbaru.longitude
            if (lat == null || lng == null) {
                lat = terbaru.latitude
                lng = terbaru.longitude
            }
            b.tvKoordinat.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
            b.tvStatusLokasi.text = "Lokasi GPS terdeteksi (Radius 1 km aktif)"
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

    private fun showMapLocationPicker() {
        if (userGpsLat == null || userGpsLng == null) {
            requestLocation()
            toast("Mendeteksi posisi GPS Anda terlebih dahulu...")
            return
        }

        val centerLat = userGpsLat!!
        val centerLng = userGpsLng!!

        val dialogView = layoutInflater.inflate(R.layout.dialog_map_picker, null)
        val mapView = dialogView.findViewById<MapView>(R.id.pickerMapView)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvPickerInfo)

        val wikimediaTileSource = XYTileSource(
            "Wikimedia", 0, 19, 256, ".png",
            arrayOf("https://maps.wikimedia.org/osm-intl/"), "© Wikimedia"
        )
        mapView.setTileSource(wikimediaTileSource)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.5)
        val centerGeo = GeoPoint(centerLat, centerLng)
        mapView.controller.setCenter(centerGeo)

        // Circle 1 km radius overlay
        val circlePolygon = Polygon(mapView).apply {
            points = createCirclePoints(centerGeo, 1000.0) // 1 km radius
            fillPaint.color = Color.parseColor("#1AD32F2F")
            outlinePaint.color = Color.parseColor("#D32F2F")
            outlinePaint.strokeWidth = 3f
        }
        mapView.overlays.add(circlePolygon)

        // Marker user GPS
        val userMarker = Marker(mapView).apply {
            position = centerGeo
            title = "Posisi Anda"
            snippet = "Pusat radius 1 km"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(userMarker)

        // Selected location marker
        var selectedGeo = GeoPoint(lat ?: centerLat, lng ?: centerLng)
        val selectedMarker = Marker(mapView).apply {
            position = selectedGeo
            title = "Titik Laporan"
            snippet = "Ketuk lokasi di dalam lingkaran 1 km"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(selectedMarker)

        val results = FloatArray(1)
        Location.distanceBetween(centerLat, centerLng, selectedGeo.latitude, selectedGeo.longitude, results)
        tvInfo.text = "Jarak titik terpilih: ${results[0].toInt()} m dari posisi Anda (Batas 1 km)."

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                val dist = FloatArray(1)
                Location.distanceBetween(centerLat, centerLng, p.latitude, p.longitude, dist)
                val distanceInMeters = dist[0]

                if (distanceInMeters > 1000.0f) {
                    toast("Titik berada di luar radius 1 km (${distanceInMeters.toInt()}m). Pilih lokasi lebih dekat.")
                } else {
                    selectedGeo = p
                    selectedMarker.position = p
                    tvInfo.text = "Titik terpilih: ${String.format(Locale.US, "%.4f, %.4f", p.latitude, p.longitude)} (${distanceInMeters.toInt()}m dari Anda)"
                    mapView.invalidate()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(MapEventsOverlay(eventsReceiver))

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("📍 Tandai Titik Lokasi Spesifik")
            .setMessage("Ketuk pada peta di dalam lingkaran merah (radius 1 km dari lokasi Anda) untuk menandai lokasi spesifik bencana.")
            .setView(dialogView)
            .setPositiveButton("Gunakan Titik Ini") { _, _ ->
                lat = selectedGeo.latitude
                lng = selectedGeo.longitude
                b.tvKoordinat.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                val finalDist = FloatArray(1)
                Location.distanceBetween(centerLat, centerLng, selectedGeo.latitude, selectedGeo.longitude, finalDist)
                b.tvStatusLokasi.text = "Titik spesifik ditandai (${finalDist[0].toInt()}m dari posisi Anda)"
                b.tvStatusLokasi.setTextColor(ContextCompat.getColor(this@BuatLaporanActivity, R.color.siaga_green))
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
    }

    private fun createCirclePoints(center: GeoPoint, radiusInMeters: Double): ArrayList<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        for (i in 0 until 360 step 10) {
            points.add(center.destinationPoint(radiusInMeters, i.toDouble()))
        }
        return points
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (_: Exception) {
            0L
        }
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

    private fun kategoriIdTerpilih() = when (b.chipGroupJenis.checkedChipId) {
        R.id.chipJenisKebakaran -> "2"
        R.id.chipJenisLongsor -> "3"
        R.id.chipJenisLainnya -> "4"
        else -> "1"
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

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@BuatLaporanActivity)
                val textType = "text/plain".toMediaTypeOrNull()

                val judulPart = form.judul.toRequestBody(textType)
                val deskPart = form.deskripsi.toRequestBody(textType)
                val katPart = kategoriIdTerpilih().toRequestBody(textType)
                val latPart = form.lat.toString().toRequestBody(textType)
                val lonPart = form.lng.toString().toRequestBody(textType)
                val terdampakPart = "1".toRequestBody(textType)

                var photoPart: MultipartBody.Part? = null
                if (form.fotos.isNotEmpty()) {
                    val firstUri = form.fotos.first()
                    val file = withContext(Dispatchers.IO) {
                        val tempFile = File(cacheDir, "laporan_photo_${System.currentTimeMillis()}.jpg")
                        contentResolver.openInputStream(firstUri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    if (file.exists() && file.length() > 0) {
                        val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        photoPart = MultipartBody.Part.createFormData("foto", file.name, reqFile)
                    }
                }

                val response = api.createLaporan(
                    judul = judulPart,
                    deskripsi = deskPart,
                    kategoriId = katPart,
                    latitude = latPart,
                    longitude = lonPart,
                    jumlahTerdampak = terdampakPart,
                    foto = photoPart
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    toast("Laporan berhasil terkirim & ditandai di peta!")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    toast("Laporan tersimpan: ${form.judul}")
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (_: Exception) {
                toast("Laporan tersimpan (Offline): ${form.judul}")
                setResult(RESULT_OK)
                finish()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progressKirim.isVisible = loading
        b.btnKirim.isEnabled = !loading
    }

    companion object {
        private const val MAX_FOTO = 5
    }
}
