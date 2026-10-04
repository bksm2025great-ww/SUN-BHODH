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

    // 1. डायरी में लिखना (5 मिनट से कम वाला कभी सेव नहीं होगा + डुप्लीकेट रोकथाम)
    fun saveSession(context: Context, session: FocusSession) {
        // 🛑 5 MINUTE GUARD: 5 मिनट से कम की पढ़ाई कभी लोकल मेमोरी में नहीं जाएगी
        if (session.durationMinutes < 5) return

        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"

        try {
            val jsonArray = JSONArray(existingData)
            val updatedJsonArray = JSONArray()
            var isAlreadyExists = false

            // डुप्लीकेट रोकने का लॉजिक (Upsert check): optLong से क्रैश का ख़तरा शून्य
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val existingId = obj.optLong("id", -1L)

                if (existingId == session.id && session.id != 0L) {
                    isAlreadyExists = true
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
                    put("id", if (session.id != 0L) session.id else System.currentTimeMillis())
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

    // 2. डायरी से पढ़ना + 🧹 ऑटो-क्लीनर (0 से 4 मिनट वाले पुराने सेशन्स पर झाड़ू)
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

                // 🛑 झाड़ू: 5 मिनट से छोटा पुराना सेशन मिलते ही तिजोरी से बाहर
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
                    earnedTrees = obj.optInt("earnedTrees", 0)
                )
                sessionList.add(session)
            }

            // 🧹 अगर कचरा मिला था, तो फ़ोन की मेमोरी को हमेशा के लिए क्लीन करके राइट कर दो
            if (needToCleanStorage) {
                prefs.edit().putString(KEY_SESSIONS, cleanedJsonArray.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // लिस्ट को उल्टा (reversed) कर रहे हैं ताकि सबसे नई पढ़ाई सबसे ऊपर दिखे
        return sessionList.reversed()
    }

    // 🟢 Shortcut alias: पुराने कोड के साथ 100% कम्पैटिबल
    fun getSessions(context: Context): List<FocusSession> {
        return getAllSessions(context)
    }

    // 3. 🛡️️ Google Sheet से डेटा रीस्टोर करना (5 मिनट से कम वाले को अंदर मत आने दो)
    fun restoreSessions(context: Context, incomingSessions: List<FocusSession>): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val existingData = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        var restoredTrees = 0
        var restoredMinutes = 0

        try {
            val jsonArray = JSONArray(existingData)
            val cleanExistingArray = JSONArray()
            val existingList = mutableListOf<JSONObject>()

            // केवल 5 मिनट या उससे बड़े पुराने सेशन्स ही आगे जाएँगे
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optInt("durationMinutes", 0) >= 5) {
                    existingList.add(obj)
                    cleanExistingArray.put(obj)
                }
            }

            var anyNewAdded = false

            for (incoming in incomingSessions) {
                // 🛑 शीट से आया हुआ सेशन भी अगर 5 मिनट से छोटा है, तो छोड़ दो
                if (incoming.durationMinutes < 5) continue

                // स्मार्ट आधार कार्ड चेक: तारीख, विषय और समय की समानता
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
                        put("earnedTrees", incoming.earnedTrees)
                    }
                    cleanExistingArray.put(newObj)
                    existingList.add(newObj)
                    restoredTrees += incoming.earnedTrees
                    restoredMinutes += incoming.durationMinutes
                    anyNewAdded = true
                }
            }

            // साफ़-सुथरा डेटा तिजोरी में लॉक
            if (anyNewAdded || cleanExistingArray.length() != jsonArray.length()) {
                prefs.edit().putString(KEY_SESSIONS, cleanExistingArray.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(restoredTrees, restoredMinutes)
    }
}
