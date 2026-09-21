package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object ThemeManager {
    // थीम की सेटिंग्स सेव करने वाली तिजोरी
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_MODE = "selected_mode"

    // 🟡 1. Accent Color State (Classic Yellow ya Luxe Gold)
    val currentTheme = mutableStateOf("Luxe Gold")

    // 🌓 2. App Mode State (Dark ya Light)
    val isDarkTheme = mutableStateOf(true)
    val appMode = mutableStateOf("Dark") // "Dark", "Light", "Default"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 🟢 1. App khulte hi purani saved setting load karna
    fun loadTheme(context: Context) {
        val prefs = getPrefs(context)
        val savedTheme = prefs.getString(KEY_THEME, "Luxe Gold") ?: "Luxe Gold"
        val savedMode = prefs.getString(KEY_MODE, "Dark") ?: "Dark"
        
        currentTheme.value = savedTheme
        appMode.value = savedMode
        isDarkTheme.value = (savedMode != "Light")
    }

    // 🟡 2. Accent Color (Yellow / Gold) save karna
    fun saveTheme(context: Context, themeName: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_THEME, themeName).apply()
        currentTheme.value = themeName
    }

    // 🌓 3. App Mode (Light / Dark / Default) save karna
    fun saveMode(context: Context, modeName: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_MODE, modeName).apply()
        appMode.value = modeName
        isDarkTheme.value = (modeName != "Light")
    }

    // 🎨 4. Current Accent Color nikaalne ka helper
    fun getAccentColor(): Color {
        return if (currentTheme.value == "Classic Yellow") {
            Color(0xFFF5A524) // Classic Yellow
        } else {
            Color(0xFFF3C669) // Royal Luxe Gold
        }
    }

    // 🟢 नया जोड़ा गया: पूरी स्क्रीन का बैकग्राउंड रंग
    fun getBackgroundColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFF121214) // Deep Dark
        } else {
            Color(0xFFF6F6F8) // Clean Bright Light
        }
    }

    // 🟢 नया जोड़ा गया: कार्ड्स और डिब्बों का बैकग्राउंड रंग
    fun getCardColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFF1E1E22) // Dark Card
        } else {
            Color(0xFFFFFFFF) // Pure White Card
        }
    }

    // 🟢 नया जोड़ा गया: मुख्य लिखावट (Text) का रंग
    fun getTextColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFFFFFFFF) // सफ़ेद लिखावट
        } else {
            Color(0xFF19191C) // गहरा काला लिखावट
        }
    }

    // 🟢 नया जोड़ा गया: हल्की लिखावट (Sub-text / Muted) का रंग
    fun getTextMutedColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFFA0A0A5) // Light gray
        } else {
            Color(0xFF6E6E75) // Dim gray
        }
    }
}
