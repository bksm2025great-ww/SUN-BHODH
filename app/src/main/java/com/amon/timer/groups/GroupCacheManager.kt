package com.amon.timer.groups

import android.content.Context
import android.content.SharedPreferences

object GroupCacheManager {
    private const val PREF_NAME = "amon_group_cache"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Leaderboard cache save karna
    fun saveLeaderboard(context: Context, groupId: String, jsonString: String) {
        getPrefs(context).edit().putString("cache_group_$groupId", jsonString).apply()
    }

    // Leaderboard cache read karna
    fun getCachedLeaderboard(context: Context, groupId: String): String? {
        return getPrefs(context).getString("cache_group_$groupId", null)
    }
}
