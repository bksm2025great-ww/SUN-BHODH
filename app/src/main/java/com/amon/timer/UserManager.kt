package com.example.amon // (अपनी ऐप का सही पैकेज नाम यहाँ रहने दें)

import android.content.Context
import android.content.SharedPreferences
import kotlin.random.Random

class UserManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("amon_user_prefs", Context.MODE_PRIVATE)

    // यूज़र का नाम सेव करने के लिए
    fun setUserName(name: String) {
        val cleanName = name.trim().ifEmpty { "Vision" }
        prefs.edit().putString("display_name", cleanName).apply()
        
        // अगर 4-अंकों का गुप्त कोड पहले से नहीं बना है, तो नया बना लें
        if (getSecretCode().isEmpty()) {
            val randomCode = Random.nextInt(1000, 9999).toString()
            prefs.edit().putString("secret_code", randomCode).apply()
        }
    }

    // स्क्रीन पर दिखाने के लिए यूज़र का नाम
    fun getUserName(): String {
        return prefs.getString("display_name", "") ?: ""
    }

    // बैकग्राउंड का गुप्त 4-अंकों का कोड
    fun getSecretCode(): String {
        return prefs.getString("secret_code", "") ?: ""
    }

    // Google Sheet के टैब का पूरा नाम (जैसे Vision_4821)
    fun getSheetTabId(): String {
        val name = getUserName().ifEmpty { "Vision" }
        val code = getSecretCode()
        return if (code.isNotEmpty()) "${name}_$code" else name
    }

    // क्या यूज़र ने पहली बार नाम सेट कर दिया है?
    fun isUserRegistered(): Boolean {
        return getUserName().isNotEmpty() && getSecretCode().isNotEmpty()
    }
}
