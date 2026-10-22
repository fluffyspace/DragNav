package com.ingokodba.dragnav

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.rainbow.PathConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Renders app icons once at the size they are drawn at and keeps them as WebP files in the cache dir,
 * so a warm start only decodes small bitmaps. Files are keyed by package, last update time and size.
 */
class IconBitmapStore(
    private val context: Context,
    private val colorOf: (Drawable) -> Int,
) {
    private val packageManager = context.packageManager
    private val cacheRoot = File(context.cacheDir, "icons")
    val sizePx: Int = targetSizePx(context)
    private val dir = File(cacheRoot, sizePx.toString()).apply { mkdirs() }

    /**
     * Loads icons for [apps]. Cached ones are decoded first and passed to [onCached] so they can be
     * shown while the rest is rendered. Missing ones get their [AppInfo.color] filled in.
     */
    suspend fun loadAll(
        apps: List<AppInfo>,
        onCached: suspend (Map<String, Drawable?>) -> Unit,
    ): Map<String, Drawable?> = withContext(Dispatchers.Default) {
        val installed = installedPackages()
        val permits = Semaphore(PARALLELISM)
        val result = mutableMapOf<String, Drawable?>()
        val used = HashSet<String>()

        val misses = mutableListOf<AppInfo>()
        val decoded = coroutineScope {
            apps.map { app ->
                async {
                    permits.withPermit {
                        val file = fileFor(app.packageName, installed)
                        val bitmap = file.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                        Triple(app, file, bitmap)
                    }
                }
            }.awaitAll()
        }
        for ((app, file, bitmap) in decoded) {
            if (bitmap == null) {
                misses.add(app)
                continue
            }
            val drawable = BitmapDrawable(context.resources, bitmap)
            if (app.color.isEmpty()) app.color = colorOf(drawable).toString()
            result[app.packageName] = drawable
            used.add(file.name)
        }
        if (misses.isNotEmpty() && result.isNotEmpty()) onCached(result.toMap())

        val rendered = coroutineScope {
            misses.map { app ->
                async {
                    permits.withPermit {
                        val file = fileFor(app.packageName, installed)
                        Triple(app, file, render(app.packageName)?.also { write(file, it) })
                    }
                }
            }.awaitAll()
        }
        for ((app, file, bitmap) in rendered) {
            if (bitmap == null) {
                result[app.packageName] = null
                continue
            }
            val drawable = BitmapDrawable(context.resources, bitmap)
            if (app.color.isEmpty()) app.color = colorOf(drawable).toString()
            result[app.packageName] = drawable
            used.add(file.name)
        }

        Log.d(TAG, "Loaded ${result.size} icons at ${sizePx}px: ${decoded.size - misses.size} cached, ${misses.size} rendered")
        removeStale(used)
        result
    }

    /** Loads a single icon, returning it with its dominant color. */
    fun loadOne(packageName: String): Pair<Drawable?, String> {
        val file = fileFor(packageName, installedPackages())
        val bitmap = file.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            ?: render(packageName)?.also { write(file, it) }
            ?: return Pair(null, "0")
        val drawable = BitmapDrawable(context.resources, bitmap)
        return Pair(drawable, colorOf(drawable).toString())
    }

    private fun render(packageName: String): Bitmap? = try {
        val icon = packageManager.getApplicationIcon(packageName)
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
            icon.setBounds(0, 0, sizePx, sizePx)
            icon.draw(Canvas(it))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load icon for $packageName", e)
        null
    }

    private fun write(file: File, bitmap: Bitmap) {
        try {
            val tmp = File(file.path + ".tmp")
            FileOutputStream(tmp).use { bitmap.compress(WEBP, 100, it) }
            tmp.renameTo(file)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache icon ${file.name}", e)
        }
    }

    private fun fileFor(packageName: String, installed: Map<String, PackageInfo>): File {
        val stamp = installed[packageName]?.lastUpdateTime
            ?: try {
                packageManager.getPackageInfo(packageName, 0).lastUpdateTime
            } catch (e: Exception) {
                0L
            }
        return File(dir, "${packageName}_$stamp.webp")
    }

    @Suppress("DEPRECATION")
    private fun installedPackages(): Map<String, PackageInfo> =
        try {
            packageManager.getInstalledPackages(0).associateBy { it.packageName }
        } catch (e: Exception) {
            emptyMap()
        }

    /** Deletes cached files of removed or updated apps and of other icon sizes. */
    private fun removeStale(used: Set<String>) {
        dir.list()?.filter { it !in used }?.forEach { File(dir, it).delete() }
        cacheRoot.listFiles { f -> f.isDirectory && f.name != dir.name }?.forEach { it.deleteRecursively() }
    }

    companion object {
        private const val TAG = "IconBitmapStore"
        private const val PARALLELISM = 4
        private const val MIN_ICON_DP = 56 // largest fixed-size use, in the search results

        @Suppress("DEPRECATION")
        private val WEBP =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS
            else Bitmap.CompressFormat.WEBP

        /** Size icons are drawn at in the path, or the search results' size if that is larger. */
        fun targetSizePx(context: Context): Int {
            val metrics = context.resources.displayMetrics
            val config = try {
                context.getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
                    .getString("config", null)?.let { Gson().fromJson(it, PathConfig::class.java) }
            } catch (e: Exception) {
                null
            } ?: PathConfig()
            val pathPx = config.appIconSize * metrics.widthPixels
            return maxOf(pathPx, MIN_ICON_DP * metrics.density).roundToInt().coerceIn(48, 512)
        }
    }
}
