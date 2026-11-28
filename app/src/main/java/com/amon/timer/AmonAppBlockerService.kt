package com.amon.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay

class AmonAppBlockerService : Service() {

    companion object {
        private const val CHANNEL_ID = "amon_blocker_channel"
        private const val NOTIFICATION_ID = 9042
        private const val POLL_INTERVAL_MS = 450L
        private const val BACK_TO_HOME_COOLDOWN_MS = 1500L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentlyBlockedPackage: String? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var suppressedPackage: String? = null
    private var suppressedUntilElapsed: Long = 0L
    private val openAttemptsState = mutableIntStateOf(0)

    private val pollRunnable = object : Runnable {
        override fun run() {
            val shouldContinue = checkActiveAppAndShield()
            if (shouldContinue) {
                mainHandler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager

        val isFocusRunning = TimerService.isTimerRunning.value || TimerService.isTimerPaused.value
        val isBlockerActive = BlockAppManager.isBlockingEnabled(this) &&
            BlockAppManager.hasUsageStatsPermission(this)

        if (!isFocusRunning || !isBlockerActive) {
            stopSelf()
            return
        }

        startAsForegroundService()
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isFocusRunning = TimerService.isTimerRunning.value || TimerService.isTimerPaused.value
        val isBlockerActive = BlockAppManager.isBlockingEnabled(this) &&
            BlockAppManager.hasUsageStatsPermission(this)

        if (!isFocusRunning || !isBlockerActive) {
            removeOverlayShield()
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForegroundService()
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pollRunnable)
        removeOverlayShield()
        super.onDestroy()
    }

    private fun startAsForegroundService() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AMON Focus Shield",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors active apps during focus sessions"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AMON App Blocker")
            .setContentText("Monitoring distracting apps during focus")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setSilent(true)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun checkActiveAppAndShield(): Boolean {
        val isFocusRunning = TimerService.isTimerRunning.value || TimerService.isTimerPaused.value
        val isBlockerActive = BlockAppManager.isBlockingEnabled(this) &&
            BlockAppManager.hasUsageStatsPermission(this)

        if (!isFocusRunning || !isBlockerActive) {
            removeOverlayShield()
            stopSelf()
            return false
        }

        val settingsConfig = BlockSettingsManager.loadConfig(this)
        if (TimerService.isPomoBreakActive.value && !settingsConfig.blockAppsDuringBreak) {
            removeOverlayShield()
            return true
        }

        val pkgName = BlockAppManager.getForegroundPackageName(this).orEmpty()
        if (pkgName.isBlank()) return true

        if (pkgName == packageName || isSafeSystemOrLauncherPackage(pkgName)) {
            if (pkgName == packageName) {
                removeOverlayShield()
            }
            return true
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        if (suppressedPackage == pkgName && nowElapsed < suppressedUntilElapsed) {
            return true
        } else if (nowElapsed >= suppressedUntilElapsed) {
            suppressedPackage = null
            suppressedUntilElapsed = 0L
        }

        if (BlockAppManager.isPackageBlocked(this, pkgName)) {
            if (currentlyBlockedPackage != pkgName || overlayView == null) {
                openAttemptsState.intValue += 1
                showOverlayShield(pkgName)
            }
        } else {
            removeOverlayShield()
        }
        return true
    }

    private fun isSafeSystemOrLauncherPackage(pkgName: String): Boolean {
        val safePrefixes = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.miui.home",
            "com.oppo.launcher",
            "com.sec.android.app.launcher"
        )
        return safePrefixes.any { pkgName.startsWith(it) }
    }

    private fun handleBackToPhoneHome() {
        val pkgToSuppress = currentlyBlockedPackage
        if (!pkgToSuppress.isNullOrBlank()) {
            suppressedPackage = pkgToSuppress
            suppressedUntilElapsed = SystemClock.elapsedRealtime() + BACK_TO_HOME_COOLDOWN_MS
        }
        removeOverlayShield()
        sendToPhoneHome()
    }

    private fun formatDailyTargetText(): String {
        val savedHours = try {
            getSharedPreferences("amon_stats_goals", MODE_PRIVATE)
                .getFloat("daily_goal_hours", 0f)
        } catch (_: Exception) {
            0f
        }
        val totalMins = if (savedHours > 0f) (savedHours * 60f).toInt() else 30
        return if (totalMins >= 60) {
            val h = totalMins / 60
            val m = totalMins % 60
            if (m == 0) "${h}h daily" else "${h}h ${m}m daily"
        } else {
            "${totalMins}m daily"
        }
    }

    private fun formatTodayFocusedText(): String {
        val mins = try {
            val sessions = FocusSessionManager.getAllSessions(this)
            calculateTodayMinutesFromSessions(sessions)
        } catch (_: Exception) {
            0
        }
        return if (mins >= 60) {
            val h = mins / 60
            val m = mins % 60
            if (m == 0) "${h}h focused today" else "${h}h ${m}m focused today"
        } else {
            "${mins}m focused today"
        }
    }

    private fun showOverlayShield(blockedPkg: String) {
        val wm = windowManager ?: (getSystemService(WINDOW_SERVICE) as? WindowManager)?.also {
            windowManager = it
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        removeOverlayShield()

        val appBitmap = try {
            BlockAppManager.getAppIcon(this, blockedPkg)?.toBitmap(width = 180, height = 180)
        } catch (_: Exception) {
            null
        }

        val targetText = formatDailyTargetText()
        val focusedTodayText = formatTodayFocusedText()

        val lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
            onResume()
        }
        overlayLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                AmonOverlayShieldScreen(
                    appIconBitmap = appBitmap,
                    remainingSeconds = TimerService.remainingSeconds.intValue,
                    isStopwatch = TimerService.activeModeType.value == "STOPWATCH",
                    stopwatchMillis = TimerService.stopwatchElapsedMillis.value,
                    dailyTargetText = targetText,
                    focusedTodayText = focusedTodayText,
                    openAttemptsCount = openAttemptsState.intValue,
                    onReturnToAmon = {
                        removeOverlayShield()
                        sendToPhoneHome()
                        openAmonApp()
                    }
                )
            }
        }

        val shieldContainer = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    handleBackToPhoneHome()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            addView(composeView)
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            wm.addView(shieldContainer, params)
            overlayView = shieldContainer
            currentlyBlockedPackage = blockedPkg
            shieldContainer.post { shieldContainer.requestFocus() }
        } catch (_: Exception) {
            lifecycleOwner.destroy()
            overlayLifecycleOwner = null
        }
    }

    private fun removeOverlayShield() {
        val viewToRemove = overlayView
        val wm = windowManager
        if (viewToRemove != null && wm != null) {
            try {
                wm.removeViewImmediate(viewToRemove)
            } catch (_: Exception) {
                try {
                    wm.removeView(viewToRemove)
                } catch (_: Exception) {}
            }
        }
        overlayView = null
        currentlyBlockedPackage = null

        overlayLifecycleOwner?.destroy()
        overlayLifecycleOwner = null
    }

    private fun sendToPhoneHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {}
    }

    private fun openAmonApp() {
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(launchIntent)
        } catch (_: Exception) {}
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = registry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun onCreate() {
            savedStateController.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onResume() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            if (registry.currentState != Lifecycle.State.DESTROYED) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        }
    }
}

@Composable
private fun AmonOverlayShieldScreen(
    appIconBitmap: Bitmap?,
    remainingSeconds: Int,
    isStopwatch: Boolean,
    stopwatchMillis: Long,
    dailyTargetText: String,
    focusedTodayText: String,
    openAttemptsCount: Int,
    onReturnToAmon: () -> Unit
) {
    val bgDark = Color(0xFF121216)
    val goldAccent = Color(0xFFE5B44C)
    val subTextColor = Color(0xFF9CA3AF)
    val chipBg = Color(0xFF1C1C22)
    val chipBorder = Color(0x26FFFFFF)

    val breatheTransition = rememberInfiniteTransition(label = "shield_breathe")
    val outerRingScale by breatheTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_ring_scale"
    )
    val innerRingScale by breatheTransition.animateFloat(
        initialValue = 1.04f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_ring_scale"
    )
    val urgeButtonScale by breatheTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "urge_btn_scale"
    )

    var breatheRemaining by remember { mutableIntStateOf(0) }
    LaunchedEffect(breatheRemaining) {
        if (breatheRemaining > 0) {
            delay(1000L)
            breatheRemaining -= 1
        }
    }

    val timerDisplay = if (isStopwatch) {
        val totalSecs = (stopwatchMillis / 1000L).toInt()
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    } else {
        val safeSecs = remainingSeconds.coerceAtLeast(0)
        val h = safeSecs / 3600
        val m = (safeSecs % 3600) / 60
        val s = safeSecs % 60
        if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    val dimmedIconFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply {
                setToSaturation(0.28f)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "AMON",
                color = goldAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.5.sp
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(196.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = goldAccent.copy(alpha = 0.22f),
                        radius = size.minDimension * 0.46f * outerRingScale,
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                    drawCircle(
                        color = goldAccent.copy(alpha = 0.36f),
                        radius = size.minDimension * 0.35f * innerRingScale,
                        center = center,
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }

                Box(contentAlignment = Alignment.BottomEnd) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap.asImageBitmap(),
                            contentDescription = "Blocked App Icon",
                            colorFilter = dimmedIconFilter,
                            modifier = Modifier
                                .size(84.dp)
                                .alpha(0.55f)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, goldAccent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(84.dp)
                                .alpha(0.6f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E1E28))
                                .border(1.5.dp, goldAccent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = "APP",
                                color = goldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = 5.dp, y = 5.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(goldAccent)
                            .border(2.dp, bgDark, CircleShape)
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val w = size.width
                            val h = size.height
                            drawRoundRect(
                                color = bgDark,
                                topLeft = Offset(w * 0.22f, h * 0.46f),
                                size = Size(w * 0.56f, h * 0.42f),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                            val shackle = Path().apply {
                                moveTo(w * 0.32f, h * 0.46f)
                                lineTo(w * 0.32f, h * 0.32f)
                                cubicTo(
                                    w * 0.32f, h * 0.16f,
                                    w * 0.68f, h * 0.16f,
                                    w * 0.68f, h * 0.32f
                                )
                                lineTo(w * 0.68f, h * 0.46f)
                            }
                            drawPath(
                                path = shackle,
                                color = bgDark,
                                style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Focus Session Active",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This app is blocked during focus",
                    color = subTextColor,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timerDisplay,
                    color = goldAccent,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isStopwatch) "elapsed" else "remaining",
                    color = subTextColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Your Target: $dailyTargetText",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = focusedTodayText,
                            color = subTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$openAttemptsCount open attempts",
                            color = subTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onReturnToAmon,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goldAccent,
                        contentColor = bgDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Return to AMON",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .scale(urgeButtonScale)
                        .clip(RoundedCornerShape(50))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(50))
                        .clickable {
                            if (breatheRemaining == 0) {
                                breatheRemaining = 10
                            }
                        }
                ) {
                    Text(
                        text = if (breatheRemaining > 0) {
                            "Breathe slowly... ${breatheRemaining}s"
                        } else {
                            "Feeling the urge? Breathe for 10 sec"
                        },
                        color = if (breatheRemaining > 0) goldAccent else Color(0xFFD1D5DB),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
