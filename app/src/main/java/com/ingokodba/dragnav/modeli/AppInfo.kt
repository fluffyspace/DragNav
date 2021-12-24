package com.ingokodba.dragnav.modeli

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AppInfo (
    @PrimaryKey(autoGenerate = true) var id:Int,
    val label:String,
    val packageName:String,
    var color:String = ""
)