package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object FocusSessionManager {
    // यह हमारे फ़ोन के अंदर की छुपी हुई तिजोरी (Locker) का नाम है
    private const val PREFS_NAME = "amon_forest_prefs"
    private const val KEY_SESSIONS = "saved_sessions"

    // तिजोरी खोलने की चाबी
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 1. डायरी में लिखना (Save Data with Duplicate Prevention / Upsert)
    fun saveSession(context: Context, session: FocusSession) {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"

        try {
            val jsonArray = JSONArray(existingData)
            val updatedJsonArray = JSONArray()
            var isAlreadyExists = false

            // डुप्लीकेट रोकने का लॉजिक (Upsert check): अगर वही ID पहले से है तो उसे अपडेट करो
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getLong("id") == session.id) {
                    isAlreadyExists = true
                    // Update existing object
                    obj.put("date", session.date)
                    obj.put("subject", session.subject)
                    obj.put("durationMinutes", session.durationMinutes)
                    obj.put("earnedTrees", session.earnedTrees)
                }
                updatedJsonArray.put(obj)
            }

            // अगर नया सेशन है, तो इसे सूची में जोड़ दो
            if (!isAlreadyExists) {
                val newSessionObj = JSONObject().apply {
                    put("id", session.id)
                    put("date", session.date)
                    put("subject", session.subject)
                    put("durationMinutes", session.durationMinutes)
                    put("earnedTrees", session.earnedTrees)
                }
                updatedJsonArray.put(newSessionObj)
            }

            // वापस तिजोरी में सुरक्षित रख दो
            prefs.edit().putString(KEY_SESSIONS, updatedJsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. डायरी से पढ़ना (Read Data)
    fun getAllSessions(context: Context): List<FocusSession> {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val sessionList = mutableListOf<FocusSession>()

        try {
            val jsonArray = JSONArray(existingData)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val session = FocusSession(
                    id = obj.getLong("id"),
                    date = obj.getString("date"),
                    subject = obj.getString("subject"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    earnedTrees = obj.getInt("earnedTrees")
                )
                sessionList.add(session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // लिस्ट को उल्टा (reversed) कर रहे हैं ताकि सबसे नई पढ़ाई सबसे ऊपर दिखे
        return sessionList.reversed()
    }

    // 🟢 Shortcut alias to support any legacy getSessions calls smoothly
    fun getSessions(context: Context): List<FocusSession> {
        return getAllSessions(context)
    }
}
