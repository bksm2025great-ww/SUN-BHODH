package com.amon.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    // 🔒 मास्टर फ़ोकस लॉक
    val isFocusActive = isRunning || isPaused

    // 🔕 TRUE IMMERSIVE MODE (तैरता हुआ सिस्टम बार - बिना किसी कटी खाली जगह के)
    DisposableEffect(isFocusActive) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isFocusActive) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
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

    val appPrefs = remember {
        context.getSharedPreferences("amon_app_prefs", Context.MODE_PRIVATE)
    }
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
        // 🛡️ 0 Insets ताकि स्क्रीन पर कोई कटी पट्टी न बचे
        contentWindowInsets = if (isFocusActive) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (!isFocusActive) {
                AmonCurvedBottomBar(
                    selectedIndex = currentNavIndex,
                    onTabSelected = { newIndex -> currentNavIndex = newIndex },
                    isDark = isDark,
                    goldColor = goldColor,
                    cardBg = cardBg,
                    borderCol = glassBorder
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .then(
                    if (!isFocusActive) Modifier.padding(paddingValues) else Modifier
                )
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
                        appPrefs.edit().putString("whats_new_seen_version", currentVersion).apply()
                        showWhatsNew = false
                    }
                )
            }
        }
    }
}

// =============================================================================
// 🟢 1. HOME TAB (TIMER, DYNAMIC SUBJECTS & CONTROLS)
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity

    var userSubjects by remember { mutableStateOf(listOf<String>()) }
    var selectedSubjectName by rememberSaveable { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGiveUpDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    val subjectPrefs = remember {
        context.getSharedPreferences("amon_subject_prefs", Context.MODE_PRIVATE)
    }

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

    val toggleOrientation = {
        activity?.let { act ->
            act.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isFocusActive) {
            // ----------------- NORMAL PORTRAIT / LANDSCAPE MODE -----------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.4.dp, goldColor, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_mascot_amon),
                                    contentDescription = "Amon Mascot",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                            val timeGreeting = remember(currentHour) {
                                when (currentHour) {
                                    in 4..11 -> "Good Morning ☀️"
                                    in 12..16 -> "Good Afternoon 🌤️"
                                    in 17..19 -> "Good Evening 🌆"
                                    else -> "Good Night 🌙"
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AMON",
                                    color = textMain,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "• $timeGreeting",
                                    color = goldColor,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // STREAK CAPSULE
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
                                .border(1.dp, glassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
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
                }

                // DYNAMIC SUBJECT RIBBON
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
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
                }

                // WATCH CONTAINER
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
                    timeFormatted = timeFormatted,
                    hours = hours
                )

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
                            val intent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_START
                                initialTotalSeconds = if (totalSeconds > 0) totalSeconds else (dialMinutes.toInt() * 60)
                                putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
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
                            text = "Start Focus",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            // =================================================================
            // 🎯 MASTER FOCUS SCREEN (PORTRAIT & LANDSCAPE 100% BALANCED)
            // =================================================================
            if (!isLandscape) {
                // 📱 PORTRAIT MODE: Clean Center + Lifted Buttons (+20dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // TOP BAR: Subject Pill + Rotate Icon
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Center Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
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

                        // Top-Right Rotate Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                .border(1.2.dp, goldColor.copy(alpha = 0.8f), CircleShape)
                                .clickable { toggleOrientation() }
                        ) {
                            ScreenOrientationIcon(tint = goldColor)
                        }
                    }

                    // CENTER WATCH
                    Box(
                        modifier = Modifier.weight(1f),
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
                            hours = hours
                        )
                    }

                    // BOTTOM ACTION CONTROLS (LIFTED & ERGONOMIC)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ⏸️ / ▶ Button
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

                            // ⏹ Stop Button
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
                // 🔄 LANDSCAPE MODE: 3-Zone Cinema Layout (Left Subject + Center Huge Clock + Right Dock)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // 1. TOP-LEFT SUBJECT (Relaxed Padding)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
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

                    // 2. CENTER CLOCK (Full Height Maximum Freedom)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 70.dp),
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
                            hours = hours
                        )
                    }

                    // 3. RIGHT SIDE-DOCK: Vertical Controls Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 🔄 Rotate Screen Button (Top)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF16161B) else Color(0xFFF1F5F9))
                                .border(1.2.dp, goldColor.copy(alpha = 0.8f), CircleShape)
                                .clickable { toggleOrientation() }
                        ) {
                            ScreenOrientationIcon(tint = goldColor)
                        }

                        // Middle & Bottom Actions
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // ⏸️ / ▶ Pause-Play Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
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
                                        Box(modifier = Modifier.width(3.dp).height(15.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
                                        Box(modifier = Modifier.width(3.dp).height(15.dp).clip(RoundedCornerShape(1.dp)).background(textMain))
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

                            // ⏹️ Stop Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
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

        AnimatedVisibility(
            visible = showSoundPanel,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 114.dp, end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardBg)
                    .border(1.5.dp, glassBorder, RoundedCornerShape(26.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSoundOn) goldColor.copy(alpha = 0.25f)
                                else (if (isDark) Color(0xFF2A2A32) else Color(0xFFE2E8F0))
                            )
                            .clickable { isSoundOn = !isSoundOn }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isSoundOn) "ON" else "OFF",
                            color = if (isSoundOn) goldColor else textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSoundOn) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val soundOptions = listOf(
                        Pair("Rain", "🌧️"),
                        Pair("Forest", "🌲"),
                        Pair("Clock Tick", "⏱️"),
                        Pair("White Noise", "🌊"),
                        Pair("Focus Piano", "🎹")
                    )

                    soundOptions.forEach { (name, icon) ->
                        val isSelected = isSoundOn && selectedSound == name
                        val tileBg = if (isSelected) {
                            goldColor
                        } else {
                            if (isDark) Color(0xFF24242A) else Color(0xFFF1F5F9)
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(tileBg)
                                .border(
                                    1.dp,
                                    if (isSelected) glowYellow else glassBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedSound = name
                                    isSoundOn = true
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.Black else textMain,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 🎨 POPUP DIALOG (MANAGE SUBJECTS)
        // =====================================================================
        if (showAddDialog) {
            Dialog(onDismissRequest = {
                showAddDialog = false
                newSubjectInput = ""
            }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.5.dp, glassBorder, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Manage Subjects",
                            color = goldColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to Show (Color) or Hide (Plain)",
                            color = textMuted,
                            fontSize = 13.sp
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
                                            if (isDark) Color(0xFF222228) else Color(0xFFF1F5F9)
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
                                .background(if (isDark) Color(0xFF18181B) else Color(0xFFF8FAFC))
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
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
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
                                    .clickable {
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
                                                showAddDialog = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Add",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                showAddDialog = false
                                newSubjectInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF2A2A32) else Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Done", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                        val resetSecs = dialMinutes.toInt() * 60
                                        TimerService.remainingSeconds.intValue = resetSecs
                                        initialTotalSeconds = resetSecs
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
// 🔄 SCREEN ORIENTATION VECTOR ICON (HAND-DRAWN CANVAS ART)
// =============================================================================
@Composable
private fun ScreenOrientationIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.28f, h * 0.18f),
            size = Size(w * 0.44f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke)
        )

        val topArrow = Path().apply {
            moveTo(w * 0.16f, h * 0.38f)
            cubicTo(w * 0.14f, h * 0.20f, w * 0.28f, h * 0.10f, w * 0.44f, h * 0.08f)
        }
        drawPath(topArrow, color = tint.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round))

        val botArrow = Path().apply {
            moveTo(w * 0.84f, h * 0.62f)
            cubicTo(w * 0.86f, h * 0.80f, w * 0.72f, h * 0.90f, w * 0.56f, h * 0.92f)
        }
        drawPath(botArrow, color = tint.copy(alpha = 0.85f), style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round))
    }
}

// =============================================================================
// 🔥 ANIMATED LIVING FLAME ICON
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
            colors = listOf(
                topYellow.copy(alpha = 0.95f),
                midOrange,
                bottomRed
            ),
            startY = (h * 0.15f) * (1f - colorPhase),
            endY = h
        )

        val innerBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFDE7),
                Color(0xFFFFEE58),
                Color(0xFFFFA726)
            ),
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
// ✨ 2. WHAT'S NEW DIALOG
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

                Text(
                    text = "Built for deeper focus & zero distractions",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WhatsNewItem(
                        icon = "👑",
                        title = "3 Luxury Watch Styles",
                        description = "Swipe between Classic Ring, Rotary Dial, and Retro Desk Flip Clock.",
                        textMain = textMain,
                        textMuted = textMuted
                    )

                    WhatsNewItem(
                        icon = "🎛️",
                        title = "Silent Rotary 0–180m Dial",
                        description = "Smooth circular touch with zero noise and a clean minimal display.",
                        textMain = textMain,
                        textMuted = textMuted
                    )

                    WhatsNewItem(
                        icon = "📜",
                        title = "3D Split-Flap Desk Clock",
                        description = "Authentic mechanical flip simulation with tap-to-type input and minimal Play/Stop controls.",
                        textMain = textMain,
                        textMuted = textMuted
                    )

                    WhatsNewItem(
                        icon = "🍩",
                        title = "Subject Breakdown Chart",
                        description = "Modern donut analytics in Stats to track your study balance across days, weeks & months.",
                        textMain = textMain,
                        textMuted = textMuted
                    )

                    WhatsNewItem(
                        icon = "📦",
                        title = "Direct APK Share",
                        description = "Share Amon directly to WhatsApp & Quick Share without external links.",
                        textMain = textMain,
                        textMuted = textMuted
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

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

@Composable
private fun WhatsNewItem(
    icon: String,
    title: String,
    description: String,
    textMain: Color,
    textMuted: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 20.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textMain,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = textMuted,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// =============================================================================
// 🟢 3. CURVED BOTTOM BAR
// =============================================================================
@Composable
fun AmonCurvedBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    goldColor: Color,
    cardBg: Color,
    borderCol: Color
) {
    val tabItems = listOf(
        Triple("Home", R.drawable.ic_nav_home, 0),
        Triple("Forest", R.drawable.ic_nav_forest, 1),
        Triple("Stats", R.drawable.ic_nav_stats, 2),
        Triple("Profile", R.drawable.ic_nav_profile, 3)
    )

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
                cubicTo(
                    left, startY,
                    left, startY + notchRadius * 0.95f,
                    notchCenterX, startY + notchRadius * 0.95f
                )
                cubicTo(
                    right, startY + notchRadius * 0.95f,
                    right, startY,
                    right + shoulderWidth, startY
                )

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
                val activeIconRes = tabItems[selectedIndex].second
                Image(
                    painter = painterResource(id = activeIconRes),
                    contentDescription = tabItems[selectedIndex].first,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabItems.forEachIndexed { index, item ->
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
                            Image(
                                painter = painterResource(id = item.second),
                                contentDescription = item.first,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.first,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = inactiveColor
                            )
                        } else {
                            Spacer(modifier = Modifier.height(26.dp))
                            Text(
                                text = item.first,
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
// 🔥 Helper: Streak Calculator (Min 30 mins / day)
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
