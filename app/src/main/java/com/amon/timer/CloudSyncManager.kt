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

data class LeaderboardItem(
    val id: String,
    val name: String,
    val totalMinutes: Long
)

object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private const val SUPABASE_URL = "https://mgenghztpcqaqaepvevw.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_flD-UULZ2YGzMrVLw2QHoA_7cXEg7FX"

    private var lastSyncTimestamp: Long = 0L
    private var lastSyncedMinutes: Long = 0L

    suspend fun signInWithGoogleIdToken(context: Context, idToken: String): String? = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank() || idToken.isBlank()) return@withContext null

        val jsonPayload = JSONObject().apply {
            put("provider", "google")
            put("id_token", idToken)
        }

        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/token?grant_type=id_token")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()
            if (response.isSuccessful && responseText.isNotBlank()) {
                val obj = JSONObject(responseText)
                val userObj = obj.optJSONObject("user")
                val authUid = userObj?.optString("id", "")?.trim().orEmpty()
                if (authUid.isNotBlank()) {
                    UserManager(context).setUserId(authUid)
                    return@withContext authUid
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth exception: ${e.message}")
        }
        return@withContext null
    }

    fun markGroupAsAdmin(context: Context, groupId: String) {
        context.getSharedPreferences("amon_admin_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("admin_$groupId", true).apply()
    }

    fun isGroupAdmin(context: Context, groupId: String): Boolean {
        return context.getSharedPreferences("amon_admin_prefs", Context.MODE_PRIVATE)
            .getBoolean("admin_$groupId", false)
    }

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

    fun registerUserOnCloud(context: Context, userId: String? = null, nickname: String? = null) {
        val userManager = UserManager(context)
        if (userManager.isGuestUser()) return
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return

        val finalUserId = (userId ?: userManager.getUserId()).trim()
        if (finalUserId.isBlank()) return

        val finalUserName = (nickname ?: userManager.getUserName()).trim().ifBlank { "Aspirant" }

        val jsonPayload = JSONObject().apply {
            put("id", finalUserId)
            put("display_name", finalUserName)
        }

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/profiles?on_conflict=id")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Profile registration failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    suspend fun fetchGroups(context: Context): List<StudyGroup> = withContext(Dispatchers.IO) {
        val localBackup = GroupCacheManager.getCachedGroupsList(context)
        val userManager = UserManager(context)
        if (userManager.isGuestUser() || SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext localBackup

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
                    if (currentUserId.isNotBlank() && uId == currentUserId) {
                        joinedGroupIds.add(gId)
                    }
                    groupCountsMap[gId] = (groupCountsMap[gId] ?: 0) + 1
                }
            }
        } catch (e: Exception) {
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
                    val isDefaultGroup = obj.optBoolean("is_default", false)
                    val isUserAdmin = (!isDefaultGroup) && isGroupAdmin(context, gId)

                    val membersList = if (isUserAdmin && currentUserId.isNotBlank()) {
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

                val mergedList = GroupCacheManager.mergeWithCloud(context, list)
                GroupCacheManager.saveCachedGroupsList(context, mergedList)
                return@withContext mergedList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch groups error: ${e.message}")
        }

        return@withContext localBackup
    }

    suspend fun createGroupOnCloud(context: Context, group: StudyGroup): Boolean = withContext(Dispatchers.IO) {
        val userManager = UserManager(context)
        if (userManager.isGuestUser() || SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext false

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
                if (!group.isDefault) markGroupAsAdmin(context, group.id)
                joinGroupOnCloud(context, group.id)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create group error: ${e.message}")
        }
        return@withContext false
    }

    suspend fun joinGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val userManager = UserManager(context)
        if (userManager.isGuestUser()) return@withContext false
        try {
            val userId = userManager.getUserId()
            if (userId.isBlank()) return@withContext false
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
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Join error: ${e.message}")
        }
        return@withContext false
    }

    suspend fun leaveGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        val userManager = UserManager(context)
        if (userManager.isGuestUser()) return@withContext false
        try {
            val userId = userManager.getUserId()
            if (userId.isBlank()) return@withContext false

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/group_members?group_id=eq.$groupId&user_id=eq.$userId")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Leave error: ${e.message}")
        }
        return@withContext false
    }

    suspend fun deleteGroupOnCloud(groupId: String): Boolean = withContext(Dispatchers.IO) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext false
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
            Log.e(TAG, "Delete error: ${e.message}")
            return@withContext false
        }
    }

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
            Log.e(TAG, "Fetch by invite error: ${e.message}")
        }
        return@withContext null
    }

    fun syncSession(context: Context, subject: String, durationMinutes: Long, sessionId: Long = System.currentTimeMillis()) {
        val userManager = UserManager(context)
        if (userManager.isGuestUser()) return
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank() || durationMinutes <= 0) return

        val now = System.currentTimeMillis()
        if (now - lastSyncTimestamp < 3000 && lastSyncedMinutes == durationMinutes) return
        lastSyncTimestamp = now
        lastSyncedMinutes = durationMinutes

        val userId = userManager.getUserId()
        if (userId.isBlank()) return

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
                        Log.d(TAG, "Session synced for user: $userId")
                        updateUserWeeklyMinutes(userId)
                    }
                }
            }
        })
    }

    fun updateUserWeeklyMinutes(userId: String) {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank() || userId.isBlank() || userId.startsWith("guest_")) return

        val (startDate, endDate) = getWeekDateRange(isCurrentWeek = true)
        val queryUrl = "$SUPABASE_URL/rest/v1/study_sessions?user_id=eq.$userId&session_date=gte.$startDate&session_date=lte.$endDate&select=duration_minutes"

        val request = Request.Builder()
            .url(queryUrl)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
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
                                override fun onFailure(pCall: Call, pE: IOException) {}
                                override fun onResponse(pCall: Call, pResponse: Response) {
                                    pResponse.close()
                                }
                            })
                        } catch (_: Exception) {}
                    }
                }
            }
        })
    }

    suspend fun fetchSessions(context: Context): List<FocusSession> = withContext(Dispatchers.IO) {
        val userManager = UserManager(context)
        if (userManager.isGuestUser() || SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) return@withContext emptyList()
        val userId = userManager.getUserId()
        if (userId.isBlank()) return@withContext emptyList()

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
                            durationMinutes = item.optInt("duration_minutes", 0)
                        )
                    )
                }
                return@withContext resultList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch sessions error: ${e.message}")
        }
        return@withContext emptyList()
    }

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

    suspend fun fetchGroupLeaderboard(context: Context, groupId: String, isCurrentWeek: Boolean): List<GroupMember> = withContext(Dispatchers.IO) {
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
                if (uId.isNotBlank()) membersMap[uId] = nick
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
            Log.e(TAG, "Group leaderboard error: ${e.message}")
        }
        return@withContext cached
    }
}
