package com.ingokodba.dragnav.modeli

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_recordings")
data class VoiceRecording(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    val filePath: String,
    val fileName: String,
    val startTime: Long, // Unix timestamp when recording started
    val endTime: Long = 0, // Unix timestamp when recording ended
    val durationMs: Long = 0, // Duration in milliseconds
    @ColumnInfo(defaultValue = "") val title: String = "", // Optional user-defined title
    @ColumnInfo(defaultValue = "0") val fileSize: Long = 0, // File size in bytes
    @ColumnInfo(defaultValue = "audio/mp4") val mimeType: String = "audio/mp4",
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false // Soft delete flag
)
