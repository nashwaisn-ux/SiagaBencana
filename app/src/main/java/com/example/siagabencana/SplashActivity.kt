package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash1)

        val ring1 = findViewById<View>(R.id.ring1)
        val ring2 = findViewById<View>(R.id.ring2)
        val ring3 = findViewById<View>(R.id.ring3)
        val logoContainer = findViewById<View>(R.id.logoContainer)
        val tvAppName = findViewById<View>(R.id.tvAppName)
        val tvTagline = findViewById<View>(R.id.tvTagline)
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)

        val rippleAnim = AnimationUtils.loadAnimation(this, R.anim.ripple)
        ring1?.startAnimation(rippleAnim.apply { startOffset = 0 })
        ring2?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ripple).apply { startOffset = 800 })
        ring3?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ripple).apply { startOffset = 1600 })

        logoContainer?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop_in))
        tvAppName?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_up))
        tvTagline?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_up).apply { startOffset = 200 })

        val blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink)
        dot1?.startAnimation(blinkAnim.apply { startOffset = 0 })
        dot2?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink).apply { startOffset = 300 })
        dot3?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink).apply { startOffset = 600 })

        lifecycleScope.launch {
            delay(2000)
            startActivity(Intent(this@SplashActivity, Splash2Activity::class.java))
            finish()
        }
    }
}
