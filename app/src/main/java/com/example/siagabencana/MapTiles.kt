package com.example.siagabencana

import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource

/**
 * Sumber tile peta terpusat. Key MapTiler dibaca dari local.properties (MAPTILER_KEY)
 * lewat BuildConfig. Kalau key kosong, jatuh ke OSM standar (bisa diblokir OSM).
 */
object MapTiles {
    fun source(): ITileSource {
        val key = BuildConfig.MAPTILER_KEY
        if (key.isBlank()) return TileSourceFactory.MAPNIK

        return XYTileSource(
            "MapTilerStreets",
            0, 19, 256, ".png?key=$key",
            arrayOf("https://api.maptiler.com/maps/streets-v2/256/"),
            "© MapTiler © OpenStreetMap contributors"
        )
    }
}