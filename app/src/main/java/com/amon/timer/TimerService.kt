package com.amon.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    // 🟢 असली टाइम और शुरू होने का टाइमस्टैम्प
    private var originalTimerSeconds = 0
    private var sessionStartTimeMillis: Long = 0L

    companion object {
        const val CHANNEL_ID = "amon_focus_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.amon.timer.START"
        const val ACTION_PAUSE = "com.amon.timer.PAUSE"
        const val ACTION_STOP = "com.amon.timer.STOP"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_SUBJECT = "extra_subject"

        val remainingSeconds = mutableIntStateOf(25 * 60)
        val isTimerRunning = mutableStateOf(false)
        val currentSubjectName = mutableStateOf("All")

        // ⏱️ स्क्रीन ऑन होते ही रिंग और टाइमर को तुरंत री-सिंक करने के लिए टारगेट टाइम
        var sessionEndTimeMillis: Long = 0L
            private set

        // 🔄 स्क्रीन खुलते ही 1 मिलीसेकंड में टाइम सिंक करने वाला जादुई फ़ंक्शन
        fun syncRemainingTime() {
            if (isTimerRunning.value && sessionEndTimeMillis > 0L) {
                val left = ((sessionEndTimeMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                remainingSeconds.intValue = left
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val secs = intent.getIntExtra(EXTRA_SECONDS, remainingSeconds.intValue)
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: currentSubjectName.value
                startTimer(secs, subject)
            }
            ACTION_PAUSE, ACTION_STOP -> {
                pauseTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int, subject: String) {
        originalTimerSeconds = seconds
        sessionStartTimeMillis = System.currentTimeMillis()
        timerJob?.cancel()
        remainingSeconds.intValue = seconds
        currentSubjectName.value = subject
        isTimerRunning.value = true

        val isStopwatch = (seconds == 0)
        val startTime = sessionStartTimeMillis
        val endTime = if (isStopwatch) startTime else startTime + (seconds * 1000L)
        sessionEndTimeMillis = endTime

        val referenceTime = if (isStopwatch) startTime else endTime

        // 🛡️ Samsung & Android 14+ सुरक्षा कवच: Foreground Service कभी क्रैश नहीं होगी
        try {
            val notification = buildNotification(seconds, subject, referenceTime, isStopwatch)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("AmonTimer", "Safe startForeground catch: ${e.localizedMessage}")
        }

        timerJob = serviceScope.launch {
            if (isStopwatch) {
                while (isActive) {
                    delay(1000L)
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000L).toInt()
                    remainingSeconds.intValue = elapsed

                    if (elapsed >= 7200) { // 120 मिनट पर ऑटो-स्टॉप
                        onTimerFinished()
                        break
                    }
                }
            } else {
                while (isActive && remainingSeconds.intValue > 0) {
                    delay(1000L)
                    val left = ((endTime - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                    remainingSeconds.intValue = left
                    if (left == 0) {
                        onTimerFinished()
                        break
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        if (isTimerRunning.value) {
            saveSessionToDiary()
        }
        timerJob?.cancel()
        isTimerRunning.value = false
        sessionEndTimeMillis = 0L
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun onTimerFinished() {
        if (isTimerRunning.value) {
            saveSessionToDiary()
        }
        isTimerRunning.value = false
        sessionEndTimeMillis = 0L
        triggerGentleVibration()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    // 🟢 सुरक्षित डायरी व क्लाउड सेव
    private fun saveSessionToDiary() {
        if (sessionStartTimeMillis == 0L) return

        try {
            // 1. बीता हुआ असली समय सेकंड में निकालना
            val elapsedMillis = System.currentTimeMillis() - sessionStartTimeMillis
            val elapsedSeconds = (elapsedMillis / 1000L).toInt().coerceAtLeast(0)

            val completedSeconds = if (originalTimerSeconds > 0) {
                minOf(elapsedSeconds, originalTimerSeconds)
            } else {
                elapsedSeconds
            }

            sessionStartTimeMillis = 0L

            val minutes = completedSeconds / 60
            if (minutes < 1) return // 1 मिनट से कम पर सेव नहीं होगा

            // 2. 🌲 नए स्लैब के हिसाब से पेड़ का नियम
            val trees = when {
                minutes >= 105 -> 4
                minutes >= 75  -> 3
                minutes >= 45  -> 2
                minutes >= 15  -> 1
                else -> 0
            }

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val currentDate = sdf.format(java.util.Date())

            // 3. फ़ोन की लोकल डायरी में सुरक्षित सेव
            val session = FocusSession(
                date = currentDate,
                subject = currentSubjectName.value,
                durationMinutes = minutes,
                earnedTrees = trees
            )
            FocusSessionManager.saveSession(this, session)

            // 4. 🌐 Google Sheet में बैकअप
            CloudSyncManager.syncSession(
                context = this,
                subject = currentSubjectName.value,
                durationMinutes = minutes.toLong(),
                earnedTrees = trees
            )
        } catch (e: Exception) {
            Log.e("AmonTimer", "Safe session save catch: ${e.localizedMessage}")
        }
    }

    private fun triggerGentleVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(350L)
                }
            }
        } catch (e: Exception) {
            Log.e("AmonTimer", "Vibration error: ${e.localizedMessage}")
        }
    }

    private fun buildNotification(seconds: Int, subject: String, referenceTime: Long, isStopwatch: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (isStopwatch) "Stopwatch running in background" else "Timer running in background"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session: $subject")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setUsesChronometer(true)
            .setChronometerCountDown(!isStopwatch)
            .setWhen(referenceTime)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Amon Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live focus countdown timer"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
}
