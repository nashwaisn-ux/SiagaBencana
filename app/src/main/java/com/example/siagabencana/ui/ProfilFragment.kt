package com.example.siagabencana.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.LoginActivity
import com.example.siagabencana.R
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val tvNamaProfil = view.findViewById<TextView>(R.id.tvNamaProfil)
        val tvRole = view.findViewById<TextView>(R.id.tvRole)
        val btnKeluar = view.findViewById<MaterialButton>(R.id.btnKeluar)

        // Display user info from session
        tvNamaProfil?.text = sessionManager.getUserName().ifEmpty { "Pengguna" }
        val role = sessionManager.getUserRole()
        tvRole?.text = role.replaceFirstChar { it.uppercase() }

        btnKeluar?.setOnClickListener {
            btnKeluar.isEnabled = false
            lifecycleScope.launch {
                try {
                    // Call backend logout
                    RetrofitClient.getApiService(requireContext()).logout()
                } catch (_: Exception) {
                    // Ignore network failure on logout and clear local session anyway
                } finally {
                    sessionManager.clearSession()
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
    }
}
