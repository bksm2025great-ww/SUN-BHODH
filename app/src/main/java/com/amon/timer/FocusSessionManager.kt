package com.amon.timer

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object FocusSessionManager {

    private const val MIN_SAVING_MINUTES = 5 // 🛑 5-मिनट का नियम

    // 1. नया सेशन रूम डेटाबेस में सेव करना (और क्लाउड पर भेजना)
    fun saveSession(context: Context, session: FocusSession) {
        // 5 मिनट से कम की पढ़ाई कभी सेव नहीं होगी
        if (session.durationMinutes < MIN_SAVING_MINUTES) return

        try {
            val db = AppDatabase.getDatabase(context)
            val currentTime = System.currentTimeMillis()
            
            // local_date format: yyyy-MM-dd (या session.date अगर फॉर्मेट सही है)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val localDateStr = if (session.date.isNotBlank()) session.date else dateFormatter.format(Date(currentTime))

            // Room Entity बनाना
            val sessionEntity = SessionEntity(
                id = if (session.id != 0L) session.id.toString() else UUID.randomUUID().toString(),
                user_id = null,
                started_at = currentTime,
                local_date = localDateStr,
                minutes = session.durationMinutes,
                subject_id = session.subject,
                synced = false,
                updated_at = currentTime,
                deleted = false,
                start_estimated = false
            )

            db.sessionDao().insertSessionSync(sessionEntity)

            // 🚀 सीधे Supabase क्लाउड पर डेटा भेजना
            CloudSyncManager.syncSession(
                context = context,
                subject = session.subject,
                durationMinutes = session.durationMinutes.toLong()
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. रूम डेटाबेस से सभी सेशन्स पढ़ना (5 मिनट से छोटे सेशन्स को इग्नोर करना)
    fun getAllSessions(context: Context): List<FocusSession> {
        return try {
            val db = AppDatabase.getDatabase(context)
            val entities = db.sessionDao().getAllSessionsSync()
            
            val sessionList = mutableListOf<FocusSession>()
            for (entity in entities) {
                if (entity.minutes >= MIN_SAVING_MINUTES && !entity.deleted) {
                    // ID को Long में बदलने का सेफ तरीका (अगर UUID है तो hashCode या 0L)
                    val numericId = entity.id.toLongOrNull() ?: entity.id.hashCode().toLong()
                    
                    val session = FocusSession(
                        id = numericId,
                        date = entity.local_date,
                        subject = entity.subject_id,
                        durationMinutes = entity.minutes
                    )
                    sessionList.add(session)
                }
            }
            // नई पढ़ाई सबसे ऊपर दिखाने के लिए
            sessionList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 🟢 Shortcut alias: पुरानी स्क्रीनों के साथ तालमेल के लिए
    fun getSessions(context: Context): List<FocusSession> {
        return getAllSessions(context)
    }

    // 3. बैकअप या क्लाउड से डेटा रीस्टोर करना (Room में सेव करना)
    fun restoreSessions(context: Context, incomingSessions: List<FocusSession>): Pair<Int, Int> {
        var restoredMinutes = 0
        try {
            val db = AppDatabase.getDatabase(context)
            val existingEntities = db.sessionDao().getAllSessionsSync()
            
            val currentTime = System.currentTimeMillis()
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (incoming in incomingSessions) {
                if (incoming.durationMinutes < MIN_SAVING_MINUTES) continue

                // चेक करो कि क्या यह सेशन पहले से डेटाबेस में मौजूद है
                val alreadyExists = existingEntities.any { 
                    it.local_date == incoming.date && 
                    it.subject_id.equals(incoming.subject, ignoreCase = true) && 
                    it.minutes == incoming.durationMinutes 
                }

                if (!alreadyExists) {
                    val entity = SessionEntity(
                        id = if (incoming.id > 0) incoming.id.toString() else UUID.randomUUID().toString(),
                        user_id = null,
                        started_at = currentTime,
                        local_date = if (incoming.date.isNotBlank()) incoming.date else dateFormatter.format(Date()),
                        minutes = incoming.durationMinutes,
                        subject_id = incoming.subject,
                        synced = true, // क्लाउड से आया है तो synced true मान सकते हैं
                        updated_at = currentTime,
                        deleted = false,
                        start_estimated = false
                    )
                    db.sessionDao().insertSessionSync(entity)
                    restoredMinutes += incoming.durationMinutes
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(0, restoredMinutes)
    }
}
