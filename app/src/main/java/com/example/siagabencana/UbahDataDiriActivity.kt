package com.example.siagabencana

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.siagabencana.databinding.ActivityUbahDataDiriBinding

class UbahDataDiriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUbahDataDiriBinding
    private var selectedPhotoUri: Uri? = null

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                Toast.makeText(this, "Foto profil dipilih", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbahDataDiriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupListeners()
    }

    private fun prefs() = getSharedPreferences("siaga_session", Context.MODE_PRIVATE)

    private fun loadUserData() {
        val sp = prefs()
        val currentName = sp.getString("nama", "")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Rina Anggraini"
        val currentEmail = sp.getString("email", "").orEmpty()
        val currentPhone = sp.getString("phone", "").orEmpty()
        val currentAddress = sp.getString("address", "").orEmpty()

        binding.etFullName.setText(currentName)
        if (currentEmail.isNotEmpty()) binding.etEmail.setText(currentEmail)
        if (currentPhone.isNotEmpty()) binding.etPhone.setText(currentPhone)
        if (currentAddress.isNotEmpty()) binding.etAddress.setText(currentAddress)

        updateAvatarInitial(currentName)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etFullName.doAfterTextChanged { text ->
            updateAvatarInitial(text?.toString().orEmpty())
        }

        binding.btnChangePhoto.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnSaveProfile.setOnClickListener {
            val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val address = binding.etAddress.text?.toString()?.trim().orEmpty()

            binding.tilFullName.error = null
            binding.tilEmail.error = null
            binding.tilPhone.error = null

            when {
                fullName.isEmpty() -> {
                    binding.tilFullName.error = "Nama lengkap wajib diisi"
                    binding.etFullName.requestFocus()
                }
                email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.tilEmail.error = "Format email tidak valid"
                    binding.etEmail.requestFocus()
                }
                phone.isNotEmpty() && phone.length < 8 -> {
                    binding.tilPhone.error = "Nomor HP minimal 8 digit"
                    binding.etPhone.requestFocus()
                }
                else -> {
                    val sp = prefs()
                    val token = sp.getString("token", "")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "demo-token"
                    val role = sp.getString("role", "")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: ROLE_MASYARAKAT

                    sp.edit()
                        .putString("token", token)
                        .putString("role", role)
                        .putString("nama", fullName)
                        .putString("email", email)
                        .putString("phone", phone)
                        .putString("address", address)
                        .apply()

                    Toast.makeText(this, "Data diri berhasil disimpan", Toast.LENGTH_SHORT).show()
                    setResult(
                        RESULT_OK,
                        Intent().apply {
                            putExtra("updated_name", fullName)
                            putExtra("updated_email", email)
                            putExtra("updated_phone", phone)
                            putExtra("updated_address", address)
                        }
                    )
                    finish()
                }
            }
        }
    }

    private fun updateAvatarInitial(rawName: String) {
        val parts = rawName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val initial = when {
            parts.isEmpty() -> "RA"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
        binding.tvAvatarInitial.text = initial
        binding.tvAvatarInitial.isVisible = selectedPhotoUri == null
    }

    companion object {
        private const val ROLE_MASYARAKAT = "masyarakat"
    }
}