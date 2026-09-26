package com.amon.timer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
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

// 📦 अपडेट की सारी जानकारी का डिब्बा
data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val whatsNew: String,
    val downloadUrl: String
)

class UpdateManager(private val context: Context) {

    // 📱 फ़ोन के सिस्टम से सीधे असली वर्ज़न पढ़ना (अब कभी हार्डकोड नहीं रहेगा)
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

    // 🌐 GitHub Releases से लेटेस्ट अपडेट चेक करना
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
            val apiUrl = "https://api.github.com/repos/SUN-BHODH/SUN-BHODH/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", activeVersion)
                val body = json.optString("body", "Bug fixes and performance improvements.")
                val htmlUrl = json.optString("html_url", "https://github.com")

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

                val isNewer = isVersionNewer(tagName, activeVersion)

                updateInfo = AppUpdateInfo(
                    hasUpdate = isNewer,
                    currentVersion = activeVersion,
                    latestVersion = tagName,
                    whatsNew = body.ifBlank { "✨ New features and performance optimizations." },
                    downloadUrl = apkDownloadUrl
                )
            }
        } catch (_: Exception) {
            // नेटवर्क न होने पर ऐप क्रैश नहीं होगा
        }

        updateInfo
    }

    // 📥 इन-ऐप बैकग्राउंड डाउनलोड (सीधे कैशे में, कोई कचरा नहीं बचेगा)
    suspend fun downloadUpdateApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            // पुराना कोई भी APK हो तो पहले ही साफ़ कर दो (Single-file rule)
            val apkFile = File(updateDir, "amon_update.apk")
            if (apkFile.exists()) apkFile.delete()

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 10000
                readTimeout = 15000
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

    // ⚙️ सिस्टम का इंस्टॉलर स्क्रीन पर खोलना
    fun installApk(apkFile: File) {
        try {
            val authority = "${context.packageName}.provider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // अगर सीधे इंस्टॉलर न खुले तो ब्राउज़र बैकअप
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SUN-BHODH/SUN-BHODH/releases/latest"))
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(browserIntent)
        }
    }

    // 🔢 सही वर्ज़न तुलना (v1.0.2 vs v1.0.1)
    private fun isVersionNewer(latest: String, current: String): Boolean {
        return try {
            val cleanLatest = latest.replace("v", "").replace("V", "").trim().split(".")
            val cleanCurrent = current.replace("v", "").replace("V", "").trim().split(".")

            val maxLength = maxOf(cleanLatest.size, cleanCurrent.size)
            for (i in 0 until maxLength) {
                val latestPart = cleanLatest.getOrNull(i)?.toIntOrNull() ?: 0
                val currentPart = cleanCurrent.getOrNull(i)?.toIntOrNull() ?: 0
                if (latestPart > currentPart) return true
                if (latestPart < currentPart) return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
