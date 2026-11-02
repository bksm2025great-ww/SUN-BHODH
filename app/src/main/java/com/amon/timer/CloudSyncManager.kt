package com.amon.timer

import android.content.Context
import android.util.Log
import com.amon.timer.groups.GroupMember
import com.amon.timer.groups.MemberRole
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
import java.util.Calendar
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

    // 🛡️ डुप्लीकेट सेशन सिंक रोकने के लिए गार्ड
    private var lastSyncTimestamp: Long = 0L
    private var lastSyncedMinutes: Long = 0L

    /**
     * 📅 हफ़्ते (Current Week vs Last Week) की Monday से Sunday की तारीखें निकालना
     */
    fun getWeekDateRange(isCurrentWeek: Boolean): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        if (!isCurrentWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -7)
        }
        val startDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val endDate = sdf.format(cal.time)

        return Pair(startDate, endDate)
    }

    /**
     * ✨ 1. नया छात्र आते ही Supabase के profiles रजिस्टर में नाम, UUID और ईमेल दर्ज करना
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
     * 👥 2. ग्रुप्स को Supabase से लोड करना
     */
    suspend fun fetchGroups(context: Context): List<StudyGroup> = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()

        val userManager = UserManager(context)
        val currentUserId = userManager.getUserId()

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

        val groupsUrl = "$SUPABASE_URL/rest/v1/study_groups?select=*"
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
                            isDefault = false,
                            inviteCode = obj.optString("invite_code", "")
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
     * 🗑️ 6. ग्रुप हमेशा के लिए डिलीट करना (Only Admin)
     */
    suspend fun deleteGroupOnCloud(groupId: String): Boolean = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext false

        try {
            // 1. ग्रुप के सभी मेंबर्स डिलीट करें
            val delMembersReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()
            client.newCall(delMembersReq).execute()

            // 2. ग्रुप को study_groups टेबल से हमेशा के लिए डिलीट करें
            val delGroupReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/study_groups?id=eq.$groupId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            val res = client.newCall(delGroupReq).execute()
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Delete group error: ${e.message}")
            return@withContext false
        }
    }

    /**
     * 🔍 7. इनवाइट कोड से प्राइवेट ग्रुप ढूँढना
     */
    suspend fun fetchGroupByInviteCode(inviteCode: String): StudyGroup? = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank() || inviteCode.isBlank()) return@withContext null

        val cleanCode = inviteCode.trim()
        val url = "$SUPABASE_URL/rest/v1/study_groups?invite_code=eq.$cleanCode&limit=1"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful && text.isNotBlank()) {
                val arr = JSONArray(text)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    return@withContext StudyGroup(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Private Group"),
                        category = obj.optString("category", "General"),
                        isPublic = obj.optBoolean("is_public", false),
                        memberCount = obj.optInt("member_count", 1),
                        focusingCount = 0,
                        isPinned = false,
                        isJoined = false,
                        isDefault = false,
                        inviteCode = obj.optString("invite_code", cleanCode)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search by invite code error: ${e.message}")
        }
        return@withContext null
    }

    /**
     * ⏱️ 8. पढ़ाई पूरी होने पर सेशन सिंक करना (डुप्लीकेट गार्ड के साथ)
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

        val now = System.currentTimeMillis()

        // 🛑 डुप्लीकेट फ़िल्टर: पिछले 3 सेकंड में अगर वही मिनट दोबारा आए तो रोक दो
        if (now - lastSyncTimestamp < 3000 && lastSyncedMinutes == durationMinutes) {
            Log.w(TAG, "Duplicate session sync blocked!")
            return
        }

        lastSyncTimestamp = now
        lastSyncedMinutes = durationMinutes

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
     * 📥 9. पढ़ाई का पिछला डेटा मंगाना
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
     * 👑 10. ग्लोबल लीडरबोर्ड डेटा खींचना
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

    /**
     * 🏆 11. ग्रुप लीडरबोर्ड (This Week vs Last Week) एक्टिव फ़िल्टर
     */
    suspend fun fetchGroupLeaderboard(
        context: Context,
        groupId: String,
        isCurrentWeek: Boolean
    ): List<GroupMember> = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()

        try {
            // 1. उस ग्रुप के सभी जुड़े हुए मेंबर्स लाओ
            val membersUrl = "$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId&select=user_id,nickname"
            val memReq = Request.Builder()
                .url(membersUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val memRes = client.newCall(memReq).execute()
            val memText = memRes.body?.string().orEmpty()
            if (!memRes.isSuccessful || memText.isBlank()) return@withContext emptyList()

            val memArray = JSONArray(memText)
            val membersMap = mutableMapOf<String, String>() // userId -> nickname
            for (i in 0 until memArray.length()) {
                val obj = memArray.getJSONObject(i)
                val uId = obj.optString("user_id")
                val nick = obj.optString("nickname", "Aspirant")
                if (uId.isNotBlank()) {
                    membersMap[uId] = nick
                }
            }

            if (membersMap.isEmpty()) return@withContext emptyList()

            // 2. उस हफ़्ते (Current vs Last) की तारीखें निकालो
            val (startDate, endDate) = getWeekDateRange(isCurrentWeek)

            // 3. उन तारीखों के बीच के सभी सेशन फ़ेच करो
            val sessionsUrl = "$SUPABASE_URL/rest/v1/study_sessions?session_date=gte.$startDate&session_date=lte.$endDate&select=user_id,duration_minutes"
            val sessReq = Request.Builder()
                .url(sessionsUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val sessRes = client.newCall(sessReq).execute()
            val sessText = sessRes.body?.string().orEmpty()

            val minutesMap = mutableMapOf<String, Int>()
            if (sessRes.isSuccessful && sessText.isNotBlank()) {
                val sessArray = JSONArray(sessText)
                for (i in 0 until sessArray.length()) {
                    val sObj = sessArray.getJSONObject(i)
                    val uId = sObj.optString("user_id")
                    val duration = sObj.optInt("duration_minutes", 0)
                    if (uId.isNotBlank()) {
                        minutesMap[uId] = (minutesMap[uId] ?: 0) + duration
                    }
                }
            }

            // 4. मेंबर्स को उनके पढ़े गए कुल मिनटों के हिसाब से रैंक करें
            val result = membersMap.map { (uId, nick) ->
                val totalMins = minutesMap[uId] ?: 0
                GroupMember(
                    id = uId,
                    nickname = nick,
                    role = MemberRole.MEMBER,
                    studyTimeMinutes = totalMins,
                    isCurrentlyFocusing = false,
                    lastActive = if (totalMins > 0) "This week" else "No sessions"
                )
            }.sortedByDescending { it.studyTimeMinutes }

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Group leaderboard fetch error: ${e.message}")
        }

        return@withContext emptyList()
    }
}
