package com.example.siagabencana

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class SiagaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global Configuration untuk OSMDroid
        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("osmdroid_global_prefs", MODE_PRIVATE))
            userAgentValue = "com.example.siagabencana/1.0 (Android OS; contact: dev@siagabencana.app)"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }
}
