package com.ingokodba.dragnav.modeli

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A shortcut pinned to the launcher by another app (via "Add to home screen").
 * The path and folders work with [AppInfo], so a pinned shortcut is exposed to them
 * through [toAppInfo], with a synthetic package name that [isShortcut] recognises.
 */
@Entity
data class PinnedShortcut(
    @PrimaryKey(autoGenerate = true) var id: Int,
    val shortcutId: String,
    val packageName: String,
    val label: String,
    var favorite: Boolean = false,
) {
    val key: String get() = keyOf(packageName, shortcutId)

    fun toAppInfo() = AppInfo(
        id = -id,
        label = label,
        packageName = key,
        favorite = favorite,
    )

    companion object {
        const val KEY_PREFIX = "shortcut:"

        fun keyOf(packageName: String, shortcutId: String) = "$KEY_PREFIX$packageName/$shortcutId"

        fun isShortcut(app: AppInfo) = app.packageName.startsWith(KEY_PREFIX)

        /** Splits a key made by [keyOf] into (packageName, shortcutId). */
        fun parseKey(key: String): Pair<String, String>? {
            if (!key.startsWith(KEY_PREFIX)) return null
            val rest = key.removePrefix(KEY_PREFIX)
            val slash = rest.indexOf('/')
            if (slash <= 0) return null
            return rest.substring(0, slash) to rest.substring(slash + 1)
        }
    }
}
