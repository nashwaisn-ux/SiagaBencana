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
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.BuatLaporanActivity
import com.example.siagabencana.MainActivity
import com.example.siagabencana.PetaSiagaActivity
import com.example.siagabencana.R
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.network.RetrofitClient
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val tvNama = view.findViewById<TextView>(R.id.tvNama)
        val tvStatBaru = view.findViewById<TextView>(R.id.tvStatBaru)
        val tvStatProses = view.findViewById<TextView>(R.id.tvStatProses)

        tvNama?.text = sessionManager.getUserName().ifEmpty { "Pengguna" }

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
            (activity as? MainActivity)?.navigateTo(R.id.nav_relawan)
        }

        view.findViewById<LinearLayout>(R.id.aksiRiwayat)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_riwayat)
        }

        view.findViewById<LinearLayout>(R.id.bannerSiaga)?.setOnClickListener {
            startActivity(Intent(requireContext(), PetaSiagaActivity::class.java))
        }

        view.findViewById<TextView>(R.id.tvLihatSemua)?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_laporan)
        }

        // Fetch dashboard stats from backend API
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(requireContext()).getDashboard()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        tvStatBaru?.text = (data.per_status["baru"] ?: 0).toString()
                        val prosesCount = (data.per_status["diverifikasi"] ?: 0) + (data.per_status["ditangani"] ?: 0)
                        tvStatProses?.text = prosesCount.toString()
                    }
                }
            } catch (_: Exception) {
                // Keep default/placeholder text on network failure
            }
        }
    }
}