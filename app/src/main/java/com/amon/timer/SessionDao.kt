package com.amon.timer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {

    // 📥 सेशन को सिंक्रोनस तरीके से लोकल डेटाबेस में सेव करने के लिए
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSessionSync(session: FocusSessionEntity)

    // 📤 सभी सेशन्स को सिंक्रोनस तरीके से फेच करने के लिए
    @Query("SELECT * FROM focus_sessions")
    fun getAllSessionsSync(): List<FocusSessionEntity>
}
