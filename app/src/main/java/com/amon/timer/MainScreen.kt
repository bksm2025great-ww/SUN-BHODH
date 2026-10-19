package com.amon.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    var currentNavIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ThemeManager.loadTheme(context)
        TimerService.syncRemainingTime(context)
    }

    val isDark = ThemeManager.isDarkTheme.value
    val isRunning = TimerService.isTimerRunning.value
    val isPaused = TimerService.isTimerPaused.value
    val isFocusActive = isRunning || isPaused

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
        val lastSeenVersion = appPrefs.getString("whats_new_seen_version", "")
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
                    isFocusActive = isFocusActive
                )
                1 -> AmonGardenComingSoonTab(
                    goldColor = goldColor,
                    isDark = isDark
                )
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
                        appPrefs.edit().putString("whats_new_seen_version", currentVersion).apply()
                        showWhatsNew = false
                    }
                )
            }
        }
    }
}

// =============================================================================
// 🌲 AMON GARDEN • COMING SOON TAB
// =============================================================================
@Composable
fun AmonGardenComingSoonTab(
    goldColor: Color,
    isDark: Boolean
) {
    val gardenBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF132D1B),
            Color(0xFF0A1C10),
            Color(0xFF061009)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gardenBg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF422E1B),
                border = BorderStroke(2.dp, Color(0xFF6E5030)),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Amon Garden",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF3E5AB),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF131A15).copy(alpha = 0.94f),
                border = BorderStroke(1.2.dp, Color(0xFF334B3B)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌲", fontSize = 54.sp)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "FOREST EXPEDITION",
                        color = Color(0xFF65C887),
                        fontSize = 11.5.sp,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Coming Soon ✨",
                        color = Color(0xFFFFD56A),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "We are hand-crafting deep focus landscapes & lush garden ecosystems.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Stay focused and build your study discipline while the forest grows!",
                        color = Color(0xFFA0AEC0),
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1E2820),
                        border = BorderStroke(1.dp, Color(0xFF8D733E))
                    ) {
                        Text(
                            text = "🚀 In Active Development",
                            color = Color(0xFFE8C576),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 🟢 1. HOME TAB (TIMER, STOPWATCH, POMODORO MODES & DYNAMIC SUBJECTS)
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
    isFocusActive: Boolean
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
    var customMinutes by rememberSaveable { mutableIntStateOf(25) }
    var customSeconds by rememberSaveable { mutableIntStateOf(0) }

    val subjectPrefs = remember { context.getSharedPreferences("amon_subject_prefs", Context.MODE_PRIVATE) }

    var hiddenSubjects by remember {
        val saved = subjectPrefs.getStringSet("hidden_subjects", null)
        if (saved == null) {
            val initialHidden = SubjectManager.getUserSubjects(context).toSet()
            subjectPrefs.edit().putStringSet("hidden_subjects", initialHidden).apply()
            mutableStateOf(initialHidden)
        } else {
            mutableStateOf(saved)
        }
    }

    var isSoundOn by rememberSaveable { mutableStateOf(false) }
    var showSoundPanel by remember { mutableStateOf(false) }
    var selectedSound by rememberSaveable { mutableStateOf("Rain") }

    BackHandler(enabled = isFocusActive) {
        showGiveUpDialog = true
    }

    LaunchedEffect(Unit) {
        userSubjects = SubjectManager.getUserSubjects(context)
    }

    var dialMinutes by rememberSaveable { mutableFloatStateOf(25f) }
    var isCustomMode by rememberSaveable { mutableStateOf(false) }
    var initialTotalSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }

    val totalSeconds = TimerService.remainingSeconds.intValue

    var streakDays by remember { mutableIntStateOf(0) }
    LaunchedEffect(isFocusActive) {
        if (!isFocusActive) {
            val sessions = FocusSessionManager.getAllSessions(context)
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

    val allSessions = remember(isFocusActive) { FocusSessionManager.getAllSessions(context) }
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
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
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
                            AnimatedLivingFlame()
                            Text(
                                text = "$streakDays",
                                color = if (streakDays > 0) Color(0xFFFB923C) else textMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // DYNAMIC SUBJECT RIBBON
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
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

                // 🎛️ TIMER, STOPWATCH & POMODORO MODE SELECTOR BAR
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = cardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, glassBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modes = listOf("Timer", "Stopwatch", "Pomodoro")
                            modes.forEachIndexed { index, modeName ->
                                val isSelected = selectedFocusMode == index
                                Button(
                                    onClick = { selectedFocusMode = index },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) goldColor else (if (isDark) Color(0xFF22222A) else Color(0xFFE2E8F0))
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = modeName,
                                        color = if (isSelected) Color.Black else textMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (selectedFocusMode == 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { if (customMinutes > 1) customMinutes-- }) {
                                        Text("-", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${customMinutes}m", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    TextButton(onClick = { customMinutes++ }) {
                                        Text("+", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { if (customSeconds > 0) customSeconds-- }) {
                                        Text("-", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${customSeconds}s", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    TextButton(onClick = { if (customSeconds < 59) customSeconds++ }) {
                                        Text("+", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // WATCH CONTAINER
                when (selectedFocusMode) {
                    0, 2 -> {
                        TimerWatchesContainer(
                            dialMinutes = if (selectedFocusMode == 2) customMinutes.toFloat() else dialMinutes,
                            onDialMinutesChange = { nextMins ->
                                dialMinutes = nextMins
                                val secs = if (nextMins == 0f) 0 else (nextMins.toInt() * 60)
                                TimerService.remainingSeconds.intValue = secs
                                initialTotalSeconds = if (secs > 0) secs else 60
                            },
                            totalSeconds = if (selectedFocusMode == 2) (customMinutes * 60 + customSeconds) else totalSeconds,
                            initialTotalSeconds = initialTotalSeconds,
                            isRunning = false,
                            isCustomMode = isCustomMode,
                            onCustomModeToggle = { isCustomMode = !isCustomMode },
                            goldColor = goldColor,
                            glowYellow = glowYellow,
                            cardBg = cardBg,
                            glassBorder = glassBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            isDark = isDark,
                            timeFormatted = if (selectedFocusMode == 2) String.format("%02d:%02d", customMinutes, customSeconds) else timeFormatted,
                            hours = hours,
                            selectedSubjectName = selectedSubjectName
                        )
                    }
                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("STOPWATCH COUNT", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("00:00:00", color = textMain, fontSize = 42.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // START FOCUS BUTTON
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
                            val targetSecs = if (selectedFocusMode == 2) (customMinutes * 60 + customSeconds) else totalSeconds
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

        // =====================================================================
        // 🎧 SOUND OVERLAY PANEL
        // =====================================================================
        if (showSoundPanel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSoundPanel = false }
            )
        }

        // =====================================================================
        // 🎨 FROSTED TRANSLUCENT POPUP (MANAGE SUBJECTS)
        // =====================================================================
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

        // =====================================================================
        // ⚠️ DISCIPLINE WARNING POPUP (END SESSION)
        // =====================================================================
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
                            text = "End Focus Session? ⏱️",
                            color = textMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Giving up now will forfeit this focus session. Are you sure you want to stop?",
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
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_STOP
                                        }
                                        context.startService(intent)
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
// 🔥 2. ANIMATED LIVING FLAME ICON
// =============================================================================
@Composable
private fun AnimatedLivingFlame(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame_anim")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    val sway by infiniteTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(440, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_sway"
    )

    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_color"
    )

    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height

        val topYellow = Color(0xFFFFEB3B)
        val midOrange = Color(0xFFFF9800)
        val bottomRed = Color(0xFFE53935)

        val outerBrush = Brush.verticalGradient(
            colors = listOf(topYellow.copy(alpha = 0.95f), midOrange, bottomRed),
            startY = (h * 0.15f) * (1f - colorPhase),
            endY = h
        )

        val innerBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFEE58), Color(0xFFFFA726)),
            startY = h * 0.35f,
            endY = h * 0.95f
        )

        val tipX = (w * 0.5f) + (sway * (w / 16f))
        val tipY = h * 0.04f * (2f - scale)

        val outerFlame = Path().apply {
            moveTo(tipX, tipY)
            cubicTo(w * 0.86f, h * 0.35f, w * 0.96f, h * 0.65f, w * 0.76f, h * 0.90f)
            cubicTo(w * 0.64f, h * 1.02f, w * 0.36f, h * 1.02f, w * 0.24f, h * 0.90f)
            cubicTo(w * 0.04f, h * 0.65f, w * 0.14f, h * 0.35f, tipX, tipY)
            close()
        }
        drawPath(path = outerFlame, brush = outerBrush)

        val innerTipX = (w * 0.5f) + (sway * 0.4f * (w / 16f))
        val innerFlame = Path().apply {
            moveTo(innerTipX, h * 0.38f)
            cubicTo(w * 0.70f, h * 0.52f, w * 0.74f, h * 0.72f, w * 0.62f, h * 0.86f)
            cubicTo(w * 0.56f, h * 0.94f, w * 0.44f, h * 0.94f, w * 0.38f, h * 0.86f)
            cubicTo(w * 0.26f, h * 0.72f, w * 0.30f, h * 0.52f, innerTipX, h * 0.38f)
            close()
        }
        drawPath(path = innerFlame, brush = innerBrush)
    }
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
// 🟢 4. SIMPLE VECTOR ICON COMPOSABLE (बिना किसी इमेज फाइल के)
// =============================================================================
@Composable
private fun SimpleNavIcon(
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
            // 🌲 सिंपल और क्रिस्प मिनिमल ट्री आइकन (प्योर कैनवास)
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height

                // पेड़ का तना
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.42f, h * 0.72f),
                    size = Size(w * 0.16f, h * 0.22f)
                )

                // पेड़ के पत्ते (त्रिकोण)
                val treePath = Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    lineTo(w * 0.85f, h * 0.70f)
                    lineTo(w * 0.15f, h * 0.70f)
                    close()
                }
                drawPath(treePath, color = tint)
            }
        }
        2 -> {
            // 📊 सिंपल और क्लीन 3-बार एनालिटिक्स चार्ट (प्योर कैनवास)
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val barW = w * 0.20f
                val gap = w * 0.12f
                val startX = w * 0.10f

                // बार 1 (मध्यम)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(startX, h * 0.45f),
                    size = Size(barW, h * 0.50f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // बार 2 (ऊँचा)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(startX + barW + gap, h * 0.15f),
                    size = Size(barW, h * 0.80f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // बार 3 (छोटा)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(startX + (barW + gap) * 2f, h * 0.60f),
                    size = Size(barW, h * 0.35f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
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
// 🟢 5. CURVED BOTTOM BAR (WITH SIMPLE ICONS & FLOATING BUBBLE)
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
    val tabTitles = listOf("Home", "Forest", "Stats", "Profile")

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
        // स्मूथ कर्व पाथ
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

            // ऊपर उठता हुआ फ्लोटिंग बबल
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

            // नीचे के सामान्य इन-एक्टिव टैब्स
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
private fun calculateStreakDays(sessions: List<FocusSession>): Int {
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
