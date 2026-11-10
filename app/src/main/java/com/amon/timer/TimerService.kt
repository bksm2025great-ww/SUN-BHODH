package com.amon.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
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
        const val CHANNEL_ID = "amon_focus_live_v6"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.amon.timer.START"
        const val ACTION_CANCEL = "com.amon.timer.CANCEL"
        const val ACTION_STOP = "com.amon.timer.STOP"
        const val ACTION_PAUSE = "com.amon.timer.PAUSE"
        const val ACTION_RESUME = "com.amon.timer.RESUME"
        const val ACTION_SKIP_BREAK = "com.amon.timer.SKIP_BREAK"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_POMO_SHORT_BREAK = "extra_pomo_short_break"
        const val EXTRA_POMO_LONG_BREAK = "extra_pomo_long_break"
        const val EXTRA_POMO_TOTAL_ROUNDS = "extra_pomo_total_rounds"
        const val EXTRA_POMO_AUTO_START = "extra_pomo_auto_start"

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
        private const val KEY_MODE = "key_mode"

        val remainingSeconds = mutableIntStateOf(25 * 60)
        val stopwatchElapsedMillis = mutableStateOf(0L)
        val isTimerRunning = mutableStateOf(false)
        val isTimerPaused = mutableStateOf(false)
        val currentSubjectName = mutableStateOf("All")

        val activeModeType = mutableStateOf("TIMER")
        val isPomoBreakActive = mutableStateOf(false)
        val currentPomoRound = mutableIntStateOf(1)
        val totalPomoRounds = mutableIntStateOf(4)
        var pomoFocusSecs: Int = 25 * 60
        var pomoShortBreakSecs: Int = 5 * 60
        var pomoLongBreakSecs: Int = 15 * 60
        var pomoAutoStartNext: Boolean = true

        val lastCompletedMinutes = mutableIntStateOf(0)
        val lastCompletedMode = mutableStateOf("")
        val lastCompletedRounds = mutableIntStateOf(0)
        val showCompletionDialogTrigger = mutableStateOf(false)

        var originalTimerSeconds: Int = 25 * 60

        var sessionStartElapsedRealtime: Long = 0L
            private set
        var sessionTargetElapsedRealtime: Long = 0L
            private set

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
                    activeModeType.value = prefs.getString(KEY_MODE, "TIMER") ?: "TIMER"
                    val isStopwatch = (activeModeType.value == "STOPWATCH")

                    if (wasPaused) {
                        isTimerPaused.value = true
                        remainingSeconds.intValue = prefs.getInt(KEY_REMAINING, originalTimerSeconds)
                        stopwatchElapsedMillis.value = accumulatedActiveMillis
                        sessionResumeElapsedRealtime = 0L
                    } else {
                        sessionStartElapsedRealtime = prefs.getLong(KEY_START_ELAPSED, 0L)
                        sessionResumeElapsedRealtime = prefs.getLong(KEY_RESUME_ELAPSED, SystemClock.elapsedRealtime())

                        if (isStopwatch) {
                            val activeNow = accumulatedActiveMillis + (SystemClock.elapsedRealtime() - sessionResumeElapsedRealtime)
                            stopwatchElapsedMillis.value = activeNow
                            remainingSeconds.intValue = (activeNow / 1000L).toInt().coerceAtLeast(0)
                            isTimerRunning.value = true
                        } else {
                            val targetTime = prefs.getLong(KEY_TARGET_ELAPSED, 0L)
                            sessionTargetElapsedRealtime = targetTime
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
                .putString(KEY_MODE, activeModeType.value)
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
            sessionStartElapsedRealtime = 0L
            sessionTargetElapsedRealtime = 0L
            stopwatchElapsedMillis.value = 0L
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
                val secs = intent.getIntExtra(EXTRA_SECONDS, 25 * 60)
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: currentSubjectName.value
                val mode = intent.getStringExtra(EXTRA_MODE) ?: "TIMER"

                activeModeType.value = mode
                if (mode == "POMODORO") {
                    pomoFocusSecs = secs
                    pomoShortBreakSecs = intent.getIntExtra(EXTRA_POMO_SHORT_BREAK, 5) * 60
                    pomoLongBreakSecs = intent.getIntExtra(EXTRA_POMO_LONG_BREAK, 15) * 60
                    totalPomoRounds.intValue = intent.getIntExtra(EXTRA_POMO_TOTAL_ROUNDS, 4)
                    pomoAutoStartNext = intent.getBooleanExtra(EXTRA_POMO_AUTO_START, true)
                    currentPomoRound.intValue = 1
                    isPomoBreakActive.value = false
                }

                triggerHapticBuzz(HapticType.LIGHT)
                startSession(secs, subject, isResume = false)
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_SKIP_BREAK -> {
                skipBreakAndStartNextRound()
            }
            ACTION_CANCEL, ACTION_STOP -> {
                cancelSessionGiveUp()
            }
        }
        return START_NOT_STICKY
    }

    private fun startSession(seconds: Int, subject: String, isResume: Boolean = false) {
        val nowElapsed = SystemClock.elapsedRealtime()
        sessionResumeElapsedRealtime = nowElapsed

        if (!isResume) {
            originalTimerSeconds = seconds
            if (!isPomoBreakActive.value) {
                accumulatedActiveMillis = 0L
                stopwatchElapsedMillis.value = 0L
            }
            sessionStartElapsedRealtime = nowElapsed
        }

        timerJob?.cancel()
        currentSubjectName.value = subject
        isTimerRunning.value = true
        isTimerPaused.value = false

        val isStopwatch = (activeModeType.value == "STOPWATCH")

        if (isStopwatch) {
            val totalActive = accumulatedActiveMillis + (nowElapsed - sessionResumeElapsedRealtime)
            stopwatchElapsedMillis.value = totalActive
            remainingSeconds.intValue = (totalActive / 1000L).toInt()
            sessionTargetElapsedRealtime = 0L
        } else {
            remainingSeconds.intValue = seconds
            sessionTargetElapsedRealtime = nowElapsed + (seconds * 1000L)
        }

        persistSessionState(this, isRunning = true, isPaused = false)
        updateForegroundNotification()

        timerJob = serviceScope.launch {
            if (isStopwatch) {
                while (isActive) {
                    delay(40L)
                    val now = SystemClock.elapsedRealtime()
                    val totalActive = accumulatedActiveMillis + (now - sessionResumeElapsedRealtime)
                    stopwatchElapsedMillis.value = totalActive
                    remainingSeconds.intValue = (totalActive / 1000L).toInt()

                    if (totalActive >= 43200000L) {
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
                        handleCountdownFinished()
                        break
                    }
                }
            }
        }
    }

    private fun handleCountdownFinished() {
        val now = SystemClock.elapsedRealtime()

        if (activeModeType.value == "POMODORO") {
            if (!isPomoBreakActive.value) {
                if (sessionResumeElapsedRealtime > 0L) {
                    accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
                }
                sessionResumeElapsedRealtime = 0L

                triggerHapticBuzz(HapticType.DOUBLE)
                FocusSoundManager.playCompletionSound(this)

                val breakSecs = if (currentPomoRound.intValue >= totalPomoRounds.intValue) pomoLongBreakSecs else pomoShortBreakSecs
                isPomoBreakActive.value = true

                startSession(breakSecs, currentSubjectName.value, isResume = false)
            } else {
                sessionResumeElapsedRealtime = 0L
                triggerHapticBuzz(HapticType.LIGHT)

                if (currentPomoRound.intValue >= totalPomoRounds.intValue) {
                    triggerHapticBuzz(HapticType.LONG)
                    onTimerFinished()
                } else {
                    currentPomoRound.intValue += 1
                    isPomoBreakActive.value = false

                    if (pomoAutoStartNext) {
                        startSession(pomoFocusSecs, currentSubjectName.value, isResume = false)
                    } else {
                        remainingSeconds.intValue = pomoFocusSecs
                        isTimerRunning.value = false
                        isTimerPaused.value = true
                        updateForegroundNotification()
                    }
                }
            }
        } else {
            if (sessionResumeElapsedRealtime > 0L) {
                accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
            }
            triggerHapticBuzz(HapticType.DOUBLE)
            FocusSoundManager.playCompletionSound(this)
            onTimerFinished()
        }
    }

    private fun skipBreakAndStartNextRound() {
        if (activeModeType.value == "POMODORO" && isPomoBreakActive.value) {
            timerJob?.cancel()
            triggerHapticBuzz(HapticType.LIGHT)

            if (currentPomoRound.intValue >= totalPomoRounds.intValue) {
                triggerHapticBuzz(HapticType.LONG)
                onTimerFinished()
            } else {
                currentPomoRound.intValue += 1
                isPomoBreakActive.value = false
                startSession(pomoFocusSecs, currentSubjectName.value, isResume = false)
            }
        }
    }

    private fun pauseTimer() {
        if (!isTimerRunning.value || isTimerPaused.value) return
        val now = SystemClock.elapsedRealtime()

        if (!isPomoBreakActive.value && sessionResumeElapsedRealtime > 0L) {
            accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
        }
        sessionResumeElapsedRealtime = 0L

        timerJob?.cancel()
        isTimerRunning.value = false
        isTimerPaused.value = true

        val isStopwatch = (activeModeType.value == "STOPWATCH")
        if (isStopwatch) {
            stopwatchElapsedMillis.value = accumulatedActiveMillis
            remainingSeconds.intValue = (accumulatedActiveMillis / 1000L).toInt()
        }

        persistSessionState(this, isRunning = false, isPaused = true)
        updateForegroundNotification()
    }

    private fun resumeTimer() {
        if (isTimerRunning.value) return
        val isStopwatch = (activeModeType.value == "STOPWATCH")
        val currentSecs = remainingSeconds.intValue
        val subject = currentSubjectName.value
        startSession(if (isStopwatch) 0 else currentSecs, subject, isResume = true)
    }

    private fun cancelSessionGiveUp() {
        val now = SystemClock.elapsedRealtime()
        if (!isPomoBreakActive.value && isTimerRunning.value && sessionResumeElapsedRealtime > 0L) {
            accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
        }

        val isStopwatch = (activeModeType.value == "STOPWATCH")
        if (isTimerRunning.value || isTimerPaused.value) {
            saveSessionToDiary(isCancelled = if (isStopwatch) false else true)
        }

        endServiceCleanup()
    }

    private fun onTimerFinished() {
        val now = SystemClock.elapsedRealtime()
        if (!isPomoBreakActive.value && isTimerRunning.value && sessionResumeElapsedRealtime > 0L) {
            accumulatedActiveMillis += (now - sessionResumeElapsedRealtime)
        }

        if (isTimerRunning.value || isTimerPaused.value) {
            saveSessionToDiary(isCancelled = false)
        }

        endServiceCleanup()
    }

    private fun endServiceCleanup() {
        timerJob?.cancel()
        isTimerRunning.value = false
        isTimerPaused.value = false
        isPomoBreakActive.value = false
        sessionStartElapsedRealtime = 0L
        sessionTargetElapsedRealtime = 0L
        sessionResumeElapsedRealtime = 0L
        accumulatedActiveMillis = 0L
        stopwatchElapsedMillis.value = 0L

        clearPersistedState(this)
        remainingSeconds.intValue = if (originalTimerSeconds > 0) originalTimerSeconds else 0

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}

        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}

        stopSelf()
    }

    private fun saveSessionToDiary(isCancelled: Boolean) {
        try {
            val rawSeconds = ((accumulatedActiveMillis + 500L) / 1000L).toInt().coerceAtLeast(0)
            val completedSeconds = if (!isCancelled && originalTimerSeconds > 0 && activeModeType.value == "TIMER") {
                originalTimerSeconds
            } else {
                rawSeconds
            }

            if (completedSeconds < 60) {
                return
            }

            val minutes = (completedSeconds / 60).coerceAtLeast(1)

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
            val currentSessionId = System.currentTimeMillis()

            val session = FocusSession(
                id = currentSessionId,
                date = currentDate,
                subject = currentSubjectName.value,
                durationMinutes = minutes,
                earnedTrees = trees
            )
            
            FocusSessionManager.saveSession(this, session)

            CloudSyncManager.syncSession(
                context = this,
                subject = currentSubjectName.value.ifBlank { "All" },
                durationMinutes = minutes.toLong(),
                earnedTrees = trees,
                sessionId = currentSessionId
            )

            lastCompletedMinutes.intValue = minutes
            lastCompletedMode.value = activeModeType.value
            lastCompletedRounds.intValue = currentPomoRound.intValue
            showCompletionDialogTrigger.value = true

        } catch (e: Exception) {
            Log.e("AmonTimer", "Safe session save catch: ${e.localizedMessage}")
        }
    }

    enum class HapticType { LIGHT, DOUBLE, LONG }

    private fun triggerHapticBuzz(type: HapticType) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.LIGHT -> VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.DOUBLE -> VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 200), -1)
                    HapticType.LONG -> VibrationEffect.createOneShot(700L, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.LIGHT -> vibrator?.vibrate(120L)
                    HapticType.DOUBLE -> vibrator?.vibrate(longArrayOf(0, 150, 100, 200), -1)
                    HapticType.LONG -> vibrator?.vibrate(700L)
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateForegroundNotification() {
        val nowElapsed = SystemClock.elapsedRealtime()
        val isStopwatch = (activeModeType.value == "STOPWATCH")
        val chronometerBase = if (isStopwatch) {
            nowElapsed - accumulatedActiveMillis
        } else {
            sessionTargetElapsedRealtime
        }

        try {
            val notification = buildNotification(chronometerBase, isStopwatch, isTimerPaused.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}
    }

    private fun buildNotification(chronometerBase: Long, isStopwatch: Boolean, isPaused: Boolean): Notification {
        createNotificationChannel()

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, TimerService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            2,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeText = when {
            isStopwatch -> "STOPWATCH"
            activeModeType.value == "POMODORO" -> if (isPomoBreakActive.value) "☕ BREAK" else "POMODORO (R${currentPomoRound.intValue}/${totalPomoRounds.intValue})"
            else -> "FOCUS TIMER"
        }

        val currentSub = if (currentSubjectName.value.isNotBlank()) currentSubjectName.value else "All"

        val customLayout = RemoteViews(packageName, R.layout.notification_box).apply {
            // 🛑 POMODORO/MODE टेक्स्ट को छुपा दिया ताकि टाइमर पूरा दिखे और कटे नहीं
            setViewVisibility(R.id.notif_mode_tag, View.GONE)
            setTextViewText(R.id.notif_subject_tag, "Subject • $currentSub")

            if (isPaused) {
                val pausedBase = SystemClock.elapsedRealtime() + (remainingSeconds.intValue * 1000L)
                setChronometer(R.id.notif_timer, pausedBase, null, false)
            } else {
                setChronometer(R.id.notif_timer, chronometerBase, null, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.notif_timer, !isStopwatch)
            }

            if (isPaused) {
                setImageViewResource(R.id.btn_notif_pause, android.R.drawable.ic_media_play)
            } else {
                setImageViewResource(R.id.btn_notif_pause, R.drawable.ic_notif_pause)
            }

            setOnClickPendingIntent(R.id.btn_notif_pause, pauseResumePendingIntent)
            setOnClickPendingIntent(R.id.btn_notif_stop, stopPendingIntent)
            setOnClickPendingIntent(R.id.notif_info_container, openAppPendingIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_logo)
            .setContentTitle("Amon • Focus")
            .setContentText(if (isPaused) "Paused • $currentSub" else "$currentSub • Session active")
            .setOngoing(!isPaused)
            .setContentIntent(openAppPendingIntent)
            .setCustomContentView(customLayout)
            .setCustomBigContentView(customLayout)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Amon Focus Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live focus countdown & stopwatch timer"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
