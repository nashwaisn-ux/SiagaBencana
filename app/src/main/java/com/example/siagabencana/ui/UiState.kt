package com.example.siagabencana.ui

/**
 * Satu-satunya jalur antara data dan layar. Anggota 5 cukup memanggil render(...)
 * dengan Loading, Success, Empty, atau Error dari hasil Retrofit.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String, val isNetwork: Boolean = false) : UiState<Nothing>
}
