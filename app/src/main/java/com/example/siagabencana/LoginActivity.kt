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
import com.example.siagabencana.model.RegisterRequest
import com.example.siagabencana.network.RetrofitClient
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        val tvHeaderTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvHeaderSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
        val tvRoleLabel = findViewById<TextView>(R.id.tvRoleLabel)
        val toggleRole = findViewById<MaterialButtonToggleGroup>(R.id.toggleRole)

        val tilNama = findViewById<TextInputLayout>(R.id.tilNama)
        val etNama = findViewById<EditText>(R.id.etNama)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val etEmail = findViewById<EditText>(R.id.etEmail)

        val tilNoHp = findViewById<TextInputLayout>(R.id.tilNoHp)
        val etNoHp = findViewById<EditText>(R.id.etNoHp)

        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        val tvLupaSandi = findViewById<TextView>(R.id.tvLupaSandi)
        val progressLogin = findViewById<LinearProgressIndicator>(R.id.progressLogin)
        val btnMasuk = findViewById<Button>(R.id.btnMasuk)
        val tvDaftar = findViewById<TextView>(R.id.tvDaftar)
        val tvInfoRole = findViewById<TextView>(R.id.tvInfoRole)

        fun updateUI() {
            if (isRegisterMode) {
                tvHeaderTitle?.text = "Buat akun baru"
                tvHeaderSubtitle?.text = "Daftar sebagai masyarakat untuk melaporkan bencana."
                tvRoleLabel?.visibility = View.GONE
                toggleRole?.visibility = View.GONE
                tilNama?.visibility = View.VISIBLE
                tilNoHp?.visibility = View.VISIBLE
                tilConfirmPassword?.visibility = View.VISIBLE
                tvLupaSandi?.visibility = View.GONE
                btnMasuk?.text = "Daftar Akun"
                tvDaftar?.text = "Sudah punya akun? Masuk"
                tvInfoRole?.visibility = View.GONE
            } else {
                tvHeaderTitle?.text = "Selamat datang kembali"
                tvHeaderSubtitle?.text = "Masuk untuk melapor dan memantau penanganan bencana."
                tvRoleLabel?.visibility = View.VISIBLE
                toggleRole?.visibility = View.VISIBLE
                tilNama?.visibility = View.GONE
                tilNoHp?.visibility = View.GONE
                tilConfirmPassword?.visibility = View.GONE
                tvLupaSandi?.visibility = View.VISIBLE
                btnMasuk?.text = "Masuk"
                tvDaftar?.text = "Belum punya akun? Daftar"
                tvInfoRole?.visibility = View.VISIBLE
            }
        }

        tvDaftar?.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateUI()
        }

        tvLupaSandi?.setOnClickListener {
            Toast.makeText(this, "Silakan hubungi administrator BPBD untuk reset kata sandi.", Toast.LENGTH_LONG).show()
        }

        btnMasuk?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val password = etPassword?.text?.toString()?.trim() ?: ""

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

            if (isRegisterMode) {
                val nama = etNama?.text?.toString()?.trim() ?: ""
                val noHp = etNoHp?.text?.toString()?.trim() ?: ""
                val confirmPassword = etConfirmPassword?.text?.toString()?.trim() ?: ""

                if (nama.isEmpty()) {
                    tilNama?.error = "Nama lengkap wajib diisi"
                    return@setOnClickListener
                } else {
                    tilNama?.error = null
                }

                if (password.length < 8) {
                    tilPassword?.error = "Kata sandi minimal 8 karakter"
                    return@setOnClickListener
                }

                if (password != confirmPassword) {
                    tilConfirmPassword?.error = "Konfirmasi kata sandi tidak cocok"
                    return@setOnClickListener
                } else {
                    tilConfirmPassword?.error = null
                }

                // Call Register API
                progressLogin?.visibility = View.VISIBLE
                btnMasuk.isEnabled = false

                lifecycleScope.launch {
                    try {
                        val request = RegisterRequest(
                            nama = nama,
                            email = email,
                            password = password,
                            password_confirmation = confirmPassword,
                            no_hp = noHp.ifEmpty { null }
                        )
                        val response = RetrofitClient.getApiService(this@LoginActivity).register(request)
                        progressLogin?.visibility = View.GONE
                        btnMasuk.isEnabled = true

                        if (response.isSuccessful && response.body()?.success == true) {
                            val authData = response.body()?.data
                            if (authData != null) {
                                sessionManager.saveAuthToken(authData.token)
                                sessionManager.saveUser(
                                    id = authData.user.id,
                                    nama = authData.user.nama,
                                    email = authData.user.email,
                                    role = authData.user.role,
                                    noHp = authData.user.no_hp
                                )
                                Toast.makeText(this@LoginActivity, "Registrasi Berhasil! Selamat datang, ${authData.user.nama}", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        } else {
                            val errorJson = response.errorBody()?.string()
                            val errorMsg = parseErrorMessage(errorJson, "Registrasi gagal")
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        progressLogin?.visibility = View.GONE
                        btnMasuk.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Gagal terhubung ke server: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Call Login API
                progressLogin?.visibility = View.VISIBLE
                btnMasuk.isEnabled = false

                lifecycleScope.launch {
                    try {
                        val request = LoginRequest(email = email, password = password)
                        val response = RetrofitClient.getApiService(this@LoginActivity).login(request)
                        progressLogin?.visibility = View.GONE
                        btnMasuk.isEnabled = true

                        if (response.isSuccessful && response.body()?.success == true) {
                            val authData = response.body()?.data
                            if (authData != null) {
                                sessionManager.saveAuthToken(authData.token)
                                sessionManager.saveUser(
                                    id = authData.user.id,
                                    nama = authData.user.nama,
                                    email = authData.user.email,
                                    role = authData.user.role,
                                    noHp = authData.user.no_hp
                                )
                                Toast.makeText(this@LoginActivity, "Login Berhasil! Selamat datang, ${authData.user.nama}", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        } else {
                    val errorJson = response.errorBody()?.string()
                            val errorMsg = parseErrorMessage(errorJson, "Email atau kata sandi salah")
                            Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        progressLogin?.visibility = View.GONE
                        btnMasuk.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Gagal terhubung ke server: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun parseErrorMessage(errorJson: String?, defaultMsg: String): String {
        return try {
            val json = JSONObject(errorJson ?: "")
            val msg = json.optString("message", defaultMsg)
            val errorsObj = json.optJSONObject("errors")
            if (errorsObj != null) {
                val keys = errorsObj.keys()
                val sb = StringBuilder(msg)
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = errorsObj.optJSONArray(key)
                    if (arr != null && arr.length() > 0) {
                        sb.append("\n• ").append(arr.getString(0))
                    }
                }
                sb.toString()
            } else {
                msg
            }
        } catch (_: Exception) {
            defaultMsg
        }
    }
}
