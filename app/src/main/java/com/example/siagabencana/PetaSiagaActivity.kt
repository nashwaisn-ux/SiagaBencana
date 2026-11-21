package com.example.siagabencana

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.model.MapPoint
import com.example.siagabencana.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.io.IOException

class PetaSiagaActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var progressSos: LinearProgressIndicator
    private lateinit var btnSos: MaterialButton
    private lateinit var tvRingkasan: TextView
    private lateinit var tvTerdekatJudul: TextView
    private lateinit var tvTerdekatJarak: TextView
    private lateinit var chipGroupJenis: ChipGroup
    private lateinit var chipGroupRadius: ChipGroup

    private var sosAnimator: ValueAnimator? = null
    private var isSosTriggered = false
    private var allMapPoints: List<MapPoint> = emptyList()

    private var currentCenterGeo = GeoPoint(0.5070677, 101.4477930)
    private var currentRadiusMeters = 3000.0 // Default 3 km
    private var currentCategoryFilter: String? = null

    private var focusLat: Double = Double.NaN
    private var focusLng: Double = Double.NaN
    private var focusJudul: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "com.example.siagabencana/1.0 (Android OS; contact: dev@siagabencana.app)"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }

        setContentView(R.layout.activity_peta_siaga)

        mapView = findViewById(R.id.mapView)
        progressSos = findViewById(R.id.progressSos)
        btnSos = findViewById(R.id.btnSos)
        tvRingkasan = findViewById(R.id.tvRingkasan)
        tvTerdekatJudul = findViewById(R.id.tvTerdekatJudul)
        tvTerdekatJarak = findViewById(R.id.tvTerdekatJarak)
        chipGroupJenis = findViewById(R.id.chipGroupJenis)
        chipGroupRadius = findViewById(R.id.chipGroupRadius)

        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnZoomIn)?.setOnClickListener {
            mapView.controller.zoomIn()
        }
        findViewById<ImageButton>(R.id.btnZoomOut)?.setOnClickListener {
            mapView.controller.zoomOut()
        }

        val cartoTileSource = XYTileSource(
            "CartoDBVoyager",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
            ),
            "© OpenStreetMap contributors, © CartoDB"
        )
        mapView.setTileSource(cartoTileSource)
        mapView.setMultiTouchControls(true)
        mapView.overlays.add(CopyrightOverlay(this))

        focusLat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        focusLng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
        focusJudul = intent.getStringExtra(EXTRA_JUDUL)

        if (!focusLat.isNaN() && !focusLng.isNaN()) {
            currentCenterGeo = GeoPoint(focusLat, focusLng)
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(currentCenterGeo)
        } else {
            mapView.controller.setZoom(14.0)
            mapView.controller.setCenter(currentCenterGeo)
        }

        setupSosTouchListener()
        setupCategoryFilter()
        setupRadiusFilter()

        muatLaporan()
    }

    @SuppressLint("MissingPermission")
    private fun fetchUserLocation(): GeoPoint? {
        return try {
            val lm = getSystemService(LOCATION_SERVICE) as LocationManager
            val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            if (location != null) {
                GeoPoint(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setupCategoryFilter() {
        chipGroupJenis.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentCategoryFilter = when (checkedIds.first()) {
                R.id.chipBanjir -> "banjir"
                R.id.chipKebakaran -> "kebakaran"
                R.id.chipLongsor -> "longsor"
                else -> null
            }
            filterAndDisplayPoints()
        }
    }

    private fun setupRadiusFilter() {
        chipGroupRadius.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val (radiusMeters, zoomLevel, label) = when (checkedIds.first()) {
                R.id.chip1km -> Triple(1000.0, 15.5, "1 km")
                R.id.chip5km -> Triple(5000.0, 13.0, "5 km")
                R.id.chip10km -> Triple(10000.0, 11.5, "10 km")
                else -> Triple(3000.0, 14.0, "3 km")
            }

            currentRadiusMeters = radiusMeters
            mapView.controller.animateTo(currentCenterGeo, zoomLevel, 800L)
            filterAndDisplayPoints()
            Toast.makeText(this, "Zoom peta disesuaikan ke area radius $label", Toast.LENGTH_SHORT).show()
        }
    }

    private fun muatLaporan() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(this@PetaSiagaActivity).getPeta()
                val data = resp.body()?.data
                if (resp.isSuccessful && data != null) {
                    allMapPoints = data
                    filterAndDisplayPoints()
                } else {
                    Toast.makeText(this@PetaSiagaActivity, "Gagal memuat peta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this@PetaSiagaActivity, "Tidak ada koneksi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@PetaSiagaActivity, "Terjadi kesalahan memuat data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterAndDisplayPoints() {
        mapView.overlays.removeAll { it is Marker || it is Polygon }

        val radiusPolygon = Polygon(mapView).apply {
            points = createCirclePoints(currentCenterGeo, currentRadiusMeters)
            fillPaint.color = Color.parseColor("#1AD32F2F")
            outlinePaint.color = Color.parseColor("#D32F2F")
            outlinePaint.strokeWidth = 3f
        }
        mapView.overlays.add(0, radiusPolygon)

        val filteredPoints = allMapPoints.filter { p ->
            val kat = p.kategori.orEmpty()
            val matchesCategory = currentCategoryFilter.isNullOrEmpty() ||
                    kat.contains(currentCategoryFilter!!, ignoreCase = true)

            val dist = FloatArray(1)
            Location.distanceBetween(
                currentCenterGeo.latitude, currentCenterGeo.longitude,
                p.latitude, p.longitude, dist
            )
            val matchesRadius = dist[0] <= currentRadiusMeters

            matchesCategory && matchesRadius
        }

        filteredPoints.forEach { p ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(p.latitude, p.longitude)
                title = p.judul
                snippet = "${p.kategori ?: "Bencana"} • Urgensi: ${p.urgensi} • Status: ${p.status}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ikonUrgensi(p.urgensi)

                setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    tvTerdekatJudul.text = p.judul
                    tvTerdekatJarak.text = "${p.kategori ?: "Bencana"} · Urgensi: ${p.urgensi} · Status: ${p.status}"
                    true
                }
            }
            mapView.overlays.add(marker)

            if (!focusLat.isNaN() && !focusLng.isNaN() &&
                Math.abs(p.latitude - focusLat) < 0.0001 && Math.abs(p.longitude - focusLng) < 0.0001
            ) {
                marker.showInfoWindow()
                tvTerdekatJudul.text = p.judul
                tvTerdekatJarak.text = "${p.kategori ?: "Bencana"} · Urgensi: ${p.urgensi} · Status: ${p.status}"
            }
        }

        val radiusLabelInKm = (currentRadiusMeters / 1000).toInt()
        tvRingkasan.text = "${filteredPoints.size} laporan dalam radius $radiusLabelInKm km"

        mapView.invalidate()
    }

    private fun createCirclePoints(center: GeoPoint, radiusInMeters: Double): ArrayList<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        for (i in 0 until 360 step 10) {
            points.add(center.destinationPoint(radiusInMeters, i.toDouble()))
        }
        return points
    }

    private fun ikonUrgensi(urgensi: String): Drawable {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_location)!!.mutate()
        val wrappedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(wrappedDrawable, warnaUrgensi(urgensi))
        return wrappedDrawable
    }

    private fun warnaUrgensi(urgensi: String): Int = when (urgensi.lowercase()) {
        "kritis" -> ContextCompat.getColor(this, R.color.siaga_red)
        "tinggi" -> ContextCompat.getColor(this, R.color.siaga_orange)
        "sedang" -> Color.parseColor("#CA8A04")
        else -> ContextCompat.getColor(this, R.color.siaga_green)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSosTouchListener() {
        btnSos.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isSosTriggered = false
                    progressSos.progress = 0

                    sosAnimator = ValueAnimator.ofInt(0, 100).apply {
                        duration = 3000
                        addUpdateListener { animation ->
                            val value = animation.animatedValue as Int
                            progressSos.progress = value
                            if (value == 100 && !isSosTriggered) {
                                isSosTriggered = true
                                triggerSosEmergency()
                            }
                        }
                        start()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    sosAnimator?.cancel()
                    progressSos.progress = 0
                    if (!isSosTriggered) {
                        Toast.makeText(this, "Tahan tombol 3 detik untuk mengirim SOS", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun triggerSosEmergency() {
        val userLocation = fetchUserLocation()
        val lat = userLocation?.latitude ?: if (focusLat.isNaN()) currentCenterGeo.latitude else null
        val lon = userLocation?.longitude ?: if (focusLng.isNaN()) currentCenterGeo.longitude else null

        if (lat == null || lon == null) {
            Toast.makeText(this, "Lokasi GPS Anda belum terdeteksi. Nyalakan GPS HP Anda terlebih dahulu.", Toast.LENGTH_LONG).show()
            progressSos.progress = 0
            return
        }

        Toast.makeText(this, "🚨 MENGIRIM SINYAL SOS DARURAT...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@PetaSiagaActivity)

                val textType = "text/plain".toMediaTypeOrNull()
                val judulPart = "Sinyal SOS Darurat!".toRequestBody(textType)
                val deskPart = "Sinyal SOS Darurat dikirim pengguna dari Peta Siaga. Butuh bantuan evakuasi darurat!".toRequestBody(textType)
                val katPart = "6".toRequestBody(textType)
                val latPart = lat.toString().toRequestBody(textType)
                val lonPart = lon.toString().toRequestBody(textType)
                val terdampakTest = "1".toRequestBody(textType)
                val sosPart = "1".toRequestBody(textType)

                val response = api.createLaporan(
                    judul = judulPart,
                    deskripsi = deskPart,
                    kategoriId = katPart,
                    latitude = latPart,
                    longitude = lonPart,
                    jumlahTerdampak = terdampakTest,
                    foto = null,
                    sos = sosPart
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@PetaSiagaActivity,
                        "🚨 SINYAL SOS DARURAT BERHASIL DIKIRIM KE PETUGAS!",
                        Toast.LENGTH_LONG
                    ).show()
                    muatLaporan()
                } else {
                    Toast.makeText(this@PetaSiagaActivity, "Gagal mengirim SOS: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PetaSiagaActivity, "Gagal terhubung ke server untuk SOS", Toast.LENGTH_LONG).show()
            } finally {
                progressSos.progress = 0
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }

    companion object {
        const val EXTRA_LAT = "EXTRA_LAT"
        const val EXTRA_LNG = "EXTRA_LNG"
        const val EXTRA_JUDUL = "EXTRA_JUDUL"
    }
}
