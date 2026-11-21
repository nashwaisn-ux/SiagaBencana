package com.example.siagabencana

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.siagabencana.databinding.ActivityMainBinding
import com.example.siagabencana.ui.DashboardFragment
import com.example.siagabencana.ui.FragmenRelawanPetugas
import com.example.siagabencana.ui.LaporanFragment
import com.example.siagabencana.ui.ProfilFragment
import com.example.siagabencana.ui.RelawanFragment
import com.example.siagabencana.ui.StaticFragment
import com.example.siagabencana.ui.toast
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var activeRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        activeRole = bacaRoleAktif()
        setupBottomNavigation(activeRole)

        if (savedInstanceState == null) {
            toast("Masuk sebagai ${activeRole.replaceFirstChar { it.uppercase() }}")
            val targetId = if (b.bottomNav.menu.hasItemId(R.id.nav_beranda)) {
                R.id.nav_beranda
            } else {
                b.bottomNav.menu.firstMenuItem()?.itemId
            }
            if (targetId != null) {
                b.bottomNav.selectedItemId = targetId
            }
        }
    }

    private fun bacaRoleAktif(): String {
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

    private fun setupBottomNavigation(role: String) {
        val menuRes = when (role) {
            ROLE_PETUGAS -> R.menu.bottom_nav_petugas
            ROLE_RELAWAN -> R.menu.bottom_nav_relawan
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
            R.id.nav_relawan -> if (activeRole == ROLE_PETUGAS) {
                FragmenRelawanPetugas()
            } else {
                RelawanFragment()
            }
            R.id.nav_riwayat -> StaticFragment.of(R.layout.fragment_riwayat)
            else -> ProfilFragment()
        }

        supportFragmentManager
            .beginTransaction()
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

    companion object {
        private const val ROLE_PETUGAS = "petugas"
        private const val ROLE_RELAWAN = "relawan"
        private const val ROLE_MASYARAKAT = "masyarakat"
    }
}