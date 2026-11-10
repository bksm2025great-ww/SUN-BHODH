package com.amon.timer

import android.content.Context
import android.util.Log
import com.amon.timer.groups.GroupCacheManager
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

    // 👑 एडमिन राइट्स को सुरक्षित रूप से लोकल प्रेफ्स में ट्रैक करना
    fun markGroupAsAdmin(context: Context, groupId: String) {
        context.getSharedPreferences("amon_admin_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("admin_$groupId", true).apply()
    }

    fun isGroupAdmin(context: Context, groupId: String): Boolean {
        return context.getSharedPreferences("amon_admin_prefs", Context.MODE_PRIVATE)
            .getBoolean("admin_$groupId", false)
    }

    fun removeGroupAdmin(context: Context, groupId: String) {
        context.getSharedPreferences("amon_admin_prefs", Context.MODE_PRIVATE)
            .edit().remove("admin_$groupId").apply()
    }

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
     * ✨ 1. छात्र की स्थायी ID के साथ profiles रजिस्टर करना
     */
    fun registerUserOnCloud(
        context: Context,
        userId: String? = null,
        nickname: String? = null,
        email: String? = null
    ) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return

        val userManager = UserManager(context)
        val finalUserId = (userId ?: userManager.getUserId()).trim()
        val finalUserName = (nickname ?: userManager.getUserName()).trim().ifBlank { "Aspirant" }
        val finalEmail = (email ?: userManager.getUserEmail()).trim().lowercase()

        val jsonPayload = JSONObject().apply {
            put("id", finalUserId)
            put("display_name", finalUserName)
            if (finalEmail.isNotBlank()) {
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
                        Log.d(TAG, "User '$finalUserName' ($finalUserId) permanent profile synced!")
                    } else {
                        Log.e(TAG, "Profile sync error: ${response.code} body: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    /**
     * 🔄 पेंडिंग ऑफ़लाइन ऐक्शन को नेट आने पर बैकग्राउंड में सुपबेस को भेजना
     */
    suspend fun syncPendingGroupActions(context: Context) = withContext(Dispatchers.IO) {
        val pending = GroupCacheManager.getPendingActions(context)
        if (pending.isEmpty()) return@withContext

        for (item in pending.toList()) {
            val parts = item.split(":")
            if (parts.size == 2) {
                val action = parts[0]
                val gId = parts[1]
                val success = if (action == "JOIN") {
                    joinGroupOnCloudDirect(context, gId)
                } else {
                    leaveGroupOnCloudDirect(context, gId)
                }
                if (success) {
                    GroupCacheManager.removePendingAction(context, item)
                    Log.d(TAG, "Successfully synced pending action: $item")
                }
            }
        }
    }

    /**
     * 👥 2. ग्रुप्स को सुपबेस से लोड करना (ऑफ़लाइन फ़र्स्ट: नेट न होने पर तुरंत लोकल कैशे)
     */
    suspend fun fetchGroups(context: Context): List<StudyGroup> = withContext(Dispatchers.IO) {
        val localBackup = GroupCacheManager.getCachedGroupsList(context)

        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) {
            return@withContext localBackup
        }

        // पहले पेंडिंग ऑफ़लाइन ऐक्शन को सिंक करें
        try {
            syncPendingGroupActions(context)
        } catch (_: Exception) {}

        val userManager = UserManager(context)
        val currentUserId = userManager.getUserId()
        val currentUserName = userManager.getUserName().ifBlank { "Aspirant" }

        val joinedGroupIds = mutableSetOf<String>()
        val groupCountsMap = mutableMapOf<String, Int>()

        try {
            val allMembersReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/group_members?select=group_id,user_id")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val allMembersRes = client.newCall(allMembersReq).execute()
            val allMembersText = allMembersRes.body?.string().orEmpty()
            if (allMembersRes.isSuccessful && allMembersText.isNotBlank()) {
                val arr = JSONArray(allMembersText)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val gId = obj.optString("group_id")
                    val uId = obj.optString("user_id")

                    if (uId == currentUserId) {
                        joinedGroupIds.add(gId)
                    }
                    groupCountsMap[gId] = (groupCountsMap[gId] ?: 0) + 1
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Members count fetch error (offline?): ${e.message}")
            return@withContext localBackup
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
                    val realMemberCount = groupCountsMap[gId] ?: obj.optInt("member_count", 1)

                    val isDefaultGroup = (gId == "universal_lib") || obj.optBoolean("is_default", false)
                    val isUserAdmin = (!isDefaultGroup) && isGroupAdmin(context, gId)

                    val membersList = if (isUserAdmin) {
                        listOf(GroupMember(id = currentUserId, nickname = currentUserName, role = MemberRole.ADMIN))
                    } else {
                        emptyList()
                    }

                    list.add(
                        StudyGroup(
                            id = gId,
                            name = obj.optString("name", "Study Group"),
                            category = obj.optString("category", "General"),
                            isPublic = obj.optBoolean("is_public", true),
                            memberCount = realMemberCount,
                            focusingCount = 0,
                            isPinned = false,
                            isJoined = isJoined,
                            isDefault = isDefaultGroup,
                            inviteCode = obj.optString("invite_code", ""),
                            members = membersList
                        )
                    )
                }

                val hasUniversal = list.any { it.id == "universal_lib" }
                if (!hasUniversal) {
                    val defaultLib = StudyGroup(
                        id = "universal_lib",
                        name = "Universal Library",
                        category = "Global",
                        isPublic = true,
                        memberCount = 1,
                        focusingCount = 0,
                        isPinned = true,
                        isJoined = joinedGroupIds.contains("universal_lib"),
                        isDefault = true,
                        inviteCode = "GLOBAL"
                    )
                    list.add(0, defaultLib)
                    createGroupOnCloud(context, defaultLib)
                }

                GroupCacheManager.saveCachedGroupsList(context, list)
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Groups fetch error (fallback to local): ${e.message}")
        }

        return@withContext localBackup
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
            put("is_default", group.isDefault)
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
                if (!group.isDefault) {
                    markGroupAsAdmin(context, group.id)
                }
                joinGroupOnCloud(context, group.id)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create group exception: ${e.message}")
        }
        return@withContext false
    }

    /**
     * 🚪 4. ग्रुप जॉइन करना (नेट न होने पर ऑटो-डायरी में दर्ज)
     */
    suspend fun joinGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val success = joinGroupOnCloudDirect(context, groupId)
        if (!success) {
            GroupCacheManager.addPendingAction(context, "JOIN", groupId)
        }
        return@withContext success
    }

    private fun joinGroupOnCloudDirect(context: Context, groupId: String): Boolean {
        try {
            val userManager = UserManager(context)
            val userId = userManager.getUserId()
            val userName = userManager.getUserName().ifBlank { "Aspirant" }

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

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 409) {
                updateUserWeeklyMinutes(userId)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Join direct error: ${e.message}")
        }
        return false
    }

    /**
     * ❌ 5. ग्रुप छोड़ना (नेट न होने पर ऑटो-डायरी में दर्ज)
     */
    suspend fun leaveGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val success = leaveGroupOnCloudDirect(context, groupId)
        if (!success) {
            GroupCacheManager.addPendingAction(context, "LEAVE", groupId)
        }
        return@withContext success
    }

    private fun leaveGroupOnCloudDirect(context: Context, groupId: String): Boolean {
        try {
            val userManager = UserManager(context)
            val userId = userManager.getUserId()

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId&user_id=eq.$userId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Leave direct error: ${e.message}")
        }
        return false
    }

    /**
     * 🗑️ 6. ग्रुप हमेशा के लिए डिलीट करना (Universal Library सुरक्षित)
     */
    suspend fun deleteGroupOnCloud(groupId: String): Boolean = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext false

        if (groupId == "universal_lib") {
            Log.w(TAG, "Default Universal Library cannot be deleted from cloud!")
            return@withContext false
        }

        try {
            val delMembersReq = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()
            client.newCall(delMembersReq).execute()

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
     * 🔍 7. इनवाइट कोड से ग्रुप ढूँढना
     */
    suspend fun fetchGroupByInviteCode(inviteCode: String): StudyGroup? = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext null

        val cleanCode = inviteCode.trim()
        if (cleanCode.isBlank()) return@withContext null

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
     * ⏱️ 8. पढ़ाई पूरी होने पर सेशन सिंक करना
     */
    fun syncSession(
        context: Context,
        subject: String,
        durationMinutes: Long,
        earnedTrees: Int = 0,
        sessionId: Long = System.currentTimeMillis()
    ) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return
        if (durationMinutes <= 0) return

        val now = System.currentTimeMillis()
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
            .addHeader("Prefer", "return=representation")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Session sync fail: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Session safely synced to Supabase for permanent user: $userId")
                        updateUserWeeklyMinutes(userId)
                    } else {
                        Log.e(TAG, "Supabase session sync rejected! Code: ${response.code} Body: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    /**
     * 🔄 8.1 group_members टेबल में weekly_study_minutes अपडेट करना
     */
    fun updateUserWeeklyMinutes(userId: String) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank() || userId.isBlank()) return

        val (startDate, endDate) = getWeekDateRange(isCurrentWeek = true)
        val queryUrl = "$SUPABASE_URL/rest/v1/study_sessions?user_id=eq.$userId&session_date=gte.$startDate&session_date=lte.$endDate&select=duration_minutes"

        val request = Request.Builder()
            .url(queryUrl)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to calculate weekly minutes: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val text = response.body?.string().orEmpty()
                    if (response.isSuccessful && text.isNotBlank()) {
                        try {
                            val arr = JSONArray(text)
                            var totalWeeklyMins = 0L
                            for (i in 0 until arr.length()) {
                                totalWeeklyMins += arr.getJSONObject(i).optLong("duration_minutes", 0L)
                            }

                            val patchPayload = JSONObject().apply {
                                put("weekly_study_minutes", totalWeeklyMins)
                            }

                            val patchRequest = Request.Builder()
                                .url("$SUPABASE_URL/rest/v1/group_members?user_id=eq.$userId")
                                .addHeader("apikey", SUPABASE_KEY)
                                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                                .addHeader("Content-Type", "application/json")
                                .patch(patchPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                                .build()

                            client.newCall(patchRequest).enqueue(object : Callback {
                                override fun onFailure(pCall: Call, pE: IOException) {
                                    Log.e(TAG, "Failed to update weekly_study_minutes: ${pE.message}")
                                }

                                override fun onResponse(pCall: Call, pResponse: Response) {
                                    pResponse.use {
                                        if (pResponse.isSuccessful) {
                                            Log.d(TAG, "weekly_study_minutes updated to $totalWeeklyMins for $userId")
                                        }
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing weekly minutes: ${e.message}")
                        }
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
     * 🏆 11. ग्रुप लीडरबोर्ड (This Week vs Last Week)
     */
    suspend fun fetchGroupLeaderboard(
        context: Context,
        groupId: String,
        isCurrentWeek: Boolean
    ): List<GroupMember> = withContext(Dispatchers.IO) {
        val cached = GroupCacheManager.getCachedLeaderboard(context, groupId)

        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext cached

        try {
            val membersUrl = "$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId&select=user_id,nickname"
            val memReq = Request.Builder()
                .url(membersUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            val memRes = client.newCall(memReq).execute()
            val memText = memRes.body?.string().orEmpty()
            if (!memRes.isSuccessful || memText.isBlank()) return@withContext cached

            val memArray = JSONArray(memText)
            val membersMap = mutableMapOf<String, String>()
            for (i in 0 until memArray.length()) {
                val obj = memArray.getJSONObject(i)
                val uId = obj.optString("user_id")
                val nick = obj.optString("nickname", "Aspirant")
                if (uId.isNotBlank()) {
                    membersMap[uId] = nick
                }
            }

            if (membersMap.isEmpty()) return@withContext cached

            val (startDate, endDate) = getWeekDateRange(isCurrentWeek)

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

            if (isCurrentWeek && result.isNotEmpty()) {
                GroupCacheManager.saveLeaderboard(context, groupId, result)
            }

            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Group leaderboard fetch error (returning cache): ${e.message}")
        }

        return@withContext cached
    }
}
