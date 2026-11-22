package com.example.siagabencana.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siagabencana.LoginActivity
import com.example.siagabencana.R
import com.example.siagabencana.TentangAplikasiActivity
import com.example.siagabencana.UbahDataDiriActivity
import com.example.siagabencana.UbahPasswordActivity
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var tvNamaProfil: TextView? = null
    private var tvRole: TextView? = null
    private var tvAvatar: TextView? = null
    private var imgAvatarProfil: ImageView? = null

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

        tvNamaProfil = view.findViewById(R.id.tvNamaProfil)
        tvRole = view.findViewById(R.id.tvRole)
        tvAvatar = view.findViewById(R.id.tvAvatar)
        imgAvatarProfil = view.findViewById(R.id.imgAvatarProfil)

        val btnKeluar = view.findViewById<MaterialButton>(R.id.btnKeluar)
        val menuUbahData = view.findViewById<LinearLayout>(R.id.menuUbahData)
        val menuUbahSandi = view.findViewById<LinearLayout>(R.id.menuUbahSandi)
        val menuTentang = view.findViewById<LinearLayout>(R.id.menuTentang)

        updateProfileUI()

        menuUbahData?.setOnClickListener {
            startActivity(Intent(requireContext(), UbahDataDiriActivity::class.java))
        }

        menuUbahSandi?.setOnClickListener {
            startActivity(Intent(requireContext(), UbahPasswordActivity::class.java))
        }

        menuTentang?.setOnClickListener {
            startActivity(Intent(requireContext(), TentangAplikasiActivity::class.java))
        }

        btnKeluar?.setOnClickListener {
            btnKeluar.isEnabled = false
            lifecycleScope.launch {
                try {
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

    override fun onResume() {
        super.onResume()
        updateProfileUI()
    }

    private fun updateProfileUI() {
        val userName = sessionManager.getUserName().ifEmpty { "Masyarakat" }
        tvNamaProfil?.text = userName

        val role = sessionManager.getUserRole()
        tvRole?.text = role.replaceFirstChar { it.uppercase() }

        val photoUrl = sessionManager.getUserFotoUrl()
        if (!photoUrl.isNullOrEmpty()) {
            try {
                if (imgAvatarProfil != null) {
                    Glide.with(this)
                        .load(Uri.parse(photoUrl))
                        .circleCrop()
                        .into(imgAvatarProfil!!)
                }
                imgAvatarProfil?.isVisible = true
                tvAvatar?.isVisible = false
            } catch (_: Exception) {
                showInitialAvatar(userName)
            }
        } else {
            showInitialAvatar(userName)
        }
    }

    private fun showInitialAvatar(name: String) {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val initial = when {
            parts.isEmpty() -> "SB"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
        tvAvatar?.text = initial
        tvAvatar?.isVisible = true
        imgAvatarProfil?.isVisible = false
    }
}
