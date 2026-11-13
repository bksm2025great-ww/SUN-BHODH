package com.amon.timer

import android.content.Context
import java.util.Locale
import java.util.UUID

object SubjectManager {

    // फ़ोन की Room Database से सेव किए हुए विषय निकालना
    fun getUserSubjects(context: Context): List<String> {
        return try {
            val db = AppDatabase.getDatabase(context)
            db.subjectDao().getAllSubjectsSync()
                .filter { !it.deleted }
                .map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // नया विषय जोड़ने के लिए (पहला अक्षर हमेशा ऑटो-कैपिटल होगा)
    fun addSubject(context: Context, newSubject: String): Boolean {
        val trimmed = newSubject.trim()
        if (trimmed.isEmpty() || trimmed.equals("All", ignoreCase = true)) return false

        // 🔤 पहला अक्षर ऑटोमैटिक कैपिटल (Titlecase)
        val formattedSubject = trimmed.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        return try {
            val db = AppDatabase.getDatabase(context)
            val existingSubjects = db.subjectDao().getAllSubjectsSync()
            
            // अगर पहले से लिस्ट में है
            val existing = existingSubjects.find { it.name.equals(formattedSubject, ignoreCase = true) }
            if (existing != null) {
                if (existing.deleted) {
                    // अगर पहले डिलीट हुआ था तो उसे दोबारा एक्टिव कर दो
                    val restored = existing.copy(deleted = false, synced = false)
                    db.subjectDao().insertSubjectSync(restored)
                    return true
                }
                return false
            }

            // नया विषय डेटाबेस में जोड़ें
            val newEntity = SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = formattedSubject,
                color = null,
                sort_order = existingSubjects.size,
                deleted = false,
                synced = false
            )
            db.subjectDao().insertSubjectSync(newEntity)
            true
        } catch (e: Exception) {
            false
        }
    }

    // विषय को लिस्ट से हटाने के लिए (Soft delete)
    fun removeSubject(context: Context, subjectToRemove: String): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            val subjects = db.subjectDao().getAllSubjectsSync()
            val target = subjects.find { it.name.equals(subjectToRemove, ignoreCase = true) }
            if (target != null) {
                // डेटाबेस में deleted = true मार्क कर देंगे
                val updated = target.copy(deleted = true, synced = false)
                db.subjectDao().insertSubjectSync(updated)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
