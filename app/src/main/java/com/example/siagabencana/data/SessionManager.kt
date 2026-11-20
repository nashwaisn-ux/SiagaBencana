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
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "siagabencana_session"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAMA = "user_nama"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NOHP = "user_nohp"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(id: Int, nama: String, email: String?, role: String?, noHp: String?) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, id)
            putString(KEY_USER_NAMA, nama)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            putString(KEY_USER_NOHP, noHp)
            apply()
        }
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserName(): String = prefs.getString(KEY_USER_NAMA, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "masyarakat") ?: "masyarakat"
    fun getUserNoHp(): String = prefs.getString(KEY_USER_NOHP, "") ?: ""

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrEmpty()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
