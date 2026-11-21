package com.example.siagabencana


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.siagabencana.databinding.ActivityLupaSandiBinding

class LupaSandiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLupaSandiBinding
    private var targetAccount: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLupaSandiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Tombol kembali ke halaman sebelumnya
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tahap 1: Kirim kode verifikasi
        binding.btnSendCode.setOnClickListener {
            val account = binding.etAccount.text?.toString()?.trim().orEmpty()
            binding.tilAccount.error = null

            if (account.isEmpty()) {
                binding.tilAccount.error = "Email atau nomor HP wajib diisi"
                binding.etAccount.requestFocus()
                return@setOnClickListener
            }

            targetAccount = account
            // TODO: Hubungkan ke endpoint API kirim OTP/reset password jika sudah tersedia
            binding.tvSentInfo.text = "Kode verifikasi telah dikirim ke $targetAccount."
            showStepTwo()
            Toast.makeText(this, "Kode verifikasi dikirim", Toast.LENGTH_SHORT).show()
        }

        // Tahap 2: Kirim ulang kode verifikasi
        binding.tvResendCode.setOnClickListener {
            if (targetAccount.isNotEmpty()) {
                // TODO: Panggil ulang endpoint kirim kode verifikasi
                Toast.makeText(
                    this,
                    "Kode verifikasi dikirim ulang ke $targetAccount",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Tahap 2: Validasi OTP & simpan kata sandi baru
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
                    // TODO: Hubungkan ke endpoint API reset password di backend
                    showSuccessStep()
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
        binding.tvSubtitle.text = "Masukkan kode verifikasi lalu buat kata sandi baru untuk akun Anda."
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