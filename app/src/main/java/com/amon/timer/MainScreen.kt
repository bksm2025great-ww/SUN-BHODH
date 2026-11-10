package com.amon.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.amon.timer.groups.GroupsRootScreen
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

    var activeMode by rememberSaveable { mutableStateOf(FocusModeType.TIMER) }
    var pomodoroConfig by remember { mutableStateOf(PomodoroConfig()) }

    var isStopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var isStopwatchFocusActive by rememberSaveable { mutableStateOf(false) }
    var stopwatchBaseTime by rememberSaveable { mutableStateOf(0L) }
    var stopwatchAccumulatedMillis by rememberSaveable { mutableStateOf(0L) }
    var stopwatchElapsedMillis by rememberSaveable { mutableStateOf(0L) }

    var selectedSubjectName by rememberSaveable { mutableStateOf("All") }
    var initialTotalSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }

    // 🍅 पोमोडोरो लाइव स्टेट्स (TimerService से सिंक)
    val isBreakActive = TimerService.isPomoBreakActive.value
    val currentSessionIndex = TimerService.currentPomoRound.intValue

    // 🌟 मिनी बार के लिए स्टेट (बैक दबाने पर मिनीमाइज़ होगा)
    var isFocusMinimized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            ThemeManager.loadTheme(context)
            TimerService.syncRemainingTime(context)

            if (TimerService.originalTimerSeconds == 0 && (TimerService.isTimerRunning.value || TimerService.isTimerPaused.value)) {
                activeMode = FocusModeType.STOPWATCH
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
                isFocusMinimized = false
            } else if (isPaused && isStopwatchRunning) {
                isStopwatchRunning = false
                stopwatchAccumulatedMillis = stopwatchElapsedMillis
            } else if (isRunning && !isStopwatchRunning) {
                stopwatchBaseTime = SystemClock.elapsedRealtime()
                isStopwatchRunning = true
            }
        } else if (!isRunning && !isPaused) {
            isFocusMinimized = false
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
    val isFullScreenFocus = isFocusActive && !isFocusMinimized

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

        if (isStopwatchFocusActive) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            context.startService(intent)

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
        isFocusMinimized = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            activity?.window?.let { win ->
                win.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
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

    // 🔙 BACK BUTTON:
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
    val swMainTime = String.format("%02d:%02d:%02d", swH, swM, swS)

    val liveBarTime = if (activeMode == FocusModeType.STOPWATCH) swMainTime else "$timeFormatted left"

    Scaffold(
        containerColor = bgColor,
        contentWindowInsets = if (isFullScreenFocus) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (!isFullScreenFocus) {
                Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
                    // 🌟 जब सेशन चल रहा हो और मिनीमाइज़ हो तो बॉटम बार के ऊपर मिनी बार दिखेगी
                    if (isFocusActive && isFocusMinimized) {
                        MiniFocusBar(
                            subjectName = if (isBreakActive) "Break Time" else selectedSubjectName,
                            timeText = liveBarTime,
                            isRunning = if (activeMode == FocusModeType.STOPWATCH) isStopwatchRunning else isRunning,
                            isPaused = if (activeMode == FocusModeType.STOPWATCH) !isStopwatchRunning else isPaused,
                            goldColor = if (isBreakActive) Color(0xFF10B981) else goldColor,
                            isDark = isDark,
                            onTogglePlayPause = {
                                FocusSoundManager.triggerHaptic(context, false)
                                if (activeMode == FocusModeType.STOPWATCH) {
                                    if (isStopwatchRunning) {
                                        isStopwatchRunning = false
                                        stopwatchAccumulatedMillis = stopwatchElapsedMillis
                                    } else {
                                        stopwatchBaseTime = SystemClock.elapsedRealtime()
                                        isStopwatchRunning = true
                                    }
                                } else {
                                    val act = if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                                    context.startService(Intent(context, TimerService::class.java).apply { action = act })
                                }
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .then(if (!isFullScreenFocus) Modifier.padding(paddingValues) else Modifier)
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
                    isFocusActive = isFullScreenFocus,
                    activeMode = activeMode,
                    onActiveModeChange = { activeMode = it },
                    pomodoroConfig = pomodoroConfig,
                    onPomodoroConfigChange = { pomodoroConfig = it },
                    isBreakActive = isBreakActive,
                    currentSessionIndex = currentSessionIndex,
                    isStopwatchRunning = isStopwatchRunning,
                    isStopwatchFocusActive = isStopwatchFocusActive,
                    stopwatchElapsedMillis = stopwatchElapsedMillis,
                    selectedSubjectName = selectedSubjectName,
                    onSelectedSubjectChange = { selectedSubjectName = it },
                    initialTotalSeconds = initialTotalSeconds,
                    onInitialTotalSecondsChange = { initialTotalSeconds = it },
                    onStartStopwatch = {
                        isStopwatchRunning = true
                        isStopwatchFocusActive = true
                        stopwatchAccumulatedMillis = 0L
                        stopwatchElapsedMillis = 0L
                        stopwatchBaseTime = SystemClock.elapsedRealtime()
                        isFocusMinimized = false
                    },
                    onPauseStopwatch = {
                        isStopwatchRunning = false
                        stopwatchAccumulatedMillis = stopwatchElapsedMillis
                    },
                    onResumeStopwatch = {
                        stopwatchBaseTime = SystemClock.elapsedRealtime()
                        isStopwatchRunning = true
                    },
                    onDirectEndSession = onDirectEndSession
                )
                1 -> GroupsRootScreen(onStartFocus = { currentNavIndex = 0 })
                2 -> StatsScreen()
                3 -> ProfileScreen()
            }
        }
    }

    // 🎉 CELEBRATION POPUP DIALOG (5-SECOND AUTO-DISMISS)
    if (TimerService.showCompletionDialogTrigger.value) {
        LaunchedEffect(Unit) {
            delay(5000L)
            TimerService.showCompletionDialogTrigger.value = false
        }

        Dialog(onDismissRequest = { TimerService.showCompletionDialogTrigger.value = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (isDark) Color(0xFF161622) else Color.White)
                    .border(1.5.dp, goldColor, RoundedCornerShape(26.dp))
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 Congratulations!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = goldColor
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                                .clickable { TimerService.showCompletionDialogTrigger.value = false }
                        ) {
                            Text("✕", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val mins = TimerService.lastCompletedMinutes.intValue
                    val mode = TimerService.lastCompletedMode.value
                    val rounds = TimerService.lastCompletedRounds.intValue

                    val detailMsg = if (mode == "POMODORO") {
                        "You completed $rounds rounds of Pomodoro!\nTotal: $mins minutes focused."
                    } else {
                        "You completed a $mins minute focus session!"
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
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Awesome! 🎯", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 🌟 MINIMAL MINI FOCUS FLOATING BAR (बिना किसी गोल आइकन के, एकदम क्लीन)
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
            // 1. Play / Pause Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(goldColor.copy(alpha = 0.2f))
                    .clickable { onTogglePlayPause() }
            ) {
                Text(
                    text = if (isPaused) "▶" else "⏸",
                    color = goldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // 2. Subject Name • Time
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

            // 3. Expand Button (˄)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                    .clickable { onExpand() }
            ) {
                Text(
                    text = "˄",
                    color = textMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
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

    var isSoundOn by remember { mutableStateOf(FocusSoundManager.isSoundEnabled(context)) }
    var showSoundDialog by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                FocusSoundManager.saveSoundUri(context, uri)
                FocusSoundManager.playCompletionSound(context)
            }
        }
    }

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
                // TOP HEADER
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SOUND BUTTON
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSoundOn) goldColor.copy(alpha = 0.18f) else (if (isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9)))
                                .border(1.dp, if (isSoundOn) goldColor else glassBorder, RoundedCornerShape(50))
                                .clickable {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    showSoundDialog = true
                                }
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = if (isSoundOn) "🔔 Sound" else "🔕 Mute",
                                color = if (isSoundOn) goldColor else textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // STREAK
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subject Bar
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

                // Center Watches Container
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimerWatchesContainer(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = { nextMins ->
                            dialMinutes = nextMins
                            val secs = nextMins.toInt() * 60
                            TimerService.remainingSeconds.intValue = secs
                            onInitialTotalSecondsChange(secs)
                        },
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = false,
                        isCustomMode = false,
                        onCustomModeToggle = { showFocusLauncherSheet = true },
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

                Spacer(modifier = Modifier.height(14.dp))

                // START FOCUS BUTTON
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
        } else {
            // Fullscreen Active Focus View
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

                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                        Text(text = swMainTime, color = textMain, fontSize = 52.sp, fontWeight = FontWeight.Black)
                        Text(text = swMilliText, color = goldColor, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp))
                    }

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

                    if (activeMode == FocusModeType.POMODORO) {
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

                    // 🎯 बटन कंट्रोल्स (ब्रेक होने पर Skip Break बटन दिखेगा)
                    if (isBreakActive) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // ⏭ SKIP BREAK BUTTON
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
                        // नॉर्मल फोकस समय के बटन
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

        // FOCUS LAUNCHER BOTTOM SHEET
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
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_SECONDS, 0)
                                putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                putExtra(TimerService.EXTRA_MODE, "STOPWATCH")
                            })
                        }
                        FocusModeType.TIMER -> {
                            onInitialTotalSecondsChange(durationSeconds)
                            TimerService.remainingSeconds.intValue = durationSeconds
                            isFocusMinimized = false
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
                            isFocusMinimized = false
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

        // SOUND DIALOG
        if (showSoundDialog) {
            Dialog(onDismissRequest = {
                FocusSoundManager.stopSound()
                showSoundDialog = false
            }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isDark) Color(0xFF1C1C28) else Color.White)
                        .border(1.5.dp, goldColor, RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Session Alert Sound 🔔", fontWeight = FontWeight.Black, fontSize = 17.sp, color = textMain)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Play Sound on Completion", color = textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = isSoundOn,
                                onCheckedChange = {
                                    isSoundOn = it
                                    FocusSoundManager.setSoundEnabled(context, it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = goldColor)
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, FocusSoundManager.getSelectedSoundUri(context))
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2A2A3C) else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🎵 Choose from Device Ringtones", color = textMain, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                FocusSoundManager.playCompletionSound(context)
                                FocusSoundManager.triggerHaptic(context, isCompletion = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = goldColor.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, goldColor),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("▶ Test Sound & Haptic Buzz", color = goldColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                FocusSoundManager.stopSound()
                                showSoundDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }

        // Subject Add Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF181824) else Color.White)
                        .border(1.5.dp, goldColor, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Add Subject", color = goldColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newSubjectInput,
                            onValueChange = { newSubjectInput = it },
                            placeholder = { Text("Subject name...", color = textMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onAddSubjectAction()
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Save Subject", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
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
