package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

object ThemeManager {
    // यह थीम की सेटिंग सेव करने वाली छोटी तिजोरी का नाम है
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    // यह ऐप को लाइव बताएगा कि अभी कौन सा रंग चल रहा है (शुरुआत में Yellow)
    val currentTheme = mutableStateOf("Yellow")

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 1. जब ऐप खुलेगी, तो यह पुरानी सेव की हुई थीम निकालेगा
    fun loadTheme(context: Context) {
        val prefs = getPrefs(context)
        val savedTheme = prefs.getString(KEY_THEME, "Yellow") ?: "Yellow"
        currentTheme.value = savedTheme
    }

    // 2. जब यूज़र Profile से नया रंग (जैसे "Gold") चुनेगा, तो यह उसे हमेशा के लिए सेव कर लेगा
    fun saveTheme(context: Context, themeName: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_THEME, themeName).apply()
        currentTheme.value = themeName // यह लाइन बदलते ही ऐप का रंग तुरंत बदल जाएगा
    }
}
