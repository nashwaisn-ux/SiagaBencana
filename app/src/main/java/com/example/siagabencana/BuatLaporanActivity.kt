package com.example.siagabencana

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout

class BuatLaporanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buat_laporan)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val tilJudul = findViewById<TextInputLayout>(R.id.tilJudul)
        val etJudul = findViewById<EditText>(R.id.etJudul)
        val btnKirim = findViewById<Button>(R.id.btnKirim)

        toolbar?.setNavigationOnClickListener {
            finish()
        }

        btnKirim?.setOnClickListener {
            val judul = etJudul?.text?.toString()?.trim()
            if (judul.isNullOrEmpty() || (judul.length < 5)) {
                tilJudul?.error = "Judul wajib diisi, minimal 5 karakter."
                return@setOnClickListener
            }

            Toast.makeText(this, "Laporan berhasil dikirim!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
