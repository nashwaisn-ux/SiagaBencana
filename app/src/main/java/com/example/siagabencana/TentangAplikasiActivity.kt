package com.example.siagabencana


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.siagabencana.databinding.ActivityTentangAplikasiBinding

class TentangAplikasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTentangAplikasiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTentangAplikasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupVersionInfo()
        setupListeners()
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            binding.tvAppVersion.text = "Versi $versionName (Build $versionCode)"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "Versi 1.0.0"
        }
    }

    private fun setupListeners() {
        // Tombol Kembali
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCallEmergency.setOnClickListener {
            showEmergencyCallDialog()
        }

        binding.itemTerms.setOnClickListener {
            showInfoDialog(
                title = "Syarat & Ketentuan",
                message = "1. Pengguna wajib memberikan informasi kejadian bencana yang benar dan dapat dipertanggungjawabkan.\n" +
                        "2. Penyalahgunaan fitur pelaporan darurat atau informasi palsu dapat dikenai tindakan sesuai aturan yang berlaku.\n" +
                        "3. Data lokasi digunakan untuk menampilkan peringatan radius bahaya dan koordinasi penanganan."
            )
        }

        binding.itemPrivacy.setOnClickListener {
            showInfoDialog(
                title = "Kebijakan Privasi",
                message = "Kami menjaga keamanan data pribadi dan lokasi Anda. Informasi lokasi hanya dipakai untuk fitur Peta Siaga, notifikasi radius bahaya, dan pelaporan kejadian darurat tanpa dijual kepada pihak ketiga."
            )
        }

        // Menu Tim Pengembang & Mitra
        binding.itemCredits.setOnClickListener {
            showInfoDialog(
                title = "Tim Pengembang & Mitra",
                message = "Dikembangkan untuk mendukung kesiapsiagaan bencana, pelaporan cepat masyarakat, dan koordinasi relawan di lapangan.\n\n" +
                        "Kontak Bantuan: support@siagabencana.id"
            )
        }
    }

    private fun showEmergencyCallDialog() {
        val options = arrayOf(
            "BNPB (117) - Penanggulangan Bencana",
            "BASARNAS (115) - Pencarian & Pertolongan"
        )
        val numbers = arrayOf("117", "115")

        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Layanan Darurat")
            .setItems(options) { _, which ->
                dialNumber(numbers[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun dialNumber(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
    }

    private fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Tutup", null)
            .show()
    }
}