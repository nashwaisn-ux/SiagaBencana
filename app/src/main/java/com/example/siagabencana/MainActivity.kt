package com.example.siagabencana

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityMainBinding
import com.example.siagabencana.ui.DashboardFragment
import com.example.siagabencana.ui.LaporanFragment
import com.example.siagabencana.ui.ProfilFragment
import com.example.siagabencana.ui.StaticFragment
import com.example.siagabencana.ui.toast

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val currentRole = session.role
        setupBottomNavigation(currentRole)

        if (savedInstanceState == null) {
            toast("Masuk sebagai ${currentRole.replaceFirstChar { it.uppercase() }}")
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

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
        }
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