package com.example.dragnav.modeli

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MeniPolja(
    @PrimaryKey val id:Int,
    val polja: List<Int>
)
