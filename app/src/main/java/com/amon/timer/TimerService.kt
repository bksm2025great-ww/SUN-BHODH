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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "amon_focus_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.amon.timer.START"
        const val ACTION_PAUSE = "com.amon.timer.PAUSE"
        const val ACTION_STOP = "com.amon.timer.STOP"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_SUBJECT = "extra_subject"

        // MainScreen is state ko seedhe read karti hai
        val remainingSeconds = mutableIntStateOf(25 * 60)
        val isTimerRunning = mutableStateOf(false)
        val currentSubjectName = mutableStateOf("All")
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
        timerJob?.cancel()
        remainingSeconds.intValue = seconds
        currentSubjectName.value = subject
        isTimerRunning.value = true

        // 🟢 0 होने पर स्टॉपवॉच, वरना टाइमर
        val isStopwatch = (seconds == 0)
        val startTime = System.currentTimeMillis()
        val endTime = if (isStopwatch) startTime else startTime + seconds * 1000L

        val referenceTime = if (isStopwatch) startTime else endTime
        val notification = buildNotification(seconds, subject, referenceTime, isStopwatch)
        startForeground(NOTIFICATION_ID, notification)

        timerJob = serviceScope.launch {
            if (isStopwatch) {
                // ⏱️ स्टॉपवॉच मोड: 0 से आगे बढ़ेगा (120 मिनट की लिमिट)
                while (isActive) {
                    delay(1000L)
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000L).toInt()
                    remainingSeconds.intValue = elapsed
                    
                    // 120 मिनट (7200 सेकंड) पूरे होते ही ऑटो-स्टॉप
                    if (elapsed >= 7200) {
                        onTimerFinished()
                        break
                    }
                }
            } else {
                // ⏳ टाइमर मोड: उल्टी गिनती
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
        timerJob?.cancel()
        isTimerRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerFinished() {
        isTimerRunning.value = false
        triggerGentleVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Session pura hone par 1 halki shaant vibration
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
            e.printStackTrace()
        }
    }

    // Lock screen aur notification bar ka card
    private fun buildNotification(seconds: Int, subject: String, endTime: Long): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session: $subject")
            .setContentText("Timer running in background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endTime)
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
