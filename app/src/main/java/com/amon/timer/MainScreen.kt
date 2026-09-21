package com.amon.timer

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentNavIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ThemeManager.loadTheme(context)
    }

    val isDark = ThemeManager.isDarkTheme.value
    val isRunning = TimerService.isTimerRunning.value

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
// 🟢 1. HOME TAB (360° TO 0° DYNAMIC RING WITH PERSISTENT MEMORY)
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
    var selectedSubject by remember { mutableStateOf(PlantRegistry.defaultSubjects.first()) }
    var isSoundOn by remember { mutableStateOf(false) }

    var dialMinutes by rememberSaveable { mutableFloatStateOf(25f) }
    var isCustomMode by rememberSaveable { mutableStateOf(false) }
    var initialTotalSeconds by rememberSaveable { mutableIntStateOf(25 * 60) }

    val totalSeconds = TimerService.remainingSeconds.intValue

    // 🟢 अपडेट 1: डायरी से असली स्ट्रीक लोड करना
    var streakDays by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRunning) {
        if (!isRunning) {
            val sessions = FocusSessionManager.getAllSessions(context)
            streakDays = calculateStreakDays(sessions)
        }
    }

    val displayMinutes = totalSeconds / 60
    val displaySeconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", displayMinutes, displaySeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (!isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
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
                            .size(40.dp)
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
                    Text(
                        text = "AMON",
                        color = textMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0x33F5A524) else Color(0x22D97706))
                            .border(1.2.dp, goldColor, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔥", fontSize = 9.sp)
                            Text(
                                text = "$streakDays DAYS",
                                color = goldColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(cardBg)
                            .border(1.dp, goldColor, RoundedCornerShape(50))
                            .clickable { isSoundOn = !isSoundOn }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isSoundOn) "🔊 Sound" else "🔈 Sound",
                            color = goldColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (!isRunning) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(PlantRegistry.defaultSubjects) { subject ->
                        val isSelected = subject.id == selectedSubject.id
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) goldColor else cardBg)
                                .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                                .clickable { selectedSubject = subject }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = subject.name,
                                color = if (isSelected) Color.White else textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                    text = "🎯 ${selectedSubject.name}",
                    color = goldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

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
                Text(text = "🌱", fontSize = 26.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormatted,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain
                )
            }
        }

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
                            fontSize = 11.sp,
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (!isRunning) {
            if (isCustomMode) {
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
                                detectHorizontalDragGestures { _, dragAmount ->
                                    val sensitivity = 0.25f
                                    val newMins = (dialMinutes - (dragAmount * sensitivity)).coerceIn(0f, 120f)
                                    dialMinutes = newMins
                                    val secs = if (newMins == 0f) 0 else (newMins.toInt() * 60)
                                    TimerService.remainingSeconds.intValue = secs
                                    initialTotalSeconds = if (secs > 0) secs else 60
                                }
                            }
                    ) {
                        Text(
                            text = if (dialMinutes == 0f) "0 min (Stopwatch)" else "${dialMinutes.toInt()} min",
                            color = textMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val centerY = canvasHeight / 2f

                            val totalTicks = 31
                            val spacing = canvasWidth / (totalTicks - 1)

                            val normalizedValue = dialMinutes / 120f
                            val centerIndex = (normalizedValue * (totalTicks - 1)).toInt()

                            for (i in 0 until totalTicks) {
                                val x = i * spacing
                                val distFromCenter = abs(i - centerIndex)
                                val isCenter = (i == centerIndex)

                                val tickHeight = if (isCenter) 24.dp.toPx() else max(6.dp.toPx(), 16.dp.toPx() - (distFromCenter * 0.7f))
                                val tickWidth = if (isCenter) 3.2.dp.toPx() else 1.5.dp.toPx()
                                
                                val tickColor = if (isCenter) textMain else textMuted.copy(alpha = max(0.2f, 1.0f - distFromCenter * 0.07f))

                                drawLine(
                                    color = tickColor,
                                    start = Offset(x, centerY - tickHeight / 2f),
                                    end = Offset(x, centerY + tickHeight / 2f),
                                    strokeWidth = tickWidth,
                                    cap = StrokeCap.Round
                                )
                            }
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
                Text(
                    text = "Standard Mode Active",
                    color = textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Text(
                text = "Focus Mode Active • Stay Distraction-Free",
                color = goldColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cardBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                    .clickable {
                        val intent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }
                        context.startService(intent)
                        val resetSecs = dialMinutes.toInt() * 60
                        TimerService.remainingSeconds.intValue = resetSecs
                        initialTotalSeconds = resetSecs
                    }
            ) {
                Text(text = "Cancel", color = goldColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // 🟢 अपडेट 2: रनिंग मोड में बटन का चमकदार गोल्डन में बदलना
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isRunning) goldColor else cardBg)
                    .border(1.dp, if (isRunning) glowYellow else glassBorder, RoundedCornerShape(50))
                    .clickable {
                        val intent = Intent(context, TimerService::class.java)
                        if (isRunning) {
                            intent.action = TimerService.ACTION_PAUSE
                            context.startService(intent)
                        } else {
                            initialTotalSeconds = if (totalSeconds > 0) totalSeconds else (dialMinutes.toInt() * 60)
                            intent.action = TimerService.ACTION_START
                            intent.putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
                            intent.putExtra(TimerService.EXTRA_SUBJECT, selectedSubject.name)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    }
            ) {
                Text(
                    text = if (isRunning) "Pause" else "Plant 🌳",
                    color = if (isRunning) Color.White else goldColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
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
// 🔥 Helper: Calculate streak for Home Tab (Min 30 mins / day)
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
