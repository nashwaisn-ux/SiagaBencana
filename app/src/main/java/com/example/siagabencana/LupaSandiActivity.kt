package com.example.siagabencana

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.databinding.ActivityLupaSandiBinding
import com.example.siagabencana.model.ResetPasswordRequest
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.launch

class LupaSandiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLupaSandiBinding
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLupaSandiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etEmail.setText(intent.getStringExtra(EXTRA_EMAIL).orEmpty())

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSimpan.setOnClickListener {
            if (!isSubmitting) validateAndSubmit()
        }
    }

    private fun validateAndSubmit() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val newPass = binding.etNewPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        binding.tilEmail.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email wajib diisi"
                binding.etEmail.requestFocus()
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Format email tidak valid"
                binding.etEmail.requestFocus()
            }
            newPass.length < 6 -> {
                binding.tilNewPassword.error = "Kata sandi baru minimal 6 karakter"
                binding.etNewPassword.requestFocus()
            }
            confirm != newPass -> {
                binding.tilConfirmPassword.error = "Konfirmasi kata sandi tidak cocok"
                binding.etConfirmPassword.requestFocus()
            }
            else -> submit(email, newPass, confirm)
        }
    }

    private fun submit(email: String, newPass: String, confirm: String) {
        isSubmitting = true
        setLoading(true)

        lifecycleScope.launch {
            val res = apiCall {
                RetrofitClient.getApiService(this@LupaSandiActivity).resetPassword(
                    ResetPasswordRequest(
                        email = email,
                        password = newPass,
                        password_confirmation = confirm
                    )
                )
            }

            setLoading(false)
            isSubmitting = false

            when (res) {
                is ApiResult.Success -> {
                    Toast.makeText(
                        this@LupaSandiActivity,
                        "Kata sandi berhasil diubah, silakan masuk",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                is ApiResult.Failure -> {
                    Toast.makeText(this@LupaSandiActivity, res.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressSave.isVisible = loading
        binding.btnSimpan.isEnabled = !loading
    }

    companion object {
        const val EXTRA_EMAIL = "EXTRA_EMAIL"
    }
}
