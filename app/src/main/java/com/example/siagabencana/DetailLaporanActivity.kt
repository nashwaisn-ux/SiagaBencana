package com.example.siagabencana

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetailLaporanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_laporan)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvBukaPeta = findViewById<TextView>(R.id.tvBukaPeta)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnHapus = findViewById<Button>(R.id.btnHapus)

        btnBack?.setOnClickListener {
            finish()
        }

        tvBukaPeta?.setOnClickListener {
            Toast.makeText(this, "Membuka Lokasi di Peta", Toast.LENGTH_SHORT).show()
        }

        btnEdit?.setOnClickListener {
            Toast.makeText(this, "Edit Laporan", Toast.LENGTH_SHORT).show()
        }

        btnHapus?.setOnClickListener {
            Toast.makeText(this, "Hapus Laporan", Toast.LENGTH_SHORT).show()
        }
    }
}
