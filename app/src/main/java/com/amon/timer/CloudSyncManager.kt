package com.amon.timer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudSyncManager {

    private const val TAG = "CloudSyncManager"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * ✨ 1. लॉगिन करते ही तुरंत Users_List में नाम और पासवर्ड दर्ज करना
     */
    fun registerUserOnCloud(context: Context) {
        val sheetUrl = BuildConfig.GOOGLE_SHEET_URL
        if (sheetUrl.isBlank()) {
            Log.w(TAG, "Google Sheet URL set nahi hai!")
            return
        }

        val userManager = UserManager(context)
        val userName = userManager.getUserName().trim()
        val password = userManager.getPassword().trim()

        if (userName.isBlank() || userName.equals("Guest", ignoreCase = true)) {
            return // Guest user ko Users_List me register nahi karna
        }

        val jsonPayload = JSONObject().apply {
            put("action", "register")
            put("userName", userName)
            put("password", password)
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(sheetUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "User registration on cloud failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "User '$userName' safaltapoorvak Users_List me register ho gaya!")
                    } else {
                        Log.e(TAG, "Registration error code: ${response.code}")
                    }
                }
            }
        })
    }

    /**
     * 🌿 2. पढ़ाई पूरी होने पर डेटा Google Sheet में भेजना (POST)
     * अब यह सेशन के साथ-साथ पासवर्ड भी भेजेगा ताकि Users_List हमेशा अपडेट रहे!
     */
    fun syncSession(
        context: Context,
        subject: String,
        durationMinutes: Long,
        earnedTrees: Int
    ) {
        val sheetUrl = BuildConfig.GOOGLE_SHEET_URL

        if (sheetUrl.isBlank()) {
            Log.w(TAG, "Google Sheet URL set nahi hai!")
            return
        }

        // UserManager se live naam aur password lena
        val userManager = UserManager(context)
        val userName = userManager.getUserName().ifEmpty { "Vision" }
        val password = userManager.getPassword()
        val uniqueId = userManager.getSecretCode()

        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val jsonPayload = JSONObject().apply {
            put("action", "save_sessions")
            put("userName", userName)
            put("password", password) // 🔐 पासवर्ड अब साथ जाएगा!
            put("userId", uniqueId)
            put("tabName", "${userName}_$uniqueId")
            put("date", currentDateTime)
            put("subject", subject)
            put("durationMinutes", durationMinutes)
            put("earnedTrees", earnedTrees)
            put("status", "Completed")
        }

        val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(sheetUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Sheet sync fail ho gaya: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Session Google Sheet me safaltapoorvak save ho gaya!")
                    } else {
                        Log.e(TAG, "Server error code: ${response.code}")
                    }
                }
            }
        })
    }

    /**
     * 📥 3. Google Sheet से डेटा वापस मंगाना (GET)
     */
    suspend fun fetchSessions(context: Context): List<FocusSession> = withContext(Dispatchers.IO) {
        val sheetUrl = BuildConfig.GOOGLE_SHEET_URL
        if (sheetUrl.isBlank()) return@withContext emptyList()

        val userManager = UserManager(context)
        val userName = userManager.getUserName().ifEmpty { "Vision" }
        val uniqueId = userManager.getSecretCode()

        val fetchUrl = "$sheetUrl?userName=$userName&userId=$uniqueId"
        val request = Request.Builder()
            .url(fetchUrl)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (responseText.isNotBlank()) {
                val json = JSONObject(responseText)
                if (json.optString("status") == "success") {
                    val sessionsArray = json.optJSONArray("sessions") ?: return@withContext emptyList()
                    val resultList = mutableListOf<FocusSession>()

                    for (i in 0 until sessionsArray.length()) {
                        val item = sessionsArray.getJSONObject(i)
                        resultList.add(
                            FocusSession(
                                id = System.currentTimeMillis() + i,
                                date = item.optString("date"),
                                subject = item.optString("subject", "General"),
                                durationMinutes = item.optInt("durationMinutes", 0),
                                earnedTrees = item.optInt("earnedTrees", 0)
                            )
                        )
                    }
                    return@withContext resultList
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sheet se data lane me error: ${e.message}")
        }

        return@withContext emptyList()
    }
}
