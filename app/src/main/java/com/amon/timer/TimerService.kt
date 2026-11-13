package com.amon.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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

        val showCompletionDialogTrigger = mutableStateOf(false)
        val showSessionTooShortDialogTrigger = mutableStateOf(false)

        var originalTimerSeconds: Int = 25 * 60
        var sessionStartElapsedRealtime: Long = 0L
            private set
        var sessionTargetElapsedRealtime: Long = 0L
            private set

        var accumulatedActiveMillis: Long = 0L
            private set
        var sessionResumeElapsedRealtime: Long = 0L
            private set
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

                startSession(secs, subject, isResume = false)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_SKIP_BREAK -> skipBreakAndStartNextRound()
            ACTION_CANCEL, ACTION_STOP -> cancelSessionGiveUp()
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

        // 🟢 TIMER CHALU HOTE HI SUPABASE KO BHEJO: USER IS ACTIVE
        serviceScope.launch(Dispatchers.IO) {
            try {
                CloudSyncManager.updateUserActiveStatus(applicationContext, isActive = true)
            } catch (_: Exception) {}
        }

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

        updateForegroundNotification()

        timerJob = serviceScope.launch {
            if (isStopwatch) {
                while (isActive) {
                    delay(40L)
                    val now = SystemClock.elapsedRealtime()
                    val totalActive = accumulatedActiveMillis + (now - sessionResumeElapsedRealtime)
                    stopwatchElapsedMillis.value = totalActive
                    remainingSeconds.intValue = (totalActive / 1000L).toInt()
                }
            } else {
                while (isActive && remainingSeconds.intValue > 0) {
                    delay(1000L)
                    val leftMillis = sessionTargetElapsedRealtime - SystemClock.elapsedRealtime()
                    val left = (leftMillis / 1000L).toInt().coerceAtLeast(0)
                    remainingSeconds.intValue = left
                    if (left == 0) {
                        endServiceCleanup()
                        break
                    }
                }
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
        updateForegroundNotification()
    }

    private fun resumeTimer() {
        if (isTimerRunning.value) return
        val isStopwatch = (activeModeType.value == "STOPWATCH")
        startSession(if (isStopwatch) 0 else remainingSeconds.intValue, currentSubjectName.value, isResume = true)
    }

    private fun cancelSessionGiveUp() {
        endServiceCleanup()
    }

    private fun endServiceCleanup() {
        // 🔴 TIMER BAND HOTE HI SUPABASE KO BHEJO: USER IS OFFLINE
        serviceScope.launch(Dispatchers.IO) {
            try {
                CloudSyncManager.updateUserActiveStatus(applicationContext, isActive = false)
            } catch (_: Exception) {}
        }

        timerJob?.cancel()
        isTimerRunning.value = false
        isTimerPaused.value = false
        isPomoBreakActive.value = false
        stopSelf()
    }

    private fun updateForegroundNotification() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_clock)
                .setContentTitle("Amon • Focus")
                .setContentText("Session active")
                .build()
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Amon Focus Timer", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
}
