package com.example.dragnav.baza

import androidx.room.*
import com.example.dragnav.modeli.MeniJednoPolje
import com.example.dragnav.modeli.MeniPolja

@Dao
interface MeniJednoPoljeDao {
    @Query("SELECT * FROM MeniJednoPolje")
    fun getAll(): List<MeniJednoPolje>

    @Query("SELECT * FROM MeniJednoPolje WHERE id = :id")
    fun findById(id: Int): List<MeniJednoPolje>

    @Insert
    fun insertAll(vararg polja: MeniJednoPolje): List<Long>

    @Update
    fun update(polje: MeniJednoPolje)

    @Query("SELECT * FROM MeniJednoPolje WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): List<MeniJednoPolje>

    @Delete
    fun delete(polje: MeniJednoPolje)
}

/*@Dao
interface MeniPoljaDao {
    @Query("SELECT * FROM MeniPolja")
    fun getAll(): List<MeniPolja>

    @Query("SELECT * FROM MeniPolja WHERE id = :id")
    fun findById(id: Int): List<MeniPolja>

    @Insert
    fun insertAll(vararg polja: MeniPolja)

    @Delete
    fun delete(polje: MeniPolja)
}*/
