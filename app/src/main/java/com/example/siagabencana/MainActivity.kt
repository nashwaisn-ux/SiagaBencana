package com.example.siagabencana

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.siagabencana.ui.DashboardFragment
import com.example.siagabencana.ui.LaporanFragment
import com.example.siagabencana.ui.ProfilFragment
import com.example.siagabencana.ui.RelawanFragment
import com.example.siagabencana.ui.RiwayatFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_laporan -> {
                    loadFragment(LaporanFragment())
                    true
                }
                R.id.nav_relawan -> {
                    loadFragment(RelawanFragment())
                    true
                }
                R.id.nav_riwayat -> {
                    loadFragment(RiwayatFragment())
                    true
                }
                R.id.nav_profil -> {
                    loadFragment(ProfilFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun navigateToTab(menuItemId: Int) {
        bottomNavigation.selectedItemId = menuItemId
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
