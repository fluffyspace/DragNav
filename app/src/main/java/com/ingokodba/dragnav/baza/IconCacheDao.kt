package com.ingokodba.dragnav.baza

import androidx.room.*
import com.ingokodba.dragnav.modeli.IconCacheEntry

@Dao
interface IconCacheDao {

    @Query("SELECT * FROM icon_cache WHERE packageName = :packageName")
    fun getCachedIcon(packageName: String): IconCacheEntry?

    @Query("SELECT * FROM icon_cache")
    fun getAllCachedIcons(): List<IconCacheEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun cacheIcon(entry: IconCacheEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun cacheIcons(entries: List<IconCacheEntry>)

    @Delete
    fun deleteIcon(entry: IconCacheEntry)

    @Query("DELETE FROM icon_cache WHERE packageName = :packageName")
    fun deleteIconByPackageName(packageName: String)

    @Query("DELETE FROM icon_cache WHERE packageName IN (:packageNames)")
    fun deleteIconsByPackageNames(packageNames: List<String>)

    @Query("DELETE FROM icon_cache")
    fun clearCache()

    @Query("SELECT COUNT(*) FROM icon_cache")
    fun getCacheSize(): Int

    @Query("DELETE FROM icon_cache WHERE timestamp < :oldestTimestamp")
    fun deleteOldEntries(oldestTimestamp: Long)
}
