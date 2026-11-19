package com.example.siagabencana.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.siagabencana.BuatLaporanActivity
import com.example.siagabencana.MainActivity
import com.example.siagabencana.PetaSiagaActivity
import com.example.siagabencana.R

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnNotifikasi)?.setOnClickListener {
            Toast.makeText(requireContext(), "Tidak ada notifikasi baru", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.aksiLapor)?.setOnClickListener {
            startActivity(Intent(requireContext(), BuatLaporanActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.aksiPeta)?.setOnClickListener {
            startActivity(Intent(requireContext(), PetaSiagaActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.aksiRelawan)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_relawan)
        }

        view.findViewById<LinearLayout>(R.id.aksiRiwayat)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_riwayat)
        }

        view.findViewById<LinearLayout>(R.id.bannerSiaga)?.setOnClickListener {
            startActivity(Intent(requireContext(), PetaSiagaActivity::class.java))
        }

        view.findViewById<TextView>(R.id.tvLihatSemua)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_laporan)
        }
    }
}
