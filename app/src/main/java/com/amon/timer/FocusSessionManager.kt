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

    // 3. 🛡️ Google Sheet से आया डेटा जोड़ना (स्मार्ट आधार कार्ड चेक - डुप्लीकेट नहीं बनेगा)
    // यह फ़ंक्शन वापस बताता है: (कितने नए पेड़ जुड़े, कितने मिनट जुड़े)
    fun restoreSessions(context: Context, incomingSessions: List<FocusSession>): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        var restoredTrees = 0
        var restoredMinutes = 0

        try {
            val jsonArray = JSONArray(existingData)
            val existingList = mutableListOf<JSONObject>()
            for (i in 0 until jsonArray.length()) {
                existingList.add(jsonArray.getJSONObject(i))
            }

            var anyNewAdded = false

            for (incoming in incomingSessions) {
                // स्मार्ट आधार कार्ड चेक: क्या यह सेशन तारीख, विषय और समय से पहले से मौजूद है?
                val alreadyExists = existingList.any { obj ->
                    val sameId = obj.optLong("id", -1L) == incoming.id
                    val sameDate = obj.optString("date") == incoming.date
                    val sameSubject = obj.optString("subject").equals(incoming.subject, ignoreCase = true)
                    val sameDuration = obj.optInt("durationMinutes") == incoming.durationMinutes

                    sameId || (sameDate && sameSubject && sameDuration)
                }

                // अगर यह सेशन फ़ोन में नहीं है, तभी जोड़ेंगे
                if (!alreadyExists) {
                    val newObj = JSONObject().apply {
                        put("id", if (incoming.id > 0) incoming.id else System.currentTimeMillis())
                        put("date", incoming.date)
                        put("subject", incoming.subject)
                        put("durationMinutes", incoming.durationMinutes)
                        put("earnedTrees", incoming.earnedTrees)
                    }
                    jsonArray.put(newObj)
                    existingList.add(newObj)
                    restoredTrees += incoming.earnedTrees
                    restoredMinutes += incoming.durationMinutes
                    anyNewAdded = true
                }
            }

            // अगर कोई नया सेशन जुड़ा है, तो तिजोरी में पक्का सेव कर दो
            if (anyNewAdded) {
                prefs.edit().putString(KEY_SESSIONS, jsonArray.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(restoredTrees, restoredMinutes)
    }
}
