package com.amon.timer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🛡️ AMON VAULT ENGINE
 * FocusSession मॉडल के अनुसार 100% सही और क्रैश-प्रूफ़ बैकअप टूल
 */
object BackupManager {

    /**
     * 📤 1. बैकअप फ़ाइल बनाना और Share / Drive मेनू खोलना
     */
    fun exportBackup(context: Context, userName: String = "Traveler"): Boolean {
        return try {
            val sessions = FocusSessionManager.getAllSessions(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "Amon_Vault_${userName}_$timeStamp.json"

            val rootObj = JSONObject().apply {
                put("app", "Amon Focus Tracker")
                put("version", "1.1.0")
                put("userName", userName)
                put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                val sessionsArray = JSONArray()
                sessions.forEach { s ->
                    val sObj = JSONObject().apply {
                        put("id", s.id)
                        put("date", s.date)
                        put("subject", s.subject)
                        put("durationMinutes", s.durationMinutes)
                        put("earnedTrees", s.earnedTrees)
                    }
                    sessionsArray.put(sObj)
                }
                put("sessions", sessionsArray)
            }

            val cacheFolder = File(context.cacheDir, "backups").apply { mkdirs() }
            val backupFile = File(cacheFolder, fileName)
            FileOutputStream(backupFile).use { it.write(rootObj.toString(2).toByteArray()) }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Amon Vault Backup - $userName")
                putExtra(Intent.EXTRA_TEXT, "Amon Study Tracker backup file ($fileName). Keep this safe!")
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
     * 📥 2. बैकअप फ़ाइल से डेटा रीस्टोर करना
     */
    fun importBackup(context: Context, fileUri: Uri): Pair<Boolean, String> {
        return try {
            val content = context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { it.readText() }
                ?: return Pair(false, "Could not read the selected file.")

            val rootObj = JSONObject(content)
            if (!rootObj.has("sessions")) {
                return Pair(false, "Invalid Amon backup file format.")
            }

            val sessionsArray = rootObj.getJSONArray("sessions")
            val restoredSessions = mutableListOf<FocusSession>()

            for (i in 0 until sessionsArray.length()) {
                val item = sessionsArray.getJSONObject(i)
                restoredSessions.add(
                    FocusSession(
                        id = item.optLong("id", System.currentTimeMillis()),
                        date = item.optString("date", ""),
                        subject = item.optString("subject", "General Study"),
                        durationMinutes = item.optInt("durationMinutes", 0),
                        earnedTrees = item.optInt("earnedTrees", 0)
                    )
                )
            }

            // CloudSyncManager / FocusSessionManager का वही फ़ंक्शन जो प्रोफ़ाइल में इस्तेमाल होता है
            val (restoredTrees, restoredMinutes) = FocusSessionManager.restoreSessions(context, restoredSessions)

            Pair(true, "Successfully restored! $restoredMinutes mins & $restoredTrees trees recovered.")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Error restoring file: ${e.localizedMessage}")
        }
    }

    /**
     * 📊 3. एक्सेल शीट (CSV फ़ाइल) एक्सपोर्ट करना
     */
    fun exportCsvReport(context: Context, userName: String = "Traveler"): Boolean {
        return try {
            val sessions = FocusSessionManager.getAllSessions(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "Amon_Study_Report_${userName}_$timeStamp.csv"

            val sb = java.lang.StringBuilder()
            sb.append("Date & Time,Subject,Duration (Mins),Trees Earned\n")

            sessions.forEach { s ->
                val safeSubject = s.subject.replace(",", " ")
                sb.append("${s.date},$safeSubject,${s.durationMinutes},${s.earnedTrees}\n")
            }

            val cacheFolder = File(context.cacheDir, "reports").apply { mkdirs() }
            val csvFile = File(cacheFolder, fileName)
            FileOutputStream(csvFile).use { it.write(sb.toString().toByteArray()) }

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
