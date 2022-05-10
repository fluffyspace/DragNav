package com.ingokodba.dragnav.modeli

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ingokodba.dragnav.MainActivity
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Action (
    val title:String,
    val description:String,
    val type: MainActivity.Companion.ActionTypes
)