package com.example.siagabencana

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.siagabencana.data.SessionManager
import com.example.siagabencana.network.ApiResult
import com.example.siagabencana.network.RetrofitClient
import com.example.siagabencana.network.apiCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash1)

        val sessionManager = SessionManager(this)

        lifecycleScope.launch {
            delay(1200)

            if (sessionManager.isLoggedIn()) {
                val res = apiCall {
                    RetrofitClient.getApiService(this@SplashActivity).getMe()
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
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    }
                    is ApiResult.Failure -> {
                        if (res.code == 401) {
                            sessionManager.clearSession()
                            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        } else {
                            // Toleran offline: jika hanya masalah jaringan, tetap masuk ke MainActivity
                            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        }
                    }
                }
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
