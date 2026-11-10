package com.amon.timer.groups

import android.content.Context
import android.content.SharedPreferences

object GroupCacheManager {
    private const val PREF_NAME = "amon_group_cache"
    private const val KEY_PENDING_ACTIONS = "key_pending_group_actions"
    private const val KEY_CACHED_GROUPS = "cache_home_groups"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 🏆 लीडरबोर्ड सेव करना
    fun saveLeaderboard(context: Context, groupId: String, members: List<GroupMember>) {
        val serialized = members.joinToString(";;") {
            "${it.id}::${it.nickname}::${it.studyTimeMinutes}::${it.isCurrentlyFocusing}::${it.lastActive}"
        }
        getPrefs(context).edit().putString("cache_group_$groupId", serialized).apply()
    }

    // 🏆 लीडरबोर्ड कैशे से लोड करना
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
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ⚡ 1. ग्रुप्स की लिस्ट को तुरंत लोकल मेमोरी में सेव करना
    fun saveCachedGroupsList(context: Context, groups: List<StudyGroup>) {
        if (groups.isEmpty()) return
        val serialized = groups.joinToString("###") { g ->
            "${g.id}::${g.name}::${g.category}::${g.isPublic}::${g.memberCount}::${g.focusingCount}::${g.isPinned}::${g.isJoined}::${g.isDefault}::${g.inviteCode}"
        }
        getPrefs(context).edit().putString(KEY_CACHED_GROUPS, serialized).apply()
    }

    // ⚡ 2. 0 सेकंड में ऑफ़लाइन/लोकल ग्रुप्स लोड करना
    fun getCachedGroupsList(context: Context): List<StudyGroup> {
        val serialized = getPrefs(context).getString(KEY_CACHED_GROUPS, null) ?: return emptyList()
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split("###").mapNotNull { row ->
                val parts = row.split("::")
                if (parts.size >= 10) {
                    StudyGroup(
                        id = parts[0],
                        name = parts[1],
                        category = parts[2],
                        isPublic = parts[3].toBoolean(),
                        memberCount = parts[4].toIntOrNull() ?: 1,
                        focusingCount = parts[5].toIntOrNull() ?: 0,
                        isPinned = parts[6].toBoolean(),
                        isJoined = parts[7].toBoolean(),
                        isDefault = parts[8].toBoolean(),
                        inviteCode = parts.getOrNull(9)?.ifBlank { "" } ?: ""
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // 📝 3. ऑफ़लाइन डायरी: अगर नेट बंद हो तो पेंडिंग काम नोट करना (e.g. "JOIN:grp_123" या "LEAVE:grp_123")
    fun addPendingAction(context: Context, action: String, groupId: String) {
        val currentSet = getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        // अगर पहले से उल्टा ऐक्शन था तो उसे साफ़ करें
        val oppositeAction = if (action == "JOIN") "LEAVE:$groupId" else "JOIN:$groupId"
        currentSet.remove(oppositeAction)
        currentSet.add("$action:$groupId")
        getPrefs(context).edit().putStringSet(KEY_PENDING_ACTIONS, currentSet).apply()
    }

    fun getPendingActions(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet()) ?: emptySet()
    }

    fun removePendingAction(context: Context, actionEntry: String) {
        val currentSet = getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet())?.toMutableSet() ?: return
        currentSet.remove(actionEntry)
        getPrefs(context).edit().putStringSet(KEY_PENDING_ACTIONS, currentSet).apply()
    }

    fun clearGroupCache(context: Context, groupId: String) {
        try {
            getPrefs(context).edit().remove("cache_group_$groupId").apply()
        } catch (_: Exception) {}
    }

    fun clearAllGroupCache(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
        } catch (_: Exception) {}
    }
}
