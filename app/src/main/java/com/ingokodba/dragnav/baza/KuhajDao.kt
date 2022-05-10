package com.ingokodba.dragnav.baza

import androidx.room.*
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama

@Dao
interface KrugSAplikacijamaDao {
    @Query("SELECT * FROM KrugSAplikacijama")
    fun getAll(): List<KrugSAplikacijama>

    @Query("SELECT * FROM KrugSAplikacijama WHERE id = :id")
    fun findById(id: Int): List<KrugSAplikacijama>

    @Insert
    fun insertAll(vararg polja: KrugSAplikacijama): List<Long>

    @Update
    fun update(polje: KrugSAplikacijama)

    @Query("SELECT * FROM KrugSAplikacijama WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): List<KrugSAplikacijama>

    @Delete
    fun delete(polje: KrugSAplikacijama)
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
