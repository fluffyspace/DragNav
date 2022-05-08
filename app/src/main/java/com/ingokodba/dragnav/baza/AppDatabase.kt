package com.ingokodba.dragnav.baza

import android.content.Context
import androidx.room.*
import com.ingokodba.dragnav.AppListener
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MIGRATION_1_2
import com.ingokodba.dragnav.modeli.MeniJednoPolje

@Database(entities = arrayOf(MeniJednoPolje::class, AppInfo::class/*, MeniPolja::class*/), version = 2, exportSchema = true, autoMigrations = [AutoMigration (from = 1, to = 2)])
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meniJednoPoljeDao(): MeniJednoPoljeDao
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
            return Room.databaseBuilder(context, AppDatabase::class.java, "database-name").addMigrations(MIGRATION_1_2)
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
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }
}