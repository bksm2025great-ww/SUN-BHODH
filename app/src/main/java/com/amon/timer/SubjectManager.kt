package com.amon.timer

import android.content.Context
import org.json.JSONArray

object SubjectManager {
    private const val PREFS_NAME = "amon_subjects_prefs"
    private const val KEY_SUBJECTS = "user_subjects_list"

    // डिफ़ॉल्ट विषय जो पहली बार ऐप खोलने पर दिखेंगे
    private val DEFAULT_SUBJECTS = listOf("Math", "Hindi", "English")

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
            if (list.isEmpty()) DEFAULT_SUBJECTS else list
        } catch (e: Exception) {
            DEFAULT_SUBJECTS
        }
    }

    // नया विषय जोड़ने के लिए
    fun addSubject(context: Context, newSubject: String): Boolean {
        val trimmed = newSubject.trim()
        if (trimmed.isEmpty() || trimmed.equals("All", ignoreCase = true)) return false

        val currentList = getUserSubjects(context).toMutableList()
        // अगर पहले से लिस्ट में है तो दोबारा नहीं जोड़ेंगे
        if (currentList.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }

        currentList.add(trimmed)
        saveList(context, currentList)
        return true
    }

    // विषय को पट्टी से हटाने के लिए
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
