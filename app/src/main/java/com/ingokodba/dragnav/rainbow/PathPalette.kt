package com.ingokodba.dragnav.rainbow

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import com.google.android.material.color.utilities.Hct

/**
 * Soft path colors: Material 3 style tone 80 / medium chroma, instead of fully saturated HSV.
 */
object PathPalette {
    private const val TONE = 80.0
    private const val CHROMA = 40.0

    private fun pastel(hue: Double): Int = Hct.from(hue, CHROMA, TONE).toInt()

    /** Evenly spread pastel hues, used when no wallpaper colors are available. */
    val fixed: List<Int> = listOf(15.0, 45.0, 85.0, 140.0, 175.0, 215.0, 255.0, 290.0, 330.0).map { pastel(it) }

    /**
     * Pastels derived from the wallpaper's primary/secondary/tertiary colors (Android 8.1+),
     * plus their complementary hues; empty if the wallpaper colors cannot be read.
     */
    fun fromWallpaper(context: Context): List<Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return emptyList()
        return try {
            val colors = WallpaperManager.getInstance(context)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return emptyList()
            val seeds = listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
            val hues = seeds.map { Hct.fromInt(it.toArgb()).hue }
            (hues + hues.map { (it + 180.0) % 360.0 }).map { pastel(it) }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
