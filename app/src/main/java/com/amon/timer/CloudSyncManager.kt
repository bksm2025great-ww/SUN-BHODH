package com.amon.timer

import android.content.Context
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

    // 🌐 Offline-First Mode: All cloud & group dependencies removed. Data stays 100% local.

    suspend fun signInWithGoogleIdToken(context: Context, idToken: String): String? = null

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

    // 🎯 Offline Focus Status (No-op)
    suspend fun updateUserFocusStatus(
        context: Context,
        userId: String,
        isFocusing: Boolean,
        currentSubject: String?
    ) {}

    // 📂 Local Session Sync Stub
    fun syncSession(context: Context, subject: String, durationMinutes: Long, sessionId: Long = System.currentTimeMillis()) {}

    fun updateUserWeeklyMinutes(userId: String) {}

    suspend fun fetchSessions(context: Context): List<FocusSession> {
        return emptyList()
    }

    suspend fun fetchLeaderboard(): List<LeaderboardItem> {
        return emptyList()
    }
}
