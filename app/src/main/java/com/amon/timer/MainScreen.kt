package com.amon.timer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentNavIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ThemeManager.loadTheme(context)
        TimerService.syncRemainingTime()
    }

    val isDark = ThemeManager.isDarkTheme.value
    val isRunning = TimerService.isTimerRunning.value

    // 🔙 बैक गेस्चर: किसी भी टैब से बैक करने पर होम (टाइमर) पर लौटेगा
    BackHandler(enabled = currentNavIndex != 0 && !isRunning) {
        currentNavIndex = 0
    }

    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val glassBorder = if (isDark) Color(0x33F3C669) else Color(0xFFCBD5E1)
    val glowYellow = if (ThemeManager.currentTheme.value == "Classic Yellow") Color(0xFFFDE68A) else Color(0xFFFFE082)

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

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            if (!isRunning) {
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
                .padding(paddingValues)
        ) {
            when (currentNavIndex) {
                0 -> HomeTimerTab(context, goldColor, glowYellow, cardBg, glassBorder, textMuted, textMain, isDark, isRunning)
                1 -> ForestScreen()
                2 -> StatsScreen()
                3 -> ProfileScreen()
            }

            if (showWhatsNew && !isRunning) {
                WhatsNewDialog(
                    versionName = currentVersion,
                    goldColor = goldColor,
                    glowYellow = glowYellow,
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
// 🟢 1. HOME TAB (TIMER & SMART RANKED SUBJECTS)
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
    isRunning: Boolean
) {
    var userSubjects by remember { mutableStateOf(listOf<String>()) }
    var selectedSubjectName by rememberSaveable { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGiveUpDialog by remember { mutableStateOf(false) }
    var newSubjectInput by remember { mutableStateOf("") }

    val subjectPrefs = remember {
        context.getSharedPreferences("amon_subject_prefs", Context.MODE_PRIVATE)
    }
    var hiddenSubjects by remember {
        mutableStateOf(subjectPrefs.getStringSet("hidden_subjects", emptySet())?.toSet() ?: emptySet())
    }

    // ⌚ वर्तमान में चुनी गई घड़ी की शैली ट्रैक करना (0: Ring, 1: Rotary, 2: Flip)
    val watchPrefs = remember {
        context.getSharedPreferences("amon_watch_prefs", Context.MODE_PRIVATE)
    }
    var selectedWatchStyle by remember {
        mutableIntStateOf(watchPrefs.getInt("selected_watch_style", 0))
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "selected_watch_style") {
                selectedWatchStyle = watchPrefs.getInt("selected_watch_style", 0)
            }
        }
        watchPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            watchPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var isSoundOn by rememberSaveable { mutableStateOf(false) }
    var showSoundPanel by remember { mutableStateOf(false) }
    var selectedSound by rememberSaveable { mutableStateOf("Rain") }

    BackHandler(enabled = isRunning) {
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
    LaunchedEffect(isRunning) {
        if (!isRunning) {
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

    val allSessions = remember(isRunning) { FocusSessionManager.getAllSessions(context) }
    val subjectMinutesMap = remember(allSessions) {
        allSessions.groupBy { it.subject }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
    }
    val activeSortedSubjects = remember(userSubjects, hiddenSubjects, subjectMinutesMap) {
        userSubjects
            .filter { it !in hiddenSubjects }
            .sortedByDescending { subjectMinutesMap[it] ?: 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ----------------- TOP BRACKET / HEADER -----------------
            if (!isRunning) {
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

                        // Streak Capsule
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0x33F5A524) else Color(0x22D97706))
                                .border(1.2.dp, goldColor, RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(text = "🔥", fontSize = 11.sp)
                                Text(
                                    text = "$streakDays",
                                    color = goldColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ----------------- DYNAMIC SUBJECT RIBBON -----------------
            if (!isRunning) {
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
                                        text = "+ Add",
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
                                        .border(
                                            1.dp,
                                            if (isSelected) glowYellow else glassBorder,
                                            RoundedCornerShape(50)
                                        )
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
            } else {
                // 🌟 मिनिमल और क्लासी सब्जेक्ट पिल
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
            }

            // ----------------- 🕰️ CENTRAL WATCH CONTAINER -----------------
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
                isRunning = isRunning,
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

            // ----------------- ACTION BUTTONS -----------------
            val isFlipClock = (selectedWatchStyle == 2)

            if (isFlipClock) {
                // 🌟 फ्लिप क्लॉक स्पेशल: मिनिमल गोल Play (▶) और Stop (⏹) बटन
                if (!isRunning) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .shadow(
                                elevation = 14.dp,
                                shape = CircleShape,
                                spotColor = goldColor,
                                ambientColor = goldColor
                            )
                            .clip(CircleShape)
                            .background(goldColor)
                            .border(1.2.dp, glowYellow, CircleShape)
                            .clickable {
                                val intent = Intent(context, TimerService::class.java)
                                initialTotalSeconds = if (totalSeconds > 0) totalSeconds else (dialMinutes.toInt() * 60)
                                intent.action = TimerService.ACTION_START
                                intent.putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
                                intent.putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            }
                    ) {
                        Text(
                            text = "▶",
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.offset(x = 2.dp)
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF241616) else Color(0xFFFEE2E2))
                            .border(1.4.dp, Color(0xFFEF4444).copy(alpha = 0.7f), CircleShape)
                            .clickable {
                                showGiveUpDialog = true
                            }
                    ) {
                        Text(
                            text = "⏹",
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // ⏱️ क्लासिक रिंग और रोटरी डायल के लिए मानक चौड़ा बटन
                if (!isRunning) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(53.dp)
                            .shadow(
                                elevation = 14.dp,
                                shape = RoundedCornerShape(50),
                                spotColor = goldColor,
                                ambientColor = goldColor
                            )
                            .clip(RoundedCornerShape(50))
                            .background(goldColor)
                            .border(1.dp, glowYellow, RoundedCornerShape(50))
                            .clickable {
                                val intent = Intent(context, TimerService::class.java)
                                initialTotalSeconds = if (totalSeconds > 0) totalSeconds else (dialMinutes.toInt() * 60)
                                intent.action = TimerService.ACTION_START
                                intent.putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
                                intent.putExtra(TimerService.EXTRA_SUBJECT, selectedSubjectName)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            }
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Start Focus 🎯",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(53.dp)
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0xFF221616) else Color(0xFFFEE2E2))
                            .border(1.4.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable {
                                showGiveUpDialog = true
                            }
                    ) {
                        Text(
                            text = "Give Up",
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    ) {
                        showSoundPanel = false
                    }
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
                    .border(1.5.dp, goldColor, RoundedCornerShape(26.dp))
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
                        .border(1.5.dp, goldColor, RoundedCornerShape(24.dp))
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
                            text = "Tap to Show (Gold) or Hide (Plain)",
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
                                onValueChange = { newSubjectInput = it },
                                placeholder = {
                                    Text("Type new subject...", color = textMuted, fontSize = 13.sp)
                                },
                                singleLine = true,
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
                                            val cleanInput = newSubjectInput.trim()
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
        // ⚠️ DISCIPLINE WARNING POPUP (100% ENGLISH TRANSLATION)
        // =====================================================================
        if (showGiveUpDialog) {
            Dialog(onDismissRequest = { showGiveUpDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.6.dp, goldColor, RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "End Focus Session? ⚠️",
                            color = goldColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "A focus session is currently in progress. Giving up now will forfeit this session.\n\nAre you sure you want to end your focus?",
                            color = textMain,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0xFF2A1C1C) else Color(0xFFFEE2E2))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .clickable {
                                        showGiveUpDialog = false
                                        val intent = Intent(context, TimerService::class.java).apply {
                                            action = TimerService.ACTION_STOP
                                        }
                                        context.startService(intent)
                                        val resetSecs = dialMinutes.toInt() * 60
                                        TimerService.remainingSeconds.intValue = resetSecs
                                        initialTotalSeconds = resetSecs
                                    }
                            ) {
                                Text(
                                    text = "End Session",
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(goldColor)
                                    .clickable {
                                        showGiveUpDialog = false
                                    }
                            ) {
                                Text(
                                    text = "Keep Focusing 🎯",
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
