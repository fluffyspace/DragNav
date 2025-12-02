package com.ingokodba.dragnav.modeli

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RainbowMapa (
    @PrimaryKey(autoGenerate = true) var id:Int,
    val folderName:String,
    val apps: MutableList<AppInfo> = mutableListOf(),
    val favorite: Boolean = false,
)