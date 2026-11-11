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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.StrokeJoin
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

@Composable
fun VectorGoldTrophy(
    tint: Color,
    modifier: Modifier = Modifier.size(54.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val cupPath = Path().apply {
            moveTo(w * 0.28f, h * 0.20f)
            lineTo(w * 0.72f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.50f)
            cubicTo(w * 0.65f, h * 0.66f, w * 0.35f, h * 0.66f, w * 0.32f, h * 0.50f)
            close()
        }
        drawPath(cupPath, color = tint.copy(alpha = 0.22f))
        drawPath(cupPath, color = tint, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))

        val leftHandle = Path().apply {
            moveTo(w * 0.29f, h * 0.26f)
            cubicTo(w * 0.12f, h * 0.26f, w * 0.12f, h * 0.46f, w * 0.31f, h * 0.46f)
        }
        drawPath(leftHandle, color = tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

        val rightHandle = Path().apply {
            moveTo(w * 0.71f, h * 0.26f)
            cubicTo(w * 0.88f, h * 0.26f, w * 0.88f, h * 0.46f, w * 0.69f, h * 0.46f)
        }
        drawPath(rightHandle, color = tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.64f),
            end = Offset(w * 0.50f, h * 0.76f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.76f),
            size = Size(w * 0.40f, h * 0.08f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

@Composable
fun VectorBrokenArcAlert(
    tint: Color,
    modifier: Modifier = Modifier.size(54.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

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

        drawLine(
            color = tint,
            start = Offset(w * 0.74f, h * 0.20f),
            end = Offset(w * 0.82f, h * 0.28f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

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
fun AndroidAutoRotateLineIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val strokeWidth = 1.4.dp.toPx()

        val phoneW = w * 0.32f
        val phoneH = h * 0.50f
        drawRoundRect(
            color = tint,
            topLeft = Offset(center.x - phoneW / 2f, center.y - phoneH / 2f),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        val arcRadius = w * 0.38f
        drawArc(
            color = tint,
            startAngle = 205f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val arrowPath1 = Path().apply {
            moveTo(center.x + arcRadius * 0.10f, center.y - arcRadius * 1.02f)
            lineTo(center.x + arcRadius * 0.40f, center.y - arcRadius * 0.90f)
            lineTo(center.x + arcRadius * 0.25f, center.y - arcRadius * 0.62f)
            close()
        }
        drawPath(path = arrowPath1, color = tint)

        drawArc(
            color = tint,
            startAngle = 25f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val arrowPath2 = Path().apply {
            moveTo(center.x - arcRadius * 0.10f, center.y + arcRadius * 1.02f)
            lineTo(center.x - arcRadius * 0.40f, center.y + arcRadius * 0.90f)
            lineTo(center.x - arcRadius * 0.25f, center.y - arcRadius * 0.62f)
            close()
        }
        drawPath(path = arrowPath2, color = tint)
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
    var isLandscape by remember { mutableStateOf(false) }

    // 🌟 Zen Mode: स्क्रीन टैप करने पर कंट्रोल्स दिखना/छिपना
    var areControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, areControlsVisible) {
        if (isRunning && areControlsVisible) {
            delay(4000L) // 4 सेकंड बाद अपने-आप स्मूथली छिप जाएँगे
            areControlsVisible = false
        }
    }

    // जब भी पॉज़ होगा, बटन्स तुरंत वापस दिखेंगे
    LaunchedEffect(isPaused) {
        if (isPaused) {
            areControlsVisible = true
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            isFocusMinimized = false
            areControlsVisible = true
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
        isLandscape = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    DisposableEffect(isFullScreenFocus) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val bgHex = if (isDark) "#121216" else "#F8FAFC"
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.parseColor(bgHex)))

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
                // फुल स्क्रीन से बाहर आते ही सख्त पोर्ट्रेट
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                isLandscape = false
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isLandscape = false
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
        isLandscape = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            areControlsVisible = areControlsVisible || isPaused,
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 👆 खाली स्क्रीन पर टैप करने से कंट्रोल्स दिखेंगे/छिपेंगे
                    areControlsVisible = !areControlsVisible
                }
        ) {
            focusScreenContent()

            // 🔄 केवल तब दिखेगा जब टाइमर रनिंग हो और कंट्रोल्स ऑन हों (पॉज़ पर छिप जाएगा)
            val showRotate = isRunning && (areControlsVisible || isPaused)
            AnimatedVisibility(
                visible = showRotate,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E1E26) else Color.White)
                        .border(
                            1.dp,
                            if (isLandscape) goldColor else glassBorder,
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (activity != null) {
                                if (isLandscape) {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    isLandscape = false
                                } else {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    isLandscape = true
                                }
                            }
                        }
                ) {
                    AndroidAutoRotateLineIcon(
                        tint = if (isLandscape) goldColor else textMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
                        areControlsVisible = true,
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
    // 🏆 CONGRATULATIONS DIALOG (15s Auto-Close + 2D Trophy)
    // =========================================================================
    if (TimerService.showCompletionDialogTrigger.value) {
        LaunchedEffect(Unit) {
            delay(15000L)
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
    // ⏱️ SESSION TOO SHORT DIALOG (< 5 min) (15s Auto-Close + Broken Arc)
    // =========================================================================
    if (TimerService.showSessionTooShortDialogTrigger.value) {
        LaunchedEffect(Unit) {
            delay(15000L)
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
// 📱 MINI FOCUS BAR
// =============================================================================
@Composable
fun MiniFocusBar(
    subjectName: String,
    timeText: String,
    isRunning: Boolean,
    isPaused: Boolean,
    goldColor: Color,
    isDark: Boolean,
    onTogglePlayPause: () -> Unit,
    onExpand: () -> Unit
) {
    val barBg = if (isDark) Color(0xFF16161F) else Color(0xFFFFFFFF)
    val textMain = if (isDark) Color.White else Color(0xFF1E293B)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(50))
                .background(barBg)
                .border(1.2.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable { onExpand() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(goldColor)
                    .clickable { onTogglePlayPause() }
            ) {
                if (isPaused) {
                    Canvas(modifier = Modifier.size(13.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.22f, size.height * 0.15f)
                            lineTo(size.width * 0.88f, size.height * 0.5f)
                            lineTo(size.width * 0.22f, size.height * 0.85f)
                            close()
                        }
                        drawPath(path, color = Color.Black)
                    }
                } else {
                    Canvas(modifier = Modifier.size(13.dp)) {
                        val barW = size.width * 0.28f
                        val barH = size.height * 0.85f
                        val cr = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        drawRoundRect(
                            color = Color.Black,
                            topLeft = Offset(size.width * 0.14f, size.height * 0.075f),
                            size = Size(barW, barH),
                            cornerRadius = cr
                        )
                        drawRoundRect(
                            color = Color.Black,
                            topLeft = Offset(size.width * 0.58f, size.height * 0.075f),
                            size = Size(barW, barH),
                            cornerRadius = cr
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = subjectName,
                    color = textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "•",
                    color = textMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = timeText,
                    color = goldColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                    .clickable { onExpand() }
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * 0.18f, size.height * 0.65f)
                        lineTo(size.width * 0.5f, size.height * 0.32f)
                        lineTo(size.width * 0.82f, size.height * 0.65f)
                    }
                    drawPath(
                        path = p,
                        color = textMain,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// ⏱️ HOME TIMER TAB (Zen Mode Controls Included)
// =============================================================================
@Composable
fun HomeTimerTab(
    context: Context,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMuted: Color,
    textMain: Color,
    isDark: Boolean,
    isRunning: Boolean,
    isPaused: Boolean,
    isFocusActive: Boolean,
    isSessionActive: Boolean = false,
    activeMode: FocusModeType,
    onActiveModeChange: (FocusModeType) -> Unit,
    pomodoroConfig: PomodoroConfig,
    onPomodoroConfigChange: (PomodoroConfig) -> Unit,
    isBreakActive: Boolean,
    currentSessionIndex: Int,
    isStopwatchRunning: Boolean,
    isStopwatchFocusActive: Boolean,
    stopwatchElapsedMillis: Long,
    selectedSubjectName: String,
    onSelectedSubjectChange: (String) -> Unit,
    initialTotalSeconds: Int,
    onInitialTotalSecondsChange: (Int) -> Unit,
    areControlsVisible: Boolean = true,
    onStartStopwatch: () -> Unit,
    onPauseStopwatch: () -> Unit,
    onResumeStopwatch: () -> Unit,
    onDirectEndSession: () -> Unit
) {
    var userSubjects by remember { mutableStateOf(listOf<String>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    var showFocusLauncherSheet by remember { mutableStateOf(false) }
    var dialMinutes by rememberSaveable { mutableFloatStateOf(25f) }

    val subjectPrefs = remember { context.getSharedPreferences("amon_subject_prefs", Context.MODE_PRIVATE) }
    var hiddenSubjects by remember {
        val saved = try { subjectPrefs.getStringSet("hidden_subjects", null) } catch (_: Exception) { null }
        if (saved == null) {
            val initial = try { SubjectManager.getUserSubjects(context).toSet() } catch (_: Exception) { emptySet() }
            try { subjectPrefs.edit().putStringSet("hidden_subjects", initial).apply() } catch (_: Exception) {}
            mutableStateOf(initial)
        } else {
            mutableStateOf(saved)
        }
    }

    LaunchedEffect(Unit) {
        userSubjects = try { SubjectManager.getUserSubjects(context) } catch (_: Exception) { emptyList() }
    }

    val totalSeconds = TimerService.remainingSeconds.intValue

    var streakDays by remember { mutableIntStateOf(0) }
    LaunchedEffect(isFocusActive) {
        if (!isFocusActive) {
            val sessions = try { FocusSessionManager.getAllSessions(context) } catch (_: Exception) { emptyList() }
            streakDays = calculateStreakDays(sessions)
        }
    }

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
    val swMilliText = String.format(".%02d", swMs)

    val activeSortedSubjects = remember(userSubjects, hiddenSubjects) {
        userSubjects.filter { it !in hiddenSubjects }
    }

    val onAddSubjectAction = {
        if (newSubjectInput.isNotBlank()) {
            val clean = newSubjectInput.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            if (SubjectManager.addSubject(context, clean)) {
                userSubjects = SubjectManager.getUserSubjects(context)
                if (clean in hiddenSubjects) {
                    val newHidden = hiddenSubjects - clean
                    hiddenSubjects = newHidden
                    subjectPrefs.edit().putStringSet("hidden_subjects", newHidden).apply()
                }
                onSelectedSubjectChange(clean)
                newSubjectInput = ""
            }
        }
    }

    val cancelColor = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isFocusActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AMON",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF1C1917),
                        letterSpacing = 2.5.sp
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
                            .border(1.dp, glassBorder, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StreakFlame(streakDays = streakDays, isLarge = false, modifier = Modifier.size(20.dp))
                            Text(
                                text = "$streakDays D",
                                color = if (streakDays > 0) Color(0xFFFB923C) else textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(cardBg)
                                    .border(1.2.dp, goldColor, RoundedCornerShape(50))
                                    .clickable { showAddDialog = true }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                            ) {
                                Text("+ Subject", color = goldColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            val isSelected = selectedSubjectName.equals("All", ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) goldColor else cardBg)
                                    .border(1.dp, if (isSelected) goldColor else glassBorder, RoundedCornerShape(50))
                                    .clickable { onSelectedSubjectChange("All") }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All",
                                    color = if (isSelected) (if (isDark) Color.Black else Color.White) else textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        items(activeSortedSubjects) { subj ->
                            val isSelected = selectedSubjectName.equals(subj, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) goldColor else cardBg)
                                    .border(1.dp, if (isSelected) goldColor else glassBorder, RoundedCornerShape(50))
                                    .clickable { onSelectedSubjectChange(subj) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = subj,
                                    color = if (isSelected) (if (isDark) Color.Black else Color.White) else textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimerWatchesContainer(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = { nextMins ->
                            if (!isSessionActive) {
                                dialMinutes = nextMins
                                val secs = nextMins.toInt() * 60
                                TimerService.remainingSeconds.intValue = secs
                                onInitialTotalSecondsChange(secs)
                            }
                        },
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning || isStopwatchRunning,
                        isCustomMode = false,
                        onCustomModeToggle = { if (!isSessionActive) showFocusLauncherSheet = true },
                        goldColor = goldColor,
                        glowYellow = glowYellow,
                        cardBg = cardBg,
                        glassBorder = glassBorder,
                        textMain = textMain,
                        textMuted = textMuted,
                        isDark = isDark,
                        timeFormatted = timeFormatted,
                        hours = hours,
                        selectedSubjectName = selectedSubjectName
                    )
                }

                if (!isSessionActive) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(48.dp)
                            .shadow(12.dp, RoundedCornerShape(50), spotColor = goldColor, ambientColor = goldColor)
                            .clip(RoundedCornerShape(50))
                            .background(goldColor)
                            .border(1.dp, glowYellow, RoundedCornerShape(50))
                            .clickable { showFocusLauncherSheet = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "▶", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text(
                                text = "START FOCUS",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            if (activeMode == FocusModeType.STOPWATCH) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Text(
                            text = "$selectedSubjectName • Stopwatch",
                            color = textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                .border(1.dp, glassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                        Text(text = swMainTime, color = textMain, fontSize = 52.sp, fontWeight = FontWeight.Black)
                        Text(text = swMilliText, color = goldColor, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp))
                    }

                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    if (isStopwatchRunning) onPauseStopwatch() else onResumeStopwatch()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(text = if (isStopwatchRunning) "⏸ Pause" else "▶ Resume", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                            }

                            OutlinedButton(
                                onClick = onDirectEndSession,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.2.dp, cancelColor),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("End Session", color = cancelColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isBreakActive) "☕ Break Time" else selectedSubjectName,
                                color = if (isBreakActive) Color(0xFF10B981) else textMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (activeMode == FocusModeType.POMODORO) {
                        AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200))
                        ) {
                            PomodoroSessionTracker(
                                totalSessions = pomodoroConfig.totalRounds,
                                currentSessionIndex = currentSessionIndex,
                                isBreakActive = isBreakActive,
                                isRunning = isRunning,
                                goldColor = goldColor,
                                glowYellow = glowYellow,
                                isDark = isDark
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimerWatchesContainer(
                            dialMinutes = dialMinutes,
                            onDialMinutesChange = {},
                            totalSeconds = totalSeconds,
                            initialTotalSeconds = initialTotalSeconds,
                            isRunning = true,
                            isCustomMode = false,
                            onCustomModeToggle = {},
                            goldColor = if (isBreakActive) Color(0xFF10B981) else goldColor,
                            glowYellow = if (isBreakActive) Color.Transparent else glowYellow,
                            cardBg = cardBg,
                            glassBorder = glassBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            isDark = isDark,
                            timeFormatted = timeFormatted,
                            hours = hours,
                            selectedSubjectName = selectedSubjectName
                        )
                    }

                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        if (isBreakActive) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        FocusSoundManager.triggerHaptic(context, false)
                                        context.startService(Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_SKIP_BREAK
                                        })
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1.2f).height(48.dp)
                                ) {
                                    Text("⏭ Skip Break", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        FocusSoundManager.triggerHaptic(context, false)
                                        val act = if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                                        context.startService(Intent(context, TimerService::class.java).apply { action = act })
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(if (isPaused) "▶ Resume" else "⏸ Pause", color = Color.Black, fontWeight = FontWeight.Black)
                                }

                                OutlinedButton(
                                    onClick = onDirectEndSession,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.2.dp, cancelColor),
                                    modifier = Modifier.weight(0.9f).height(48.dp)
                                ) {
                                    Text("End", color = cancelColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Button(
                                    onClick = {
                                        FocusSoundManager.triggerHaptic(context, false)
                                        val act = if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                                        context.startService(Intent(context, TimerService::class.java).apply { action = act })
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(text = if (isPaused) "Resume 🎯" else "⏸ Pause", color = Color.Black, fontWeight = FontWeight.Black)
                                }

                                OutlinedButton(
                                    onClick = onDirectEndSession,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.2.dp, cancelColor),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("End Session", color = cancelColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFocusLauncherSheet) {
            FocusLauncherBottomSheet(
                isDark = isDark,
                goldColor = goldColor,
                cardBg = cardBg,
                textMain = textMain,
                textMuted = textMuted,
                initialMode = activeMode,
                availableSubjects = userSubjects,
                selectedSubject = selectedSubjectName,
                onSubjectChange = { onSelectedSubjectChange(it) },
                onStartFocus = { chosenMode, durationSeconds, pConfig ->
                    onActiveModeChange(chosenMode)
                    if (pConfig != null) {
                        onPomodoroConfigChange(pConfig)
                    }

                    when (chosenMode) {
                        FocusModeType.STOPWATCH -> {
                            onStartStopwatch()
                        }
                        FocusModeType.TIMER -> {
                            onInitialTotalSecondsChange(durationSeconds)
                            TimerService.remainingSeconds.intValue = durationSeconds
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_SECONDS, durationSeconds)
                                putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                putExtra(TimerService.EXTRA_MODE, "TIMER")
                            })
                        }
                        FocusModeType.POMODORO -> {
                            onInitialTotalSecondsChange(durationSeconds)
                            TimerService.remainingSeconds.intValue = durationSeconds
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_SECONDS, durationSeconds)
                                putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                putExtra(TimerService.EXTRA_MODE, "POMODORO")
                                if (pConfig != null) {
                                    putExtra(TimerService.EXTRA_POMO_SHORT_BREAK, pConfig.shortBreakMinutes)
                                    putExtra(TimerService.EXTRA_POMO_LONG_BREAK, pConfig.longBreakMinutes)
                                    putExtra(TimerService.EXTRA_POMO_TOTAL_ROUNDS, pConfig.totalRounds)
                                    putExtra(TimerService.EXTRA_POMO_AUTO_START, pConfig.autoStartNextFocus)
                                }
                            })
                        }
                    }
                },
                onDismiss = { showFocusLauncherSheet = false }
            )
        }

        if (showAddDialog) {
            Dialog(onDismissRequest = {
                showAddDialog = false
                newSubjectInput = ""
            }) {
                val frostedDialogBg = if (isDark) Color(0xEB161620) else Color(0xF2FFFFFF)
                val suggestedSubjects = listOf("Physics", "Chemistry", "Biology", "Maths", "History", "Polity")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(frostedDialogBg)
                        .border(1.5.dp, if (isDark) Color(0x44FFFFFF) else Color(0x33000000), RoundedCornerShape(26.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Manage Subjects",
                                color = goldColor,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                                    .clickable {
                                        showAddDialog = false
                                        newSubjectInput = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Tap to Show (Color) or Hide (Plain)",
                            color = textMuted,
                            fontSize = 12.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(suggestedSubjects) { suggestion ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            newSubjectInput = suggestion
                                            onAddSubjectAction()
                                        },
                                    color = goldColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, goldColor.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "+ $suggestion",
                                        color = goldColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            userSubjects.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { subj ->
                                        val isHidden = subj in hiddenSubjects
                                        val chipBg = if (!isHidden) {
                                            goldColor.copy(alpha = 0.22f)
                                        } else {
                                            if (isDark) Color(0x22FFFFFF) else Color(0xFFF1F5F9)
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(chipBg)
                                                .border(
                                                    1.2.dp,
                                                    if (!isHidden) goldColor else glassBorder,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    val newHidden = if (isHidden) {
                                                        hiddenSubjects - subj
                                                    } else {
                                                        hiddenSubjects + subj
                                                    }
                                                    hiddenSubjects = newHidden
                                                    subjectPrefs.edit()
                                                        .putStringSet("hidden_subjects", newHidden)
                                                        .apply()

                                                    if (!isHidden && selectedSubjectName.equals(subj, ignoreCase = true)) {
                                                        onSelectedSubjectChange("All")
                                                    }
                                                }
                                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = if (!isHidden) "● $subj" else "○ $subj",
                                                color = if (!isHidden) goldColor else (if (isDark) textMuted else Color(0xFF475569)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFF8FAFC))
                                .border(1.dp, glassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubjectInput,
                                onValueChange = { input ->
                                    newSubjectInput = input.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    }
                                },
                                placeholder = {
                                    Text("Type custom subject...", color = textMuted, fontSize = 13.sp)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onAddSubjectAction() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = textMain,
                                    unfocusedTextColor = textMain,
                                    cursorColor = goldColor
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(goldColor)
                                    .clickable { onAddSubjectAction() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Add",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
