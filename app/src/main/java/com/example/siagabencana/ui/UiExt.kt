package com.example.siagabencana.ui

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.siagabencana.R
import com.example.siagabencana.model.Laporan
import com.example.siagabencana.model.StatusLaporan
import com.example.siagabencana.model.Urgensi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun String?.toUrgensi(): Urgensi {
    if (this == null) return Urgensi.SEDANG
    return try {
        Urgensi.valueOf(this.uppercase().trim())
    } catch (_: Exception) {
        when (this.lowercase().trim()) {
            "kritis" -> Urgensi.KRITIS
            "tinggi" -> Urgensi.TINGGI
            "rendah" -> Urgensi.RENDAH
            else -> Urgensi.SEDANG
        }
    }
}

fun String?.toStatusLaporan(): StatusLaporan {
    if (this == null) return StatusLaporan.BARU
    return try {
        StatusLaporan.valueOf(this.uppercase().trim())
    } catch (_: Exception) {
        when (this.lowercase().trim()) {
            "diverifikasi" -> StatusLaporan.DIVERIFIKASI
            "ditangani", "diproses" -> StatusLaporan.DITANGANI
            "selesai" -> StatusLaporan.SELESAI
            else -> StatusLaporan.BARU
        }
    }
}

fun formatTanggal(iso: String?): String {
    if (iso.isNullOrBlank()) return "Baru saja"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val date: Date = parser.parse(iso) ?: return iso
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        formatter.format(date)
    } catch (_: Exception) {
        try {
            val altParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date: Date = altParser.parse(iso) ?: return iso
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            formatter.format(date)
        } catch (_: Exception) {
            iso
        }
    }
}

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
    val st = l.status.toStatusLaporan()
    if (st == StatusLaporan.SELESAI) {
        setBackgroundResource(R.drawable.bg_badge_done)
        setTextColor(ContextCompat.getColor(context, R.color.badge_done_text))
        text = "Selesai"
    } else {
        applyUrgensi(l.urgensi.toUrgensi())
    }
}
