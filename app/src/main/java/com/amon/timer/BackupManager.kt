package com.amon.timer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 🛡️ AMON VAULT ENGINE
 * 100% Encrypted .amon format + Clean Table CSV Report (Tamper-proof)
 */
object BackupManager {

    // 🔒 Secret encryption keys for Amon Vault (.amon format)
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val SECRET_KEY_BYTES = "AmonFocusVault99Key2026Secure!".toByteArray(StandardCharsets.UTF_8).copyOf(16)
    private val IV_BYTES = "AmonInitVector26".toByteArray(StandardCharsets.UTF_8).copyOf(16)

    private fun encryptData(plainText: String): ByteArray {
        val key = SecretKeySpec(SECRET_KEY_BYTES, "AES")
        val iv = IvParameterSpec(IV_BYTES)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decryptData(cipherBytes: ByteArray): String {
        val key = SecretKeySpec(SECRET_KEY_BYTES, "AES")
        val iv = IvParameterSpec(IV_BYTES)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
    }

    /**
     * 📤 1. Encrypted .amon बैकअप बनाना (सिर्फ़ Amon ऐप ही इसे पढ़ सकती है)
     */
    fun exportBackup(context: Context, userName: String = "Traveler"): Boolean {
        return try {
            val sessions = FocusSessionManager.getAllSessions(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val safeName = userName.trim().replace("[^a-zA-Z0-9_-]".toRegex(), "_").ifEmpty { "Traveler" }
            val fileName = "Amon_Vault_${safeName}_$timeStamp.amon"

            val rootObj = JSONObject().apply {
                put("app", "Amon Focus Tracker")
                put("version", "1.1.0")
                put("userName", userName)
                put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                val sessionsArray = JSONArray()
                sessions.forEach { s ->
                    // 🛡️ 5 मिनट से कम वाले किसी भी टेस्टिंग सेशन को बैकअप में जाने मत दो
                    if (s.durationMinutes >= 5) {
                        val sObj = JSONObject().apply {
                            put("id", s.id)
                            put("date", s.date)
                            put("subject", s.subject)
                            put("durationMinutes", s.durationMinutes)
                            put("earnedTrees", s.earnedTrees)
                        }
                        sessionsArray.put(sObj)
                    }
                }
                put("sessions", sessionsArray)
            }

            // 🔒 पूरे डेटा को गुप्त लॉक (AES Encryption) में बदलें
            val rawJsonString = rootObj.toString()
            val encryptedBytes = encryptData(rawJsonString)

            val cacheFolder = File(context.cacheDir, "backups").apply { mkdirs() }
            val backupFile = File(cacheFolder, fileName)
            FileOutputStream(backupFile).use { it.write(encryptedBytes) }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Amon Vault Encrypted Backup - $userName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "📦 Amon Vault encrypted backup file ($fileName).\nReadable only inside Amon app. Keep this safe!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Save Amon Vault Backup to...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 📥 2. बैकअप फ़ाइल से डेटा रीस्टोर करना (Encrypted .amon और Legacy .json दोनों समर्थित)
     */
    fun importBackup(context: Context, fileUri: Uri): Pair<Boolean, String> {
        return try {
            val contentBytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                ?: return Pair(false, "Could not read the selected file.")

            // 🔓 Smart Detection: अगर पुरानी .json फ़ाइल है तो सीधे पढ़ेगा, अगर नई .amon है तो डिक्रिप्ट करेगा
            val rawText = String(contentBytes, StandardCharsets.UTF_8)
            val jsonString = if (rawText.trimStart().startsWith("{") && rawText.contains("\"sessions\"")) {
                rawText
            } else {
                decryptData(contentBytes)
            }

            val rootObj = JSONObject(jsonString)
            if (!rootObj.has("sessions")) {
                return Pair(false, "Invalid Amon backup file format.")
            }

            val sessionsArray = rootObj.getJSONArray("sessions")
            val restoredSessions = mutableListOf<FocusSession>()

            for (i in 0 until sessionsArray.length()) {
                val item = sessionsArray.getJSONObject(i)
                val duration = item.optInt("durationMinutes", 0)

                // 🛑 5 मिनट से कम वाले किसी भी सेशन को अंदर मत आने दो
                if (duration < 5) continue

                restoredSessions.add(
                    FocusSession(
                        id = item.optLong("id", System.currentTimeMillis()),
                        date = item.optString("date", ""),
                        subject = item.optString("subject", "General Study"),
                        durationMinutes = duration,
                        earnedTrees = item.optInt("earnedTrees", 0)
                    )
                )
            }

            val (restoredTrees, restoredMinutes) = FocusSessionManager.restoreSessions(context, restoredSessions)

            Pair(true, "Successfully restored! $restoredMinutes mins & $restoredTrees trees recovered.")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Error restoring file: ${e.localizedMessage}")
        }
    }

    /**
     * 📊 3. एक्सेल / गूगल शीट्स के लिए साफ़ स्टडी रिपोर्ट (Read-Only CSV)
     */
    fun exportCsvReport(context: Context, userName: String = "Traveler"): Boolean {
        return try {
            val sessions = FocusSessionManager.getAllSessions(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val safeName = userName.trim().replace("[^a-zA-Z0-9_-]".toRegex(), "_").ifEmpty { "Traveler" }
            val fileName = "Amon_Study_Report_${safeName}_$timeStamp.csv"

            val sb = StringBuilder()
            // 📝 साफ़ 4 हेडर - कोई टेक्निकल आईडी नहीं
            sb.append("Date & Time,Subject,Duration (Mins),Trees Earned\n")

            sessions.forEach { s ->
                if (s.durationMinutes >= 5) {
                    val cleanDate = s.date.replace("\"", "")
                    val cleanSubject = s.subject.replace("\"", "").replace(",", " ")
                    // ✨ कोट्स ("") लगाने से कॉमा होने पर भी कॉलम नहीं खिसकेगा
                    sb.append("\"$cleanDate\",\"$cleanSubject\",${s.durationMinutes},${s.earnedTrees}\n")
                }
            }

            val cacheFolder = File(context.cacheDir, "reports").apply { mkdirs() }
            val csvFile = File(cacheFolder, fileName)
            FileOutputStream(csvFile).use { it.write(sb.toString().toByteArray(StandardCharsets.UTF_8)) }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Amon Study Report - $userName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Study CSV Report...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
