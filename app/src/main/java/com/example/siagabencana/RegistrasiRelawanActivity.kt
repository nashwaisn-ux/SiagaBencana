package com.example.siagabencana

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.siagabencana.databinding.ActivityRegistrasiRelawanBinding
import com.example.siagabencana.ui.toast
import java.util.Locale

class RegistrasiRelawanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrasiRelawanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrasiRelawanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        cekAksesPetugas()
        setupActions()
    }

    private fun bacaRoleAktif(): String {
        val prefs = getSharedPreferences("siaga_session", Context.MODE_PRIVATE)
        val rawRole = prefs.getString("role", "")
            ?: prefs.getString("user_role", "")
            ?: ""
        return rawRole.trim().lowercase(Locale.ROOT)
    }

    private fun isPetugas(): Boolean {
        val role = bacaRoleAktif()
        return role == ROLE_PETUGAS || role == "role_petugas"
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
        val posko = binding.etPoskoRelawan.text?.toString()?.trim().orEmpty()
        val spesialisasi = binding.etSpesialisasiRelawan.text?.toString()?.trim().orEmpty()
        val siapBertugas = binding.switchStatusAktifRelawan.isChecked

        // TODO: Hubungkan ke endpoint API registrasi relawan (mis. POST /api/relawan/register)
        val statusLabel = if (siapBertugas) "Siap Bertugas" else "Nonaktif"
        Toast.makeText(
            this,
            "Relawan \"$nama\" ($spesialisasi • $posko) terdaftar [$statusLabel]",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    companion object {
        private const val ROLE_PETUGAS = "petugas"
    }
}