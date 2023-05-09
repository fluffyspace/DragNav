package com.ingokodba.dragnav.baza

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MIGRATION_1_2
import com.ingokodba.dragnav.modeli.KrugSAplikacijama

@Database(entities = arrayOf(KrugSAplikacijama::class, AppInfo::class), version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun krugSAplikacijamaDao(): KrugSAplikacijamaDao
    abstract fun appInfoDao(): AppInfoDao
    //abstract fun meniPoljaDao(): MeniPoljaDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        fun replaceInstance(replace: AppDatabase){
            if(instance != null) {
                instance = replace
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "database-name")
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
                    .addMigrations(MIGRATION_3_4)
                    //.fallbackToDestructiveMigration()
                    .build()
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AppInfo ADD COLUMN favorite INTEGER DEFAULT 0")
            }
        }

    }
}