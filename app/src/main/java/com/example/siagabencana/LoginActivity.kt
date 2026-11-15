package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnMasuk = findViewById<Button>(R.id.btnMasuk)
        val tvLupaSandi = findViewById<TextView>(R.id.tvLupaSandi)
        val tvDaftar = findViewById<TextView>(R.id.tvDaftar)

        btnMasuk?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim()
            val password = etPassword?.text?.toString()?.trim()

            if (email.isNullOrEmpty()) {
                etEmail?.error = "Email tidak boleh kosong"
                return@setOnClickListener
            }

            if (password.isNullOrEmpty()) {
                etPassword?.error = "Kata sandi tidak boleh kosong"
                return@setOnClickListener
            }

            Toast.makeText(this, "Login Berhasil! Selamat datang.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        tvLupaSandi?.setOnClickListener {
            Toast.makeText(this, "Fitur Pemulihan Kata Sandi", Toast.LENGTH_SHORT).show()
        }

        tvDaftar?.setOnClickListener {
            Toast.makeText(this, "Navigasi ke Pendaftaran Akun", Toast.LENGTH_SHORT).show()
        }
    }
}
