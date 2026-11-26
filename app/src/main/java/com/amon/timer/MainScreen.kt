package com.amon.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.draw.drawBehind
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

private fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun openOverlayPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
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
                    Switch(
                        checked = blockConfig.blockNotificationsDuringFocus,
                        onCheckedChange = { checked ->
                            blockConfig = blockConfig.copy(blockNotificationsDuringFocus = checked)
                            BlockSettingsManager.saveConfig(context, blockConfig)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = goldColor
                        )
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
                    Switch(
                        checked = blockConfig.blockAppsDuringBreak,
                        onCheckedChange = { checked ->
                            blockConfig = blockConfig.copy(blockAppsDuringBreak = checked)
                            BlockSettingsManager.saveConfig(context, blockConfig)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = goldColor
                        )
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

    var isAccessibilityReady by remember {
        mutableStateOf(BlockAppManager.isAccessibilityServiceEnabled(context))
    }
    var isOverlayReady by remember {
        mutableStateOf(hasOverlayPermission(context))
    }
    var isBlockingEnabled by remember {
        mutableStateOf(BlockAppManager.isBlockingEnabled(context))
    }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember {
        mutableStateOf(BlockAppManager.getLaunchableApps(context))
    }

    fun refreshBlockTabState() {
        isAccessibilityReady = BlockAppManager.isAccessibilityServiceEnabled(context)
        isOverlayReady = hasOverlayPermission(context)
        isBlockingEnabled = BlockAppManager.isBlockingEnabled(context)
        installedApps = BlockAppManager.getLaunchableApps(context)
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

    val permissionsGranted = isAccessibilityReady && isOverlayReady

    if (!permissionsGranted) {
        AmonBlockPreviewFlow(
            isDark = isDark,
            cardBg = cardBg,
            glassBorder = glassBorder,
            textMain = textMain,
            textMuted = textMuted,
            goldColor = goldColor,
            isOverlayReady = isOverlayReady,
            onEnableClick = {
                if (!isOverlayReady) {
                    openOverlayPermissionSettings(context)
                } else {
                    BlockAppManager.setBlockingEnabled(context, true)
                    isBlockingEnabled = true
                    BlockAppManager.openAccessibilitySettings(context)
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

    val blockedCount = installedApps.count { it.isBlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = "$blockedCount apps selected for focus",
                    color = textMuted,
                    fontSize = 12.sp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cardBg)
                    .border(1.dp, goldColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .clickable { onOpenBlockSettings() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = goldColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Block Settings",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // App-level On/Off toggle so user can turn shield on/off directly inside the app
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isBlockingEnabled) goldColor.copy(alpha = 0.14f) else cardBg)
                .border(
                    1.dp,
                    if (isBlockingEnabled) goldColor.copy(alpha = 0.7f) else glassBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(goldColor.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = goldColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBlockingEnabled) "App Blocker On" else "App Blocker Off",
                        color = textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBlockingEnabled) {
                            "Shield active during focus sessions"
                        } else {
                            "Shield paused inside app"
                        },
                        color = textMuted,
                        fontSize = 11.5.sp
                    )
                }
            }

            Switch(
                checked = isBlockingEnabled,
                onCheckedChange = { checked ->
                    BlockAppManager.setBlockingEnabled(context, checked)
                    isBlockingEnabled = checked
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = goldColor
                )
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
                    fontSize = 13.5.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = goldColor
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textMain,
                unfocusedTextColor = textMain,
                cursorColor = goldColor,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = goldColor,
                unfocusedBorderColor = glassBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 14.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val iconBitmap = remember(app.packageName) {
                    app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (app.isBlocked) goldColor.copy(alpha = 0.12f) else cardBg)
                        .border(
                            1.dp,
                            if (app.isBlocked) goldColor.copy(alpha = 0.6f) else glassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            BlockAppManager.toggleAppBlocked(context, app.packageName)
                            installedApps = BlockAppManager.getLaunchableApps(context)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(goldColor.copy(alpha = 0.16f))
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    color = goldColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = app.appName,
                            color = textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Switch(
                        checked = app.isBlocked,
                        onCheckedChange = {
                            BlockAppManager.toggleAppBlocked(context, app.packageName)
                            installedApps = BlockAppManager.getLaunchableApps(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = goldColor
                        )
                    )
                }
            }
        }
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
    isOverlayReady: Boolean,
    onEnableClick: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            currentStep = (currentStep + 1) % 3
        }
    }

    val stepLabel = when (currentStep) {
        0 -> "1. Allow Accessibility"
        1 -> "2. Select Apps"
        else -> "3. During Focus"
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
                text = "Allow Accessibility, choose apps to block,\nand stay focused during sessions",
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
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(256.dp)
                    .height(346.dp)
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    goldColor.copy(alpha = 0.25f),
                                    goldColor.copy(alpha = 0.07f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.maxDimension * 0.65f
                            ),
                            cornerRadius = CornerRadius(34.dp.toPx(), 34.dp.toPx())
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(cardBg)
                        .border(1.5.dp, glassBorder, RoundedCornerShape(28.dp))
                        .padding(14.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                        },
                        label = "block_preview_step",
                        modifier = Modifier.fillMaxSize()
                    ) { step ->
                        when (step) {
                            0 -> BlockPreviewStepOne(goldColor, textMain, textMuted, isDark)
                            1 -> BlockPreviewStepTwo(goldColor, textMain, textMuted, isDark)
                            else -> BlockPreviewStepThree(goldColor, textMain, textMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val selected = currentStep == idx
                    val width by animateFloatAsState(
                        targetValue = if (selected) 22f else 8f,
                        animationSpec = tween(250),
                        label = "block_dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width.dp)
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
                    text = if (!isOverlayReady) "Allow Overlay Permission" else "Allow Accessibility",
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
    val toggle by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "step1_toggle"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Accessibility", color = textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("MORE", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
                        text = "Amon App Blocker",
                        color = textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Blocks selected distracting apps during focus",
                        color = textMuted,
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(if (toggle > 0.5f) goldColor else Color(0xFF3A3A46))
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
    val infiniteTransition = rememberInfiniteTransition(label = "step3_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "step3_pulse_val"
    )

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
                    radius = (size.minDimension * 0.45f) * pulse,
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
