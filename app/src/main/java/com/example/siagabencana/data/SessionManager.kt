package com.example.siagabencana.data

import android.content.Context
import androidx.core.content.edit

class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("siaga_session", Context.MODE_PRIVATE)

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    val role: String
        get() = (prefs.getString(KEY_ROLE, ROLE_MASYARAKAT) ?: ROLE_MASYARAKAT)
            .trim()
            .lowercase()

    val nama: String
        get() = prefs.getString(KEY_NAMA, "Pengguna") ?: "Pengguna"

    val isLoggedIn: Boolean
        get() = !token.isNullOrEmpty()

    val isPetugas: Boolean
        get() = role == ROLE_PETUGAS

    val isRelawan: Boolean
        get() = role == ROLE_RELAWAN

    fun save(token: String, role: String, nama: String) {
        val normalizedRole = role.trim().lowercase().ifEmpty { ROLE_MASYARAKAT }
        prefs.edit(commit = true) {
            putString(KEY_TOKEN, token)
            putString(KEY_ROLE, normalizedRole)
            putString(KEY_NAMA, nama)
        }
    }

    fun clear() {
        prefs.edit(commit = true) {
            clear()
        }
    }

    companion object {
        const val ROLE_MASYARAKAT = "masyarakat"
        const val ROLE_RELAWAN = "relawan"
        const val ROLE_PETUGAS = "petugas"

        private const val KEY_TOKEN = "token"
        private const val KEY_ROLE = "role"
        private const val KEY_NAMA = "nama"
    }
}