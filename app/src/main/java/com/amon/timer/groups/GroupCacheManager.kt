package com.amon.timer.groups

import android.content.Context
import android.content.SharedPreferences

object GroupCacheManager {
    private const val PREF_NAME = "amon_group_cache"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLeaderboard(context: Context, groupId: String, members: List<GroupMember>) {
        val serialized = members.joinToString(";;") {
            "${it.id}::${it.nickname}::${it.studyTimeMinutes}::${it.isCurrentlyFocusing}::${it.lastActive}"
        }
        getPrefs(context).edit().putString("cache_group_$groupId", serialized).apply()
    }

    fun getCachedLeaderboard(context: Context, groupId: String): List<GroupMember> {
        val serialized = getPrefs(context).getString("cache_group_$groupId", null) ?: return emptyList()
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split(";;").mapNotNull { row ->
                val parts = row.split("::")
                if (parts.size >= 5) {
                    GroupMember(
                        id = parts[0],
                        nickname = parts[1],
                        studyTimeMinutes = parts[2].toIntOrNull() ?: 0,
                        isCurrentlyFocusing = parts[3].toBoolean(),
                        lastActive = parts[4]
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 🧹 1. किसी एक ग्रुप का कैशे हमेशा के लिए डिलीट करना
    fun clearGroupCache(context: Context, groupId: String) {
        try {
            getPrefs(context).edit().remove("cache_group_$groupId").apply()
        } catch (_: Exception) {}
    }

    // 🧼 2. पूरा पुराना कैशे साफ़ करना (अटके हुए डेटा को रीसेट करने के लिए)
    fun clearAllGroupCache(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
        } catch (_: Exception) {}
    }
}
