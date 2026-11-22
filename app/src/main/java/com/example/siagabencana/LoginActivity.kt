package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.model.LoginRequest
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        RetrofitClient.isRedirecting.set(false)
        sessionManager = SessionManager(this)

        if (intent.getBooleanExtra("EXTRA_SESSION_EXPIRED", false)) {
            Toast.makeText(this, "Sesi berakhir, silakan masuk lagi", Toast.LENGTH_LONG).show()
        }

        val toggleRole = findViewById<MaterialButtonToggleGroup>(R.id.toggleRole)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val progressLogin = findViewById<LinearProgressIndicator>(R.id.progressLogin)
        val btnMasuk = findViewById<Button>(R.id.btnMasuk)
        val tvDaftar = findViewById<TextView>(R.id.tvDaftar)

        tvDaftar?.setOnClickListener {
            startActivity(Intent(this, RegistrasiActivity::class.java))
        }

        btnMasuk?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val password = etPassword?.text?.toString() ?: ""

            if (email.isEmpty()) {
                tilEmail?.error = "Email wajib diisi"
                return@setOnClickListener
            } else {
                tilEmail?.error = null
            }

            if (password.isEmpty()) {
                tilPassword?.error = "Kata sandi wajib diisi"
                return@setOnClickListener
            } else {
                tilPassword?.error = null
            }

            val selectedRole = when (toggleRole?.checkedButtonId) {
                R.id.btnRolePetugas -> SessionManager.ROLE_PETUGAS
                R.id.btnRoleRelawan -> SessionManager.ROLE_RELAWAN
                else -> SessionManager.ROLE_MASYARAKAT
            }

            progressLogin?.visibility = View.VISIBLE
            btnMasuk.isEnabled = false

            lifecycleScope.launch {
                val res = apiCall {
                    RetrofitClient.getApiService(this@LoginActivity).login(
                        LoginRequest(email = email, password = password)
                    )
                }

                progressLogin?.visibility = View.GONE
                btnMasuk.isEnabled = true

                when (res) {
                    is ApiResult.Success -> {
                        val authData = res.data
                        if (authData != null) {
                            val serverRole = authData.user.role ?: SessionManager.ROLE_MASYARAKAT
                            if (serverRole != selectedRole) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Akun ini terdaftar sebagai ${serverRole.replaceFirstChar { it.uppercase() }}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            sessionManager.save(
                                token = authData.token,
                                role = serverRole,
                                nama = authData.user.nama,
                                email = authData.user.email,
                                id = authData.user.id,
                                noHp = authData.user.no_hp,
                                alamat = authData.user.alamat,
                                fotoUrl = authData.user.foto_url
                            )

                            Toast.makeText(
                                this@LoginActivity,
                                "Selamat datang, ${authData.user.nama}",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Data pengguna tidak ditemukan", Toast.LENGTH_LONG).show()
                        }
                    }
                    is ApiResult.Failure -> {
                        Toast.makeText(this@LoginActivity, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
