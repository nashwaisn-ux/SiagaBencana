package com.example.siagabencana.data

import android.content.Context
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
        private const val KEY_USER_ALAMAT = "user_alamat"
        private const val KEY_USER_FOTO = "user_foto"

        const val ROLE_MASYARAKAT = "masyarakat"
        const val ROLE_PETUGAS = "petugas"
        const val ROLE_RELAWAN = "relawan"
    }

    val role: String get() = getUserRole()
    val nama: String get() = getUserName()
    val email: String get() = getUserEmail()
    val token: String? get() = getAuthToken()
    val noHp: String get() = getUserNoHp()
    val alamat: String get() = getUserAlamat()
    val fotoUrl: String? get() = getUserFotoUrl()

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUser(
        id: Int,
        nama: String,
        email: String?,
        role: String?,
        noHp: String?,
        alamat: String? = null,
        fotoUrl: String? = null
    ) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, id)
            putString(KEY_USER_NAMA, nama)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role ?: ROLE_MASYARAKAT)
            putString(KEY_USER_NOHP, noHp)
            if (alamat != null) putString(KEY_USER_ALAMAT, alamat)
            if (fotoUrl != null) putString(KEY_USER_FOTO, fotoUrl)
            apply()
        }
    }

    fun saveProfilePhoto(uriOrUrl: String) {
        prefs.edit().putString(KEY_USER_FOTO, uriOrUrl).apply()
    }

    fun save(
        token: String,
        role: String,
        nama: String,
        email: String? = null,
        id: Int = -1,
        noHp: String? = null,
        alamat: String? = null,
        fotoUrl: String? = null
    ) {
        saveAuthToken(token)
        saveUser(id = id, nama = nama, email = email, role = role, noHp = noHp, alamat = alamat, fotoUrl = fotoUrl)
    }



    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUserName(): String = prefs.getString(KEY_USER_NAMA, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, ROLE_MASYARAKAT) ?: ROLE_MASYARAKAT
    fun getUserNoHp(): String = prefs.getString(KEY_USER_NOHP, "") ?: ""
    fun getUserAlamat(): String = prefs.getString(KEY_USER_ALAMAT, "") ?: ""
    fun getUserFotoUrl(): String? = prefs.getString(KEY_USER_FOTO, null)

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrEmpty()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
