package com.amon.timer

import android.content.Context
import android.provider.Settings
import android.util.Log
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
     * Study session complete hone par data ko Google Sheet me bhejta hai.
     */
    fun syncSession(
        context: Context,
        subject: String,
        durationMinutes: Long,
        earnedTrees: Int,
        userName: String = "Vision"
    ) {
        val sheetUrl = BuildConfig.GOOGLE_SHEET_URL

        // Agar secret URL nahi mila to aage nahi badhenge
        if (sheetUrl.isBlank()) {
            Log.w(TAG, "Google Sheet URL set nahi hai!")
            return
        }

        // Phone ka 4-digit unique code nikalna (taaki alag-alag users ka data mix na ho)
        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "1234"
        } catch (e: Exception) {
            "1234"
        }
        val uniqueId = if (deviceId.length >= 4) deviceId.takeLast(4) else deviceId

        // Aaj ki taareekh aur samay
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        // Data ka digital lifafa (JSON packet)
        val jsonPayload = JSONObject().apply {
            put("userName", userName)
            put("userId", uniqueId)
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

        // Background me silent request bhejna taaki app ruke ya hang na ho
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
}
