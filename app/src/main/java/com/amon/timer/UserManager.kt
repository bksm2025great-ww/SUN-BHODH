package com.amon.timer

import android.content.Context
import android.content.SharedPreferences

class UserManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("amon_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_unique_uuid"
        private const val KEY_NAME = "display_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_BIRTHDAY = "user_birthday"
        private const val KEY_IS_GUEST = "is_guest_user"
        private const val KEY_FRESH_AUTH_V1 = "fresh_auth_completed_v1"
    }

    fun getUserId(): String {
        if (isGuestUser()) {
            return "guest_local_user"
        }
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    fun setUserId(id: String) {
        if (id.isNotBlank()) {
            prefs.edit().putString(KEY_USER_ID, id.trim()).apply()
        }
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    fun setNickName(nickname: String) {
        setUserName(nickname)
    }

    fun getNickName(): String {
        return getUserName().ifBlank { if (isGuestUser()) "Guest Aspirant" else "Student" }
    }

    fun setUserEmail(email: String) {
        val cleanEmail = email.trim().lowercase()
        prefs.edit().putString(KEY_EMAIL, cleanEmail).apply()
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
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

    fun setGuestUser(isGuest: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply()
        if (isGuest) {
            if (getUserName().isBlank()) setUserName("Guest Aspirant")
            setUserId("guest_local_user")
        }
    }

    fun isGuestUser(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }

    fun isFreshAuthDone(): Boolean {
        return prefs.getBoolean(KEY_FRESH_AUTH_V1, false)
    }

    fun setFreshAuthDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_FRESH_AUTH_V1, done).apply()
    }

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

    fun getSheetTabId(): String {
        if (isGuestUser()) {
            return "Guest_User"
        }
        val cleanName = getUserName().replace(Regex("[^a-zA-Z0-9_]"), "").ifEmpty { "Vision" }
        return "User_$cleanName"
    }
}
