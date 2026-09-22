package com.amon.timer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import kotlin.math.abs

class UserManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("amon_user_prefs", Context.MODE_PRIVATE)

    // यूज़र का नाम सेव करने के लिए
    fun setUserName(name: String) {
        val cleanName = name.trim().ifEmpty { "Vision" }
        prefs.edit().putString("display_name", cleanName).apply()
    }

    // स्क्रीन पर दिखाने के लिए यूज़र का नाम
    fun getUserName(): String {
        return prefs.getString("display_name", "") ?: ""
    }

    // फ़ोन की स्थायी हार्डवेयर पहचान से 4-अंकों का फिक्स कोड
    @SuppressLint("HardwareIds")
    fun getSecretCode(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "1234"
        
        // Android ID से 1000 से 9999 के बीच का फिक्स 4-अंकों का नंबर
        val fixedNumber = abs(androidId.hashCode() % 9000) + 1000
        return fixedNumber.toString()
    }

    // Google Sheet के टैब का पूरा नाम (जैसे Vision_7392)
    fun getSheetTabId(): String {
        val name = getUserName().ifEmpty { "Vision" }
        val code = getSecretCode()
        return "${name}_$code"
    }

    // क्या यूज़र ने नाम दर्ज कर दिया है?
    fun isUserRegistered(): Boolean {
        return getUserName().isNotEmpty()
    }
}
