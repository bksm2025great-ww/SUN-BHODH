package com.amon.timer

import android.content.Context
import androidx.room.*

@Database(entities = [SessionEntity::class, SubjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    // 👈 यहाँ सिर्फ एब्सट्रैक्ट डिक्लेरेशन होगी (कोई डुप्लीकेट कोड नहीं)
    abstract fun sessionDao(): SessionDao
    abstract fun subjectDao(): SubjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amon_study_db"
                )
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
