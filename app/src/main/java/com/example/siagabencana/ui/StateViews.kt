package com.example.siagabencana.ui

import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.siagabencana.R

/**
 * Mengatur tampilan konten / loading / kosong / gagal jaringan
 * dari layout_state_loading, layout_state_empty, dan layout_state_error.
 */
class StateViews(
    private val content: View,
    private val loading: View,
    private val empty: View,
    private val error: View,
    onRetry: () -> Unit,
) {
    init {
        error.findViewById<View>(R.id.btnCobaLagi).setOnClickListener { onRetry() }
    }

    fun render(state: UiState<*>) {
        content.isVisible = state is UiState.Success<*>
        loading.isVisible = state is UiState.Loading
        empty.isVisible = state is UiState.Empty
        error.isVisible = state is UiState.Error

        val skeleton = loading.findViewById<View>(R.id.skeletonContainer)
        if (state is UiState.Loading) {
            skeleton?.startAnimation(AnimationUtils.loadAnimation(loading.context, R.anim.shimmer))
        } else {
            skeleton?.clearAnimation()
        }

        if (state is UiState.Error) {
            error.findViewById<TextView>(R.id.tvErrorTitle).text =
                if (state.isNetwork) "Gagal terhubung ke server" else "Terjadi kesalahan"
            error.findViewById<TextView>(R.id.tvErrorDesc).text = state.message
            error.findViewById<View>(R.id.bannerOffline).isVisible = state.isNetwork
        }
    }

    fun setEmptyText(title: String, desc: String, actionLabel: String?, onAction: (() -> Unit)? = null) {
        empty.findViewById<TextView>(R.id.tvEmptyTitle).text = title
        empty.findViewById<TextView>(R.id.tvEmptyDesc).text = desc
        val btn = empty.findViewById<View>(R.id.btnEmptyAksi)
        btn.isVisible = actionLabel != null
        (btn as? TextView)?.text = actionLabel
        btn.setOnClickListener { onAction?.invoke() }
    }
}
