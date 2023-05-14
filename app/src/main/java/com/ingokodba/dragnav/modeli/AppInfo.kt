package com.ingokodba.dragnav.modeli

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
data class AppInfo (
    @PrimaryKey(autoGenerate = true) var id:Int,
    val label:String,
    val packageName:String,
    var color:String = "",
    var installed: Boolean = false,
    @ColumnInfo(defaultValue = "0") var frequency: Int = 0,
    @ColumnInfo(defaultValue = "0") var lastLaunched: Long = 0,
    var favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") var hasShortcuts: Boolean = false,
)