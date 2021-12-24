package com.ingokodba.dragnav.modeli

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeniJednoPolje(
    @PrimaryKey(autoGenerate = true) var id:Int,
    var text:String,
    var nextId:String = "",
    var nextIntent: String = "",
    var polja: List<Int> = listOf(),
    var shortcut: Boolean = false,
    var color: String = ""
)
