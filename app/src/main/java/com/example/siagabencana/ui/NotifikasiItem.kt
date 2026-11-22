package com.example.siagabencana.ui

import android.content.Context
import com.example.siagabencana.model.Laporan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class NotifRow {
    data class Header(val label: String) : NotifRow()
    data class Item(
        val id: String,
        val laporanId: Int,
        val title: String,
        val meta: String,
        val summary: String,
        val kategoriNama: String,
        val unread: Boolean,
        val timestampMs: Long
    ) : NotifRow()
}

object NotifikasiMapper {
    private const val PREF_NAME = "siaga_notifikasi_prefs"
    private const val KEY_READ_IDS = "read_notif_ids"

    fun getReadIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
    }

    fun markRead(context: Context, notifId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = HashSet(getReadIds(context))
        current.add(notifId)
        prefs.edit().putStringSet(KEY_READ_IDS, current).apply()
    }

    fun markAllRead(context: Context, rows: List<NotifRow>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = HashSet(getReadIds(context))
        rows.filterIsInstance<NotifRow.Item>().forEach { current.add(it.id) }
        prefs.edit().putStringSet(KEY_READ_IDS, current).apply()
    }

    fun buildRows(context: Context, laporanList: List<Laporan>): List<NotifRow> {
        val readIds = getReadIds(context)
        val items = laporanList.map { laporan ->
            val latestRiwayat = laporan.riwayat?.lastOrNull()
            val rawDate = latestRiwayat?.created_at ?: laporan.updated_at ?: laporan.created_at
            val ts = parseIsoDate(rawDate)
            val notifId = "notif-${laporan.id}-${latestRiwayat?.id ?: 0}"
            val group = getDateGroup(ts)
            val waktu = formatRelativeTime(ts, group)
            val kategoriNama = laporan.kategori?.nama ?: "Lainnya"

            val summaryText = if (latestRiwayat != null && latestRiwayat.status_lama != null) {
                val ket = latestRiwayat.keterangan ?: "Status diperbarui"
                val desc = laporan.deskripsi.orEmpty()
                if (desc.isNotBlank()) "$ket. $desc" else ket
            } else {
                laporan.deskripsi ?: "Laporan bencana masuk."
            }

            NotifRow.Item(
                id = notifId,
                laporanId = laporan.id,
                title = laporan.judul ?: "Laporan Bencana",
                meta = "$kategoriNama • $waktu",
                summary = summaryText,
                kategoriNama = kategoriNama,
                unread = group != "Sebelumnya" && !readIds.contains(notifId),
                timestampMs = ts
            )
        }.sortedByDescending { it.timestampMs }.take(12)

        val rows = mutableListOf<NotifRow>()
        val order = listOf("Hari ini", "Kemarin", "Sebelumnya")
        for (label in order) {
            val grouped = items.filter { getDateGroup(it.timestampMs) == label }
            if (grouped.isNotEmpty()) {
                rows.add(NotifRow.Header(label))
                rows.addAll(grouped)
            }
        }
        return rows
    }

    private fun parseIsoDate(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(iso)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private fun getDateGroup(timeMs: Long): String {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffDays = ((now.timeInMillis - target.timeInMillis) / 86_400_000L).toInt()
        return when {
            diffDays <= 0 -> "Hari ini"
            diffDays == 1 -> "Kemarin"
            else -> "Sebelumnya"
        }
    }

    private fun formatRelativeTime(timeMs: Long, group: String): String {
        if (group == "Kemarin") return "Kemarin"
        if (group == "Sebelumnya") {
            return SimpleDateFormat("dd MMM", Locale("id", "ID")).format(Date(timeMs))
        }
        val diffMins = ((System.currentTimeMillis() - timeMs).coerceAtLeast(0L) / 60_000L).toInt()
        return when {
            diffMins < 1 -> "Baru saja"
            diffMins < 60 -> "$diffMins menit lalu"
            else -> "${diffMins / 60} jam lalu"
        }
    }
}
