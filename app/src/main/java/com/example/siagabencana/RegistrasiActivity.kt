package com.example.siagabencana

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.databinding.ActivityRegistrasiBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegistrasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrasiBinding
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
                submitRegistration(fullName = fullName, email = email, phone = phone)
            }
        }
    }

    private fun simpanSesiMasyarakat(fullName: String, email: String, phone: String) {
        getSharedPreferences("siaga_session", Context.MODE_PRIVATE)
            .edit()
            .putString("token", "demo-token")
            .putString("role", ROLE_MASYARAKAT)
            .putString("nama", fullName)
            .putString("email", email)
            .putString("phone", phone)
            .apply()
    }

    private fun submitRegistration(fullName: String, email: String, phone: String) {
        isSubmitting = true
        setLoading(true)

        lifecycleScope.launch {
            // Simulasi proses pendaftaran; ganti bagian ini dengan pemanggilan API backend jika sudah siap
            delay(300)

            simpanSesiMasyarakat(fullName = fullName, email = email, phone = phone)

            Toast.makeText(
                this@RegistrasiActivity,
                "Akun $email berhasil didaftarkan sebagai Masyarakat",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this@RegistrasiActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressRegister.isVisible = loading
        binding.btnDaftar.isEnabled = !loading
        binding.tvMasuk.isEnabled = !loading
        binding.cbAgreement.isEnabled = !loading
    }

    companion object {
        private const val ROLE_MASYARAKAT = "masyarakat"
    }
}