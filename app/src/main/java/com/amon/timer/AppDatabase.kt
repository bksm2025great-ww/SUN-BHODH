package com.amon.timer

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase

// --- ENTITIES ---
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val user_id: String?,
    val started_at: Long,       // UTC timestamp
    val local_date: String,     // yyyy-MM-dd
    val minutes: Int,
    val subject_id: String,
    val synced: Boolean,
    val updated_at: Long,
    val deleted: Boolean,
    val start_estimated: Boolean
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    val sort_order: Int,
    val deleted: Boolean,
    val synced: Boolean
)

// --- DAOs ---
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE deleted = 0")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjectsSync(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubjectSync(subject: SubjectEntity)
}

// --- DATABASE & MIGRATION ---
@Database(entities = [SessionEntity::class, SubjectEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
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
                .allowMainThreadQueries() // माइग्रेशन और स्मूथ लोकल एक्सेस के लिए
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
