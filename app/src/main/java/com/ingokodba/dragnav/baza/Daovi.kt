package com.ingokodba.dragnav.baza

import androidx.room.*
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.RainbowMapa

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
interface RainbowMapaDao {
    @Query("SELECT * FROM RainbowMapa")
    fun getAll(): List<RainbowMapa>

    @Query("SELECT * FROM RainbowMapa WHERE id = :id")
    fun findById(id: Int): List<RainbowMapa>

    @Insert
    fun insertAll(vararg polja: RainbowMapa): List<Long>

    @Update
    fun update(polje: RainbowMapa)

    @Query("SELECT * FROM RainbowMapa WHERE rowid = :rowId")
    fun findByRowId(rowId: Long): List<RainbowMapa>

    @Delete
    fun delete(polje: RainbowMapa)
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
