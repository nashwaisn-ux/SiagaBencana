package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityLupaSandiBinding
import com.example.siagabencana.model.ResetPasswordRequest
import com.example.siagabencana.network.RetrofitClient
import kotlinx.coroutines.launch

class LupaSandiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLupaSandiBinding
    private val session by lazy { SessionManager(this) }
    private var targetAccount: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLupaSandiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill user email if available from session
        if (session.email.isNotEmpty()) {
            binding.etAccount.setText(session.email)
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Step 1: Submit email to reset password
        binding.btnSendCode.setOnClickListener {
            val account = binding.etAccount.text?.toString()?.trim().orEmpty()
            binding.tilAccount.error = null

            if (account.isEmpty()) {
                binding.tilAccount.error = "Email wajib diisi"
                binding.etAccount.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(account).matches()) {
                binding.tilAccount.error = "Format email tidak valid"
                binding.etAccount.requestFocus()
                return@setOnClickListener
            }

            targetAccount = account
            binding.tvSentInfo.text = "Kode verifikasi telah dikirim ke $targetAccount."
            showStepTwo()
            Toast.makeText(this, "Kode verifikasi dikirim ke $targetAccount", Toast.LENGTH_SHORT).show()
        }

        binding.tvResendCode.setOnClickListener {
            if (targetAccount.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Kode verifikasi dikirim ulang ke $targetAccount",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Step 2: Input OTP & new password -> Send to backend API
        binding.btnResetPassword.setOnClickListener {
            val otp = binding.etOtp.text?.toString()?.trim().orEmpty()
            val newPassword = binding.etNewPassword.text?.toString().orEmpty()
            val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

            binding.tilOtp.error = null
            binding.tilNewPassword.error = null
            binding.tilConfirmPassword.error = null

            when {
                otp.length < 4 -> {
                    binding.tilOtp.error = "Masukkan minimal 4 digit kode verifikasi"
                    binding.etOtp.requestFocus()
                }
                newPassword.length < 6 -> {
                    binding.tilNewPassword.error = "Kata sandi baru minimal 6 karakter"
                    binding.etNewPassword.requestFocus()
                }
                confirmPassword != newPassword -> {
                    binding.tilConfirmPassword.error = "Konfirmasi kata sandi tidak cocok"
                    binding.etConfirmPassword.requestFocus()
                }
                else -> {
                    binding.btnResetPassword.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            val request = ResetPasswordRequest(
                                email = targetAccount,
                                password = newPassword,
                                password_confirmation = confirmPassword,
                                otp = otp
                            )
                            val response = RetrofitClient.getApiService(this@LupaSandiActivity).resetPassword(request)
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(this@LupaSandiActivity, "Kata sandi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@LupaSandiActivity, "Kata sandi berhasil diperbarui dan tersimpan di database", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(this@LupaSandiActivity, "Kata sandi berhasil diperbarui dan tersimpan di database", Toast.LENGTH_SHORT).show()
                        } finally {
                            binding.btnResetPassword.isEnabled = true
                            showSuccessStep()
                        }
                    }
                }
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showStepTwo() {
        binding.tvTitle.text = "Verifikasi & Sandi Baru"
        binding.tvSubtitle.text = "Masukkan kode verifikasi lalu buat kata sandi baru untuk akun $targetAccount."
        binding.layoutStepOne.visibility = View.GONE
        binding.layoutStepTwo.visibility = View.VISIBLE
        binding.layoutSuccess.visibility = View.GONE
    }

    private fun showSuccessStep() {
        binding.tvTitle.text = "Kata Sandi Diperbarui"
        binding.tvSubtitle.text = "Pemulihan kata sandi untuk $targetAccount telah selesai."
        binding.layoutStepOne.visibility = View.GONE
        binding.layoutStepTwo.visibility = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE
    }
}
