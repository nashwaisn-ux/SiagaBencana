package com.example.siagabencana.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.siagabencana.LoginActivity
import com.example.siagabencana.R
import com.example.siagabencana.TentangAplikasiActivity
import com.example.siagabencana.UbahDataDiriActivity
import com.example.siagabencana.data.SessionManager

class ProfilFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.menuUbahData)?.setOnClickListener {
            val intent = Intent(requireContext(), UbahDataDiriActivity::class.java)
            startActivity(intent)
        }

        view.findViewById<View>(R.id.menuTentang)?.setOnClickListener {
            val intent = Intent(requireContext(), TentangAplikasiActivity::class.java)
            startActivity(intent)
        }

        // Sesuaikan R.id.btnKeluar dengan ID tombol Keluar di layout Anda
        view.findViewById<View>(R.id.btnKeluar)?.setOnClickListener {
            logoutToLogin()
        }
    }

    private fun logoutToLogin() {
        val session = SessionManager(requireContext())

        session.save(
            token = "",
            role = "",
            nama = ""
        )

        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }
}