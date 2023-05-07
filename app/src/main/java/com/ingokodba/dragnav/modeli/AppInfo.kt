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
)

// Migration from 1 to 2, Room 2.1.0
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE AppInfo ADD COLUMN frequency INTEGER NOT NULL DEFAULT 0")
        database.execSQL(
            "ALTER TABLE AppInfo ADD COLUMN lastLaunched LONG NOT NULL DEFAULT 0")
    }
}