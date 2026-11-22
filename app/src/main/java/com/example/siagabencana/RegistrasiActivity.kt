package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityRegistrasiBinding
import com.example.siagabencana.model.RegisterRequest
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.launch

class RegistrasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrasiBinding
    private val session by lazy { SessionManager(this) }
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.tvMasuk.setOnClickListener {
            if (isSubmitting) return@setOnClickListener
            finish()
        }

        binding.cbAgreement.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.cbAgreement.error = null
            }
        }

        binding.btnDaftar.setOnClickListener {
            if (isSubmitting) return@setOnClickListener
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.cbAgreement.error = null

        when {
            fullName.isEmpty() -> {
                binding.tilFullName.error = "Nama lengkap wajib diisi"
                binding.etFullName.requestFocus()
            }
            email.isEmpty() -> {
                binding.tilEmail.error = "Email wajib diisi"
                binding.etEmail.requestFocus()
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Format email tidak valid"
                binding.etEmail.requestFocus()
            }
            phone.isEmpty() -> {
                binding.tilPhone.error = "Nomor HP / WhatsApp wajib diisi"
                binding.etPhone.requestFocus()
            }
            phone.length < 8 -> {
                binding.tilPhone.error = "Nomor HP minimal 8 digit"
                binding.etPhone.requestFocus()
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Kata sandi minimal 6 karakter"
                binding.etPassword.requestFocus()
            }
            confirmPassword != password -> {
                binding.tilConfirmPassword.error = "Konfirmasi kata sandi tidak cocok"
                binding.etConfirmPassword.requestFocus()
            }
            !binding.cbAgreement.isChecked -> {
                binding.cbAgreement.error = "Harap setujui ketentuan terlebih dahulu"
                Toast.makeText(
                    this,
                    "Centang persetujuan ketentuan untuk melanjutkan",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                submitRegistration(fullName = fullName, email = email)
            }
        }
    }

    private fun submitRegistration(fullName: String, email: String) {
        isSubmitting = true
        setLoading(true)

        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        lifecycleScope.launch {
            val res = apiCall {
                RetrofitClient.getApiService(this@RegistrasiActivity).register(
                    RegisterRequest(
                        nama = fullName,
                        email = email,
                        password = password,
                        password_confirmation = confirmPassword,
                        no_hp = phone.ifEmpty { null }
                    )
                )
            }

            setLoading(false)
            isSubmitting = false

            when (res) {
                is ApiResult.Success -> {
                    val auth = res.data
                    if (auth == null) {
                        Toast.makeText(
                            this@RegistrasiActivity,
                            "Respons server tidak valid",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    session.save(
                        token = auth.token,
                        role = auth.user.role ?: SessionManager.ROLE_MASYARAKAT,
                        nama = auth.user.nama,
                        email = auth.user.email,
                        id = auth.user.id,
                        noHp = auth.user.no_hp,
                        alamat = auth.user.alamat,
                        fotoUrl = auth.user.foto_url
                    )
                    Toast.makeText(
                        this@RegistrasiActivity,
                        "Akun $email berhasil didaftarkan!",
                        Toast.LENGTH_SHORT
                    ).show()
                    val intent = Intent(this@RegistrasiActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is ApiResult.Failure -> {
                    Toast.makeText(this@RegistrasiActivity, res.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressRegister.isVisible = loading
        binding.btnDaftar.isEnabled = !loading
        binding.tvMasuk.isEnabled = !loading
        binding.cbAgreement.isEnabled = !loading
    }
}
