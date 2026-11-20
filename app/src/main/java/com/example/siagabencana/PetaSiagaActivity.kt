package com.example.siagabencana

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.siagabencana.databinding.ActivityPetaSiagaBinding
import com.example.siagabencana.ui.toast

class PetaSiagaActivity : AppCompatActivity() {

    private lateinit var b: ActivityPetaSiagaBinding
    private var sosAnimator: ValueAnimator? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPetaSiagaBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }

        b.chipGroupRadius.setOnCheckedStateChangeListener { _, ids ->
            val km = when (ids.firstOrNull()) {
                R.id.chip1km -> 1
                R.id.chip5km -> 5
                R.id.chip10km -> 10
                else -> 3
            }
            // TODO Anggota 5: GET /api/laporan/sekitar?lat=&lng=&radius=$km lalu gambar ulang marker.
            b.tvRingkasan.text = "Dalam radius $km km"
        }

        b.btnSos.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startSos()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelSos()
            }
            true
        }
    }

    private fun startSos() {
        sosAnimator?.cancel()
        var dibatalkan = false
        sosAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 3000
            interpolator = LinearInterpolator()
            addUpdateListener { b.progressSos.progress = it.animatedValue as Int }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) { dibatalkan = true }
                override fun onAnimationEnd(animation: Animator) { if (!dibatalkan) onSosTriggered() }
            })
            start()
        }
    }

    private fun cancelSos() {
        sosAnimator?.cancel()
        b.progressSos.progress = 0
    }

    private fun onSosTriggered() {
        b.btnSos.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        b.progressSos.progress = 0
        // TODO Anggota 5: POST /api/sos dengan lat, lng, dan token pengguna.
        toast("SOS terkirim ke relawan dan petugas terdekat (demo)")
    }

    override fun onDestroy() {
        sosAnimator?.cancel()
        super.onDestroy()
    }
}
