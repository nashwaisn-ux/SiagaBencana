package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private val session by lazy { SessionManager(this) }
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Begitu salah satu tombol role dipilih/diklik, langsung masuk ke dashboard role tersebut
        b.toggleRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || isNavigating) return@addOnButtonCheckedListener

            val role = when (checkedId) {
                R.id.btnRoleRelawan -> SessionManager.ROLE_RELAWAN
                R.id.btnRolePetugas -> SessionManager.ROLE_PETUGAS
                R.id.btnRoleMasyarakat -> SessionManager.ROLE_MASYARAKAT
                else -> return@addOnButtonCheckedListener
            }

            enterDashboardWithRole(role)
        }

        // Tetap pertahankan tombol Masuk sebagai alternatif jika user ingin klik Masuk setelah memilih role
        b.btnMasuk.setOnClickListener {
            if (isNavigating) return@setOnClickListener
            enterDashboardWithRole(selectedRole())
        }

        b.tvLupaSandi.setOnClickListener {
            // Opsional: bisa diarahkan atau diberi info singkat
        }

        b.tvDaftar.setOnClickListener {
            // Opsional: pendaftaran menyusul
        }
    }

    private fun selectedRole(): String = when (b.toggleRole.checkedButtonId) {
        R.id.btnRoleRelawan -> SessionManager.ROLE_RELAWAN
        R.id.btnRolePetugas -> SessionManager.ROLE_PETUGAS
        else -> SessionManager.ROLE_MASYARAKAT
    }

    private fun enterDashboardWithRole(role: String) {
        isNavigating = true
        b.tilEmail.error = null
        b.tilPassword.error = null

        val rawEmail = b.etEmail.text?.toString().orEmpty().trim()
        val displayName = rawEmail.substringBefore('@').ifBlank {
            when (role) {
                SessionManager.ROLE_PETUGAS -> "Petugas BPBD"
                SessionManager.ROLE_RELAWAN -> "Relawan Siaga"
                else -> "Masyarakat"
            }
        }

        setLoading(true)

        lifecycleScope.launch {
            delay(150)
            session.save(
                token = "demo-token",
                role = role,
                nama = displayName
            )
            openMain()
        }
    }

    private fun setLoading(loading: Boolean) {
        b.progressLogin.isVisible = loading
        b.btnMasuk.isEnabled = !loading
        b.toggleRole.isEnabled = !loading
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}