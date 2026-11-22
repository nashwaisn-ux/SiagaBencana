package com.example.siagabencana

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.databinding.ActivityUbahPasswordBinding
import com.example.siagabencana.model.ChangePasswordRequest
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.launch

class UbahPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUbahPasswordBinding
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbahPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSimpan.setOnClickListener {
            if (!isSubmitting) validateAndSubmit()
        }
    }

    private fun validateAndSubmit() {
        val oldPass = binding.etOldPassword.text?.toString().orEmpty()
        val newPass = binding.etNewPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        when {
            oldPass.isEmpty() -> {
                binding.tilOldPassword.error = "Kata sandi lama wajib diisi"
                binding.etOldPassword.requestFocus()
            }
            newPass.length < 6 -> {
                binding.tilNewPassword.error = "Kata sandi baru minimal 6 karakter"
                binding.etNewPassword.requestFocus()
            }
            newPass == oldPass -> {
                binding.tilNewPassword.error = "Kata sandi baru harus berbeda dari yang lama"
                binding.etNewPassword.requestFocus()
            }
            confirm != newPass -> {
                binding.tilConfirmPassword.error = "Konfirmasi kata sandi tidak cocok"
                binding.etConfirmPassword.requestFocus()
            }
            else -> submit(oldPass, newPass, confirm)
        }
    }

    private fun submit(oldPass: String, newPass: String, confirm: String) {
        isSubmitting = true
        setLoading(true)

        lifecycleScope.launch {
            val res = apiCall {
                RetrofitClient.getApiService(this@UbahPasswordActivity).changePassword(
                    ChangePasswordRequest(
                        current_password = oldPass,
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
                        this@UbahPasswordActivity,
                        "Kata sandi berhasil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is ApiResult.Failure -> {
                    Toast.makeText(this@UbahPasswordActivity, res.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressSave.isVisible = loading
        binding.btnSimpan.isEnabled = !loading
    }
}
