package com.amon.timer

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
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
            lineTo(center.x - arcRadius * 0.25f, center.y + arcRadius * 0.62f)
            close()
        }
        drawPath(path = arrowPath2, color = tint)
    }
}

@Composable
private fun AmonSlimPillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    goldColor: Color
) {
    val trackColor = if (checked) goldColor else Color(0xFF3F3F46)
    val thumbColor = if (checked) Color.Black else Color(0xFFD1D5DB)

    Box(
        modifier = Modifier
            .width(38.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    var currentNavIndex by remember { mutableIntStateOf(0) }

    var showStreakBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showBlockSettingsSheet by rememberSaveable { mutableStateOf(false) }

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

    var areControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, areControlsVisible) {
        if (isRunning && areControlsVisible) {
            delay(4000L)
            areControlsVisible = false
        }
    }

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
        FocusSoundManager.playCompletionSound(context)
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
            isLandscape = isLandscape,
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
            onNavigateToStats = { showStreakBottomSheet = true },
            onStartStopwatch = {
                FocusSoundManager.playCompletionSound(context)
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
                    areControlsVisible = !areControlsVisible
                }
        ) {
            focusScreenContent()

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
                        isLandscape = false,
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
                        onNavigateToStats = { showStreakBottomSheet = true },
                        onStartStopwatch = {
                            FocusSoundManager.playCompletionSound(context)
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
                    1 -> StatsScreen()
                    2 -> AmonBlockTabSection(
                        isDark = isDark,
                        cardBg = cardBg,
                        glassBorder = glassBorder,
                        textMain = textMain,
                        textMuted = textMuted,
                        goldColor = goldColor,
                        onOpenBlockSettings = { showBlockSettingsSheet = true }
                    )
                    else -> ProfileScreen()
                }
            }
        }
    }

    if (showBlockSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var blockConfig by remember { mutableStateOf(BlockSettingsManager.loadConfig(context)) }

        ModalBottomSheet(
            onDismissRequest = { showBlockSettingsSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF16151F) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = textMuted.copy(alpha = 0.4f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Block Settings",
                    color = goldColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Block Notifications",
                            color = textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Silence distracting alerts during focus",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    AmonSlimPillSwitch(
                        checked = blockConfig.blockNotificationsDuringFocus,
                        onCheckedChange = { checked ->
                            blockConfig = blockConfig.copy(blockNotificationsDuringFocus = checked)
                            BlockSettingsManager.saveConfig(context, blockConfig)
                        },
                        goldColor = goldColor
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Block Apps During Break",
                            color = textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Keep blocked apps locked during break time",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    AmonSlimPillSwitch(
                        checked = blockConfig.blockAppsDuringBreak,
                        onCheckedChange = { checked ->
                            blockConfig = blockConfig.copy(blockAppsDuringBreak = checked)
                            BlockSettingsManager.saveConfig(context, blockConfig)
                        },
                        goldColor = goldColor
                    )
                }
            }
        }
    }

    if (showStreakBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStreakBottomSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF16151F) else Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = textMuted.copy(alpha = 0.4f))
            }
        ) {
            StreakBottomSheetContent(
                context = context,
                isDark = isDark,
                cardBg = cardBg,
                textMain = textMain,
                textMuted = textMuted,
                goldColor = goldColor,
                glassBorder = glassBorder,
                onDismiss = { showStreakBottomSheet = false }
            )
        }
    }

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

@Composable
private fun AmonBlockTabSection(
    isDark: Boolean,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    goldColor: Color,
    onOpenBlockSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isUsageReady by remember {
        mutableStateOf(BlockAppManager.hasUsageStatsPermission(context))
    }
    var isOverlayReady by remember {
        mutableStateOf(BlockAppManager.hasOverlayPermission(context))
    }
    var isBlockingEnabled by remember {
        mutableStateOf(BlockAppManager.isBlockingEnabled(context))
    }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember {
        mutableStateOf(BlockAppManager.getLaunchableApps(context))
    }

    fun refreshBlockTabState() {
        isUsageReady = BlockAppManager.hasUsageStatsPermission(context)
        isOverlayReady = BlockAppManager.hasOverlayPermission(context)
        isBlockingEnabled = BlockAppManager.isBlockingEnabled(context)
        installedApps = BlockAppManager.getLaunchableApps(context)
        if (isUsageReady && isOverlayReady) {
            BlockAppManager.startBlockerService(context)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshBlockTabState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionsGranted = isUsageReady && isOverlayReady

    if (!permissionsGranted) {
        AmonBlockPreviewFlow(
            isDark = isDark,
            cardBg = cardBg,
            glassBorder = glassBorder,
            textMain = textMain,
            textMuted = textMuted,
            goldColor = goldColor,
            isUsageReady = isUsageReady,
            isOverlayReady = isOverlayReady,
            onEnableClick = {
                if (!isUsageReady) {
                    BlockAppManager.openUsageAccessSettings(context)
                } else if (!isOverlayReady) {
                    BlockAppManager.openOverlayPermissionSettings(context)
                }
            }
        )
        return
    }

    val filteredApps = remember(installedApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) installedApps
        else installedApps.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    val selectedBlockedApps = remember(filteredApps) {
        filteredApps.filter { it.isBlocked }.sortedBy { it.appName.lowercase() }
    }

    val unselectedApps = remember(filteredApps) {
        filteredApps.filter { !it.isBlocked }.sortedBy { it.appName.lowercase() }
    }

    val totalBlockedCount = installedApps.count { it.isBlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Block",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF1C1917)
                )
                Text(
                    text = "$totalBlockedCount apps selected for focus",
                    color = textMuted,
                    fontSize = 11.5.sp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cardBg)
                    .border(1.dp, goldColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .clickable { onOpenBlockSettings() }
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = goldColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Block Settings",
                        color = goldColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isBlockingEnabled) goldColor.copy(alpha = 0.14f) else cardBg)
                .border(
                    0.8.dp,
                    if (isBlockingEnabled) goldColor.copy(alpha = 0.7f) else glassBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isBlockingEnabled) "App Blocker On • Active" else "App Blocker Off • Paused",
                    color = textMain,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AmonSlimPillSwitch(
                checked = isBlockingEnabled,
                onCheckedChange = { checked ->
                    BlockAppManager.setBlockingEnabled(context, checked)
                    isBlockingEnabled = checked
                    if (checked) {
                        BlockAppManager.startBlockerService(context)
                    }
                },
                goldColor = goldColor
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search apps...",
                    color = textMuted,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = goldColor,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        color = textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { searchQuery = "" }
                            .padding(8.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textMain,
                unfocusedTextColor = textMain,
                cursorColor = goldColor,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = goldColor,
                unfocusedBorderColor = glassBorder.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 14.dp)
        ) {
            item(key = "header_selected_apps") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Apps",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "${selectedBlockedApps.size}",
                        color = textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedBlockedApps.isEmpty()) {
                item(key = "empty_selected_apps") {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No apps selected yet. Toggle any app below to block during focus."
                        } else {
                            "No blocked apps match \"$searchQuery\"."
                        },
                        color = textMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(selectedBlockedApps, key = { "blocked_${it.packageName}" }) { app ->
                    BlockAppRowItem(
                        app = app,
                        glassBorder = glassBorder,
                        textMain = textMain,
                        goldColor = goldColor,
                        onToggle = {
                            BlockAppManager.toggleAppBlocked(context, app.packageName)
                            installedApps = BlockAppManager.getLaunchableApps(context)
                        }
                    )
                }
            }

            item(key = "header_all_apps") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Apps",
                        color = textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "${unselectedApps.size}",
                        color = textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(unselectedApps, key = { "available_${it.packageName}" }) { app ->
                BlockAppRowItem(
                    app = app,
                    glassBorder = glassBorder,
                    textMain = textMain,
                    goldColor = goldColor,
                    onToggle = {
                        BlockAppManager.toggleAppBlocked(context, app.packageName)
                        installedApps = BlockAppManager.getLaunchableApps(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockAppRowItem(
    app: InstalledBlockableApp,
    glassBorder: Color,
    textMain: Color,
    goldColor: Color,
    onToggle: () -> Unit
) {
    val iconBitmap = remember(app.packageName) {
        app.icon?.toBitmap(width = 80, height = 80)?.asImageBitmap()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(goldColor.copy(alpha = 0.14f))
                    ) {
                        Text(
                            text = app.appName.take(1).uppercase(),
                            color = goldColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = app.appName,
                    color = textMain,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AmonSlimPillSwitch(
                checked = app.isBlocked,
                onCheckedChange = { onToggle() },
                goldColor = goldColor
            )
        }

        HorizontalDivider(
            thickness = 0.6.dp,
            color = glassBorder.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun AmonBlockPreviewFlow(
    isDark: Boolean,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    goldColor: Color,
    isUsageReady: Boolean,
    isOverlayReady: Boolean,
    onEnableClick: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val stepLabel = when (currentStep) {
        0 -> "1. Usage Permission"
        1 -> "2. Display Over Apps"
        else -> "3. During Focus"
    }

    val buttonLabel = when {
        !isUsageReady -> "Allow Usage Permission"
        !isOverlayReady -> "Allow Display Over Apps"
        else -> "Continue"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "App Blocker",
                color = textMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Allow Usage & Display over other apps\nto block distractions during focus",
                color = textMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(goldColor.copy(alpha = 0.14f))
                    .border(1.dp, goldColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = stepLabel,
                    color = goldColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(248.dp)
                    .height(330.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBg)
                    .border(1.5.dp, glassBorder, RoundedCornerShape(28.dp))
                    .padding(14.dp)
            ) {
                when (currentStep) {
                    0 -> BlockPreviewStepOne(goldColor, textMain, textMuted, isDark)
                    1 -> BlockPreviewStepTwo(goldColor, textMain, textMuted, isDark)
                    else -> BlockPreviewStepThree(goldColor, textMain, textMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val selected = currentStep == idx
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (selected) 22.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) goldColor else textMuted.copy(alpha = 0.35f))
                            .clickable { currentStep = idx }
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEnableClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goldColor,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Trusted by students",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun BlockPreviewStepOne(
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Usage Access", color = textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("PERMISSIONS", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF26231C) else goldColor.copy(alpha = 0.14f))
                .border(1.2.dp, goldColor.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Amon Usage Access",
                        color = textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detects active apps during focus sessions",
                        color = textMuted,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(goldColor)
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockPreviewStepTwo(
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean
) {
    val itemsList = listOf(
        Triple("Social", Color(0xFFE1306C), true),
        Triple("Video", Color(0xFFFF0000), true),
        Triple("Games", Color(0xFF3B82F6), false)
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("Select Apps", color = textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        itemsList.forEach { (label, color, checked) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (checked) {
                            if (isDark) Color(0xFF26231C) else goldColor.copy(alpha = 0.14f)
                        } else {
                            if (isDark) Color(0xFF22222A) else Color(0xFFF1F5F9)
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(label, color = textMain, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(if (checked) goldColor else Color(0xFF3A3A46))
                        .padding(2.dp),
                    contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (checked) Color.Black else Color(0xFFA1A1AA))
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockPreviewStepThree(
    goldColor: Color,
    textMain: Color,
    textMuted: Color
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(122.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = goldColor.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.45f,
                    center = c,
                    style = Stroke(width = 1.3.dp.toPx())
                )
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
                            )
                        )
                ) {
                    Text("APP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(goldColor)
                        .border(2.dp, Color(0xFF1A1A20), CircleShape)
                ) {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        val w = size.width
                        val h = size.height
                        drawRoundRect(
                            color = Color.Black,
                            topLeft = Offset(w * 0.22f, h * 0.46f),
                            size = Size(w * 0.56f, h * 0.42f),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
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
                            color = Color.Black,
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Focus Session Active",
                color = textMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This app is blocked during focus",
                color = textMuted,
                fontSize = 10.sp
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(goldColor)
        ) {
            Text(
                text = "Return to AMON",
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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
    isLandscape: Boolean = false,
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
    onNavigateToStats: () -> Unit = {},
    onStartStopwatch: () -> Unit,
    onPauseStopwatch: () -> Unit,
    onResumeStopwatch: () -> Unit,
    onDirectEndSession: () -> Unit
) {
    var userSubjects by remember { mutableStateOf(listOf<String>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    var suggestionPool by remember {
        mutableStateOf(
            listOf(
                "Physics", "Chemistry", "Biology", "Maths", "History", "Polity",
                "Geography", "Economics", "English", "Coding", "Current Affairs", "Reasoning"
            )
        )
    }

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
    var todayStudiedMinutes by remember { mutableIntStateOf(0) }

    var dailyGoalMinutes by remember {
        val savedHours = try {
            context.getSharedPreferences("amon_stats_goals", Context.MODE_PRIVATE)
                .getFloat("daily_goal_hours", 0f)
        } catch (_: Exception) {
            0f
        }
        mutableIntStateOf(if (savedHours > 0f) (savedHours * 60).toInt() else 30)
    }

    LaunchedEffect(isFocusActive, TimerService.showCompletionDialogTrigger.value) {
        val updatedHours = try {
            context.getSharedPreferences("amon_stats_goals", Context.MODE_PRIVATE)
                .getFloat("daily_goal_hours", 0f)
        } catch (_: Exception) {
            0f
        }
        dailyGoalMinutes = if (updatedHours > 0f) (updatedHours * 60).toInt() else 30

        if (!isFocusActive) {
            val sessions = try { FocusSessionManager.getAllSessions(context) } catch (_: Exception) { emptyList() }
            streakDays = calculateStreakDays(sessions)
            todayStudiedMinutes = calculateTodayMinutesFromSessions(sessions)
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

    val availableSuggestions = remember(suggestionPool, userSubjects) {
        val existingLower = userSubjects.map { it.lowercase(Locale.getDefault()) }.toSet()
        suggestionPool.filterNot { it.lowercase(Locale.getDefault()) in existingLower }
    }

    val selectAndAddSubject: (String) -> Unit = { rawName ->
        val clean = rawName.trim().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        if (clean.isNotBlank()) {
            SubjectManager.addSubject(context, clean)
            userSubjects = SubjectManager.getUserSubjects(context)

            val updatedPool = suggestionPool.filterNot {
                it.equals(clean, ignoreCase = true)
            }.toMutableList()
            if (clean !in userSubjects && updatedPool.none { it.equals(clean, ignoreCase = true) }) {
                updatedPool.add(clean)
            }
            suggestionPool = updatedPool

            if (clean in hiddenSubjects) {
                val newHidden = hiddenSubjects - clean
                hiddenSubjects = newHidden
                subjectPrefs.edit().putStringSet("hidden_subjects", newHidden).apply()
            }
            onSelectedSubjectChange(clean)
        }
    }

    val onAddSubjectAction: () -> Unit = {
        if (newSubjectInput.isNotBlank()) {
            val typed = newSubjectInput.trim()
            selectAndAddSubject(typed)
            newSubjectInput = ""
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
                            .clickable { onNavigateToStats() }
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
                            AssistChip(
                                onClick = {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    showAddDialog = true
                                },
                                label = {
                                    Text(
                                        text = "+ Subject",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = cardBg,
                                    labelColor = goldColor
                                ),
                                border = BorderStroke(1.2.dp, goldColor)
                            )
                        }

                        item {
                            val isSelected = selectedSubjectName.equals("All", ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    onSelectedSubjectChange("All")
                                },
                                label = {
                                    Text(
                                        text = "All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = cardBg,
                                    labelColor = textMuted,
                                    selectedContainerColor = goldColor,
                                    selectedLabelColor = if (isDark) Color.Black else Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = glassBorder,
                                    selectedBorderColor = goldColor
                                )
                            )
                        }

                        items(activeSortedSubjects, key = { it }) { subj ->
                            val isSelected = selectedSubjectName.equals(subj, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    onSelectedSubjectChange(subj)
                                },
                                label = {
                                    Text(
                                        text = subj,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = cardBg,
                                    labelColor = textMuted,
                                    selectedContainerColor = goldColor,
                                    selectedLabelColor = if (isDark) Color.Black else Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = glassBorder,
                                    selectedBorderColor = goldColor
                                )
                            )
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
                    val safeGoal = if (dailyGoalMinutes > 0) dailyGoalMinutes else 30
                    val goalFraction = (todayStudiedMinutes.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
                    val goalPercent = ((todayStudiedMinutes.toFloat() / safeGoal.toFloat()) * 100).toInt().coerceAtMost(100)
                    val isGoalCompleted = todayStudiedMinutes >= safeGoal

                    val animatedFraction by animateFloatAsState(
                        targetValue = goalFraction,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        label = "goal_progress_fill"
                    )

                    val infiniteTransition = rememberInfiniteTransition(label = "goal_bar_anim")
                    val shimmerOffset by infiniteTransition.animateFloat(
                        initialValue = -0.35f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "goal_shimmer"
                    )
                    val goalPulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.75f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "goal_complete_pulse"
                    )

                    val goalTextFormatted = if (safeGoal >= 60) {
                        val h = safeGoal / 60
                        val m = safeGoal % 60
                        if (m == 0) "${h}h" else "${h}h ${m}m"
                    } else {
                        "${safeGoal}m"
                    }

                    val studiedTextFormatted = if (todayStudiedMinutes >= 60) {
                        val h = todayStudiedMinutes / 60
                        val m = todayStudiedMinutes % 60
                        if (m == 0) "${h}h" else "${h}h ${m}m"
                    } else {
                        "${todayStudiedMinutes}m"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Focus: $studiedTextFormatted / $goalTextFormatted",
                                color = textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$goalPercent%",
                                color = goldColor.copy(alpha = if (isGoalCompleted) goalPulseAlpha else 1f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0))
                        ) {
                            if (animatedFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(goldColor.copy(alpha = if (isGoalCompleted) goalPulseAlpha else 1f))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val highlightWidth = size.width * 0.35f
                                        val startX = size.width * shimmerOffset
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                ),
                                                startX = startX,
                                                endX = startX + highlightWidth
                                            ),
                                            size = size
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                if (isLandscape) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "$selectedSubjectName • Stopwatch",
                                color = textMain,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = swMainTime, color = textMain, fontSize = 48.sp, fontWeight = FontWeight.Black)
                            Text(text = swMilliText, color = goldColor, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 5.dp))
                        }

                        AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isStopwatchRunning) {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            onPauseStopwatch()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.width(120.dp).height(38.dp)
                                    ) {
                                        Text("Pause", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            onResumeStopwatch()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.width(120.dp).height(38.dp)
                                    ) {
                                        Text("Resume", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = onDirectEndSession,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, cancelColor),
                                        modifier = Modifier.width(120.dp).height(38.dp)
                                    ) {
                                        Text("End", color = cancelColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(top = 10.dp, bottom = 44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.TopCenter)
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

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = swMainTime, color = textMain, fontSize = 52.sp, fontWeight = FontWeight.Black)
                            Text(text = swMilliText, color = goldColor, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp))
                        }

                        AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            if (isStopwatchRunning) {
                                Button(
                                    onClick = {
                                        FocusSoundManager.triggerHaptic(context, false)
                                        onPauseStopwatch()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth(0.75f).height(48.dp)
                                ) {
                                    Text("Pause", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            onResumeStopwatch()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Resume", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                    }

                                    OutlinedButton(
                                        onClick = onDirectEndSession,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.2.dp, cancelColor),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("End", color = cancelColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (isLandscape) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            AnimatedVisibility(
                                visible = areControlsVisible,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(200))
                            ) {
                                Text(
                                    text = if (isBreakActive) "Break Time" else selectedSubjectName,
                                    color = if (isBreakActive) Color(0xFF10B981) else textMain,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                        .border(1.dp, glassBorder, RoundedCornerShape(50))
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                )
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
                        }

                        Box(contentAlignment = Alignment.Center) {
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
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isBreakActive) {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            context.startService(Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_SKIP_BREAK
                                            })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.width(124.dp).height(38.dp)
                                    ) {
                                        Text("Skip Break", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onDirectEndSession,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, cancelColor),
                                        modifier = Modifier.width(124.dp).height(38.dp)
                                    ) {
                                        Text("End", color = cancelColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else if (!isPaused) {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            context.startService(Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_PAUSE
                                            })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.width(124.dp).height(38.dp)
                                    ) {
                                        Text("Pause", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            context.startService(Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_RESUME
                                            })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.width(124.dp).height(38.dp)
                                    ) {
                                        Text("Resume", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onDirectEndSession,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, cancelColor),
                                        modifier = Modifier.width(124.dp).height(38.dp)
                                    ) {
                                        Text("End", color = cancelColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(top = 10.dp, bottom = 44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedVisibility(
                            visible = areControlsVisible,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isBreakActive) "Break Time" else selectedSubjectName,
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
                                exit = fadeOut(tween(200)),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 36.dp)
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

                        Box(contentAlignment = Alignment.Center) {
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
                            exit = fadeOut(tween(200)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            if (isBreakActive) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        Text("Skip Break", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onDirectEndSession,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.2.dp, cancelColor),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("End", color = cancelColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (!isPaused) {
                                Button(
                                    onClick = {
                                        FocusSoundManager.triggerHaptic(context, false)
                                        context.startService(Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_PAUSE
                                        })
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth(0.75f).height(48.dp)
                                ) {
                                    Text("Pause", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Button(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            context.startService(Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_RESUME
                                            })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Resume", color = Color.Black, fontWeight = FontWeight.Black)
                                    }

                                    OutlinedButton(
                                        onClick = onDirectEndSession,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.2.dp, cancelColor),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("End", color = cancelColor, fontWeight = FontWeight.Bold)
                                    }
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
                    FocusSoundManager.playCompletionSound(context)
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
                            text = "Tap suggestion to move to Selected, or toggle visibility",
                            color = textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (availableSuggestions.isNotEmpty()) {
                            Text(
                                text = "Suggestions",
                                color = goldColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(availableSuggestions, key = { it }) { suggestion ->
                                    SuggestionChip(
                                        onClick = {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            selectAndAddSubject(suggestion)
                                        },
                                        label = {
                                            Text(
                                                text = "+ $suggestion",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = goldColor.copy(alpha = 0.12f),
                                            labelColor = goldColor
                                        ),
                                        border = BorderStroke(1.dp, goldColor.copy(alpha = 0.4f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(
                            text = "Selected Subjects",
                            color = textMain,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            userSubjects.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { subj ->
                                        val isHidden = subj in hiddenSubjects
                                        val isShown = !isHidden
                                        val chipBg = if (isShown) {
                                            goldColor.copy(alpha = 0.22f)
                                        } else {
                                            if (isDark) Color(0xFF1E1E28) else Color(0xFFF1F5F9)
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(chipBg)
                                                .border(
                                                    1.dp,
                                                    if (isShown) goldColor else glassBorder,
                                                    RoundedCornerShape(50)
                                                )
                                                .clickable {
                                                    FocusSoundManager.triggerHaptic(context, false)
                                                    val newHidden = if (isHidden) {
                                                        hiddenSubjects - subj
                                                    } else {
                                                        hiddenSubjects + subj
                                                    }
                                                    hiddenSubjects = newHidden
                                                    subjectPrefs.edit()
                                                        .putStringSet("hidden_subjects", newHidden)
                                                        .apply()

                                                    if (isHidden) {
                                                        onSelectedSubjectChange(subj)
                                                    } else if (selectedSubjectName.equals(subj, ignoreCase = true)) {
                                                        onSelectedSubjectChange("All")
                                                    }
                                                }
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = if (isShown) "✓ $subj" else "○ $subj",
                                                color = if (isShown) goldColor else (if (isDark) textMuted else Color(0xFF475569)),
                                                fontSize = 11.sp,
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

@Composable
fun StreakBottomSheetContent(
    context: Context,
    isDark: Boolean,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    goldColor: Color,
    glassBorder: Color,
    onDismiss: () -> Unit
) {
    val sessions = remember { FocusSessionManager.getAllSessions(context) }
    val streakDays = remember(sessions) { calculateStreakDays(sessions) }
    val todayMinutes = remember(sessions) { calculateTodayMinutesFromSessions(sessions) }
    val totalMinutes = remember(sessions) { sessions.sumOf { it.durationMinutes } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(goldColor.copy(alpha = 0.08f))
                .border(1.5.dp, goldColor.copy(alpha = 0.35f), CircleShape)
        ) {
            StreakFlame(
                streakDays = streakDays,
                isLarge = true,
                modifier = Modifier.size(92.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "$streakDays Days Streak",
            color = if (streakDays > 0) Color(0xFFFB923C) else textMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (todayMinutes >= 30) "Today's 30m streak goal is complete! Keep the flame alive."
                   else "Focus ${30 - todayMinutes} more mins today to extend your streak!",
            color = textMuted,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "LAST 7 DAYS (30m+ / day)",
                    color = textMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                val last7Days = remember(sessions) { getLast7DaysStreakStatus(sessions) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    last7Days.forEach { (dayLabel, isCompleted) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) Color(0xFFFB923C).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        1.2.dp,
                                        if (isCompleted) Color(0xFFFB923C) else glassBorder,
                                        CircleShape
                                    )
                            ) {
                                Text(
                                    text = if (isCompleted) "✓" else "•",
                                    color = if (isCompleted) Color(0xFFFB923C) else textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = dayLabel,
                                color = if (isCompleted) textMain else textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Today's Focus", color = textMuted, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("${todayMinutes}m", color = goldColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Total Focused", color = textMuted, fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("${totalMinutes / 60}h ${totalMinutes % 60}m", color = textMain, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = goldColor),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text("Keep Going!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

fun calculateTodayMinutesFromSessions(sessions: List<FocusSession>): Int {
    if (sessions.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayKey = sdf.format(Date())
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    )
    var total = 0
    sessions.forEach { s ->
        for (fmt in formats) {
            try {
                val d = fmt.parse(s.date)
                if (d != null) {
                    if (sdf.format(d) == todayKey) {
                        total += s.durationMinutes
                    }
                    break
                }
            } catch (_: Exception) {}
        }
    }
    return total
}

fun getLast7DaysStreakStatus(sessions: List<FocusSession>): List<Pair<String, Boolean>> {
    val dayMinutesMap = mutableMapOf<String, Int>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    )

    sessions.forEach { s ->
        for (fmt in formats) {
            try {
                val d = fmt.parse(s.date)
                if (d != null) {
                    val key = sdf.format(d)
                    dayMinutesMap[key] = (dayMinutesMap[key] ?: 0) + s.durationMinutes
                    break
                }
            } catch (_: Exception) {}
        }
    }

    val result = mutableListOf<Pair<String, Boolean>>()
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -6)
    for (i in 0..6) {
        val key = sdf.format(cal.time)
        val label = dayLabelFormat.format(cal.time).take(2)
        val mins = dayMinutesMap[key] ?: 0
        result.add(Pair(label, mins >= 30))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
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
