package com.example.siagabencana.ui

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.siagabencana.R
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.model.StatusLaporan
import com.example.siagabencana.model.Urgensi

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun TextView.applyUrgensi(u: Urgensi) {
    val (bg, fg, label) = when (u) {
        Urgensi.KRITIS -> Triple(R.drawable.bg_badge_critical, R.color.badge_critical_text, "Kritis")
        Urgensi.TINGGI -> Triple(R.drawable.bg_badge_high, R.color.badge_high_text, "Tinggi")
        Urgensi.SEDANG -> Triple(R.drawable.bg_badge_medium, R.color.badge_medium_text, "Sedang")
        Urgensi.RENDAH -> Triple(R.drawable.bg_badge_low, R.color.badge_low_text, "Rendah")
    }
    setBackgroundResource(bg)
    setTextColor(ContextCompat.getColor(context, fg))
    text = label
}

/** Laporan selesai menampilkan badge hijau "Selesai", selain itu badge urgensi. */
fun TextView.applyBadge(l: Laporan) {
    if (l.status == StatusLaporan.SELESAI) {
        setBackgroundResource(R.drawable.bg_badge_done)
        setTextColor(ContextCompat.getColor(context, R.color.badge_done_text))
        text = "Selesai"
    } else {
        applyUrgensi(l.urgensi)
    }
}
