package com.example.coloranalysis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.coloranalysis.data.dao.OutfitDao
import com.example.coloranalysis.data.dao.ProfileDao
import com.example.coloranalysis.data.helper.Converters
import com.example.coloranalysis.data.models.Outfit
import com.example.coloranalysis.data.models.Profile

@Database(entities = [Profile::class,
    Outfit::class],
    version = 1)
@TypeConverters(Converters::class) // <--- THÊM DÒNG NÀY Ở ĐÂY
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun outfitDao(): OutfitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "color_analysis_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}