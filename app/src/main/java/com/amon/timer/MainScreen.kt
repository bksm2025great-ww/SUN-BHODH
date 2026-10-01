package com.amon.timer

import android.content.Intent
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.*

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

    // 🔙 बैक गेस्चर फिजिक्स: किसी भी टैब (Forest, Stats, Profile) से बैक स्वाइप करने पर पहले सीधे होम (टाइमर) पर आएगा
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
        }
    }
}

// =============================================================================
// 🟢 1. HOME TAB (TIMER & SMART RANKED SUBJECTS)
// =============================================================================
@Composable
fun HomeTimerTab(
    context: android.content.Context,
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
        context.getSharedPreferences("amon_subject_prefs", android.content.Context.MODE_PRIVATE)
    }
    var hiddenSubjects by remember {
        mutableStateOf(subjectPrefs.getStringSet("hidden_subjects", emptySet())?.toSet() ?: emptySet())
    }

    var isSoundOn by rememberSaveable { mutableStateOf(false) }
    var showSoundPanel by remember { mutableStateOf(false) }
    var selectedSound by rememberSaveable { mutableStateOf("Rain") }

    // 🔒 Strict Discipline Mode: Active timer के दौरान Back दबाने पर वार्निंग पॉपअप
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

    // ⏱️ घंटे, मिनट और सेकंड (HH:MM:SS या MM:SS) का स्मार्ट फॉर्मूला
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
                        // Mascot + AMON + Greeting
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
                                        .clickable {
                                            selectedSubjectName = subj
                                        }
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

                    // 🎵 Sound Button: विजिबिलिटी सुरक्षित रूप से बंद रखी गई है
                    val isSoundFeatureVisible = false
                    if (isSoundFeatureVisible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSoundOn) goldColor.copy(alpha = 0.25f) else cardBg)
                                    .border(1.2.dp, if (isSoundOn) glowYellow else goldColor.copy(alpha = 0.7f), RoundedCornerShape(50))
                                    .clickable { showSoundPanel = true }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "🎵", fontSize = 11.sp)
                                    Text(
                                        text = if (isSoundOn) "Sound: ON" else "Sound",
                                        color = goldColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(cardBg)
                        .border(1.2.dp, goldColor, RoundedCornerShape(50))
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎯 $selectedSubjectName",
                        color = goldColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ----------------- TIMER RING (258 DP) -----------------
            Box(
                modifier = Modifier
                    .size(258.dp)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f - 10.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 1. Background base circle
                    drawCircle(
                        color = if (isDark) Color(0xFF1E1E28) else Color(0xFFE2E8F0),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 5.dp.toPx())
                    )

                    val maxSeconds = if (initialTotalSeconds > 0) initialTotalSeconds else maxOf(totalSeconds, 1)
                    val sweep = if (!isRunning) {
                        360f
                    } else {
                        (totalSeconds.toFloat() / maxSeconds.toFloat() * 360f).coerceIn(0f, 360f)
                    }

                    // ✨ 2. Outer Glow
                    drawArc(
                        color = goldColor.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // ✨ 3. Mid Glow
                    drawArc(
                        color = goldColor.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 🌟 4. Main Core Ring Line
                    drawArc(
                        color = goldColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "🎯", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = if (hours > 0) 36.sp else 43.sp,
                        fontWeight = FontWeight.Black,
                        color = textMain,
                        letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
                    )
                }
            }

            // ----------------- PRESET BUTTONS -----------------
            if (!isRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(25f, 45f, 60f)
                    presets.forEach { mins ->
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
                                color = if (isSelected) Color.White else textMain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isCustomMode) goldColor else cardBg)
                            .border(1.dp, if (isCustomMode) glowYellow else glassBorder, RoundedCornerShape(50))
                            .clickable { isCustomMode = !isCustomMode }
                    ) {
                        Text(
                            text = "Custom",
                            color = if (isCustomMode) Color.White else textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ----------------- 🌟 NEW CUSTOM 5-MIN SLIDER (0 - 180 MIN) -----------------
            if (!isRunning) {
                if (isCustomMode) {
                    var dragAccumulator by remember { mutableFloatStateOf(0f) }

                    val sliderDisplayTitle = remember(dialMinutes) {
                        val mins = dialMinutes.toInt()
                        when {
                            mins == 0 -> "0 min (Stopwatch)"
                            mins >= 60 -> {
                                val h = mins / 60
                                val m = mins % 60
                                if (m > 0) "$mins min ($h h $m m)" else "$mins min ($h h)"
                            }
                            else -> "$mins min"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.4.dp, goldColor, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = { dragAccumulator = 0f },
                                        onDragCancel = { dragAccumulator = 0f }
                                    ) { _, dragAmount ->
                                        // 🧲 5-मिनट का चुंबकीय स्टेप: हर ~18 पिक्सल ड्रैग पर 5 मिनट का स्नैप
                                        val stepSensitivity = 18f
                                        dragAccumulator -= dragAmount
                                        if (abs(dragAccumulator) >= stepSensitivity) {
                                            val steps = (dragAccumulator / stepSensitivity).toInt()
                                            val nextMins = (dialMinutes + steps * 5f).coerceIn(0f, 180f)
                                            if (nextMins != dialMinutes) {
                                                dialMinutes = nextMins
                                                val secs = if (nextMins == 0f) 0 else (nextMins.toInt() * 60)
                                                TimerService.remainingSeconds.intValue = secs
                                                initialTotalSeconds = if (secs > 0) secs else 60
                                            }
                                            dragAccumulator -= steps * stepSensitivity
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = sliderDisplayTitle,
                                color = textMain,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val centerX = canvasWidth / 2f
                                val centerY = canvasHeight / 2f

                                // हर 5-मिनट टिक के बीच की दूरी
                                val spacing = 22.dp.toPx()

                                // 0 से 180 तक 5-5 मिनट के अंतराल की डंडियाँ
                                for (m in 0..180 step 5) {
                                    val x = centerX + ((m - dialMinutes) / 5f) * spacing
                                    if (x >= -20f && x <= canvasWidth + 20f) {
                                        val distFromCenter = abs(x - centerX)
                                        val alpha = (1f - distFromCenter / (canvasWidth / 2f)).coerceIn(0f, 1f)

                                        val isMajor30 = (m % 30 == 0)
                                        val isMajor15 = (m % 15 == 0)

                                        // डंडियों की ऊँचाई और मोटाई
                                        val tickHeight = when {
                                            isMajor30 -> 22.dp.toPx()
                                            isMajor15 -> 16.dp.toPx()
                                            else -> 10.dp.toPx()
                                        }
                                        val tickWidth = when {
                                            isMajor30 -> 2.4.dp.toPx()
                                            isMajor15 -> 1.8.dp.toPx()
                                            else -> 1.2.dp.toPx()
                                        }
                                        val tickColor = if (isMajor30 || isMajor15) {
                                            textMain.copy(alpha = alpha * 0.9f)
                                        } else {
                                            textMuted.copy(alpha = alpha * 0.6f)
                                        }

                                        drawLine(
                                            color = tickColor,
                                            start = Offset(x, centerY - tickHeight / 2f),
                                            end = Offset(x, centerY + tickHeight / 2f),
                                            strokeWidth = tickWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }

                                // 📍 फिक्स्ड सेंटर मेन स्टिक (Center Golden Needle)
                                val needleHeight = 28.dp.toPx()
                                drawLine(
                                    color = goldColor,
                                    start = Offset(centerX, centerY - needleHeight / 2f),
                                    end = Offset(centerX, centerY + needleHeight / 2f),
                                    strokeWidth = 3.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                // सुई के शीर्ष पर सूक्ष्म गोल्डन डॉट
                                drawCircle(
                                    color = glowYellow,
                                    radius = 2.2.dp.toPx(),
                                    center = Offset(centerX, centerY - needleHeight / 2f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Focus >",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "Strict Focus Active • Stay Distraction-Free",
                    color = goldColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ----------------- ACTION BUTTONS -----------------
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

        // =====================================================================
        // 🎧 SOUND OVERLAY PANEL (कोड पूरी तरह सुरक्षित है)
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
        // ⚠️ DISCIPLINE WARNING POPUP (सफ़ाई: पेड़ और विलाप हटाया गया)
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
                            text = "Focus session चालू है। अभी छोड़ने पर यह सत्र समाप्त और रद्द हो जाएगा।\n\nक्या आप सच में सत्र समाप्त करना चाहते हैं?",
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
                            // End Session Button
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

                            // Keep Focusing Button
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
// 🟢 2. CURVED BOTTOM BAR
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
