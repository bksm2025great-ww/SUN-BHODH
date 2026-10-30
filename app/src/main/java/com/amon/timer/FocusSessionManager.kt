package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object FocusSessionManager {
    // फ़ोन के अंदर डेटा सुरक्षित रखने की तिजोरी
    private const val PREFS_NAME = "amon_focus_prefs"
    private const val KEY_SESSIONS = "saved_sessions"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 1. डायरी में लिखना (5 मिनट से कम वाला सेव नहीं होगा + सीधे Supabase पर भेजना)
    fun saveSession(context: Context, session: FocusSession) {
        // 🛑 5 मिनट से कम की पढ़ाई कभी मेमोरी में नहीं जाएगी
        if (session.durationMinutes < 5) return

        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"

        try {
            val jsonArray = JSONArray(existingData)
            val updatedJsonArray = JSONArray()
            var isAlreadyExists = false

            // डुप्लीकेट रोकथाम लॉजिक
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

            // नया सेशन जोड़ने का ब्लॉक (पेड़ों का झंझट बाहर)
            if (!isAlreadyExists) {
                val newSessionObj = JSONObject().apply {
                    put("id", if (session.id != 0L) session.id else System.currentTimeMillis())
                    put("date", session.date)
                    put("subject", session.subject)
                    put("durationMinutes", session.durationMinutes)
                }
                updatedJsonArray.put(newSessionObj)
            }

            // फ़ोन की मेमोरी में सुरक्षित लॉक
            prefs.edit().putString(KEY_SESSIONS, updatedJsonArray.toString()).apply()

            // 🚀 डाकिया (CloudSyncManager) को Supabase में डेटा भेजने का सीधा आदेश
            CloudSyncManager.syncSession(
                context = context,
                subject = session.subject,
                durationMinutes = session.durationMinutes.toLong()
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. डायरी से पढ़ना + 🧹 5 मिनट से छोटे पुराने सेशन्स पर झाड़ू
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

                // 🛑 झाड़ू: 5 मिनट से छोटा कचरा बाहर
                if (duration < 5) {
                    needToCleanStorage = true
                    continue
                }

                cleanedJsonArray.put(obj)

                val session = FocusSession(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    date = obj.optString("date", ""),
                    subject = obj.optString("subject", "All"),
                    durationMinutes = duration,
                    earnedTrees = 0 // स्क्रीन क्रैश न हो इसलिए डिफ़ॉल्ट 0
                )
                sessionList.add(session)
            }

            // अगर सफ़ाई हुई हो तो मेमोरी अपडेट
            if (needToCleanStorage) {
                prefs.edit().putString(KEY_SESSIONS, cleanedJsonArray.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // नई पढ़ाई सबसे ऊपर
        return sessionList.reversed()
    }

    // 🟢 Shortcut alias: पुरानी स्क्रीन के साथ तालमेल के लिए
    fun getSessions(context: Context): List<FocusSession> {
        return getAllSessions(context)
    }

    // 3. डेटा रीस्टोर करना (शुद्ध मिनटों के साथ)
    fun restoreSessions(context: Context, incomingSessions: List<FocusSession>): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        var restoredMinutes = 0

        try {
            val jsonArray = JSONArray(existingData)
            val cleanExistingArray = JSONArray()
            val existingList = mutableListOf<JSONObject>()

            // केवल 5 मिनट या उससे बड़े सेशन्स
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
