package com.example.siagabencana

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityMainBinding
import com.example.siagabencana.ui.DashboardFragment
import com.example.siagabencana.ui.LaporanFragment
import com.example.siagabencana.ui.ProfilFragment
import com.example.siagabencana.ui.StaticFragment
import com.example.siagabencana.ui.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val session by lazy { SessionManager(this) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            toast("Izin lokasi telah diberikan")
        } else {
            toast("Izin lokasi diperlukan untuk fitur peta & penandaan radius 1 km")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val currentRole = session.role
        setupBottomNavigation(currentRole)

        if (savedInstanceState == null) {
            toast("Masuk sebagai ${currentRole.replaceFirstChar { it.uppercase() }}")
            
            // Explicitly load DashboardFragment immediately on startup
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()

            val targetId = if (b.bottomNav.menu.hasItemId(R.id.nav_beranda)) {
                R.id.nav_beranda
            } else {
                b.bottomNav.menu.firstMenuItem()?.itemId
            }
            if (targetId != null) {
                b.bottomNav.selectedItemId = targetId
            }

            checkAndNotifyLocationPermission()
        }
    }

    private fun checkAndNotifyLocationPermission() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation != PackageManager.PERMISSION_GRANTED && coarseLocation != PackageManager.PERMISSION_GRANTED) {
            MaterialAlertDialogBuilder(this)
                .setTitle("📍 Akses Lokasi Diperlukan")
                .setMessage("Aplikasi SiagaBencana memerlukan izin akses lokasi Anda untuk:\n\n• Mendeteksi posisi saat bencana secara cepat\n• Menandai lokasi laporan spesifik dalam radius 1 km\n• Menampilkan peringatan bahaya di sekitar Anda")
                .setCancelable(false)
                .setPositiveButton("Izinkan Lokasi") { dialog, _ ->
                    dialog.dismiss()
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                .setNegativeButton("Nanti") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupBottomNavigation(role: String) {
        val menuRes = when (role) {
            SessionManager.ROLE_PETUGAS -> R.menu.bottom_nav_petugas
            SessionManager.ROLE_RELAWAN -> R.menu.bottom_nav_relawan
            else -> R.menu.bottom_nav_masyarakat
        }

        b.bottomNav.menu.clear()
        b.bottomNav.inflateMenu(menuRes)

        b.bottomNav.setOnItemSelectedListener { item ->
            show(item.itemId)
        }

        b.bottomNav.setOnItemReselectedListener {
            // Tidak me-reload fragment yang sama saat tab aktif ditekan ulang
        }
    }

    /** Dipanggil fragment untuk berpindah tab, misalnya dari tombol "Lihat semua". */
    fun navigateTo(@IdRes menuId: Int) {
        if (b.bottomNav.menu.hasItemId(menuId) && b.bottomNav.selectedItemId != menuId) {
            b.bottomNav.selectedItemId = menuId
        }
    }

    private fun show(@IdRes menuId: Int): Boolean {
        val fragment: Fragment = when (menuId) {
            R.id.nav_beranda -> DashboardFragment()
            R.id.nav_laporan -> LaporanFragment()
            R.id.nav_relawan -> StaticFragment.of(R.layout.fragment_relawan)
            R.id.nav_riwayat -> StaticFragment.of(R.layout.fragment_riwayat)
            else -> ProfilFragment()
        }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun Menu.hasItemId(@IdRes id: Int): Boolean = findItem(id) != null

    private fun Menu.firstMenuItem(): MenuItem? {
        for (i in 0 until size()) {
            val item = getItem(i)
            if (item.isVisible) return item
        }
        return if (size() > 0) getItem(0) else null
    }
}
