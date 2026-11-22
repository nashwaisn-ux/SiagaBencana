package com.example.siagabencana

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityDetailProfilBinding
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetailProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailProfilBinding
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKembaliDetailProfil.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        tampilkanDariSession()
        segarkanDariServer()
    }

    private fun segarkanDariServer() {
        lifecycleScope.launch {
            val res = apiCall {
                RetrofitClient.getApiService(this@DetailProfilActivity).getMe()
            }
            when (res) {
                is ApiResult.Success -> {
                    val user = res.data ?: return@launch
                    session.saveUserFromApi(user)
                    tampilkanDariSession()
                }
                is ApiResult.Failure -> {
                    if (res.code != 401) {
                        Toast.makeText(
                            this@DetailProfilActivity,
                            "Tidak dapat memuat data terbaru. Menampilkan data tersimpan.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun tampilkanDariSession() {
        val nama = session.nama.ifBlank { "Pengguna" }
        val role = session.role
        val isRelawan = role == SessionManager.ROLE_RELAWAN

        binding.tvDetailNama.text = nama
        binding.tvDetailRole.text = role.replaceFirstChar { it.uppercase() }
        binding.tvDetailEmail.text = session.email.ifBlank { "-" }
        binding.tvDetailTelepon.text = session.noHp.ifBlank { "-" }
        binding.tvDetailAlamat.text = session.alamat.ifBlank { "-" }
        binding.tvLabelAlamat.text = if (isRelawan) "Alamat / Posko" else "Alamat"

        val spesialisasi = session.spesialisasi
        binding.layoutSpesialisasi.visibility =
            if (isRelawan && spesialisasi.isNotBlank()) View.VISIBLE else View.GONE
        binding.tvDetailSpesialisasi.text = spesialisasi.ifBlank { "-" }

        binding.tvDetailBergabung.text = formatBergabung(session.createdAt)

        binding.tvDetailStatusAkun.text = "Aktif"
        binding.tvDetailStatusAkun.setTextColor(ContextCompat.getColor(this, R.color.siaga_green))

        tampilkanFoto(session.fotoUrl, nama)
    }

    private fun tampilkanFoto(fotoUrl: String?, nama: String) {
        // Inisial selalu ada di bawah; foto (jika ada) menutupinya.
        binding.tvDetailAvatar.text = buatInisial(nama)
        binding.tvDetailAvatar.visibility = View.VISIBLE

        if (fotoUrl.isNullOrBlank()) {
            Glide.with(this).clear(binding.imgDetailAvatar)
            binding.imgDetailAvatar.visibility = View.GONE
        } else {
            binding.imgDetailAvatar.visibility = View.VISIBLE
            Glide.with(this)
                .load(fotoUrl)
                .circleCrop()
                .into(binding.imgDetailAvatar)
        }
    }

    private fun formatBergabung(iso: String): String {
        if (iso.isBlank()) return "-"
        return try {
            // Backend mengirim toIso8601String(), mis. 2026-11-22T04:18:00+00:00
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            val date = parser.parse(iso) ?: return "-"
            SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }.format(date)
        } catch (_: Exception) {
            "-"
        }
    }

    private fun buatInisial(nama: String): String {
        val bagian = nama.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            bagian.isEmpty() -> "SB"
            bagian.size == 1 -> bagian[0].take(2).uppercase()
            else -> "${bagian.first().first()}${bagian.last().first()}".uppercase()
        }
    }
}
