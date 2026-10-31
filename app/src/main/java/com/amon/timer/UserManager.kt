package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class UserManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("amon_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_unique_uuid"
        private const val KEY_NAME = "display_name"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_BIRTHDAY = "user_birthday"
        private const val KEY_IS_GUEST = "is_guest_user"
        private const val KEY_FRESH_AUTH_V1 = "fresh_auth_completed_v1"
    }

    // ----------------- 0. UNIQUE USER ID (UUID) -----------------

    // 🆔 हर यूज़र की स्थायी यूनिक ID (Supabase के लिए)
    fun getUserId(): String {
        var id = prefs.getString(KEY_USER_ID, "") ?: ""
        if (id.isBlank()) {
            // अगर पहले से ID नहीं है, तो वैध UUID बनाकर हमेशा के लिए सेव कर लो
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, id).apply()
        }
        return id
    }

    fun setUserId(id: String) {
        if (id.isNotBlank()) {
            prefs.edit().putString(KEY_USER_ID, id.trim()).apply()
        }
    }

    // ----------------- 1. USER DETAILS (नाम, पासवर्ड, बर्थडे) -----------------

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun setPassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password.trim()).apply()
    }

    fun getPassword(): String {
        return prefs.getString(KEY_PASSWORD, "") ?: ""
    }

    fun setBirthday(birthday: String) {
        prefs.edit().putString(KEY_BIRTHDAY, birthday.trim()).apply()
    }

    fun getBirthday(): String {
        return prefs.getString(KEY_BIRTHDAY, "") ?: ""
    }

    // ----------------- 2. GUEST MODE -----------------

    fun setGuestUser(isGuest: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply()
    }

    fun isGuestUser(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }

    // ----------------- 3. REFRESH ONETIME AUTH TRACKER -----------------

    fun isFreshAuthDone(): Boolean {
        return prefs.getBoolean(KEY_FRESH_AUTH_V1, false)
    }

    fun setFreshAuthDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_FRESH_AUTH_V1, done).apply()
    }

    // ----------------- 4. APP ENTRY STATUS -----------------

    fun isAccountSetupDone(): Boolean {
        val hasAccount = getUserName().isNotEmpty()
        return isFreshAuthDone() && (hasAccount || isGuestUser())
    }

    fun isUserRegistered(): Boolean {
        return isAccountSetupDone()
    }

    fun getSecretCode(): String {
        val pass = getPassword().replace(Regex("[^a-zA-Z0-9]"), "")
        return pass.ifEmpty { "123456" }
    }

    // ----------------- 5. GOOGLE SHEET TAB NAME -----------------

    fun getSheetTabId(): String {
        if (isGuestUser()) {
            return "Guest_User"
        }

        val cleanName = getUserName().replace(Regex("[^a-zA-Z0-9_]"), "").ifEmpty { "Vision" }
        val cleanPass = getPassword().replace(Regex("[^a-zA-Z0-9_]"), "")

        return if (cleanPass.isNotEmpty()) {
            "User_${cleanName}_${cleanPass}"
        } else {
            "User_${cleanName}"
        }
    }
}
