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
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "amon_focus_active_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.amon.timer.START"
        const val ACTION_CANCEL = "com.amon.timer.CANCEL"
        const val ACTION_STOP = "com.amon.timer.STOP"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_SUBJECT = "extra_subject"

        val remainingSeconds = mutableIntStateOf(25 * 60)
        val isTimerRunning = mutableStateOf(false)
        val currentSubjectName = mutableStateOf("All")

        // 🟢 शुरुआती समय की पक्की मेमोरी (25 मिनट या 0 स्टॉपवॉच)
        var originalTimerSeconds: Int = 25 * 60

        // ⏱️ हार्डवेयर काउंटर का टारगेट समय
        var sessionStartElapsedRealtime: Long = 0L
            private set
        var sessionTargetElapsedRealtime: Long = 0L
            private set

        // 🔄 स्क्रीन खुलते ही 1 मिलीसेकंड में टाइम सिंक करने वाला सुरक्षित फ़ंक्शन
        fun syncRemainingTime() {
            if (isTimerRunning.value) {
                if (sessionTargetElapsedRealtime > 0L) {
                    val leftMillis = sessionTargetElapsedRealtime - SystemClock.elapsedRealtime()
                    val left = (leftMillis / 1000L).toInt().coerceAtLeast(0)
                    remainingSeconds.intValue = left
                } else if (sessionStartElapsedRealtime > 0L) {
                    val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartElapsedRealtime
                    val elapsed = (elapsedMillis / 1000L).toInt().coerceAtLeast(0)
                    remainingSeconds.intValue = elapsed
                }
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
            ACTION_CANCEL, ACTION_STOP -> {
                cancelSessionGiveUp()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int, subject: String) {
        originalTimerSeconds = seconds
        val nowElapsed = SystemClock.elapsedRealtime()
        sessionStartElapsedRealtime = nowElapsed
        timerJob?.cancel()
        remainingSeconds.intValue = seconds
        currentSubjectName.value = subject
        isTimerRunning.value = true

        val isStopwatch = (seconds == 0)
        val targetElapsed = if (isStopwatch) nowElapsed else nowElapsed + (seconds * 1000L)
        sessionTargetElapsedRealtime = if (isStopwatch) 0L else targetElapsed

        // Chronometer का सटीक हार्डवेयर बेस टाइम
        val chronometerBase = if (isStopwatch) nowElapsed else targetElapsed

        try {
            val notification = buildNotification(chronometerBase, isStopwatch)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("AmonTimer", "Safe startForeground catch: ${e.localizedMessage}")
        }

        timerJob = serviceScope.launch {
            if (isStopwatch) {
                while (isActive) {
                    delay(1000L)
                    val elapsed = ((SystemClock.elapsedRealtime() - sessionStartElapsedRealtime) / 1000L).toInt()
                    remainingSeconds.intValue = elapsed

                    if (elapsed >= 7200) { // 120 मिनट पर ऑटो-स्टॉप
                        onTimerFinished()
                        break
                    }
                }
            } else {
                while (isActive && remainingSeconds.intValue > 0) {
                    delay(1000L)
                    val leftMillis = sessionTargetElapsedRealtime - SystemClock.elapsedRealtime()
                    val left = (leftMillis / 1000L).toInt().coerceAtLeast(0)
                    remainingSeconds.intValue = left
                    if (left == 0) {
                        onTimerFinished()
                        break
                    }
                }
            }
        }
    }

    // ✕ Give Up दबाने पर: टाइमर सही जगह रीसेट होगा
    private fun cancelSessionGiveUp() {
        if (isTimerRunning.value) {
            saveSessionToDiary(isCancelled = true)
        }
        timerJob?.cancel()
        isTimerRunning.value = false
        sessionStartElapsedRealtime = 0L
        sessionTargetElapsedRealtime = 0L

        // 🔄 जादुई सुधार: टाइमर को तुरंत शुरुआती समय (25:00 या 00:00) पर रीसेट कर देना
        remainingSeconds.intValue = originalTimerSeconds

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    // समय पूरा होने पर: सफल पौधा सेव होगा
    private fun onTimerFinished() {
        if (isTimerRunning.value) {
            saveSessionToDiary(isCancelled = false)
        }
        isTimerRunning.value = false
        sessionStartElapsedRealtime = 0L
        sessionTargetElapsedRealtime = 0L
        triggerGentleVibration()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    // 🟢 सुरक्षित डायरी व क्लाउड सेव
    private fun saveSessionToDiary(isCancelled: Boolean) {
        if (sessionStartElapsedRealtime == 0L) return

        try {
            val elapsedMillis = SystemClock.elapsedRealtime() - sessionStartElapsedRealtime
            val elapsedSeconds = (elapsedMillis / 1000L).toInt().coerceAtLeast(0)

            val completedSeconds = if (originalTimerSeconds > 0) {
                minOf(elapsedSeconds, originalTimerSeconds)
            } else {
                elapsedSeconds
            }

            val minutes = completedSeconds / 60

            val trees = if (isCancelled) {
                0
            } else {
                when {
                    minutes >= 105 -> 4
                    minutes >= 75  -> 3
                    minutes >= 45  -> 2
                    minutes >= 15  -> 1
                    else -> 0
                }
            }

            if (minutes < 1 && !isCancelled) return

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDate = sdf.format(Date())

            val session = FocusSession(
                date = currentDate,
                subject = currentSubjectName.value,
                durationMinutes = minutes,
                earnedTrees = trees
            )
            FocusSessionManager.saveSession(this, session)

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

    // 🌟 कस्टम हाफ़-ट्रांसपेरेंट बॉक्स नोटिफ़िकेशन बिल्डर
    private fun buildNotification(chronometerBase: Long, isStopwatch: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✕ Give Up इंटेंट
        val cancelIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🎨 कस्टम लेआउट (notification_box.xml) लोड करना
        val customLayout = RemoteViews(packageName, R.layout.notification_box).apply {
            setChronometer(R.id.notif_timer, chronometerBase, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.notif_timer, !isStopwatch)
            }
            setOnClickPendingIntent(R.id.btn_notif_cancel, cancelPendingIntent)
            setOnClickPendingIntent(R.id.notif_logo, openAppPendingIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Amon Focus Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live focus countdown timer"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
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
