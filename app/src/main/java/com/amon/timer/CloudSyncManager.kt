package com.amon.timer

import android.content.Context
import android.util.Log
import com.amon.timer.groups.StudyGroup
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
     * ✨ 1. नया छात्र आते ही Supabase के profiles रजिस्टर में नाम, UUID और ईमेल दर्ज करना
     * (पहले चेक करेगा कि यह Gmail पहले से है या नहीं, ताकि वही पुरानी UUID रीस्टोर हो सके)
     */
    fun registerUserOnCloud(
        context: Context,
        userId: String? = null,
        nickname: String? = null,
        email: String? = null
    ) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) {
            Log.w(TAG, "Supabase credentials set nahi hai!")
            return
        }

        val userManager = UserManager(context)
        val finalEmail = (email ?: userManager.getUserEmail()).trim()
        val finalUserName = (nickname ?: userManager.getUserName()).trim()

        if (finalUserName.isBlank() || finalUserName.equals("Guest", ignoreCase = true)) {
            return
        }

        // 🔍 Gmail चेक: अगर ईमेल पहले से Supabase में है, तो पुरानी UUID निकालो
        if (finalEmail.isNotBlank()) {
            val checkUrl = "$SUPABASE_URL/rest/v1/profiles?email=eq.$finalEmail&select=id,display_name&limit=1"
            val checkRequest = Request.Builder()
                .url(checkUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            client.newCall(checkRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Email check failed, fallback to local UUID: ${e.message}")
                    saveProfileDirectly(context, userId ?: userManager.getUserId(), finalUserName, finalEmail)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val responseText = response.body?.string().orEmpty()
                        var targetUserId = userId ?: userManager.getUserId()

                        if (response.isSuccessful && responseText.isNotBlank()) {
                            try {
                                val arr = JSONArray(responseText)
                                if (arr.length() > 0) {
                                    val existingId = arr.getJSONObject(0).getString("id")
                                    userManager.setUserId(existingId)
                                    targetUserId = existingId
                                    Log.d(TAG, "Purana account mila! UUID restored: $existingId")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "JSON parse error during email check: ${e.message}")
                            }
                        }

                        saveProfileDirectly(context, targetUserId, finalUserName, finalEmail)
                    }
                }
            })
        } else {
            val targetUserId = userId ?: userManager.getUserId()
            saveProfileDirectly(context, targetUserId, finalUserName, null)
        }
    }

    /**
     * 💾 profiles टेबल में डेटा UPSERT करने का इंटरनल फ़ंक्शन
     */
    private fun saveProfileDirectly(
        context: Context,
        finalUserId: String,
        finalUserName: String,
        finalEmail: String?
    ) {
        val jsonPayload = JSONObject().apply {
            put("id", finalUserId)
            put("display_name", finalUserName)
            if (!finalEmail.isNullOrBlank()) {
                put("email", finalEmail)
            }
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?on_conflict=id")
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
                        Log.d(TAG, "User '$finalUserName' ($finalUserId) profiles me safalta se jud gaya!")
                    } else {
                        Log.e(TAG, "Registration error code: ${response.code} body: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    /**
     * 👥 2. ग्रुप्स को Supabase से लोड करना (Universal Library auto-join नहीं होगी)
     */
    suspend fun fetchGroups(context: Context): List<StudyGroup> = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()

        val userManager = UserManager(context)
        val currentUserId = userManager.getUserId()

        // 1. जाँचें कि यूज़र किन-किन ग्रुप्स में जुड़ा है
        val joinedGroupIds = mutableSetOf<String>()
        try {
            val memberReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/group_members?user_id=eq.$currentUserId&select=group_id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val memberRes = client.newCall(memberReq).execute()
            val memberText = memberRes.body?.string().orEmpty()
            if (memberRes.isSuccessful && memberText.isNotBlank()) {
                val arr = JSONArray(memberText)
                for (i in 0 until arr.length()) {
                    joinedGroupIds.add(arr.getJSONObject(i).optString("group_id"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Joined groups fetch error: ${e.message}")
        }

        // 2. study_groups टेबल से सारे ग्रुप्स लोड करें
        val groupsUrl = "$SUPABASE_URL/rest/v1/study_groups?select=*&order=created_at.desc"
        val request = Request.Builder()
            .url(groupsUrl)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful && text.isNotBlank()) {
                val arr = JSONArray(text)
                val list = mutableListOf<StudyGroup>()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val gId = obj.optString("id")
                    val isJoined = joinedGroupIds.contains(gId)

                    list.add(
                        StudyGroup(
                            id = gId,
                            name = obj.optString("name", "Study Group"),
                            category = obj.optString("category", "General"),
                            isPublic = obj.optBoolean("is_public", true),
                            memberCount = obj.optInt("member_count", 1),
                            focusingCount = 0,
                            isPinned = false,
                            isJoined = isJoined,
                            isDefault = false, // ज़बरन ऑटो-जॉइन हटाने के लिए false रखा गया
                            inviteCode = obj.optString("invite_code", "AMON-GLOBAL")
                        )
                    )
                }

                // अगर यूनिवर्सल लाइब्रेरी सूची में नहीं है तो जोड़ें (बिना ज़बरन जोड़े)
                if (list.none { it.id == "universal_lib" }) {
                    list.add(
                        0,
                        StudyGroup(
                            id = "universal_lib",
                            name = "Universal Library",
                            category = "All",
                            isPublic = true,
                            memberCount = 1,
                            focusingCount = 0,
                            isPinned = true,
                            isJoined = joinedGroupIds.contains("universal_lib"),
                            isDefault = false, // यूज़र अपनी मर्ज़ी से जुड़ेगा
                            inviteCode = "AMON-GLOBAL"
                        )
                    )
                }

                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Groups fetch error: ${e.message}")
        }

        return@withContext emptyList()
    }

    /**
     * ➕ 3. नया ग्रुप बनाना
     */
    suspend fun createGroupOnCloud(
        context: Context,
        group: StudyGroup
    ): Boolean = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext false

        val jsonPayload = JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            put("category", group.category)
            put("is_public", group.isPublic)
            put("member_count", 1)
            put("is_default", false)
            put("invite_code", group.inviteCode)
        }

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/study_groups?on_conflict=id")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                joinGroupOnCloud(context, group.id)
                return@withContext true
            } else {
                Log.e(TAG, "Create group failed: ${response.code} body: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create group exception: ${e.message}")
        }
        return@withContext false
    }

    /**
     * 🚪 4. ग्रुप जॉइन करना
     */
    suspend fun joinGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val userManager = UserManager(context)
        val userId = userManager.getUserId()
        val userName = userManager.getUserName()

        val payload = JSONObject().apply {
            put("group_id", groupId)
            put("user_id", userId)
            put("nickname", userName)
        }

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/group_members")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Join group error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * ❌ 5. ग्रुप छोड़ना (Leave Group)
     */
    suspend fun leaveGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val userManager = UserManager(context)
        val userId = userManager.getUserId()

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId&user_id=eq.$userId")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .delete()
            .build()

        try {
            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Leave group error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * ⏱️ 6. पढ़ाई पूरी होने पर सेशन सिंक करना
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
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val jsonPayload = JSONObject().apply {
            put("user_id", userId)
            put("subject", subject.ifBlank { "All" })
            put("duration_minutes", durationMinutes)
            put("session_date", currentDate)
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/study_sessions")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
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
                        Log.e(TAG, "Server error code: ${response.code} body: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    /**
     * 📥 7. पढ़ाई का पिछला डेटा मंगाना
     */
    suspend fun fetchSessions(context: Context): List<FocusSession> = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()

        val userManager = UserManager(context)
        val userId = userManager.getUserId()

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
                            subject = item.optString("subject", "All"),
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
     * 👑 8. लाइव लीडरबोर्ड डेटा खींचना
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
