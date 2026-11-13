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

    // 📝 3. ऑफ़लाइन डायरी: हर groupId के लिए सिर्फ़ सबसे नया ऐक्शन रखना (नया JOIN पुराने LEAVE को हटाएगा और उल्टा भी)
    fun addPendingAction(context: Context, action: String, groupId: String) {
        val cleanGroupId = groupId.trim()
        if (cleanGroupId.isBlank()) return
        val currentSet = getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        // इस groupId से जुड़ी कोई भी पुरानी JOIN या LEAVE एंट्री हटा दें
        currentSet.removeAll { it.endsWith(":$cleanGroupId") }
        currentSet.add("$action:$cleanGroupId")
        getPrefs(context).edit().putStringSet(KEY_PENDING_ACTIONS, currentSet).apply()
    }

    fun getPendingActions(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet())?.toSet() ?: emptySet()
    }

    fun removePendingAction(context: Context, actionEntry: String) {
        val currentSet = getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet())?.toMutableSet() ?: return
        currentSet.remove(actionEntry)
        getPrefs(context).edit().putStringSet(KEY_PENDING_ACTIONS, currentSet).apply()
    }

    // ✅ क्लाउड से कन्फ़र्म होते ही उस ग्रुप की पेंडिंग एंट्री तुरंत डिलीट करना
    fun clearPendingActionForGroup(context: Context, groupId: String) {
        val cleanGroupId = groupId.trim()
        if (cleanGroupId.isBlank()) return
        val currentSet = getPrefs(context).getStringSet(KEY_PENDING_ACTIONS, emptySet())?.toMutableSet() ?: return
        val changed = currentSet.removeAll { it.endsWith(":$cleanGroupId") }
        if (changed) {
            getPrefs(context).edit().putStringSet(KEY_PENDING_ACTIONS, currentSet).apply()
        }
    }

    // 🎯 4. जॉइन रिकॉर्ड करना
    fun recordLocalJoin(context: Context, groupId: String) {
        addPendingAction(context, "JOIN", groupId)
    }

    // 🎯 5. लीव/डिलीट रिकॉर्ड करना
    fun recordLocalLeave(context: Context, groupId: String) {
        addPendingAction(context, "LEAVE", groupId)
    }

    // 🧠 6. स्मार्ट सिंक: क्लाउड डेटा आने पर लोकल फ़ैसलों को प्राथमिकता देना
    fun mergeWithCloud(context: Context, cloudGroups: List<StudyGroup>): List<StudyGroup> {
        val pending = getPendingActions(context)
        val joinedIds = pending.filter { it.startsWith("JOIN:") }.map { it.removePrefix("JOIN:") }.toSet()
        val leftIds = pending.filter { it.startsWith("LEAVE:") }.map { it.removePrefix("LEAVE:") }.toSet()

        return cloudGroups.map { group ->
            when {
                // अगर आपने लोकली लीव या डिलीट किया है, तो क्लाउड पुराना डेटा भेजे तब भी अन-जॉइन्ड रहेगा
                group.id in leftIds -> group.copy(isJoined = false, isPinned = false)
                // अगर आपने ऑफ़लाइन जॉइन किया है, तो क्लाउड आपको बाहर नहीं निकाल सकता
                group.id in joinedIds -> group.copy(isJoined = true)
                else -> group
            }
        }
    }

    fun clearGroupCache(context: Context, groupId: String) {
        try {
            recordLocalLeave(context, groupId)
            getPrefs(context).edit().remove("cache_group_$groupId").apply()
        } catch (_: Exception) {}
    }

    fun clearAllGroupCache(context: Context) {
        try {
            getPrefs(context).edit().clear().apply()
        } catch (_: Exception) {}
    }
}
