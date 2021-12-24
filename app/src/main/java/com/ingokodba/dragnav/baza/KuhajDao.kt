package com.ingokodba.dragnav.baza

import androidx.room.*
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MeniJednoPolje

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

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM AppInfo")
    fun getAll(): List<AppInfo>

    @Query("SELECT * FROM AppInfo WHERE id = :id")
    fun findById(id: Int): List<AppInfo>

    @Insert
    fun insertAll(vararg polja: AppInfo): List<Long>

    @Update
    fun update(polje: AppInfo)

    @Query("SELECT * FROM AppInfo WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): List<AppInfo>

    @Delete
    fun delete(polje: AppInfo)
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
