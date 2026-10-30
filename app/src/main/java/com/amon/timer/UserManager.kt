package com.amon.timer

import android.content.Context
import android.content.SharedPreferences

class UserManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("amon_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NAME = "display_name"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_BIRTHDAY = "user_birthday"
        private const val KEY_IS_GUEST = "is_guest_user"
        // 🔒 नए रिफ्रेश लॉगिन का स्थायी लॉक
        private const val KEY_FRESH_AUTH_V1 = "fresh_auth_completed_v1"
    }

    // ----------------- 1. USER DETAILS (नाम, पासवर्ड, बर्थडे) -----------------

    // 👤 यूज़र का नाम सेव करना
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_NAME, name.trim()).apply()
    }

    // 👤 यूज़र का नाम पढ़ना
    fun getUserName(): String {
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    // 🔑 पासवर्ड सेव करना
    fun setPassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password.trim()).apply()
    }

    // 🔑 पासवर्ड पढ़ना
    fun getPassword(): String {
        return prefs.getString(KEY_PASSWORD, "") ?: ""
    }

    // 🎂 बर्थडे सेव करना (जैसे 23/09/2000)
    fun setBirthday(birthday: String) {
        prefs.edit().putString(KEY_BIRTHDAY, birthday.trim()).apply()
    }

    // 🎂 बर्थडे पढ़ना
    fun getBirthday(): String {
        return prefs.getString(KEY_BIRTHDAY, "") ?: ""
    }

    // ----------------- 2. GUEST MODE (बिना अकाउंट वाला सिस्टम) -----------------

    // गेस्ट स्टेटस सेट करना
    fun setGuestUser(isGuest: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply()
    }

    // क्या यूज़र गेस्ट मोड में है?
    fun isGuestUser(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }

    // ----------------- 3. REFRESH ONETIME AUTH TRACKER -----------------

    // क्या इस नए अपडेट के बाद एक बार लॉगिन हो चुका है?
    fun isFreshAuthDone(): Boolean {
        return prefs.getBoolean(KEY_FRESH_AUTH_V1, false)
    }

    // लॉगिन होते ही इसे हमेशा के लिए true सेट करना
    fun setFreshAuthDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_FRESH_AUTH_V1, done).apply()
    }

    // ----------------- 4. APP ENTRY STATUS -----------------

    // क्या खाता सेटअप हो चुका है या गेस्ट मोड चुना गया है?
    // (अब यह चेक करेगा कि नया लॉगिन पूरा हुआ है या नहीं)
    fun isAccountSetupDone(): Boolean {
        val hasAccount = getUserName().isNotEmpty()
        return isFreshAuthDone() && (hasAccount || isGuestUser())
    }

    // 🟢 पुरानी फ़ाइलों के साथ तालमेल बनाए रखने के लिए
    fun isUserRegistered(): Boolean {
        return isAccountSetupDone()
    }

    // 🟢 पुरानी फ़ाइलों में एरर न आए, इसलिए यह सेफ़ ब्रिज रखा है
    fun getSecretCode(): String {
        val pass = getPassword().replace(Regex("[^a-zA-Z0-9]"), "")
        return pass.ifEmpty { "123456" }
    }

    // ----------------- 5. GOOGLE SHEET TAB NAME -----------------

    // 🔒 Google Sheet के टैब का स्थायी नाम (जैसे: User_Vision_Pass123)
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
