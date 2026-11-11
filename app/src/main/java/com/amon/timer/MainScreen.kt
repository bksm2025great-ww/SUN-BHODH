package com.amon.timer

import com.amon.timer.groups.GroupsRootScreen
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// 🏆 शुद्ध 2D गोल्डन ट्रॉफी वेक्टर आइकन (OPTION 1)[cite: 9]
// =============================================================================
@Composable
fun VectorGoldTrophy(
    tint: Color,
    modifier: Modifier = Modifier.size(54.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // कप बॉडी
        val cupPath = Path().apply {
            moveTo(w * 0.28f, h * 0.20f)
            lineTo(w * 0.72f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.50f)
            cubicTo(w * 0.65f, h * 0.66f, w * 0.35f, h * 0.66f, w * 0.32f, h * 0.50f)
            close()
        }
        drawPath(cupPath, color = tint.copy(alpha = 0.22f))
        drawPath(cupPath, color = tint, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))

        // बाएं हैंडल
        val leftHandle = Path().apply {
            moveTo(w * 0.29f, h * 0.26f)
            cubicTo(w * 0.12f, h * 0.26f, w * 0.12f, h * 0.46f, w * 0.31f, h * 0.46f)
        }
        drawPath(leftHandle, color = tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

        // दाएं हैंडल
        val rightHandle = Path().apply {
            moveTo(w * 0.71f, h * 0.26f)
            cubicTo(w * 0.88f, h * 0.26f, w * 0.88f, h * 0.46f, w * 0.69f, h * 0.46f)
        }
        drawPath(rightHandle, color = tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

        // तना / स्टेम
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.64f),
            end = Offset(w * 0.50f, h * 0.76f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // बेस स्टैंड
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.76f),
            size = Size(w * 0.40f, h * 0.08f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

// =============================================================================
// ⏱️ शुद्ध 2D ब्रोकन आर्क अलर्ट टाइमर आइकन (OPTION 6)[cite: 9]
// =============================================================================
@Composable
fun VectorBrokenArcAlert(
    tint: Color,
    modifier: Modifier = Modifier.size(54.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // ऊपर का टॉप बटन
        drawLine(
            color = tint,
            start = Offset(w * 0.44f, h * 0.10f),
            end = Offset(w * 0.56f, h * 0.10f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.10f),
            end = Offset(w * 0.50f, h * 0.18f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // साइड पुशर बटन
        drawLine(
            color = tint,
            start = Offset(w * 0.74f, h * 0.20f),
            end = Offset(w * 0.82f, h * 0.28f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // मुख्य इनर क्लॉक बॉडी
        drawCircle(
            color = tint.copy(alpha = 0.15f),
            radius = w * 0.34f,
            center = Offset(w * 0.50f, h * 0.56f)
        )
        drawCircle(
            color = tint,
            radius = w * 0.34f,
            center = Offset(w * 0.50f, h * 0.56f),
            style = Stroke(width = 2.2.dp.toPx())
        )

        // आउटर ब्रोकन आर्क (स्पीड/अधूरा चक्र)
        val arcRect = androidx.compose.ui.geometry.Rect(
            Offset(w * 0.08f, h * 0.14f),
            Size(w * 0.84f, h * 0.84f)
        )
        drawArc(
            color = tint,
            startAngle = 140f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // सेंटर में अलर्ट मार्क '!'
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.44f),
            end = Offset(w * 0.50f, h * 0.58f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = 1.8.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.67f)
        )
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    var currentNavIndex by remember { mutableIntStateOf(0) }

    val isDark = ThemeManager.isDarkTheme.value
    val isRunning = TimerService.isTimerRunning.value
    val isPaused = TimerService.isTimerPaused.value

    var activeMode by rememberSaveable { mutableStateOf(FocusModeType.TIMER) }
    var pomodoroConfig by remember { mutableStateOf(PomodoroConfig()) }

    var selectedSubjectName by rememberSaveable { mutableStateOf("All") }
    var initialTotalSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }

    val isBreakActive = TimerService.isPomoBreakActive.value
    val currentSessionIndex = TimerService.currentPomoRound.intValue

    var isFocusMinimized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            isFocusMinimized = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                TimerService.syncRemainingTime(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ThemeManager.loadTheme(context)
            TimerService.syncRemainingTime(context)
        } catch (_: Exception) {}
    }

    val isFocusActive = isRunning || isPaused
    val isFullScreenFocus = isFocusActive && !isFocusMinimized
    val activeModeTypeStr = TimerService.activeModeType.value
    val isStopwatchMode = (activeModeTypeStr == "STOPWATCH")
    val stopwatchElapsedMillis = TimerService.stopwatchElapsedMillis.value

    val updateManager = remember { UpdateManager(context) }
    var hasNewUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val updateInfo = updateManager.checkLatestUpdate()
            if (updateInfo.hasUpdate) {
                hasNewUpdate = true
            }
        } catch (_: Exception) {}
    }

    val onDirectEndSession: () -> Unit = {
        FocusSoundManager.triggerHaptic(context, isCompletion = true)
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
        isFocusMinimized = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    DisposableEffect(isFullScreenFocus) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val bgHex = if (isDark) "#121216" else "#F8FAFC"
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.parseColor(bgHex)))

            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            if (isFullScreenFocus) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val params = window.attributes
                    params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = params
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT

                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val amberWarningColor = Color(0xFFF59E0B)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val glowBorder = goldColor.copy(alpha = 0.6f)

    BackHandler(enabled = isFullScreenFocus) {
        isFocusMinimized = true
    }

    BackHandler(enabled = currentNavIndex != 0 && !isFullScreenFocus) {
        currentNavIndex = 0
    }

    val totalSeconds = TimerService.remainingSeconds.intValue
    val hours = totalSeconds / 3600
    val displayMinutes = (totalSeconds % 3600) / 60
    val displaySeconds = totalSeconds % 60
    val timeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, displayMinutes, displaySeconds)
    } else {
        String.format("%02d:%02d", displayMinutes, displaySeconds)
    }

    val swH = (stopwatchElapsedMillis / 3600000).toInt()
    val swM = ((stopwatchElapsedMillis % 3600000) / 60000).toInt()
    val swS = ((stopwatchElapsedMillis % 60000) / 1000).toInt()
    val swMs = ((stopwatchElapsedMillis % 1000) / 10).toInt()
    val swMainTime = String.format("%02d:%02d:%02d", swH, swM, swS)

    val liveBarTime = if (isStopwatchMode) swMainTime else "$timeFormatted left"

    val focusScreenContent = @Composable {
        HomeTimerTab(
            context = context,
            goldColor = goldColor,
            glowYellow = glowBorder,
            cardBg = cardBg,
            glassBorder = glassBorder,
            textMuted = textMuted,
            textMain = textMain,
            isDark = isDark,
            isRunning = isRunning,
            isPaused = isPaused,
            isFocusActive = true,
            isSessionActive = true,
            activeMode = activeMode,
            onActiveModeChange = { activeMode = it },
            pomodoroConfig = pomodoroConfig,
            onPomodoroConfigChange = { pomodoroConfig = it },
            isBreakActive = isBreakActive,
            currentSessionIndex = currentSessionIndex,
            isStopwatchRunning = isRunning && isStopwatchMode,
            isStopwatchFocusActive = isFocusActive && isStopwatchMode,
            stopwatchElapsedMillis = stopwatchElapsedMillis,
            selectedSubjectName = selectedSubjectName,
            onSelectedSubjectChange = { selectedSubjectName = it },
            initialTotalSeconds = initialTotalSeconds,
            onInitialTotalSecondsChange = { initialTotalSeconds = it },
            onStartStopwatch = {
                context.startService(Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_START
                    putExtra(TimerService.EXTRA_SECONDS, 0)
                    putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                    putExtra(TimerService.EXTRA_MODE, "STOPWATCH")
                })
                isFocusMinimized = false
            },
            onPauseStopwatch = {
                context.startService(Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_PAUSE
                })
            },
            onResumeStopwatch = {
                context.startService(Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_RESUME
                })
            },
            onDirectEndSession = onDirectEndSession
        )
    }

    if (isFullScreenFocus) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            focusScreenContent()
        }
    } else {
        Scaffold(
            containerColor = bgColor,
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
                    if (isFocusActive && isFocusMinimized) {
                        MiniFocusBar(
                            subjectName = if (isBreakActive) "Break Time" else selectedSubjectName,
                            timeText = liveBarTime,
                            isRunning = isRunning,
                            isPaused = isPaused,
                            goldColor = if (isBreakActive) Color(0xFF10B981) else goldColor,
                            isDark = isDark,
                            onTogglePlayPause = {
                                FocusSoundManager.triggerHaptic(context, false)
                                val act = if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                                context.startService(Intent(context, TimerService::class.java).apply { action = act })
                            },
                            onExpand = {
                                isFocusMinimized = false
                            }
                        )
                    }

                    AmonCurvedBottomBar(
                        selectedIndex = currentNavIndex,
                        onTabSelected = { newIndex -> currentNavIndex = newIndex },
                        isDark = isDark,
                        goldColor = goldColor,
                        cardBg = bgColor,
                        borderCol = Color.Transparent,
                        hasNewUpdate = hasNewUpdate
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(paddingValues)
            ) {
                when (currentNavIndex) {
                    0 -> HomeTimerTab(
                        context = context,
                        goldColor = goldColor,
                        glowYellow = glowBorder,
                        cardBg = cardBg,
                        glassBorder = glassBorder,
                        textMuted = textMuted,
                        textMain = textMain,
                        isDark = isDark,
                        isRunning = isRunning,
                        isPaused = isPaused,
                        isFocusActive = false,
                        isSessionActive = isFocusActive,
                        activeMode = activeMode,
                        onActiveModeChange = { activeMode = it },
                        pomodoroConfig = pomodoroConfig,
                        onPomodoroConfigChange = { pomodoroConfig = it },
                        isBreakActive = isBreakActive,
                        currentSessionIndex = currentSessionIndex,
                        isStopwatchRunning = isRunning && isStopwatchMode,
                        isStopwatchFocusActive = isFocusActive && isStopwatchMode,
                        stopwatchElapsedMillis = stopwatchElapsedMillis,
                        selectedSubjectName = selectedSubjectName,
                        onSelectedSubjectChange = { selectedSubjectName = it },
                        initialTotalSeconds = initialTotalSeconds,
                        onInitialTotalSecondsChange = { initialTotalSeconds = it },
                        onStartStopwatch = {
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_SECONDS, 0)
                                putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                putExtra(TimerService.EXTRA_MODE, "STOPWATCH")
                            })
                            isFocusMinimized = false
                        },
                        onPauseStopwatch = {
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_PAUSE
                            })
                        },
                        onResumeStopwatch = {
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_RESUME
                            })
                        },
                        onDirectEndSession = onDirectEndSession
                    )
                    1 -> GroupsRootScreen(onStartFocus = { 
                        currentNavIndex = 0 
                        isFocusMinimized = false
                    })
                    2 -> StatsScreen()
                    3 -> ProfileScreen()
                }
            }
        }
    }

    // =========================================================================
    // 🏆 CONGRATULATIONS DIALOG (15 सेकंड ऑटो-क्लोज़ + नो इमोजी + 2D गोल्ड ट्रॉफी)
    // =========================================================================
    if (TimerService.showCompletionDialogTrigger.value) {
        LaunchedEffect(Unit) {
            delay(15000L) // 15 सेकंड बाद ऑटो-क्लोज़
            TimerService.showCompletionDialogTrigger.value = false
        }

        Dialog(onDismissRequest = { TimerService.showCompletionDialogTrigger.value = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isDark) Color(0xFF161622) else Color.White)
                    .border(1.5.dp, goldColor, RoundedCornerShape(26.dp))
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { TimerService.showCompletionDialogTrigger.value = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        ) {
                            Text("✕", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2D गोल्डन ट्रॉफी वेक्टर आइकन[cite: 9]
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            VectorGoldTrophy(tint = goldColor)
                        }
                    }

                    Text(
                        text = "Congratulations!",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = goldColor
                    )

                    val mins = TimerService.lastCompletedMinutes.intValue
                    val mode = TimerService.lastCompletedMode.value
                    val rounds = TimerService.lastCompletedRounds.intValue

                    val detailMsg = when (mode) {
                        "POMODORO" -> "You completed $rounds rounds of Pomodoro!\nTotal: $mins minutes focused."
                        "STOPWATCH" -> "You focused for $mins minutes with Stopwatch!"
                        else -> "You completed a $mins minute focus session!"
                    }

                    Text(
                        text = detailMsg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMain,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = { TimerService.showCompletionDialogTrigger.value = false },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Awesome!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // =========================================================================
    // ⏱️ SESSION TOO SHORT DIALOG (< 5 min) (15 सेकंड ऑटो-क्लोज़ + ब्रोकन आर्क आइकन)
    // =========================================================================
    if (TimerService.showSessionTooShortDialogTrigger.value) {
        LaunchedEffect(Unit) {
            delay(15000L) // 15 सेकंड बाद ऑटो-क्लोज़
            TimerService.showSessionTooShortDialogTrigger.value = false
        }

        Dialog(onDismissRequest = { TimerService.showSessionTooShortDialogTrigger.value = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isDark) Color(0xFF161622) else Color.White)
                    .border(1.5.dp, amberWarningColor, RoundedCornerShape(26.dp))
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { TimerService.showSessionTooShortDialogTrigger.value = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        ) {
                            Text("✕", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2D ब्रोकन आर्क अलर्ट टाइमर वेक्टर आइकन[cite: 9]
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            VectorBrokenArcAlert(tint = amberWarningColor)
                        }
                    }

                    Text(
                        text = "Session Too Short",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = amberWarningColor
                    )

                    Text(
                        text = "Sessions must be at least 5 minutes to be recorded. Keep going next time!",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMain,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = { TimerService.showSessionTooShortDialogTrigger.value = false },
                        colors = ButtonDefaults.buttonColors(containerColor = amberWarningColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Got it", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 🔥 STREAK CALCULATION HELPER
// =============================================================================
fun calculateStreakDays(sessions: List<FocusSession>): Int {
    if (sessions.isEmpty()) return 0
    val dayMinutesMap = mutableMapOf<String, Int>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    )

    sessions.forEach { s ->
        var date: Date? = null
        for (fmt in formats) {
            try {
                val d = fmt.parse(s.date)
                if (d != null) { date = d; break }
            } catch (_: Exception) {}
        }
        if (date != null) {
            val key = sdf.format(date)
            dayMinutesMap[key] = (dayMinutesMap[key] ?: 0) + s.durationMinutes
        }
    }

    val cal = Calendar.getInstance()
    var streak = 0
    val todayKey = sdf.format(cal.time)
    val todayMins = dayMinutesMap[todayKey] ?: 0
    if (todayMins >= 30) {
        streak++
    }

    while (true) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dateKey = sdf.format(cal.time)
        val mins = dayMinutesMap[dateKey] ?: 0
        if (mins >= 30) {
            streak++
        } else {
            break
        }
    }
    return streak
}
