package com.ingokodba.dragnav.baza

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.RainbowMapa
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference


@Database(entities = arrayOf(KrugSAplikacijama::class, AppInfo::class, RainbowMapa::class), version = 7, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun krugSAplikacijamaDao(): KrugSAplikacijamaDao
    abstract fun rainbowMapaDao(): RainbowMapaDao
    abstract fun appInfoDao(): AppInfoDao
    //abstract fun meniPoljaDao(): MeniPoljaDao

    fun setInstanceToNull(){
        instance = WeakReference(null)
    }

    companion object {
        var DATABASE_NAME: String = "database-name"
        // For Singleton instantiation
        @Volatile private var instance: WeakReference<AppDatabase> = WeakReference(null)

        fun getInstance(context: Context): AppDatabase {
            return instance.get() ?: synchronized(this) {
                instance.get() ?: buildDatabase(context).also { instance = WeakReference(it) }
            }
        }

        fun renewInstance(context: Context): AppDatabase {
            return buildDatabase(context).also { instance = WeakReference(it) }
        }



        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    /*.addCallback(
                            object : RoomDatabase.Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>()
                                            .setInputData(workDataOf(KEY_FILENAME to PLANT_DATA_FILENAME))
                                            .build()
                                    WorkManager.getInstance(context).enqueue(request)
                                }
                            }
                    )
                    */
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    //.fallbackToDestructiveMigration()
                    .build()
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AppInfo ADD COLUMN favorite INTEGER DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AppInfo ADD COLUMN hasShortcuts INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AppInfo ADD COLUMN visible INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS RainbowMapa (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, folderName TEXT NOT NULL, apps TEXT NOT NULL, favorite INTEGER NOT NULL)")
            }
        }

    }
}