package com.example.siagabencana.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.siagabencana.RegistrasiRelawanActivity
import com.example.siagabencana.databinding.FragmentRelawanBinding
import java.util.Locale

class RelawanFragment : Fragment() {

    private var _binding: FragmentRelawanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelawanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTombolTambahRelawan()
    }

    private fun bacaRoleAktif(): String {
        val prefs = requireContext().getSharedPreferences("siaga_session", Context.MODE_PRIVATE)
        val rawRole = prefs.getString("role", "")
            ?: prefs.getString("user_role", "")
            ?: ""
        return rawRole.trim().lowercase(Locale.ROOT)
    }

    private fun isRolePetugas(): Boolean {
        val role = bacaRoleAktif()
        return role == ROLE_PETUGAS || role == "role_petugas"
    }

    private fun setupTombolTambahRelawan() {
        val petugas = isRolePetugas()
        binding.fabTambahRelawan.isVisible = petugas

        binding.fabTambahRelawan.setOnClickListener {
            if (!isRolePetugas()) {
                Toast.makeText(
                    requireContext(),
                    "Hanya Petugas yang dapat mendaftarkan relawan",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            startActivity(Intent(requireContext(), RegistrasiRelawanActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ROLE_PETUGAS = "petugas"
    }
}