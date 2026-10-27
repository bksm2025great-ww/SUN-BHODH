package com.amon.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    var currentNavIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            ThemeManager.loadTheme(context)
            TimerService.syncRemainingTime(context)
        } catch (_: Exception) {}
    }

    val isDark = ThemeManager.isDarkTheme.value
    val isRunning = TimerService.isTimerRunning.value
    val isPaused = TimerService.isTimerPaused.value

    var isStopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var isStopwatchFocusActive by rememberSaveable { mutableStateOf(false) }
    var stopwatchBaseTime by rememberSaveable { mutableStateOf(0L) }
    var stopwatchAccumulatedMillis by rememberSaveable { mutableStateOf(0L) }
    var stopwatchElapsedMillis by rememberSaveable { mutableStateOf(0L) }

    LaunchedEffect(isStopwatchRunning) {
        if (isStopwatchRunning) {
            stopwatchBaseTime = SystemClock.elapsedRealtime()
            while (isStopwatchRunning) {
                val now = SystemClock.elapsedRealtime()
                stopwatchElapsedMillis = stopwatchAccumulatedMillis + (now - stopwatchBaseTime)
                delay(30)
            }
        }
    }

    val isTimerFocusActive = isRunning || isPaused
    val isFocusActive = isTimerFocusActive || isStopwatchFocusActive

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

    DisposableEffect(isFocusActive) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isFocusActive) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            activity?.window?.let { win ->
                WindowCompat.getInsetsController(win, view).show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()

    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val glowBorder = goldColor.copy(alpha = 0.6f)

    val appPrefs = remember { context.getSharedPreferences("amon_app_prefs", Context.MODE_PRIVATE) }
    val currentVersion = remember {
        try {
            @Suppress("DEPRECATION")
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.1.0"
        } catch (_: Exception) {
            "1.1.0"
        }
    }
    var showWhatsNew by remember {
        val lastSeenVersion = try {
            appPrefs.getString("whats_new_seen_version", "")
        } catch (_: Exception) {
            ""
        }
        mutableStateOf(lastSeenVersion != currentVersion)
    }

    BackHandler(enabled = currentNavIndex != 0 && !isFocusActive) {
        currentNavIndex = 0
    }

    Scaffold(
        containerColor = bgColor,
        contentWindowInsets = if (isFocusActive) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (!isFocusActive) {
                AmonCurvedBottomBar(
                    selectedIndex = currentNavIndex,
                    onTabSelected = { newIndex -> currentNavIndex = newIndex },
                    isDark = isDark,
                    goldColor = goldColor,
                    cardBg = cardBg,
                    borderCol = glassBorder,
                    hasNewUpdate = hasNewUpdate
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .then(if (!isFocusActive) Modifier.padding(paddingValues) else Modifier)
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
                    isFocusActive = isFocusActive,
                    isStopwatchRunning = isStopwatchRunning,
                    isStopwatchFocusActive = isStopwatchFocusActive,
                    stopwatchElapsedMillis = stopwatchElapsedMillis,
                    onStartStopwatch = {
                        isStopwatchRunning = true
                        isStopwatchFocusActive = true
                    },
                    onPauseStopwatch = {
                        isStopwatchRunning = false
                        stopwatchAccumulatedMillis = stopwatchElapsedMillis
                    },
                    onResumeStopwatch = {
                        stopwatchBaseTime = SystemClock.elapsedRealtime()
                        isStopwatchRunning = true
                    },
                    onResetStopwatch = {
                        isStopwatchRunning = false
                        isStopwatchFocusActive = false
                        stopwatchAccumulatedMillis = 0L
                        stopwatchElapsedMillis = 0L
                    }
                )
                1 -> ForestScreen()
                2 -> StatsScreen()
                3 -> ProfileScreen()
            }

            if (showWhatsNew && !isFocusActive) {
                WhatsNewDialog(
                    versionName = currentVersion,
                    goldColor = goldColor,
                    glowYellow = glowBorder,
                    cardBg = cardBg,
                    textMain = textMain,
                    textMuted = textMuted,
                    onDismiss = {
                        try {
                            appPrefs.edit().putString("whats_new_seen_version", currentVersion).apply()
                        } catch (_: Exception) {}
                        showWhatsNew = false
                    }
                )
            }
        }
    }
}

// =============================================================================
// ☕ CLEAN CANVASS TEA / BREAK ICON
// =============================================================================
@Composable
fun CleanTeaCupIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val cupPath = Path().apply {
            moveTo(w * 0.18f, h * 0.32f)
            lineTo(w * 0.72f, h * 0.32f)
            cubicTo(w * 0.72f, h * 0.68f, w * 0.64f, h * 0.82f, w * 0.45f, h * 0.82f)
            cubicTo(w * 0.26f, h * 0.82f, w * 0.18f, h * 0.68f, w * 0.18f, h * 0.32f)
            close()
        }
        drawPath(cupPath, color = tint)

        val handlePath = Path().apply {
            moveTo(w * 0.70f, h * 0.40f)
            cubicTo(w * 0.90f, h * 0.40f, w * 0.90f, h * 0.65f, w * 0.68f, h * 0.65f)
        }
        drawPath(handlePath, color = tint, style = Stroke(width = 2.2.dp.toPx()))

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.86f),
            size = Size(w * 0.66f, h * 0.08f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

// =============================================================================
// 🎚️ 2 LINES + 2 DOTS PREFERENCE ICON
// =============================================================================
@Composable
fun SlidersPreferenceIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.10f, h * 0.32f),
            end = Offset(w * 0.90f, h * 0.32f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = 3.2.dp.toPx(),
            center = Offset(w * 0.35f, h * 0.32f)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.10f, h * 0.68f),
            end = Offset(w * 0.90f, h * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = 3.2.dp.toPx(),
            center = Offset(w * 0.68f, h * 0.68f)
        )
    }
}

// =============================================================================
// 🌟 0. BADA GOLA / CHHOTA GOLA SESSION TRACKER
// =============================================================================
@Composable
fun PomodoroSessionTracker(
    totalSessions: Int,
    currentSessionIndex: Int,
    isBreakActive: Boolean,
    isRunning: Boolean,
    goldColor: Color,
    glowYellow: Color,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_session")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isDark) Color(0xFF14141E) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(50))
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSessions) {
                val isCompleted = i < currentSessionIndex
                val isCurrent = i == currentSessionIndex
                val isUpcoming = i > currentSessionIndex

                // 🔴 1. BADA GOLA (Focus Session)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp)
                ) {
                    when {
                        isCompleted -> {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(goldColor)
                            )
                        }
                        isCurrent -> {
                            if (isBreakActive) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(CircleShape)
                                        .background(goldColor.copy(alpha = if (isRunning) pulseAlpha * 0.4f else 0.3f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                            }
                        }
                        isUpcoming -> {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .border(1.6.dp, if (isDark) Color(0xFF38384A) else Color(0xFF94A3B8), CircleShape)
                            )
                        }
                    }
                }

                // ⚪ 2. CHHOTA GOLA (Break Session)
                if (i < totalSessions) {
                    val isBreakCurrent = (i == currentSessionIndex && isBreakActive)
                    val isBreakDone = (i < currentSessionIndex)

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.8.dp)
                                .background(if (isBreakDone) goldColor.copy(alpha = 0.5f) else (if (isDark) Color(0xFF262636) else Color(0xFFCBD5E1)))
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isBreakCurrent) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isBreakCurrent -> Color(0xFF10B981)
                                        isBreakDone -> goldColor.copy(alpha = 0.6f)
                                        else -> if (isDark) Color(0xFF343446) else Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.8.dp)
                                .background(if (isBreakDone) goldColor.copy(alpha = 0.5f) else (if (isDark) Color(0xFF262636) else Color(0xFFCBD5E1)))
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 🟢 1. HOME TAB (CLEAN LAYOUT WITH BOTTOM SHEETS & PRESETS)
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
    isStopwatchRunning: Boolean,
    isStopwatchFocusActive: Boolean,
    stopwatchElapsedMillis: Long,
    onStartStopwatch: () -> Unit,
    onPauseStopwatch: () -> Unit,
    onResumeStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit
) {
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var userSubjects by remember { mutableStateOf(listOf<String>()) }
    var selectedSubjectName by rememberSaveable { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGiveUpDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    var selectedFocusMode by rememberSaveable { mutableIntStateOf(0) }

    // 🎯 Pomodoro Settings States
    var pomodoroFocusMinutes by rememberSaveable { mutableIntStateOf(25) }
    var pomodoroShortBreakMinutes by rememberSaveable { mutableIntStateOf(5) }
    var pomodoroLongBreakMinutes by rememberSaveable { mutableIntStateOf(15) }
    var pomodoroTotalSessions by rememberSaveable { mutableIntStateOf(4) }
    var pomodoroAutoStartBreak by rememberSaveable { mutableStateOf(false) }
    var pomodoroAutoStartFocus by rememberSaveable { mutableStateOf(false) }

    // 📋 Bottom Sheet Triggers
    var showPomodoroSettingsSheet by remember { mutableStateOf(false) }
    var showCustomTimerSheet by remember { mutableStateOf(false) }

    var isBreakActive by rememberSaveable { mutableStateOf(false) }
    var currentSessionIndex by rememberSaveable { mutableIntStateOf(1) }
    var isOvertimeActive by rememberSaveable { mutableStateOf(false) }
    var overtimeSeconds by rememberSaveable { mutableIntStateOf(0) }

    val subjectPrefs = remember { context.getSharedPreferences("amon_subject_prefs", Context.MODE_PRIVATE) }

    var hiddenSubjects by remember {
        val saved: Set<String>? = try {
            subjectPrefs.getStringSet("hidden_subjects", null)
        } catch (_: Exception) {
            try {
                subjectPrefs.edit().remove("hidden_subjects").apply()
            } catch (_: Exception) {}
            null
        }

        if (saved == null) {
            val initialHidden = try {
                SubjectManager.getUserSubjects(context).toSet()
            } catch (_: Exception) {
                emptySet()
            }
            try {
                subjectPrefs.edit().putStringSet("hidden_subjects", initialHidden).apply()
            } catch (_: Exception) {}
            mutableStateOf(initialHidden)
        } else {
            mutableStateOf(saved)
        }
    }

    BackHandler(enabled = isFocusActive) {
        showGiveUpDialog = true
    }

    LaunchedEffect(Unit) {
        userSubjects = try {
            SubjectManager.getUserSubjects(context)
        } catch (_: Exception) {
            emptyList()
        }
    }

    var dialMinutes by rememberSaveable { mutableFloatStateOf(25f) }
    var isCustomMode by rememberSaveable { mutableStateOf(false) }
    var initialTotalSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }

    val totalSeconds = TimerService.remainingSeconds.intValue

    LaunchedEffect(isRunning, totalSeconds, isBreakActive) {
        if (isRunning && totalSeconds == 0 && !isBreakActive) {
            isOvertimeActive = true
            while (isRunning && isOvertimeActive) {
                delay(1000)
                overtimeSeconds++
            }
        } else if (!isRunning) {
            isOvertimeActive = false
            overtimeSeconds = 0
        }
    }

    var streakDays by remember { mutableIntStateOf(0) }
    LaunchedEffect(isFocusActive) {
        if (!isFocusActive) {
            val sessions = try {
                FocusSessionManager.getAllSessions(context)
            } catch (_: Exception) {
                emptyList()
            }
            streakDays = calculateStreakDays(sessions)
        }
    }

    val hours = totalSeconds / 3600
    val displayMinutes = (totalSeconds % 3600) / 60
    val displaySeconds = totalSeconds % 60

    val timeFormatted = if (isOvertimeActive) {
        val otM = overtimeSeconds / 60
        val otS = overtimeSeconds % 60
        String.format("+%02d:%02d", otM, otS)
    } else if (hours > 0) {
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

    val allSessions = remember(isFocusActive) {
        try {
            FocusSessionManager.getAllSessions(context)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val subjectMinutesMap = remember(allSessions) {
        allSessions.groupBy { it.subject }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
    }
    val activeSortedSubjects = remember(userSubjects, hiddenSubjects, subjectMinutesMap) {
        userSubjects
            .filter { it !in hiddenSubjects }
            .sortedByDescending { subjectMinutesMap[it] ?: 0 }
    }

    val onAddSubjectAction = {
        if (newSubjectInput.isNotBlank()) {
            val cleanInput = newSubjectInput.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val success = SubjectManager.addSubject(context, cleanInput)
            if (success) {
                userSubjects = SubjectManager.getUserSubjects(context)
                if (cleanInput in hiddenSubjects) {
                    val newHidden = hiddenSubjects - cleanInput
                    hiddenSubjects = newHidden
                    subjectPrefs.edit().putStringSet("hidden_subjects", newHidden).apply()
                }
                selectedSubjectName = cleanInput
                newSubjectInput = ""
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isFocusActive) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. TOP HEADER (COMPACT LOTTIE FLAME_MAIN INTEGRATED)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
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
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
                            .border(1.dp, glassBorder, RoundedCornerShape(50))
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // 🔥 3rd Image: flame_main Lottie Icon in Top Pill
                            StreakFlame(
                                streakDays = streakDays,
                                isLarge = false,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "$streakDays Days",
                                color = if (streakDays > 0) Color(0xFFFB923C) else textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. DYNAMIC SUBJECT RIBBON
                Box(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(goldColor)
                                    .clickable { showAddDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+ Subject",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        item {
                            val isSelected = selectedSubjectName.equals("All", ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) goldColor else cardBg)
                                    .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                                    .clickable { selectedSubjectName = "All" }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All",
                                    color = if (isSelected) Color.White else textMuted,
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
                                    .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                                    .clickable { selectedSubjectName = subj }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = subj,
                                    color = if (isSelected) Color.White else textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. SLEEK MODE SELECTOR CAPSULE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF181820) else Color(0xFFE2E8F0))
                        .border(1.dp, glassBorder, RoundedCornerShape(50))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val modes = listOf("Timer", "Stopwatch", "Pomodoro")
                        modes.forEachIndexed { index, modeName ->
                            val isSelected = selectedFocusMode == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) goldColor else Color.Transparent)
                                    .clickable { selectedFocusMode = index }
                            ) {
                                Text(
                                    text = modeName,
                                    color = if (isSelected) Color.Black else textMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedFocusMode == 2) {
                    PomodoroSessionTracker(
                        totalSessions = pomodoroTotalSessions,
                        currentSessionIndex = currentSessionIndex,
                        isBreakActive = isBreakActive,
                        isRunning = isRunning,
                        goldColor = goldColor,
                        glowYellow = glowYellow,
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 4. CENTER STAGE: WATCH / STOPWATCH
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedFocusMode) {
                        0, 2 -> {
                            val activeMins = if (selectedFocusMode == 2) pomodoroFocusMinutes.toFloat() else dialMinutes

                            TimerWatchesContainer(
                                dialMinutes = activeMins,
                                onDialMinutesChange = { nextMins ->
                                    if (selectedFocusMode == 2) {
                                        pomodoroFocusMinutes = nextMins.toInt()
                                    } else {
                                        dialMinutes = nextMins
                                        val secs = if (nextMins == 0f) 0 else (nextMins.toInt() * 60)
                                        TimerService.remainingSeconds.intValue = secs
                                        initialTotalSeconds = if (secs > 0) secs else 60
                                    }
                                },
                                totalSeconds = if (selectedFocusMode == 2) (pomodoroFocusMinutes * 60) else totalSeconds,
                                initialTotalSeconds = initialTotalSeconds,
                                isRunning = false,
                                isCustomMode = isCustomMode,
                                onCustomModeToggle = {
                                    if (selectedFocusMode == 2) {
                                        showPomodoroSettingsSheet = true
                                    } else {
                                        showCustomTimerSheet = true
                                    }
                                },
                                goldColor = goldColor,
                                glowYellow = glowYellow,
                                cardBg = cardBg,
                                glassBorder = glassBorder,
                                textMain = textMain,
                                textMuted = textMuted,
                                isDark = isDark,
                                timeFormatted = if (selectedFocusMode == 2) String.format("%02d:00", pomodoroFocusMinutes) else timeFormatted,
                                hours = hours,
                                selectedSubjectName = selectedSubjectName,
                                showPresets = false
                            )
                        }
                        1 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "STOPWATCH PRECISION",
                                    color = goldColor,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = swMainTime,
                                        color = textMain,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = swMilliText,
                                        color = goldColor,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. SUB-CONTROL SLOT
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedFocusMode) {
                        0 -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(25f, 45f, 60f).forEach { mins ->
                                    val isSelected = !isCustomMode && dialMinutes == mins
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) goldColor else cardBg)
                                            .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                                            .clickable {
                                                isCustomMode = false
                                                dialMinutes = mins
                                                val secs = mins.toInt() * 60
                                                TimerService.remainingSeconds.intValue = secs
                                                initialTotalSeconds = secs
                                            }
                                    ) {
                                        Text(
                                            text = "${mins.toInt()}m",
                                            color = if (isSelected) Color.Black else textMain,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isCustomMode) goldColor else cardBg)
                                        .border(1.dp, if (isCustomMode) glowYellow else glassBorder, RoundedCornerShape(50))
                                        .clickable { showCustomTimerSheet = true }
                                ) {
                                    Text(
                                        text = "Custom ⏱️",
                                        color = if (isCustomMode) Color.Black else textMain,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        2 -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val pBadges = listOf(
                                        "Focus ${pomodoroFocusMinutes}m",
                                        "Short ${pomodoroShortBreakMinutes}m",
                                        "Long ${pomodoroLongBreakMinutes}m"
                                    )
                                    pBadges.forEach { title ->
                                        Surface(
                                            modifier = Modifier.clip(RoundedCornerShape(50)),
                                            color = cardBg,
                                            border = BorderStroke(1.dp, glassBorder),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = title,
                                                color = textMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF1E1E26) else Color(0xFFF1F5F9))
                                        .border(1.2.dp, goldColor, CircleShape)
                                        .clickable { showPomodoroSettingsSheet = true }
                                ) {
                                    SlidersPreferenceIcon(tint = goldColor)
                                }
                            }
                        }
                        1 -> {
                            Text(
                                text = "Zero-Drift Hardware Monotonic Clock",
                                color = textMuted.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. MAIN START BUTTON
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(44.dp)
                        .shadow(12.dp, RoundedCornerShape(50), spotColor = goldColor, ambientColor = goldColor)
                        .clip(RoundedCornerShape(50))
                        .background(goldColor)
                        .border(1.dp, glowYellow, RoundedCornerShape(50))
                        .clickable {
                            if (selectedFocusMode == 1) {
                                onStartStopwatch()
                            } else {
                                val targetSecs = if (selectedFocusMode == 2) {
                                    pomodoroFocusMinutes * 60
                                } else totalSeconds

                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_START
                                    putExtra(TimerService.EXTRA_SECONDS, if (targetSecs > 0) targetSecs else 1500)
                                    putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "▶", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = if (selectedFocusMode == 1) "Start Stopwatch" else "Start Focus",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            // 🎯 MASTER FOCUS SCREEN
            if (isStopwatchFocusActive) {
                if (!isLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 22.dp, bottom = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                .border(1.dp, glassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Text(
                                text = "$selectedSubjectName • Stopwatch",
                                color = textMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = swMainTime,
                                color = textMain,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = swMilliText,
                                color = goldColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable {
                                        if (isStopwatchRunning) onPauseStopwatch() else onResumeStopwatch()
                                    }
                            ) {
                                if (isStopwatchRunning) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.width(3.2.dp).height(16.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                        Box(modifier = Modifier.width(3.2.dp).height(16.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                    }
                                } else {
                                    Text(
                                        text = "▶",
                                        color = textMain,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.offset(x = 1.5.dp)
                                    )
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable { showGiveUpDialog = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(textMuted)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$selectedSubjectName • Stopwatch",
                                color = textMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = swMainTime,
                                    color = textMain,
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = swMilliText,
                                    color = goldColor,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable {
                                        if (isStopwatchRunning) onPauseStopwatch() else onResumeStopwatch()
                                    }
                            ) {
                                if (isStopwatchRunning) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                                        Box(modifier = Modifier.width(3.2.dp).height(15.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                        Box(modifier = Modifier.width(3.2.dp).height(15.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                    }
                                } else {
                                    Text(text = "▶", color = textMain, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable { showGiveUpDialog = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(textMuted)
                                )
                            }
                        }
                    }
                }
            } else {
                if (!isLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 18.dp, bottom = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Text(
                                    text = selectedSubjectName,
                                    color = textMain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            if (isBreakActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(goldColor.copy(alpha = 0.2f))
                                        .border(1.dp, goldColor, RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    CleanTeaCupIcon(tint = goldColor, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Break",
                                        color = goldColor,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Running Dial
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TimerWatchesContainer(
                                dialMinutes = dialMinutes,
                                onDialMinutesChange = { nextMins ->
                                    dialMinutes = nextMins
                                    val secs = if (nextMins == 0f) 0 else (nextMins.toInt() * 60)
                                    TimerService.remainingSeconds.intValue = secs
                                    initialTotalSeconds = if (secs > 0) secs else 60
                                },
                                totalSeconds = totalSeconds,
                                initialTotalSeconds = initialTotalSeconds,
                                isRunning = true,
                                isCustomMode = isCustomMode,
                                onCustomModeToggle = { },
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

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isOvertimeActive) {
                                Button(
                                    onClick = {
                                        isOvertimeActive = false
                                        isBreakActive = true
                                        val breakMins = if (currentSessionIndex >= pomodoroTotalSessions) pomodoroLongBreakMinutes else pomodoroShortBreakMinutes
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_START
                                            putExtra(TimerService.EXTRA_SECONDS, breakMins * 60)
                                            putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                        }
                                        context.startService(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CleanTeaCupIcon(tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Start Break",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                        .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable {
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = if (isRunning) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
                                        }
                                        context.startService(intent)
                                    }
                                ) {
                                    if (isRunning) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.width(3.2.dp).height(16.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                            Box(modifier = Modifier.width(3.2.dp).height(16.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                        }
                                    } else {
                                        Text(
                                            text = "▶",
                                            color = textMain,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.offset(x = 1.5.dp)
                                        )
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                        .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable { showGiveUpDialog = true }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(textMuted)
                                    )
                                }
                            }

                            if (isPaused) {
                                Text(
                                    text = "PAUSED",
                                    color = textMuted.copy(alpha = 0.65f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Text(
                                    text = selectedSubjectName,
                                    color = textMain,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimerWatchesContainer(
                                dialMinutes = dialMinutes,
                                onDialMinutesChange = { nextMins ->
                                    dialMinutes = nextMins
                                    val secs = if (nextMins == 0f) 0 else (nextMins.toInt() * 60)
                                    TimerService.remainingSeconds.intValue = secs
                                    initialTotalSeconds = if (secs > 0) secs else 60
                                },
                                totalSeconds = totalSeconds,
                                initialTotalSeconds = initialTotalSeconds,
                                isRunning = true,
                                isCustomMode = isCustomMode,
                                onCustomModeToggle = { },
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

                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable {
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = if (isRunning) TimerService.ACTION_PAUSE else TimerService.ACTION_RESUME
                                        }
                                        context.startService(intent)
                                    }
                            ) {
                                if (isRunning) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                                        Box(modifier = Modifier.width(3.2.dp).height(15.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                        Box(modifier = Modifier.width(3.2.dp).height(15.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                    }
                                } else {
                                    Text(
                                        text = "▶",
                                        color = textMain,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.offset(x = 1.2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF202028) else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDark) Color(0x44FFFFFF) else Color(0xFFCBD5E1), CircleShape)
                                    .clickable { showGiveUpDialog = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(textMuted)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 🎚️ 1. POMODORO SETTINGS MODAL BOTTOM SHEET
        // =====================================================================
        if (showPomodoroSettingsSheet) {
            PomodoroSettingsBottomSheet(
                isDark = isDark,
                goldColor = goldColor,
                glowYellow = glowYellow,
                cardBg = cardBg,
                textMain = textMain,
                textMuted = textMuted,
                focusMinutes = pomodoroFocusMinutes,
                shortBreakMinutes = pomodoroShortBreakMinutes,
                longBreakMinutes = pomodoroLongBreakMinutes,
                totalSessions = pomodoroTotalSessions,
                autoStartBreak = pomodoroAutoStartBreak,
                autoStartFocus = pomodoroAutoStartFocus,
                onFocusMinutesChange = { pomodoroFocusMinutes = it },
                onShortBreakMinutesChange = { pomodoroShortBreakMinutes = it },
                onLongBreakMinutesChange = { pomodoroLongBreakMinutes = it },
                onSessionsChange = { pomodoroTotalSessions = it },
                onAutoStartBreakChange = { pomodoroAutoStartBreak = it },
                onAutoStartFocusChange = { pomodoroAutoStartFocus = it },
                onDismiss = { showPomodoroSettingsSheet = false }
            )
        }

        // =====================================================================
        // ⏱️ 2. CUSTOM TIMER BOTTOM SHEET (FOR TIMER MODE)
        // =====================================================================
        if (showCustomTimerSheet) {
            CustomTimerBottomSheet(
                dialMinutes = dialMinutes,
                onDialMinutesChange = {
                    dialMinutes = it
                    isCustomMode = true
                    val secs = it.toInt() * 60
                    TimerService.remainingSeconds.intValue = secs
                    initialTotalSeconds = secs
                },
                isDark = isDark,
                goldColor = goldColor,
                cardBg = cardBg,
                textMain = textMain,
                textMuted = textMuted,
                onDismiss = { showCustomTimerSheet = false }
            )
        }

        // POPUPS (SUBJECTS & GIVE UP)
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
                            text = "Tap to Show (Color) or Hide (Plain)",
                            color = textMuted,
                            fontSize = 12.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
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
                                                        selectedSubjectName = "All"
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
                                    Text("Type new subject...", color = textMuted, fontSize = 13.sp)
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

        if (showGiveUpDialog) {
            Dialog(onDismissRequest = { showGiveUpDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isStopwatchFocusActive) "End Stopwatch? ⏱️" else "End Focus Session? ⏱️",
                            color = textMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isStopwatchFocusActive)
                                "Do you want to stop and reset the stopwatch session?"
                            else
                                "Giving up now will forfeit this focus session. Are you sure you want to stop?",
                            color = textMuted,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF22222A) else Color(0xFFE2E8F0))
                                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                                    .clickable {
                                        showGiveUpDialog = false
                                        if (isStopwatchFocusActive) {
                                            onResetStopwatch()
                                        } else {
                                            val intent = Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_STOP
                                            }
                                            context.startService(intent)
                                        }
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                            ) {
                                Text(
                                    text = "End Session",
                                    color = textMuted,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(goldColor)
                                    .clickable { showGiveUpDialog = false }
                            ) {
                                Text(
                                    text = "Keep Focusing 🎯",
                                    color = Color.Black,
                                    fontSize = 12.5.sp,
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

// =============================================================================
// 🍅 POMODORO SETTINGS MODAL BOTTOM SHEET
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsBottomSheet(
    isDark: Boolean,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    focusMinutes: Int,
    shortBreakMinutes: Int,
    longBreakMinutes: Int,
    totalSessions: Int,
    autoStartBreak: Boolean,
    autoStartFocus: Boolean,
    onFocusMinutesChange: (Int) -> Unit,
    onShortBreakMinutesChange: (Int) -> Unit,
    onLongBreakMinutesChange: (Int) -> Unit,
    onSessionsChange: (Int) -> Unit,
    onAutoStartBreakChange: (Boolean) -> Unit,
    onAutoStartFocusChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPillTab by remember { mutableIntStateOf(0) }
    var showEnterMinutesDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF14141E) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0x44FFFFFF) else Color(0x33000000))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pomodoro Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        .clickable { onDismiss() }
                ) {
                    Text("✕", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF1E1E2A) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                val tabWidth = maxWidth / 3
                val animatedPillOffset by animateDpAsState(
                    targetValue = tabWidth * selectedPillTab,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                    label = "pill_glide"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedPillOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(goldColor)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    val tabs = listOf(
                        Triple("Focus", "${focusMinutes}m", 0),
                        Triple("Break", "${shortBreakMinutes}m", 1),
                        Triple("Long", "${longBreakMinutes}m", 2)
                    )

                    tabs.forEach { (name, timeStr, index) ->
                        val isSelected = selectedPillTab == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedPillTab = index }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else textMuted
                            )
                            Text(
                                text = timeStr,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.Black else textMain,
                                modifier = Modifier.clickable {
                                    selectedPillTab = index
                                    showEnterMinutesDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val (currentVal, minVal, maxVal, ticks) = when (selectedPillTab) {
                    0 -> Quadruple(focusMinutes.toFloat(), 1f, 90f, listOf(5, 25, 60, 90))
                    1 -> Quadruple(shortBreakMinutes.toFloat(), 1f, 15f, listOf(1, 5, 10, 15))
                    else -> Quadruple(longBreakMinutes.toFloat(), 5f, 45f, listOf(5, 15, 30, 45))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedPillTab) {
                            0 -> "Focus Duration"
                            1 -> "Short Break"
                            else -> "Long Break"
                        },
                        fontWeight = FontWeight.Bold,
                        color = textMain,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${currentVal.toInt()} min",
                        fontWeight = FontWeight.Black,
                        color = goldColor,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = currentVal,
                    onValueChange = { newVal ->
                        when (selectedPillTab) {
                            0 -> onFocusMinutesChange(newVal.toInt())
                            1 -> onShortBreakMinutesChange(newVal.toInt())
                            else -> onLongBreakMinutesChange(newVal.toInt())
                        }
                    },
                    valueRange = minVal..maxVal,
                    colors = SliderDefaults.colors(thumbColor = goldColor, activeTrackColor = goldColor)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ticks.forEach { tick ->
                        Text(
                            text = "${tick}m",
                            color = if (currentVal.toInt() == tick) goldColor else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                when (selectedPillTab) {
                                    0 -> onFocusMinutesChange(tick)
                                    1 -> onShortBreakMinutesChange(tick)
                                    else -> onLongBreakMinutesChange(tick)
                                }
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Focus sessions before Long Break", fontWeight = FontWeight.Bold, color = textMain, fontSize = 13.sp)
                    Text("$totalSessions", fontWeight = FontWeight.Black, color = goldColor, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(2, 3, 4, 5, 6).forEach { num ->
                        val isSelected = totalSessions == num
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) goldColor else (if (isDark) Color(0xFF22222E) else Color(0xFFE2E8F0)))
                                .border(1.dp, if (isSelected) glowYellow else (if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)), CircleShape)
                            .clickable { onSessionsChange(num) }
                        ) {
                            Text(
                                text = "$num",
                                color = if (isSelected) Color.Black else textMain,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoStartBreakChange(!autoStartBreak) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-start Break after Focus", fontSize = 12.5.sp, color = textMain, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = autoStartBreak,
                        onCheckedChange = onAutoStartBreakChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = goldColor, checkedTrackColor = goldColor.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoStartFocusChange(!autoStartFocus) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-start Focus after Break", fontSize = 12.5.sp, color = textMain, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = autoStartFocus,
                        onCheckedChange = onAutoStartFocusChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = goldColor, checkedTrackColor = goldColor.copy(alpha = 0.4f))
                    )
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("Save & Apply", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }

    if (showEnterMinutesDialog) {
        val currentMinutes = when (selectedPillTab) {
            0 -> focusMinutes
            1 -> shortBreakMinutes
            else -> longBreakMinutes
        }
        var inputMinutesText by remember { mutableStateOf(currentMinutes.toString()) }

        Dialog(onDismissRequest = { showEnterMinutesDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF181824) else Color(0xFFFFFFFF))
                    .border(1.5.dp, goldColor, RoundedCornerShape(24.dp))
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
                            text = "Enter Minutes (Max 120)",
                            color = goldColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                                .clickable { showEnterMinutesDialog = false }
                        ) {
                            Text("✕", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputMinutesText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            val num = digitsOnly.toIntOrNull()
                            inputMinutesText = when {
                                num == null -> digitsOnly
                                num > 120 -> "120"
                                else -> digitsOnly
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = textMain
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            cursorColor = goldColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(68.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val finalMin = inputMinutesText.toIntOrNull()?.coerceIn(1, 120) ?: 25
                            when (selectedPillTab) {
                                0 -> onFocusMinutesChange(finalMin)
                                1 -> onShortBreakMinutesChange(finalMin.coerceAtMost(30))
                                else -> onLongBreakMinutesChange(finalMin.coerceAtMost(60))
                            }
                            showEnterMinutesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// ⏱️ CUSTOM TIMER BOTTOM SHEET (FOR TIMER MODE)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimerBottomSheet(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    isDark: Boolean,
    goldColor: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF14141E) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0x44FFFFFF) else Color(0x33000000))
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set Custom Time ⏱️",
                color = goldColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Slide smoothly to adjust (5 - 180 min)",
                color = textMuted,
                fontSize = 12.sp
            )

            val mins = dialMinutes.toInt().coerceIn(5, 180)
            val displayTitle = if (mins >= 60) {
                val h = mins / 60
                val m = mins % 60
                if (m > 0) "$mins min ($h h $m m)" else "$mins min ($h h)"
            } else {
                "$mins min"
            }

            Text(
                text = displayTitle,
                color = textMain,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )

            CustomHorizontalSlider(
                dialMinutes = dialMinutes,
                onDialMinutesChange = onDialMinutesChange,
                goldColor = goldColor,
                cardBg = cardBg,
                textMain = textMain,
                textMuted = textMuted
            )

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("Done 🎯", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 🔥 HELPER DATA CLASS FOR TABS
// -----------------------------------------------------------------------------
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// =============================================================================
// 🔥 2. ANIMATED LIVING FLAME ICON (COMPATIBILITY FALLBACK TO LOTTIE)
// =============================================================================
@Composable
fun AnimatedLivingFlame(modifier: Modifier = Modifier) {
    StreakFlame(
        streakDays = 1,
        isLarge = false,
        modifier = modifier.size(19.dp)
    )
}

// =============================================================================
// ✨ 3. WHAT'S NEW DIALOG
// =============================================================================
@Composable
fun WhatsNewDialog(
    versionName: String,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(1.6.dp, goldColor, RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(goldColor.copy(alpha = 0.2f))
                        .border(1.dp, goldColor, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Version $versionName",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "What's New in Amon ✨",
                    color = textMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(goldColor)
                        .border(1.dp, glowYellow, RoundedCornerShape(50))
                        .clickable { onDismiss() }
                ) {
                    Text(
                        text = "Got It, Let's Focus 🎯",
                        color = Color.Black,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// =============================================================================
// 🟢 4. SIMPLE VECTOR ICON COMPOSABLE
// =============================================================================
@Composable
fun SimpleNavIcon(
    index: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (index) {
        0 -> {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = tint,
                modifier = modifier
            )
        }
        1 -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val ringRadius = w * 0.32f
                val strokeWidth = 1.8.dp.toPx()
                val dotRadius = 2.4.dp.toPx()

                drawCircle(
                    color = tint.copy(alpha = 0.5f),
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidth)
                )

                val nodeOffsets = listOf(
                    Offset(cx, cy - ringRadius),
                    Offset(cx + ringRadius, cy),
                    Offset(cx, cy + ringRadius),
                    Offset(cx - ringRadius, cy)
                )
                nodeOffsets.forEach { nodeCenter ->
                    drawCircle(
                        color = tint,
                        radius = dotRadius,
                        center = nodeCenter
                    )
                }

                drawCircle(
                    color = tint.copy(alpha = 0.8f),
                    radius = 1.4.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }
        2 -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val barWidth = w * 0.18f
                val barCorner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

                drawRoundRect(
                    color = tint.copy(alpha = 0.35f),
                    topLeft = Offset(w * 0.12f, h * 0.58f),
                    size = Size(barWidth, h * 0.34f),
                    cornerRadius = barCorner
                )
                drawRoundRect(
                    color = tint.copy(alpha = 0.50f),
                    topLeft = Offset(w * 0.41f, h * 0.38f),
                    size = Size(barWidth, h * 0.54f),
                    cornerRadius = barCorner
                )
                drawRoundRect(
                    color = tint.copy(alpha = 0.70f),
                    topLeft = Offset(w * 0.70f, h * 0.22f),
                    size = Size(barWidth, h * 0.70f),
                    cornerRadius = barCorner
                )

                val trendPath = Path().apply {
                    moveTo(w * 0.08f, h * 0.66f)
                    lineTo(w * 0.21f, h * 0.52f)
                    lineTo(w * 0.50f, h * 0.34f)
                    lineTo(w * 0.79f, h * 0.16f)
                }
                drawPath(
                    path = trendPath,
                    color = tint,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                drawCircle(
                    color = tint,
                    radius = 2.8.dp.toPx(),
                    center = Offset(w * 0.79f, h * 0.16f)
                )
            }
        }
        3 -> {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = tint,
                modifier = modifier
            )
        }
    }
}

// =============================================================================
// 🟢 5. CURVED BOTTOM BAR (UPDATED: "Circle" TAB)
// =============================================================================
@Composable
fun AmonCurvedBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    goldColor: Color,
    cardBg: Color,
    borderCol: Color,
    hasNewUpdate: Boolean = false
) {
    val tabTitles = listOf("Home", "Circle", "Stats", "Profile")

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "notch_slide"
    )

    val inactiveColor = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barHeight = 64.dp.toPx()
            val startY = size.height - barHeight
            val tabWidth = size.width / 4f

            val notchCenterX = (animatedIndex + 0.5f) * tabWidth
            val notchRadius = 34.dp.toPx()
            val shoulderWidth = 14.dp.toPx()

            val path = Path().apply {
                moveTo(0f, startY)
                val left = notchCenterX - notchRadius
                val right = notchCenterX + notchRadius

                lineTo(left - shoulderWidth, startY)
                cubicTo(left, startY, left, startY + notchRadius * 0.95f, notchCenterX, startY + notchRadius * 0.95f)
                cubicTo(right, startY + notchRadius * 0.95f, right, startY, right + shoulderWidth, startY)

                lineTo(size.width, startY)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            drawPath(path = path, color = cardBg)
            drawPath(path = path, color = borderCol, style = Stroke(width = 1.2.dp.toPx()))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / 4
            val bubbleSize = 54.dp
            val bubbleX = (tabWidth * animatedIndex) + (tabWidth / 2) - (bubbleSize / 2)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = bubbleX, y = 2.dp)
                    .size(bubbleSize)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF08080B) else Color(0xFFF8FAFC))
                    .border(2.6.dp, goldColor, CircleShape)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    SimpleNavIcon(
                        index = selectedIndex,
                        tint = goldColor,
                        modifier = Modifier.size(26.dp)
                    )

                    if (hasNewUpdate && selectedIndex == 3) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.5.dp, if (isDark) Color(0xFF08080B) else Color(0xFFF8FAFC), CircleShape)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = index == selectedIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(index) }
                            .padding(top = 4.dp)
                    ) {
                        if (!isSelected) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                SimpleNavIcon(
                                    index = index,
                                    tint = inactiveColor,
                                    modifier = Modifier.size(22.dp)
                                )

                                if (hasNewUpdate && index == 3) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 3.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .border(1.dp, cardBg, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = inactiveColor
                            )
                        } else {
                            Spacer(modifier = Modifier.height(26.dp))
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = goldColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 🔥 6. STREAK CALCULATOR
// -----------------------------------------------------------------------------
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
