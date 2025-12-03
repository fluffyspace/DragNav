package com.ingokodba.dragnav

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.IconCacheEntry
import java.io.ByteArrayOutputStream

/**
 * Icon cache manager that provides persistent storage for app icons.
 * Icons are stored in a database to avoid reloading them every time the launcher starts.
 */
class IconCache(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val db = AppDatabase.getInstance(context)
    private val iconCacheDao = db.iconCacheDao()

    /**
     * Bulk load icons for multiple packages.
     * This is much more efficient than loading icons one-by-one.
     * Returns a map of packageName -> (Drawable, dominantColor)
     */
    fun getIconsInBulk(
        packageNames: List<String>,
        qualityIcons: Boolean
    ): Map<String, Pair<Drawable?, String>> {
        val result = mutableMapOf<String, Pair<Drawable?, String>>()

        if (packageNames.isEmpty()) return result

        // Step 1: Bulk query from cache (single SQL query!)
        val allCached = iconCacheDao.getAllCachedIcons()
        val cachedMap = allCached.associateBy { it.packageName }

        val packagesToLoad = mutableListOf<String>()

        // Step 2: Process cached icons and identify missing ones
        for (packageName in packageNames) {
            val cached = cachedMap[packageName]

            if (cached != null && cached.iconData != null) {
                try {
                    // Verify version
                    val currentVersionCode = try {
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    } catch (e: PackageManager.NameNotFoundException) {
                        -1
                    }

                    if (currentVersionCode == cached.versionCode) {
                        // Valid cache entry
                        val bitmap = BitmapFactory.decodeByteArray(
                            cached.iconData, 0, cached.iconData.size
                        )
                        if (bitmap != null) {
                            val drawable = BitmapDrawable(context.resources, bitmap)
                            result[packageName] = Pair(drawable, cached.dominantColor)
                            continue
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading cached icon for $packageName", e)
                }
            }

            // Cache miss or invalid - need to load
            packagesToLoad.add(packageName)
        }

        // Step 3: Load missing icons (if any)
        if (packagesToLoad.isNotEmpty()) {
            Log.d(TAG, "Bulk loading ${packagesToLoad.size} icons from system")
            for (packageName in packagesToLoad) {
                result[packageName] = loadAndCacheIcon(packageName, qualityIcons)
            }
        }

        Log.d(TAG, "Bulk load complete: ${result.size} icons loaded, " +
                   "${packageNames.size - packagesToLoad.size} from cache, " +
                   "${packagesToLoad.size} from system")

        return result
    }

    /**
     * Get an icon from cache or load it if not cached.
     * Returns a pair of (Drawable, dominantColor)
     */
    fun getIcon(packageName: String, qualityIcons: Boolean): Pair<Drawable?, String> {
        // Try to get from cache first
        val cached = iconCacheDao.getCachedIcon(packageName)
        if (cached != null && cached.iconData != null) {
            try {
                // Verify app version hasn't changed
                val currentVersionCode = try {
                    packageManager.getPackageInfo(packageName, 0).versionCode
                } catch (e: PackageManager.NameNotFoundException) {
                    -1
                }

                if (currentVersionCode == cached.versionCode) {
                    // Cache is valid, decode and return
                    val bitmap = BitmapFactory.decodeByteArray(cached.iconData, 0, cached.iconData.size)
                    if (bitmap != null) {
                        val drawable = BitmapDrawable(context.resources, bitmap)
                        Log.d(TAG, "Icon loaded from cache: $packageName")
                        return Pair(drawable, cached.dominantColor)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading cached icon for $packageName", e)
            }
        }

        // Cache miss or invalid, load from system
        return loadAndCacheIcon(packageName, qualityIcons)
    }

    /**
     * Load icon from system and store it in cache.
     */
    private fun loadAndCacheIcon(packageName: String, qualityIcons: Boolean): Pair<Drawable?, String> {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val res: Resources = packageManager.getResourcesForApplication(applicationInfo)

            val density = if (qualityIcons) {
                DisplayMetrics.DENSITY_HIGH
            } else {
                DisplayMetrics.DENSITY_LOW
            }

            var iconDrawable = res.getDrawableForDensity(
                applicationInfo.icon,
                density,
                null
            )

            if (iconDrawable != null) {
                // Scale down if necessary
                var iconBitmap = iconDrawable.toBitmap()
                if (iconBitmap.width > 200) {
                    val scale = 200f / iconBitmap.width
                    iconBitmap = Bitmap.createScaledBitmap(
                        iconBitmap,
                        (iconBitmap.width * scale).toInt(),
                        (iconBitmap.height * scale).toInt(),
                        false
                    )
                    iconDrawable = BitmapDrawable(context.resources, iconBitmap)
                }

                // Extract dominant color
                val dominantColor = MainActivity.getInstance()?.getBestPrimaryColor(iconDrawable) ?: 0

                // Cache the icon
                try {
                    val iconBytes = bitmapToByteArray(iconBitmap)
                    val versionCode = try {
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    } catch (e: PackageManager.NameNotFoundException) {
                        0
                    }

                    val cacheEntry = IconCacheEntry(
                        packageName = packageName,
                        iconData = iconBytes,
                        dominantColor = dominantColor.toString(),
                        label = applicationInfo.loadLabel(packageManager).toString(),
                        timestamp = System.currentTimeMillis(),
                        versionCode = versionCode,
                        density = density
                    )

                    iconCacheDao.cacheIcon(cacheEntry)
                    Log.d(TAG, "Icon cached: $packageName")
                } catch (e: Exception) {
                    Log.w(TAG, "Error caching icon for $packageName", e)
                }

                return Pair(iconDrawable, dominantColor.toString())
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found: $packageName", e)
        } catch (e: Resources.NotFoundException) {
            Log.w(TAG, "Resource not found for $packageName", e)
        } catch (e: Exception) {
            Log.w(TAG, "Error loading icon for $packageName", e)
        }

        return Pair(null, "0")
    }

    /**
     * Remove cached icon for a package.
     */
    fun removeIcon(packageName: String) {
        iconCacheDao.deleteIconByPackageName(packageName)
        Log.d(TAG, "Icon removed from cache: $packageName")
    }

    /**
     * Remove multiple cached icons.
     */
    fun removeIcons(packageNames: List<String>) {
        iconCacheDao.deleteIconsByPackageNames(packageNames)
        Log.d(TAG, "Icons removed from cache: ${packageNames.size}")
    }

    /**
     * Clear all cached icons.
     */
    fun clearCache() {
        iconCacheDao.clearCache()
        Log.d(TAG, "Icon cache cleared")
    }

    /**
     * Get the number of cached icons.
     */
    fun getCacheSize(): Int {
        return iconCacheDao.getCacheSize()
    }

    /**
     * Remove old cache entries (older than 30 days).
     */
    fun cleanOldEntries() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        iconCacheDao.deleteOldEntries(thirtyDaysAgo)
        Log.d(TAG, "Old icon cache entries cleaned")
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    companion object {
        private const val TAG = "IconCache"
    }
}
