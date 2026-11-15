package com.example.siagabencana

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PetaSiagaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peta_siaga)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSos = findViewById<MaterialButton>(R.id.btnSos)

        btnBack?.setOnClickListener {
            finish()
        }

        btnSos?.setOnClickListener {
            Toast.makeText(this, "🚨 Sinyal SOS Darurat dikirim ke petugas & relawan!", Toast.LENGTH_LONG).show()
        }
    }
}
