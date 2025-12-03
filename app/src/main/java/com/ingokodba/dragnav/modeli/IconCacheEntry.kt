package com.ingokodba.dragnav.modeli

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for persistent icon caching.
 * Stores icon bitmaps and metadata to avoid reloading icons on every app start.
 */
@Entity(tableName = "icon_cache")
data class IconCacheEntry(
    @PrimaryKey
    val packageName: String,

    /** Icon bitmap as byte array (PNG format) */
    val iconData: ByteArray?,

    /** Dominant color extracted from the icon */
    val dominantColor: String,

    /** App label at the time icon was cached */
    val label: String,

    /** Timestamp when icon was cached */
    val timestamp: Long = System.currentTimeMillis(),

    /** Version code of the cached icon (to detect app updates) */
    val versionCode: Int = 0,

    /** Icon density used when caching */
    val density: Int = 0
) {
    // Override equals and hashCode to exclude ByteArray from comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IconCacheEntry

        if (packageName != other.packageName) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false
        if (dominantColor != other.dominantColor) return false
        if (label != other.label) return false
        if (timestamp != other.timestamp) return false
        if (versionCode != other.versionCode) return false
        if (density != other.density) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        result = 31 * result + dominantColor.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + versionCode
        result = 31 * result + density
        return result
    }
}
