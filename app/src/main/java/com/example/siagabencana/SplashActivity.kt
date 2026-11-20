
package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_splash1)

        handler.postDelayed({

            setContentView(R.layout.activity_splash2)

            handler.postDelayed({
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, 1500)

        }, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
