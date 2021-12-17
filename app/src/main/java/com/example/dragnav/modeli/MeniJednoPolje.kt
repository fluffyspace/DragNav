package com.example.dragnav.modeli

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeniJednoPolje(
    @PrimaryKey(autoGenerate = true) var id:Int,
    var text:String,
    var nextId:Int = -1,
    var nextIntent: String = "",
    var polja: List<Int> = listOf()
)
