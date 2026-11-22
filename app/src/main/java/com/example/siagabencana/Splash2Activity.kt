package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Splash2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash2)

        val cardKebakaran = findViewById<View>(R.id.cardKebakaran)
        val cardBanjir = findViewById<View>(R.id.cardBanjir)
        val cardLongsor = findViewById<View>(R.id.cardLongsor)
        val tvTagline = findViewById<View>(R.id.tvTagline)
        val tvDesc = findViewById<View>(R.id.tvDesc)

        val riseAnim = AnimationUtils.loadAnimation(this, R.anim.rise)
        val fadeUpAnim = AnimationUtils.loadAnimation(this, R.anim.fade_up)

        cardKebakaran?.startAnimation(riseAnim.apply { startOffset = 0 })
        cardBanjir?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rise).apply { startOffset = 300 })
        cardLongsor?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rise).apply { startOffset = 600 })

        tvTagline?.startAnimation(fadeUpAnim.apply { startOffset = 800 })
        tvDesc?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_up).apply { startOffset = 1000 })

        val sessionManager = SessionManager(this)

        lifecycleScope.launch {
            delay(2500)

            if (sessionManager.isLoggedIn()) {
                val res = apiCall {
                    RetrofitClient.getApiService(this@Splash2Activity).getMe()
                }

                when (res) {
                    is ApiResult.Success -> {
                        val u = res.data
                        if (u != null) {
                            sessionManager.saveUser(
                                id = u.id,
                                nama = u.nama,
                                email = u.email,
                                role = u.role,
                                noHp = u.no_hp,
                                alamat = u.alamat,
                                fotoUrl = u.foto_url
                            )
                        }
                        startActivity(Intent(this@Splash2Activity, MainActivity::class.java))
                    }
                    is ApiResult.Failure -> {
                        if (res.code == 401) {
                            sessionManager.clearSession()
                            startActivity(Intent(this@Splash2Activity, LoginActivity::class.java))
                        } else {
                            startActivity(Intent(this@Splash2Activity, MainActivity::class.java))
                        }
                    }
                }
            } else {
                startActivity(Intent(this@Splash2Activity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
