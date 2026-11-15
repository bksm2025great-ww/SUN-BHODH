package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object FocusSessionManager {
    private const val PREFS_NAME = "amon_focus_prefs"
    private const val KEY_SESSIONS = "saved_sessions"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 1. नया सेशन सेव करना (5 मिनट से कम का सेव नहीं होगा)
    fun saveSession(context: Context, session: FocusSession) {
        if (session.durationMinutes < 5) return

        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"

        try {
            val jsonArray = JSONArray(existingData)
            val updatedJsonArray = JSONArray()
            var isAlreadyExists = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val existingId = obj.optLong("id", -1L)

                if (existingId == session.id && session.id != 0L) {
                    isAlreadyExists = true
                    obj.put("date", session.date)
                    obj.put("subject", session.subject)
                    obj.put("durationMinutes", session.durationMinutes)
                }
                updatedJsonArray.put(obj)
            }

            if (!isAlreadyExists) {
                val newSessionObj = JSONObject().apply {
                    put("id", if (session.id != 0L) session.id else System.currentTimeMillis())
                    put("date", session.date)
                    put("subject", session.subject)
                    put("durationMinutes", session.durationMinutes)
                }
                updatedJsonArray.put(newSessionObj)
            }

            prefs.edit().putString(KEY_SESSIONS, updatedJsonArray.toString()).apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. सभी सेशंस पढ़ना (5 मिनट से छोटे सेशन्स हटाना)
    fun getAllSessions(context: Context): List<FocusSession> {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val sessionList = mutableListOf<FocusSession>()
        val cleanedJsonArray = JSONArray()
        var needToCleanStorage = false

        try {
            val jsonArray = JSONArray(existingData)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val duration = obj.optInt("durationMinutes", 0)

                if (duration < 5) {
                    needToCleanStorage = true
                    continue
                }

                cleanedJsonArray.put(obj)

                val session = FocusSession(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    date = obj.optString("date", ""),
                    subject = obj.optString("subject", "All"),
                    durationMinutes = duration
                )
                sessionList.add(session)
            }

            if (needToCleanStorage) {
                prefs.edit().putString(KEY_SESSIONS, cleanedJsonArray.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sessionList.reversed()
    }

    fun getSessions(context: Context): List<FocusSession> {
        return getAllSessions(context)
    }

    // 3. डेटा रीस्टोर करना
    fun restoreSessions(context: Context, incomingSessions: List<FocusSession>): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        var restoredMinutes = 0

        try {
            val jsonArray = JSONArray(existingData)
            val cleanExistingArray = JSONArray()
            val existingList = mutableListOf<JSONObject>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optInt("durationMinutes", 0) >= 5) {
                    existingList.add(obj)
                    cleanExistingArray.put(obj)
                }
            }

            var anyNewAdded = false

            for (incoming in incomingSessions) {
                if (incoming.durationMinutes < 5) continue

                val alreadyExists = existingList.any { obj ->
                    val sameId = obj.optLong("id", -1L) == incoming.id
                    val sameDate = obj.optString("date") == incoming.date
                    val sameSubject = obj.optString("subject").equals(incoming.subject, ignoreCase = true)
                    val sameDuration = obj.optInt("durationMinutes") == incoming.durationMinutes

                    sameId || (sameDate && sameSubject && sameDuration)
                }

                if (!alreadyExists) {
                    val newObj = JSONObject().apply {
                        put("id", if (incoming.id > 0) incoming.id else System.currentTimeMillis())
                        put("date", incoming.date)
                        put("subject", incoming.subject)
                        put("durationMinutes", incoming.durationMinutes)
                    }
                    cleanExistingArray.put(newObj)
                    existingList.add(newObj)
                    restoredMinutes += incoming.durationMinutes
                    anyNewAdded = true
                }
            }

            if (anyNewAdded || cleanExistingArray.length() != jsonArray.length()) {
                prefs.edit().putString(KEY_SESSIONS, cleanExistingArray.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(0, restoredMinutes)
    }
}
