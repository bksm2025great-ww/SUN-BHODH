package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_MODE = "selected_mode"

    // 🟡 1. Accent Color State (Default Luxe Gold)
    val currentTheme = mutableStateOf("Luxe Gold")

    // 🌓 2. App Mode State (Dark ya Light)
    val isDarkTheme = mutableStateOf(true)
    val appMode = mutableStateOf("Dark") // "Dark", "Light", "Default"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 📱 फ़ोन के सिस्टम का असली लाइट/डार्क मोड चेक करने का तरीका
    private fun isSystemDark(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    // 🎯 मोड के आधार पर तय करना कि ऐप डार्क रहेगी या लाइट
    private fun resolveIsDark(context: Context, modeName: String): Boolean {
        return when (modeName) {
            "Light" -> false
            "Dark" -> true
            else -> isSystemDark(context) // "Default" ya "System" hone par phone ki setting chalegi
        }
    }

    // 🟢 1. App खुलते ही पुरानी सेटिंग लोड करना
    fun loadTheme(context: Context) {
        val prefs = getPrefs(context)
        val savedTheme = prefs.getString(KEY_THEME, "Luxe Gold") ?: "Luxe Gold"
        val savedMode = prefs.getString(KEY_MODE, "Dark") ?: "Dark"

        currentTheme.value = savedTheme
        appMode.value = savedMode
        isDarkTheme.value = resolveIsDark(context, savedMode)
    }

    // 🟡 2. Accent Color सेव करना
    fun saveTheme(context: Context, themeName: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_THEME, themeName).apply()
        currentTheme.value = themeName
    }

    // 🌓 3. App Mode (Light / Dark / Default) सेव करना
    fun saveMode(context: Context, modeName: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_MODE, modeName).apply()
        appMode.value = modeName
        isDarkTheme.value = resolveIsDark(context, modeName)
    }

    // 🎨 4. Current Accent Color निकालने का हेल्पर (7 Eye-Friendly Shades)
    fun getAccentColor(): Color {
        return when (currentTheme.value) {
            "Classic Yellow" -> Color(0xFFF5A524)
            "Soft Lavender"  -> Color(0xFFA78BFA)
            "Slate Violet"   -> Color(0xFF9381FF)
            "Warm Amber"     -> Color(0xFFFB923C)
            "Dusty Rose"     -> Color(0xFFF472B6)
            "Blossom Rose"   -> Color(0xFFE879A9)
            else             -> Color(0xFFF3C669) // Luxe Gold (Default)
        }
    }

    // 🟢 पूरी स्क्रीन का बैकग्राउंड रंग
    fun getBackgroundColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFF121214) // Deep Dark
        } else {
            Color(0xFFF6F6F8) // Clean Bright Light
        }
    }

    // 🟢 कार्ड्स और डिब्बों का बैकग्राउंड रंग
    fun getCardColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFF1E1E22) // Dark Card
        } else {
            Color(0xFFFFFFFF) // Pure White Card
        }
    }

    // 🟢 मुख्य लिखावट (Text) का रंग
    fun getTextColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFFFFFFFF) // Safed text
        } else {
            Color(0xFF19191C) // Dark text
        }
    }

    // 🟢 हल्की लिखावट (Sub-text / Muted) का रंग
    fun getTextMutedColor(): Color {
        return if (isDarkTheme.value) {
            Color(0xFFA0A0A5) // Light gray
        } else {
            Color(0xFF6E6E75) // Dim gray
        }
    }
}
