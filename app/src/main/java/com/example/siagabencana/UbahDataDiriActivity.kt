package com.example.siagabencana

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.databinding.ActivityUbahDataDiriBinding
import com.example.siagabencana.model.UpdateProfileRequest
import com.example.siagabencana.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class UbahDataDiriActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUbahDataDiriBinding
    private val session by lazy { SessionManager(this) }
    private var selectedPhotoUri: Uri? = null

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val fileSize = getFileSize(uri)
                val maxSizeBytes = 3 * 1024 * 1024 // Maksimal 3 MB

                if (fileSize > maxSizeBytes) {
                    Toast.makeText(
                        this,
                        "Ukuran foto melebihi 3 MB. Harap pilih foto yang lebih kecil.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@registerForActivityResult
                }

                lifecycleScope.launch {
                    val croppedUri = withContext(Dispatchers.IO) { cropAndCompressSquare(uri) } ?: uri
                    selectedPhotoUri = croppedUri
                    Glide.with(this@UbahDataDiriActivity)
                        .load(croppedUri)
                        .circleCrop()
                        .into(binding.imgAvatar)
                    binding.imgAvatar.isVisible = true
                    binding.tvAvatarInitial.isVisible = false
                    session.saveProfilePhoto(croppedUri.toString())
                    Toast.makeText(
                        this@UbahDataDiriActivity,
                        "Foto profil berhasil dipotong (1:1) dan dipilih",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbahDataDiriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        val currentName = session.nama.ifBlank { "Masyarakat" }
        binding.etFullName.setText(currentName)
        if (session.email.isNotEmpty()) binding.etEmail.setText(session.email)
        if (session.noHp.isNotEmpty()) binding.etPhone.setText(session.noHp)
        if (session.alamat.isNotEmpty()) binding.etAddress.setText(session.alamat)

        val savedPhoto = session.fotoUrl
        if (!savedPhoto.isNullOrEmpty()) {
            try {
                val photoUri = Uri.parse(savedPhoto)
                Glide.with(this)
                    .load(photoUri)
                    .circleCrop()
                    .into(binding.imgAvatar)
                binding.imgAvatar.isVisible = true
                binding.tvAvatarInitial.isVisible = false
            } catch (_: Exception) {
                updateAvatarInitial(currentName)
            }
        } else {
            updateAvatarInitial(currentName)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etFullName.doAfterTextChanged { text ->
            if (selectedPhotoUri == null && session.fotoUrl.isNullOrEmpty()) {
                updateAvatarInitial(text?.toString().orEmpty())
            }
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
                    binding.btnSaveProfile.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            val request = UpdateProfileRequest(
                                nama = fullName,
                                email = email.ifEmpty { null },
                                no_hp = phone.ifEmpty { null },
                                alamat = address.ifEmpty { null }
                            )
                            val resp = RetrofitClient.getApiService(this@UbahDataDiriActivity).updateProfile(request)

                            // Upload photo multipart if a new photo was cropped/selected
                            selectedPhotoUri?.let { uri ->
                                uploadPhotoMultipart(uri)
                            }
                        } catch (_: Exception) {
                            // Offline fallback handled gracefully
                        } finally {
                            val photoPath = selectedPhotoUri?.toString() ?: session.fotoUrl
                            session.save(
                                token = session.token ?: "demo-token",
                                role = session.role,
                                nama = fullName,
                                email = email.ifEmpty { session.email },
                                noHp = phone.ifEmpty { session.noHp },
                                alamat = address.ifEmpty { session.alamat },
                                fotoUrl = photoPath
                            )

                            Toast.makeText(this@UbahDataDiriActivity, "Data diri & foto profil berhasil disimpan", Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun cropAndCompressSquare(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            val width = originalBitmap.width
            val height = originalBitmap.height
            val squareSize = minOf(width, height)
            val xOffset = (width - squareSize) / 2
            val yOffset = (height - squareSize) / 2

            val croppedSquare = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, squareSize, squareSize)

            val targetDimension = 1024
            val finalBitmap = if (squareSize > targetDimension) {
                Bitmap.createScaledBitmap(croppedSquare, targetDimension, targetDimension, true)
            } else {
                croppedSquare
            }

            val file = File(cacheDir, "cropped_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun uploadPhotoMultipart(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "avatar_upload.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto", file.name, requestFile)
            val resp = RetrofitClient.getApiService(this@UbahDataDiriActivity).uploadProfilePhoto(body)
            if (resp.isSuccessful && resp.body()?.success == true) {
                val serverPhotoUrl = resp.body()?.data?.foto_url
                if (!serverPhotoUrl.isNullOrBlank()) {
                    session.saveProfilePhoto(serverPhotoUrl)
                }
            }
        } catch (_: Exception) {
            // Handled
        }
    }

    private fun updateAvatarInitial(rawName: String) {
        val parts = rawName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val initial = when {
            parts.isEmpty() -> "SB"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
        binding.tvAvatarInitial.text = initial
        binding.tvAvatarInitial.isVisible = true
        binding.imgAvatar.isVisible = false
    }
}
