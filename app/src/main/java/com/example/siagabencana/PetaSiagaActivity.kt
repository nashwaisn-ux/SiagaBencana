package com.example.siagabencana

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.siagabencana.databinding.ActivityPetaSiagaBinding
import com.example.siagabencana.ui.toast

class PetaSiagaActivity : AppCompatActivity() {

    private lateinit var b: ActivityPetaSiagaBinding
    private var sosAnimator: ValueAnimator? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPetaSiagaBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }

        b.chipGroupRadius.setOnCheckedStateChangeListener { _, ids ->
            val km = when (ids.firstOrNull()) {
                R.id.chip1km -> 1
                R.id.chip5km -> 5
                R.id.chip10km -> 10
                else -> 3
            }
            // TODO Anggota 5: GET /api/laporan/sekitar?lat=&lng=&radius=$km lalu gambar ulang marker.
            b.tvRingkasan.text = "Dalam radius $km km"
        }

        b.btnSos.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startSos()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelSos()
            }
            true
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
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

    private var sosAnimator: ValueAnimator? = null
    private var isSosTriggered = false
    private var allMapPoints: List<MapPoint> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi osmdroid
        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "SiagaBencana/1.0 (kontak: dev@siagabencana.app)"
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

        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener { finish() }

        // Setup OSMDroid Map menggunakan Wikimedia tile source (Berbasis data OSM, bebas blokir 403)
        val wikimediaTileSource = XYTileSource(
            "Wikimedia",
            0, 19, 256, ".png",
            arrayOf("https://maps.wikimedia.org/osm-intl/"),
            "© Wikimedia contributors"
        )
        mapView.setTileSource(wikimediaTileSource)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(GeoPoint(0.5070677, 101.4477930))
        mapView.overlays.add(CopyrightOverlay(this))

        setupSosTouchListener()
        setupCategoryFilter()

        muatLaporan()
    }

    private fun setupCategoryFilter() {
        chipGroupJenis.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedCategory = when (checkedIds.first()) {
                R.id.chipBanjir -> "Banjir"
                R.id.chipKebakaran -> "Kebakaran"
                R.id.chipLongsor -> "Longsor"
                else -> null
            }
            filterAndDisplayPoints(selectedCategory)
        }
    }

    private fun muatLaporan() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.getApiService(this@PetaSiagaActivity).getPeta()
                val data = resp.body()?.data
                if (resp.isSuccessful && data != null) {
                    allMapPoints = data
                    filterAndDisplayPoints(null)
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

    private fun filterAndDisplayPoints(categoryFilter: String?) {
        mapView.overlays.removeAll { it is Marker }

        val filteredPoints = if (categoryFilter.isNullOrEmpty()) {
            allMapPoints
        } else {
            allMapPoints.filter { it.kategori.equals(categoryFilter, ignoreCase = true) }
        }

        val titik = filteredPoints.map { GeoPoint(it.latitude, it.longitude) }

        filteredPoints.forEach { p ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(p.latitude, p.longitude)
                title = p.judul
                snippet = "${p.kategori ?: "-"} • urgensi ${p.urgensi} • ${p.status}"
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
        }

        tvRingkasan.text = "${filteredPoints.size} laporan siaga"

        when {
            titik.size == 1 -> {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(titik[0])
            }
            titik.size > 1 -> mapView.post {
                mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(titik), true, 120)
            }
        }

        mapView.invalidate()
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
        Toast.makeText(this, "🚨 MENGIRIM SINYAL SOS DARURAT...", Toast.LENGTH_LONG).show()

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@PetaSiagaActivity)

                val center = mapView.mapCenter
                val lat = center.latitude
                val lon = center.longitude

                val textType = "text/plain".toMediaTypeOrNull()
                val judulPart = "Sinyal SOS Darurat!".toRequestBody(textType)
                val deskPart = "Sinyal SOS Darurat dikirim pengguna dari Peta Siaga. Butuh bantuan evakuasi darurat!".toRequestBody(textType)
                val katPart = "6".toRequestBody(textType)
                val latPart = lat.toString().toRequestBody(textType)
                val lonPart = lon.toString().toRequestBody(textType)
                val terdampakTest = "1".toRequestBody(textType)

                val response = api.createLaporan(
                    judul = judulPart,
                    deskripsi = deskPart,
                    kategoriId = katPart,
                    latitude = latPart,
                    longitude = lonPart,
                    jumlahTerdampak = terdampakTest,
                    foto = null
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

    private fun startSos() {
        sosAnimator?.cancel()
        var dibatalkan = false
        sosAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 3000
            interpolator = LinearInterpolator()
            addUpdateListener { b.progressSos.progress = it.animatedValue as Int }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) { dibatalkan = true }
                override fun onAnimationEnd(animation: Animator) { if (!dibatalkan) onSosTriggered() }
            })
            start()
        }
    }

    private fun cancelSos() {
        sosAnimator?.cancel()
        b.progressSos.progress = 0
    }

    private fun onSosTriggered() {
        b.btnSos.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        b.progressSos.progress = 0
        // TODO Anggota 5: POST /api/sos dengan lat, lng, dan token pengguna.
        toast("SOS terkirim ke relawan dan petugas terdekat (demo)")
    }

    override fun onDestroy() {
        sosAnimator?.cancel()
        super.onDestroy()
    }
}
