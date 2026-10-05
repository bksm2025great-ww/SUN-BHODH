package com.amon.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
        const val ACTION_PAUSE = "com.amon.timer.PAUSE"
        const val ACTION_RESUME = "com.amon.timer.RESUME"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_SUBJECT = "extra_subject"

        private const val PREFS_SESSION = "amon_session_lock_prefs"
        private const val KEY_IS_ACTIVE = "key_is_active"
        private const val KEY_IS_PAUSED = "key_is_paused"
        private const val KEY_REMAINING = "key_remaining"
        private const val KEY_ORIGINAL = "key_original"
        private const val KEY_SUBJECT = "key_subject"
        private const val KEY_TARGET_ELAPSED = "key_target_elapsed"
        private const val KEY_START_ELAPSED = "key_start_elapsed"
        private const val KEY_ACCUMULATED_MILLIS = "key_accumulated_millis"
        private const val KEY_RESUME_ELAPSED = "key_resume_elapsed"

        val remainingSeconds = mutableIntStateOf(25 * 60)
        val isTimerRunning = mutableStateOf(false)
        val isTimerPaused = mutableStateOf(false)
        val currentSubjectName = mutableStateOf("All")

        var originalTimerSeconds: Int = 25 * 60

        var sessionStartElapsedRealtime: Long = 0L
            private set
        var sessionTargetElapsedRealtime: Long = 0L
            private set

        // 🛡️ हार्डवेयर टिक काउंटर (Time-Zone Cheat Proof)
        var accumulatedActiveMillis: Long = 0L
            private set
        var sessionResumeElapsedRealtime: Long = 0L
            private set

        fun syncRemainingTime(context: Context? = null) {
            if (context != null) {
                val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
                val wasActive = prefs.getBoolean(KEY_IS_ACTIVE, false)
                val wasPaused = prefs.getBoolean(KEY_IS_PAUSED, false)

                if (wasActive && !isTimerRunning.value && !isTimerPaused.value) {
                    originalTimerSeconds = prefs.getInt(KEY_ORIGINAL, 25 * 60)
                    currentSubjectName.value = prefs.getString(KEY_SUBJECT, "All") ?: "All"
                    accumulatedActiveMillis = prefs.getLong(KEY_ACCUMULATED_MILLIS, 0L)

                    if (wasPaused) {
                        isTimerPaused.value = true
                        remainingSeconds.intValue = prefs.getInt(KEY_REMAINING, originalTimerSeconds)
                        sessionResumeElapsedRealtime = 0L
                    } else {
                        val targetTime = prefs.getLong(KEY_TARGET_ELAPSED, 0L)
                        sessionTargetElapsedRealtime = targetTime
                        sessionStartElapsedRealtime = prefs.getLong(KEY_START_ELAPSED, 0L)
                        sessionResumeElapsedRealtime = prefs.getLong(KEY_RESUME_ELAPSED, SystemClock.elapsedRealtime())

                        val leftMillis = targetTime - SystemClock.elapsedRealtime()
                        val left = (leftMillis / 1000L).toInt().coerceAtLeast(0)
                        if (left > 0) {
                            remainingSeconds.intValue = left
                            isTimerRunning.value = true
                        } else {
                            clearPersistedState(context)
                        }
                    }
                }
            }

            if (isTimerRunning.value && !isTimerPaused.value) {
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

        private fun persistSessionState(context: Context, isRunning: Boolean, isPaused: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_IS_ACTIVE, isRunning || isPaused)
                .putBoolean(KEY_IS_PAUSED, isPaused)
                .putInt(KEY_REMAINING, remainingSeconds.intValue)
                .putInt(KEY_ORIGINAL, originalTimerSeconds)
                .putString(KEY_SUBJECT, currentSubjectName.value)
                .putLong(KEY_TARGET_ELAPSED, sessionTargetElapsedRealtime)
                .putLong(KEY_START_ELAPSED, sessionStartElapsedRealtime)
                .putLong(KEY_ACCUMULATED_MILLIS, accumulatedActiveMillis)
                .putLong(KEY_RESUME_ELAPSED, sessionResumeElapsedRealtime)
                .apply()
        }

        private fun clearPersistedState(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            accumulatedActiveMillis = 0L
            sessionResumeElapsedRealtime = 0L
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
                startTimer(secs, subject, isResume = false)
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_CANCEL, ACTION_STOP -> {
                cancelSessionGiveUp()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int, subject: String, isResume: Boolean = false) {
        val nowElapsed = SystemClock.elapsedRealtime()
        sessionResumeElapsedRealtime = nowElapsed

        if (!isResume) {
            originalTimerSeconds = seconds
            accumulatedActiveMillis = 0L
            sessionStartElapsedRealtime = nowElapsed
        }

        timerJob?.cancel()
        remainingSeconds.intValue = seconds
        currentSubjectName.value = subject
        isTimerRunning.value = true
        isTimerPaused.value = false

        val isStopwatch = (originalTimerSeconds == 0)
        val targetElapsed = if (isStopwatch) nowElapsed else nowElapsed + (seconds * 1000L)
        sessionTargetElapsedRealtime = if (isStopwatch) 0L else targetElapsed

        persistSessionState(this, isRunning = true, isPaused = false)

        val chronometerBase = if (isStopwatch) nowElapsed else targetElapsed

        try {
            val notification = buildNotification(chronometerBase, isStopwatch, isPaused = false)
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

                    if (elapsed >= 7200) {
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

    private fun pauseTimer() {
        if (!isTimerRunning.value || isTimerPaused.value) return
        val now = SystemClock.elapsedRealtime()
        if (sessionResumeElapsedRealtime > 0L) {
            accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
        }
        sessionResumeElapsedRealtime = 0L

        timerJob?.cancel()
        isTimerRunning.value = false
        isTimerPaused.value = true

        persistSessionState(this, isRunning = false, isPaused = true)

        val isStopwatch = (originalTimerSeconds == 0)
        val chronometerBase = if (isStopwatch) sessionStartElapsedRealtime else sessionTargetElapsedRealtime

        try {
            val notification = buildNotification(chronometerBase, isStopwatch, isPaused = true)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("AmonTimer", "Safe pause notification catch: ${e.localizedMessage}")
        }
    }

    private fun resumeTimer() {
        if (isTimerRunning.value) return
        val currentSecs = remainingSeconds.intValue
        val subject = currentSubjectName.value
        startTimer(currentSecs, subject, isResume = true)
    }

    private fun cancelSessionGiveUp() {
        val now = SystemClock.elapsedRealtime()
        if (isTimerRunning.value && sessionResumeElapsedRealtime > 0L) {
            accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
        }

        if (isTimerRunning.value || isTimerPaused.value) {
            saveSessionToDiary(isCancelled = true)
        }

        timerJob?.cancel()
        isTimerRunning.value = false
        isTimerPaused.value = false
        sessionStartElapsedRealtime = 0L
        sessionTargetElapsedRealtime = 0L
        sessionResumeElapsedRealtime = 0L
        accumulatedActiveMillis = 0L

        clearPersistedState(this)

        remainingSeconds.intValue = originalTimerSeconds

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun onTimerFinished() {
        val now = SystemClock.elapsedRealtime()
        if (isTimerRunning.value && sessionResumeElapsedRealtime > 0L) {
            accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
        }

        if (isTimerRunning.value || isTimerPaused.value) {
            saveSessionToDiary(isCancelled = false)
        }

        isTimerRunning.value = false
        isTimerPaused.value = false
        sessionStartElapsedRealtime = 0L
        sessionTargetElapsedRealtime = 0L
        sessionResumeElapsedRealtime = 0L
        accumulatedActiveMillis = 0L

        clearPersistedState(this)

        // 🔄 00:00 पर खत्म होने के बाद घड़ी को तुरंत मूल समय पर रीसेट करना (ताकि लूप न बने)
        remainingSeconds.intValue = if (originalTimerSeconds > 0) originalTimerSeconds else 25 * 60

        triggerGentleVibration()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun saveSessionToDiary(isCancelled: Boolean) {
        try {
            val rawSeconds = ((accumulatedActiveMillis + 500L) / 1000L).toInt().coerceAtLeast(0)
            val completedSeconds = if (!isCancelled && originalTimerSeconds > 0) {
                originalTimerSeconds
            } else {
                rawSeconds
            }

            // 🛑 5 MINUTE RULE: 300 सेकंड से कम की पढ़ाई डायरी में नहीं जाएगी (सुरक्षित)
            if (completedSeconds < 300) {
                return
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

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDate = sdf.format(Date())

            val session = FocusSession(
                date = currentDate,
                subject = currentSubjectName.value,
                durationMinutes = minutes,
                earnedTrees = trees
            )
            // 📁 लोकल डिवाइस स्टोरेज में तुरंत (माइक्रोसेकंड में) सुरक्षित सेव
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

    private fun buildNotification(chronometerBase: Long, isStopwatch: Boolean, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val customLayout = RemoteViews(packageName, R.layout.notification_box).apply {
            setChronometer(R.id.notif_timer, chronometerBase, null, !isPaused)
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
