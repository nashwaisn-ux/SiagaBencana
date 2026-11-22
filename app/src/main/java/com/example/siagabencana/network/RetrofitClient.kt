package com.example.siagabencana.network

import android.content.Context
import android.content.Intent
import com.example.siagabencana.BuildConfig
import com.example.siagabencana.LoginActivity
import com.example.siagabencana.data.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object RetrofitClient {

    @Volatile
    private var apiService: ApiService? = null
    val isRedirecting = AtomicBoolean(false)

    fun getApiService(context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createApiService(context.applicationContext).also { apiService = it }
        }
    }

    private fun createApiService(appContext: Context): ApiService {
        val sessionManager = SessionManager(appContext)

        val authInterceptor = Interceptor { chain ->
            val request = chain.request()
            val requestBuilder = request.newBuilder()
                .addHeader("Accept", "application/json")

            val token = sessionManager.getAuthToken()
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = chain.proceed(requestBuilder.build())

            if (response.code == 401) {
                val path = request.url.encodedPath
                val isAuthEndpoint = path.endsWith("/login") || path.endsWith("/register") || path.endsWith("/reset-password")
                if (!isAuthEndpoint && sessionManager.isLoggedIn()) {
                    sessionManager.clearSession()
                    if (isRedirecting.compareAndSet(false, true)) {
                        val intent = Intent(appContext, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("EXTRA_SESSION_EXPIRED", true)
                        }
                        appContext.startActivity(intent)
                    }
                }
            }

            response
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
