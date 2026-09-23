package com.amon.timer

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// 📦 अपडेट की सारी जानकारी का डिब्बा
data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val whatsNew: String,
    val downloadUrl: String
)

class UpdateManager(private val context: Context) {

    // 📱 हमारा मौजूदा वर्ज़न
    val currentVersion = "v1.0.0"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("amon_update_prefs", Context.MODE_PRIVATE)

    // 🔒 चेक करना कि क्या इस वर्ज़न का 1-टाइम पॉप-अप पहले दिखाया जा चुका है?
    fun shouldShowOneTimePopup(version: String): Boolean {
        val lastDismissedVersion = prefs.getString("dismissed_version", "")
        return lastDismissedVersion != version
    }

    // 📌 जब यूज़र पॉप-अप हटा दे, तो उसे याद रखना (ताकि दोबारा न दिखे)
    fun markPopupAsDismissed(version: String) {
        prefs.edit().putString("dismissed_version", version).apply()
    }

    // 🌐 इंटरनेट से नया अपडेट चेक करना (बैकग्राउंड में)
    suspend fun checkLatestUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        // डिफ़ॉल्ट जानकारी (अगर इंटरनेट न हो या कुछ नया न हो)
        var updateInfo = AppUpdateInfo(
            hasUpdate = false,
            currentVersion = currentVersion,
            latestVersion = currentVersion,
            whatsNew = "You are using the latest version of Amon.",
            downloadUrl = "https://github.com"
        )

        try {
            // 🔗 GitHub Releases API का लिंक
            val apiUrl = "https://api.github.com/repos/SUN-BHODH/SUN-BHODH/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", currentVersion)
                val body = json.optString("body", "Bug fixes and performance improvements.")
                val htmlUrl = json.optString("html_url", "https://github.com")

                // APK का डायरेक्ट डाउनलोड लिंक ढूँढना (अगर release में apk अपलोड है)
                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                // वर्ज़न की तुलना: अगर टैग अलग और नया है
                val isNewer = isVersionNewer(tagName, currentVersion)

                updateInfo = AppUpdateInfo(
                    hasUpdate = isNewer,
                    currentVersion = currentVersion,
                    latestVersion = tagName,
                    whatsNew = body.ifBlank { "✨ New features and performance optimizations." },
                    downloadUrl = apkDownloadUrl
                )
            }
        } catch (_: Exception) {
            // इंटरनेट एरर होने पर ऐप क्रैश नहीं होगा, शांत रहेगा
        }

        updateInfo
    }

    // वर्ज़न तुलना करने का सरल नियम (v1.0.1 > v1.0.0)
    private fun isVersionNewer(latest: String, current: String): Boolean {
        val cleanLatest = latest.replace("v", "").replace("V", "").trim()
        val cleanCurrent = current.replace("v", "").replace("V", "").trim()
        return cleanLatest.isNotEmpty() && cleanLatest != cleanCurrent
    }
}
