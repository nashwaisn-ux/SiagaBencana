package com.example.siagabencana

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityRegistrasiRelawanBinding
import com.example.siagabencana.model.TambahRelawanRequest
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.ui.toast
import kotlinx.coroutines.launch

class RegistrasiRelawanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrasiRelawanBinding
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrasiRelawanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        cekAksesPetugas()
        setupActions()
    }

    private fun isPetugas(): Boolean {
        return session.role == SessionManager.ROLE_PETUGAS
    }

    private fun setupToolbar() {
        binding.toolbarRegistrasiRelawan.setNavigationOnClickListener {
            finish()
        }
    }

    private fun cekAksesPetugas() {
        if (!isPetugas()) {
            binding.tvLabelModePetugas.text = "Akses Terbatas"
            binding.tvDeskripsiModePetugas.text =
                "Hanya akun Petugas yang dapat mendaftarkan relawan baru."
            binding.btnSimpanRelawan.isEnabled = false
            toast("Hanya Petugas yang dapat melakukan registrasi relawan")
        }
    }

    private fun setupActions() {
        binding.btnBatalRegistrasiRelawan.setOnClickListener {
            finish()
        }

        binding.btnSimpanRelawan.setOnClickListener {
            if (!isPetugas()) {
                toast("Akses ditolak: hanya Petugas yang berwenang")
                return@setOnClickListener
            }

            if (validasiForm()) {
                simpanDataRelawan()
            }
        }
    }

    private fun validasiForm(): Boolean {
        bersihkanError()

        val nama = binding.etNamaRelawan.text?.toString()?.trim().orEmpty()
        val noHp = binding.etNoHpRelawan.text?.toString()?.trim().orEmpty()
        val email = binding.etEmailRelawan.text?.toString()?.trim().orEmpty()
        val posko = binding.etPoskoRelawan.text?.toString()?.trim().orEmpty()
        val spesialisasi = binding.etSpesialisasiRelawan.text?.toString()?.trim().orEmpty()
        val password = binding.etPasswordRelawan.text?.toString().orEmpty()

        var valid = true

        if (nama.isEmpty()) {
            binding.tilNamaRelawan.error = "Nama lengkap relawan wajib diisi"
            valid = false
        }

        if (noHp.isEmpty()) {
            binding.tilNoHpRelawan.error = "Nomor HP / WhatsApp wajib diisi"
            valid = false
        } else if (noHp.length < 9) {
            binding.tilNoHpRelawan.error = "Nomor HP minimal 9 digit"
            valid = false
        }

        if (email.isEmpty()) {
            binding.tilEmailRelawan.error = "Email relawan wajib diisi"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmailRelawan.error = "Format email tidak valid"
            valid = false
        }

        if (posko.isEmpty()) {
            binding.tilPoskoRelawan.error = "Posko / wilayah penugasan wajib diisi"
            valid = false
        }

        if (spesialisasi.isEmpty()) {
            binding.tilSpesialisasiRelawan.error = "Spesialisasi relawan wajib diisi"
            valid = false
        }

        if (password.isEmpty()) {
            binding.tilPasswordRelawan.error = "Password awal wajib diisi"
            valid = false
        } else if (password.length < 6) {
            binding.tilPasswordRelawan.error = "Password minimal 6 karakter"
            valid = false
        }

        return valid
    }

    private fun bersihkanError() {
        binding.tilNamaRelawan.error = null
        binding.tilNoHpRelawan.error = null
        binding.tilEmailRelawan.error = null
        binding.tilPoskoRelawan.error = null
        binding.tilSpesialisasiRelawan.error = null
        binding.tilPasswordRelawan.error = null
    }

    private fun simpanDataRelawan() {
        val nama = binding.etNamaRelawan.text?.toString()?.trim().orEmpty()
        val email = binding.etEmailRelawan.text?.toString()?.trim().orEmpty()
        val noHp = binding.etNoHpRelawan.text?.toString()?.trim().orEmpty()
        val password = binding.etPasswordRelawan.text?.toString().orEmpty()
        val posko = binding.etPoskoRelawan.text?.toString()?.trim().orEmpty()
        val spesialisasi = binding.etSpesialisasiRelawan.text?.toString()?.trim().orEmpty()

        binding.btnSimpanRelawan.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(this@RegistrasiRelawanActivity)
                val response = api.tambahRelawan(
                    TambahRelawanRequest(
                        nama = nama,
                        email = email,
                        no_hp = noHp,
                        password = password,
                        posko = posko,
                        spesialisasi = spesialisasi
                    )
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@RegistrasiRelawanActivity,
                        "Relawan \"$nama\" berhasil didaftarkan!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val msg = response.body()?.message ?: "Gagal mendaftarkan relawan"
                    toast(msg)
                }
            } catch (e: Exception) {
                toast("Gagal terhubung ke server")
            } finally {
                binding.btnSimpanRelawan.isEnabled = true
            }
        }
    }
}
