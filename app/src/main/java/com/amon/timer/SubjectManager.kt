package com.amon.timer

import android.content.Context
import org.json.JSONArray
import java.util.Locale

object SubjectManager {
    private const val PREFS_NAME = "amon_subjects_prefs"
    private const val KEY_SUBJECTS = "user_subjects_list"

    // 🌟 अब कोई जबरन डिफ़ॉल्ट विषय नहीं रहेगा (शुरुआत में सिर्फ + Add और All दिखेंगे)
    private val DEFAULT_SUBJECTS = emptyList<String>()

    // फ़ोन की मेमोरी से सेव किए हुए विषय निकालना
    fun getUserSubjects(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(KEY_SUBJECTS, null)
        if (savedJson.isNullOrBlank()) {
            return DEFAULT_SUBJECTS
        }
        return try {
            val jsonArray = JSONArray(savedJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            DEFAULT_SUBJECTS
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

        val currentList = getUserSubjects(context).toMutableList()
        // अगर पहले से लिस्ट में है तो दोबारा नहीं जोड़ेंगे
        if (currentList.any { it.equals(formattedSubject, ignoreCase = true) }) {
            return false
        }

        currentList.add(formattedSubject)
        saveList(context, currentList)
        return true
    }

    // विषय को लिस्ट से हटाने के लिए
    fun removeSubject(context: Context, subjectToRemove: String): Boolean {
        val currentList = getUserSubjects(context).toMutableList()
        val removed = currentList.removeAll { it.equals(subjectToRemove, ignoreCase = true) }
        if (removed) {
            saveList(context, currentList)
        }
        return removed
    }

    // मेमोरी में पक्का सेव करना
    private fun saveList(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_SUBJECTS, jsonArray.toString()).apply()
    }
}
