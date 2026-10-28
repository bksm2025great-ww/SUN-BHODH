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

    val isDark = ThemeManager.isDarkTheme.value
    val isRunning = TimerService.isTimerRunning.value
    val isPaused = TimerService.isTimerPaused.value

    var isStopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var isStopwatchFocusActive by rememberSaveable { mutableStateOf(false) }
    var stopwatchBaseTime by rememberSaveable { mutableStateOf(0L) }
    var stopwatchAccumulatedMillis by rememberSaveable { mutableStateOf(0L) }
    var stopwatchElapsedMillis by rememberSaveable { mutableStateOf(0L) }

    // 🔄 Sync on Start / Restore
    LaunchedEffect(Unit) {
        try {
            ThemeManager.loadTheme(context)
            TimerService.syncRemainingTime(context)

            if (TimerService.originalTimerSeconds == 0 && (TimerService.isTimerRunning.value || TimerService.isTimerPaused.value)) {
                isStopwatchFocusActive = true
                isStopwatchRunning = TimerService.isTimerRunning.value
                stopwatchAccumulatedMillis = TimerService.accumulatedActiveMillis
                stopwatchBaseTime = TimerService.sessionResumeElapsedRealtime
                if (isStopwatchRunning && stopwatchBaseTime > 0L) {
                    stopwatchElapsedMillis = stopwatchAccumulatedMillis + (SystemClock.elapsedRealtime() - stopwatchBaseTime)
                } else {
                    stopwatchElapsedMillis = stopwatchAccumulatedMillis
                }
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(isRunning, isPaused) {
        val isStopwatchService = (TimerService.originalTimerSeconds == 0)
        if (isStopwatchFocusActive && isStopwatchService) {
            if (!isRunning && !isPaused) {
                isStopwatchRunning = false
                isStopwatchFocusActive = false
                stopwatchAccumulatedMillis = 0L
                stopwatchElapsedMillis = 0L
            } else if (isPaused && isStopwatchRunning) {
                isStopwatchRunning = false
                stopwatchAccumulatedMillis = stopwatchElapsedMillis
            } else if (isRunning && !isStopwatchRunning) {
                stopwatchBaseTime = SystemClock.elapsedRealtime()
                isStopwatchRunning = true
            }
        }
    }

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

    val onDirectEndSession = {
        if (isStopwatchFocusActive) {
            val elapsedMins = (stopwatchElapsedMillis / 60000).toInt()
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            context.startService(intent)

            if (elapsedMins >= 5) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val session = FocusSession(
                    id = System.currentTimeMillis(),
                    date = sdf.format(Date()),
                    subject = "General",
                    durationMinutes = elapsedMins,
                    earnedTrees = when {
                        elapsedMins >= 105 -> 4
                        elapsedMins >= 75 -> 3
                        elapsedMins >= 45 -> 2
                        elapsedMins >= 15 -> 1
                        else -> 0
                    }
                )
                FocusSessionManager.saveSession(context, session)
            }
            isStopwatchRunning = false
            isStopwatchFocusActive = false
            stopwatchAccumulatedMillis = 0L
            stopwatchElapsedMillis = 0L
        } else {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            context.startService(intent)
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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

    BackHandler(enabled = isFocusActive) {
        onDirectEndSession()
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
                        stopwatchAccumulatedMillis = 0L
                        stopwatchElapsedMillis = 0L
                        stopwatchBaseTime = SystemClock.elapsedRealtime()
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
                    },
                    onEndSessionDirect = onDirectEndSession
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
// 🟢 1. HOME TAB (CLEAN LAYOUT WITH MODULAR POMODORO & STOPWATCH)
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
    onResetStopwatch: () -> Unit,
    onEndSessionDirect: () -> Unit
) {
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var userSubjects by remember { mutableStateOf(listOf<String>()) }
    var selectedSubjectName by rememberSaveable { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    var selectedFocusMode by rememberSaveable { mutableIntStateOf(0) }

    var pomodoroFocusMinutes by rememberSaveable { mutableIntStateOf(25) }
    var pomodoroShortBreakMinutes by rememberSaveable { mutableIntStateOf(5) }
    var pomodoroLongBreakMinutes by rememberSaveable { mutableIntStateOf(15) }
    var pomodoroTotalSessions by rememberSaveable { mutableIntStateOf(4) }
    var pomodoroAutoStartBreak by rememberSaveable { mutableStateOf(false) }
    var pomodoroAutoStartFocus by rememberSaveable { mutableStateOf(false) }

    var showPomodoroSettingsSheet by remember { mutableStateOf(false) }
    var showCustomTimerSheet by remember { mutableStateOf(false) }

    var isBreakActive by rememberSaveable { mutableStateOf(false) }
    var currentSessionIndex by rememberSaveable { mutableIntStateOf(1) }

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
                // 1. TOP HEADER (SLIM & BALANCED 32DP STREAK PILL)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StreakFlame(
                                streakDays = streakDays,
                                isLarge = false,
                                modifier = Modifier.size(22.dp)
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

                // 🍅 POMODORO SESSION TRACKER (Modular from PomodoroStopwatch.kt)
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
                            // ⏱️ STOPWATCH PRECISION DISPLAY (Modular from PomodoroStopwatch.kt)
                            StopwatchPrecisionDisplay(
                                swMainTime = swMainTime,
                                swMilliText = swMilliText,
                                goldColor = goldColor,
                                textMain = textMain
                            )
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
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_START
                                    putExtra(TimerService.EXTRA_SECONDS, 0)
                                    putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
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
                                        if (isStopwatchRunning) {
                                            onPauseStopwatch()
                                            val intent = Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_PAUSE
                                            }
                                            context.startService(intent)
                                        } else {
                                            onResumeStopwatch()
                                            val intent = Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_RESUME
                                            }
                                            context.startService(intent)
                                        }
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
                                    .clickable { onEndSessionDirect() }
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
                                        if (isStopwatchRunning) {
                                            onPauseStopwatch()
                                            val intent = Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_PAUSE
                                            }
                                            context.startService(intent)
                                        } else {
                                            onResumeStopwatch()
                                            val intent = Intent(context, TimerService::class.java).apply {
                                                action = TimerService.ACTION_RESUME
                                            }
                                            context.startService(intent)
                                        }
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
                                    .clickable { onEndSessionDirect() }
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
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    CleanTeaCupIcon(tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Break",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Dial Container (Break hone par glow band, calm mode)
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

                        // 🎯 ADAPTIVE CONTROLS: PAUSE / RESUME / BREAK / DIRECT END SESSION
                        PomodoroFocusControls(
                            isRunning = isRunning,
                            isPaused = isPaused,
                            isBreakActive = isBreakActive,
                            goldColor = goldColor,
                            glowYellow = glowYellow,
                            cardBg = cardBg,
                            glassBorder = glassBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            onPauseClick = {
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_PAUSE
                                }
                                context.startService(intent)
                            },
                            onResumeClick = {
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_RESUME
                                }
                                context.startService(intent)
                            },
                            onSwitchToBreakClick = {
                                isBreakActive = true
                                val breakMins = if (currentSessionIndex >= pomodoroTotalSessions) pomodoroLongBreakMinutes else pomodoroShortBreakMinutes
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_START
                                    putExtra(TimerService.EXTRA_SECONDS, breakMins * 60)
                                    putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                }
                                context.startService(intent)
                            },
                            onResumeFocusFromBreak = {
                                isBreakActive = false
                                currentSessionIndex = if (currentSessionIndex >= pomodoroTotalSessions) 1 else currentSessionIndex + 1
                                val targetSecs = if (selectedFocusMode == 2) pomodoroFocusMinutes * 60 else (dialMinutes.toInt() * 60)
                                val intent = Intent(context, TimerService::class.java).apply {
                                    action = TimerService.ACTION_START
                                    putExtra(TimerService.EXTRA_SECONDS, if (targetSecs > 0) targetSecs else 1500)
                                    putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                }
                                context.startService(intent)
                            },
                            onEndSessionClick = {
                                isBreakActive = false
                                onEndSessionDirect()
                            }
                        )
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
                                    .clickable {
                                        isBreakActive = false
                                        onEndSessionDirect()
                                    }
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

        // 🍅 POMODORO SETTINGS MODAL BOTTOM SHEET (Modular from PomodoroStopwatch.kt)
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

        // POPUPS (MANAGE SUBJECTS)
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

// =============================================================================
// 🔥 2. ANIMATED LIVING FLAME ICON
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

// -----------------------------------------------------------------------------
// 🔥 4. STREAK CALCULATOR
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
