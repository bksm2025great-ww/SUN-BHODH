package com.amon.timer

import androidx.room.*

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE deleted = 0")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    // 🔄 FocusSessionManager के लिए सिंक्रोनस मेथड्स
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSessionSync(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE deleted = 0")
    fun getAllSessionsSync(): List<SessionEntity>
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjectsSync(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubjectSync(subject: SubjectEntity)
}
