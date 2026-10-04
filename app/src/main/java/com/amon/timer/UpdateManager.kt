package com.amon.timer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// 📦 अपडेट की सारी जानकारी का मॉडल
data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val whatsNew: String,
    val downloadUrl: String
)

class UpdateManager(private val context: Context) {

    companion object {
        // 🔗 आपकी असली GitHub Repository का सटीक पता
        private const val GITHUB_REPO = "bksm2025great-ww/SUN-BHODH"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    }

    // 📱 फ़ोन के सिस्टम से सीधे असली इंस्टॉल हुआ वर्ज़न पढ़ना
    val currentVersion: String
        get() {
            return try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                "v${pInfo.versionName}"
            } catch (_: Exception) {
                "v1.0.0"
            }
        }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("amon_update_prefs", Context.MODE_PRIVATE)

    fun shouldShowOneTimePopup(version: String): Boolean {
        val lastDismissedVersion = prefs.getString("dismissed_version", "")
        return lastDismissedVersion != version
    }

    fun markPopupAsDismissed(version: String) {
        prefs.edit().putString("dismissed_version", version).apply()
    }

    // 🌐 ताज़ा अपडेट बैकग्राउंड में ढूँढना
    suspend fun checkLatestUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val activeVersion = currentVersion
        var updateInfo = AppUpdateInfo(
            hasUpdate = false,
            currentVersion = activeVersion,
            latestVersion = activeVersion,
            whatsNew = "You are using the latest version of Amon.",
            downloadUrl = ""
        )

        try {
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Amon-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", activeVersion)
                val body = json.optString("body", "Bug fixes and performance improvements.")

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val isNewer = isVersionNewer(tagName, activeVersion)

                updateInfo = AppUpdateInfo(
                    hasUpdate = isNewer && apkDownloadUrl.isNotBlank(),
                    currentVersion = activeVersion,
                    latestVersion = tagName,
                    whatsNew = body.ifBlank { "✨ New features and performance optimizations." },
                    downloadUrl = apkDownloadUrl
                )
            }
        } catch (_: Exception) {
            // नेटवर्क की समस्या होने पर ऐप शांत रहेगा
        }

        updateInfo
    }

    // 📥 इन-ऐप बैकग्राउंड डाउनलोड (0% से 100%)
    suspend fun downloadUpdateApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val apkFile = File(updateDir, "amon_update.apk")
            if (apkFile.exists()) apkFile.delete()

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("User-Agent", "Amon-App")
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(apkFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            apkFile
        } catch (_: Exception) {
            null
        }
    }

    // ⚙️ फ़ोन का इंस्टॉलर स्क्रीन पर खोलना (No Browser, No GitHub)
    fun installApk(apkFile: File) {
        try {
            // 1. Android 8+ (Oreo से Android 15 तक) परमिशन चेक
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            // 2. सही FileProvider से सुरक्षित URI बनाना
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            // 3. सिस्टम पैकेज इंस्टॉलर को सीधे स्क्रीन पर ट्रिगर करना
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)

        } catch (e: Exception) {
            // कोई भी रुकावट आने पर भी GitHub पर रीडायरेक्ट नहीं होगा
            e.printStackTrace()
        }
    }

    // 🔢 गणितीय रूप से सिर्फ़ बड़े वर्ज़न पर ही ट्रू होगा (e.g. v1.1.2 > v1.1.1)
    private fun isVersionNewer(latest: String, current: String): Boolean {
        return try {
            fun parseNumbers(ver: String): List<Int> {
                val clean = ver.trim().removePrefix("v").removePrefix("V")
                val base = clean.split("-")[0].split("+")[0]
                return base.split(".")
                    .mapNotNull { part -> part.filter { it.isDigit() }.toIntOrNull() }
            }

            val latestParts = parseNumbers(latest)
            val currentParts = parseNumbers(current)

            if (latestParts.isEmpty() || currentParts.isEmpty()) return false

            val maxLength = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLength) {
                val l = latestParts.getOrNull(i) ?: 0
                val c = currentParts.getOrNull(i) ?: 0
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
