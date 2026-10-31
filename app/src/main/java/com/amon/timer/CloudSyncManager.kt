package com.amon.timer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// 🏆 लीडरबोर्ड के लिए शुद्ध डेटा मॉडल
data class LeaderboardItem(
    val id: String,
    val name: String,
    val totalMinutes: Long
)

object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // 🔐 Supabase कनेक्शन की चाबियाँ
    private const val SUPABASE_URL = "https://mgenghztpcqaqaepvevw.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_flD-UULZ2YGzMrVLw2QHoA_7cXEg7FX"

    /**
     * ✨ 1. नया छात्र आते ही Supabase के profiles रजिस्टर में नाम और UUID दर्ज करना
     */
    fun registerUserOnCloud(context: Context) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) {
            Log.w(TAG, "Supabase credentials set nahi hai!")
            return
        }

        val userManager = UserManager(context)
        val userName = userManager.getUserName().trim()
        val userId = userManager.getUserId()

        if (userName.isBlank() || userName.equals("Guest", ignoreCase = true)) {
            return
        }

        val jsonPayload = JSONObject().apply {
            put("id", userId) // 🔑 Supabase की profiles टेबल का UUID
            put("display_name", userName)
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "User registration failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "User '$userName' ($userId) profiles me safalta se jud gaya!")
                    } else {
                        Log.e(TAG, "Registration error code: ${response.code}")
                    }
                }
            }
        })
    }

    /**
     * ⏱️ 2. पढ़ाई पूरी होने पर डेटा Supabase में भेजना (user_id और sessionId के साथ)
     */
    fun syncSession(
        context: Context,
        subject: String,
        durationMinutes: Long,
        earnedTrees: Int = 0,
        sessionId: Long = System.currentTimeMillis()
    ) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) {
            Log.w(TAG, "Supabase credentials set nahi hai!")
            return
        }

        if (durationMinutes <= 0) return

        val userManager = UserManager(context)
        val userId = userManager.getUserId()

        val jsonPayload = JSONObject().apply {
            put("id", sessionId) // 🔑 यूनिक ID ताकि डुप्लीकेट न बने
            put("user_id", userId) // 👈 इससे study_sessions में NULL आना बंद होगा
            put("subject", subject.ifBlank { "General" })
            put("duration_minutes", durationMinutes)
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/study_sessions")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates") // 🛡️ डुप्लीकेट एंट्री की जगह अपडेट करेगा
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Session sync fail: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Session safely synced to Supabase for user: $userId")
                    } else {
                        Log.e(TAG, "Server error code: ${response.code}")
                    }
                }
            }
        })
    }

    /**
     * 📥 3. Supabase से पढ़ाई का पूरा हिसाब वापस मंगाना (GET)
     */
    suspend fun fetchSessions(context: Context): List<FocusSession> = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()

        val userManager = UserManager(context)
        val userId = userManager.getUserId()

        // 🔒 केवल वर्तमान छात्र का डेटा वापस आएगा
        val fetchUrl = "$SUPABASE_URL/rest/v1/study_sessions?user_id=eq.$userId&select=*&order=created_at.desc"
        val request = Request.Builder()
            .url(fetchUrl)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (responseText.isNotBlank()) {
                val sessionsArray = JSONArray(responseText)
                val resultList = mutableListOf<FocusSession>()

                for (i in 0 until sessionsArray.length()) {
                    val item = sessionsArray.getJSONObject(i)
                    resultList.add(
                        FocusSession(
                            id = item.optLong("id", System.currentTimeMillis() + i),
                            date = item.optString("session_date", ""),
                            subject = item.optString("subject", "General"),
                            durationMinutes = item.optInt("duration_minutes", 0),
                            earnedTrees = 0
                        )
                    )
                }
                return@withContext resultList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase se data lane me error: ${e.message}")
        }

        return@withContext emptyList()
    }

    /**
     * 👑 4. Supabase से लाइव लीडरबोर्ड डेटा खींचना (टॉपर्स की लिस्ट)
     */
    suspend fun fetchLeaderboard(): List<LeaderboardItem> = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()

        val fetchUrl = "$SUPABASE_URL/rest/v1/study_sessions?select=id,subject,duration_minutes&order=duration_minutes.desc&limit=50"
        val request = Request.Builder()
            .url(fetchUrl)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (response.isSuccessful && responseText.isNotBlank()) {
                val array = JSONArray(responseText)
                val list = mutableListOf<LeaderboardItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "session_$i")
                    val subject = obj.optString("subject", "Student ${i + 1}")
                    val minutes = obj.optLong("duration_minutes", 0L)
                    list.add(LeaderboardItem(id = id, name = subject, totalMinutes = minutes))
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Leaderboard fetch error: ${e.message}")
        }
        return@withContext emptyList()
    }
}
