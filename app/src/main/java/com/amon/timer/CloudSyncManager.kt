package com.amon.timer

import android.content.Context
import com.amon.timer.groups.GroupCacheManager
import com.amon.timer.groups.GroupMember
import com.amon.timer.groups.StudyGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class LeaderboardItem(
    val id: String,
    val name: String,
    val totalMinutes: Long
)

object CloudSyncManager {

    private const val TAG = "CloudSyncManager"

    // 🌐 Offline Mode: All network calls are disabled. Data stays 100% local.

    suspend fun signInWithGoogleIdToken(context: Context, idToken: String): String? = withContext(Dispatchers.IO) {
        null
    }

    fun markGroupAsAdmin(context: Context, groupId: String) {}

    fun isGroupAdmin(context: Context, groupId: String): Boolean = false

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

    fun registerUserOnCloud(context: Context, userId: String? = null, nickname: String? = null) {}

    // 🎯 Offline Focus Status (No-op to prevent compilation errors)
    suspend fun updateUserFocusStatus(
        context: Context,
        userId: String,
        isFocusing: Boolean,
        currentSubject: String?
    ) = withContext(Dispatchers.IO) {}

    suspend fun fetchGroups(context: Context): List<StudyGroup> = withContext(Dispatchers.IO) {
        GroupCacheManager.getCachedGroupsList(context)
    }

    suspend fun createGroupOnCloud(context: Context, group: StudyGroup): Boolean = withContext(Dispatchers.IO) {
        false
    }

    suspend fun joinGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        false
    }

    suspend fun leaveGroupOnCloud(context: Context, groupId: String): Boolean = withContext(Dispatchers.IO) {
        false
    }

    suspend fun deleteGroupOnCloud(groupId: String): Boolean = withContext(Dispatchers.IO) {
        false
    }

    suspend fun fetchGroupByInviteCode(inviteCode: String): StudyGroup? = withContext(Dispatchers.IO) {
        null
    }

    // 📂 Local Session Sync Stub
    fun syncSession(context: Context, subject: String, durationMinutes: Long, sessionId: Long = System.currentTimeMillis()) {}

    fun updateUserWeeklyMinutes(userId: String) {}

    suspend fun fetchSessions(context: Context): List<FocusSession> = withContext(Dispatchers.IO) {
        emptyList()
    }

    suspend fun fetchLeaderboard(): List<LeaderboardItem> = withContext(Dispatchers.IO) {
        emptyList()
    }

    suspend fun fetchGroupLeaderboard(context: Context, groupId: String, isCurrentWeek: Boolean): List<GroupMember> = withContext(Dispatchers.IO) {
        GroupCacheManager.getCachedLeaderboard(context, groupId)
    }
}
