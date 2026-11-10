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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
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

    var isBreakActive by rememberSaveable { mutableStateOf(false) }
    var currentSessionIndex by rememberSaveable { mutableIntStateOf(1) }

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

    // 🔔 टाइमर पूरा होने पर साउंड और हेप्टिक
    LaunchedEffect(TimerService.remainingSeconds.intValue, isRunning) {
        if (isRunning && TimerService.remainingSeconds.intValue == 0 && initialTotalSeconds > 0) {
            FocusSoundManager.playCompletionSound(context)
            FocusSoundManager.triggerHaptic(context, isCompletion = true)
        }
    }

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
            val elapsedMins = (stopwatchElapsedMillis / 60000).toInt()
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            context.startService(intent)

            if (elapsedMins >= 1) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val session = FocusSession(
                    id = System.currentTimeMillis(),
                    date = sdf.format(Date()),
                    subject = selectedSubjectName.ifBlank { "General" },
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
                CloudSyncManager.syncSession(
                    context = context,
                    subject = selectedSubjectName.ifBlank { "General" },
                    durationMinutes = elapsedMins.toLong(),
                    earnedTrees = session.earnedTrees,
                    sessionId = session.id
                )
            }
            isStopwatchRunning = false
            isStopwatchFocusActive = false
            stopwatchAccumulatedMillis = 0L
            stopwatchElapsedMillis = 0L
        } else {
            val totalSecsPlanned = initialTotalSeconds
            val remainingSecs = TimerService.remainingSeconds.intValue
            val elapsedSecs = (totalSecsPlanned - remainingSecs).coerceAtLeast(0)
            val elapsedMins = elapsedSecs / 60

            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            context.startService(intent)

            if (elapsedMins >= 1 && !isBreakActive) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val session = FocusSession(
                    id = System.currentTimeMillis(),
                    date = sdf.format(Date()),
                    subject = selectedSubjectName.ifBlank { "General" },
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
                CloudSyncManager.syncSession(
                    context = context,
                    subject = selectedSubjectName.ifBlank { "General" },
                    durationMinutes = elapsedMins.toLong(),
                    earnedTrees = session.earnedTrees,
                    sessionId = session.id
                )
            }
            isBreakActive = false
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    DisposableEffect(isFocusActive) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val bgHex = if (isDark) "#121216" else "#F8FAFC"
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.parseColor(bgHex)))

            if (isFocusActive) {
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

    // 🔔 साउंड टॉगल और डायलॉग स्टेट्स (मेन स्क्रीन पर)
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
                // 🌟 TOP HEADER: AMON + SOUND BUTTON + STREAK
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
                        // 🔔 SOUND BUTTON
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

                // 🚀 START FOCUS BUTTON
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

        // 🚀 FOCUS LAUNCHER BOTTOM SHEET
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
                            })
                        }
                        FocusModeType.TIMER, FocusModeType.POMODORO -> {
                            onInitialTotalSecondsChange(durationSeconds)
                            TimerService.remainingSeconds.intValue = durationSeconds
                            context.startService(Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                putExtra(TimerService.EXTRA_SECONDS, durationSeconds)
                                putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                            })
                        }
                    }
                },
                onDismiss = { showFocusLauncherSheet = false }
            )
        }

        // 🎵 MAIN SCREEN SOUND DIALOG
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
