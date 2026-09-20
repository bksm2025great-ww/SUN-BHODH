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
import kotlin.math.*

// Shared Theme State across the app (Dark / Light)
object AppThemeState {
    var isDarkTheme by mutableStateOf(true)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var currentNavIndex by remember { mutableIntStateOf(0) }

    val isDark = AppThemeState.isDarkTheme
    val isRunning = TimerService.isTimerRunning.value

    // Dynamic Theme Colors
    val bgColor = if (isDark) Color(0xFF08080B) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xEE121218) else Color(0xFFFFFFFF)
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val glassBorder = if (isDark) Color(0x2D2D38) else Color(0xFFCBD5E1)
    val goldColor = if (isDark) Color(0xFFF5A524) else Color(0xFFD97706)
    val glowYellow = if (isDark) Color(0xFFFDE68A) else Color(0xFFF59E0B)

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            // Hide bottom bar during active Focus Mode
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
                1 -> ComingSoonTab(title = "Forest", iconRes = R.drawable.ic_nav_forest, desc = "Ugaye hue pedon ka bageecha jald hi aayega", cardBg = cardBg, borderCol = glassBorder, textMain = textMain)
                2 -> ComingSoonTab(title = "Stats", iconRes = R.drawable.ic_nav_stats, desc = "Daily & Weekly focus analytics jald hi aayega", cardBg = cardBg, borderCol = glassBorder, textMain = textMain)
                3 -> ProfileScreen()
            }
        }
    }
}

// =============================================================================
// 🟢 1. HOME TAB (FOCUS MODE DISTRACTION-FREE & APPLE TICK RULER)
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
    var dialMinutes by remember { mutableFloatStateOf(25f) }
    var isCustomMode by remember { mutableStateOf(false) }

    val totalSeconds = TimerService.remainingSeconds.intValue

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
        // TOP BAR: Hidden during active timer (Focus Mode)
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
                                text = "5 DAYS",
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
            // Minimal spacing placeholder in Focus Mode
            Spacer(modifier = Modifier.height(10.dp))
        }

        // SUBJECTS ROW: Hidden during active timer
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
            // Show active subject cleanly in Focus Mode
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

        // TIME RING + TREE (ALWAYS VISIBLE)
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(color = if (isDark) Color(0xFF1E1E28) else Color(0xFFE2E8F0), radius = radius, center = center, style = Stroke(width = 5.dp.toPx()))

                val sweep = (dialMinutes / 120f) * 360f
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

        // PRESETS ROW: Hidden during active timer
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
                                TimerService.remainingSeconds.intValue = (mins.toInt() * 60)
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

        // APPLE-STYLE TICK RULER STICK CARD: Hidden during active timer
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
                                    TimerService.remainingSeconds.intValue = if (newMins == 0f) 0 else (newMins.toInt() * 60)
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

        // SIDE-BY-SIDE EQUAL ACTION BUTTONS (50:50 SPLIT - ALWAYS VISIBLE)
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
                        TimerService.remainingSeconds.intValue = dialMinutes.toInt() * 60
                    }
            ) {
                Text(text = "Cancel", color = goldColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(goldColor)
                    .border(1.dp, glowYellow, RoundedCornerShape(50))
                    .clickable {
                        val intent = Intent(context, TimerService::class.java)
                        if (isRunning) {
                            intent.action = TimerService.ACTION_PAUSE
                            context.startService(intent)
                        } else {
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
                Text(text = if (isRunning) "Pause" else "Plant 🌳", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// =============================================================================
// 🟢 2. COMING SOON TAB
// =============================================================================
@Composable
fun ComingSoonTab(title: String, iconRes: Int, desc: String, cardBg: Color, borderCol: Color, textMain: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(1.dp, borderCol, RoundedCornerShape(24.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = textMain)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Coming Soon... 🌱", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF5A524))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// =============================================================================
// 🟢 3. LIGHTWEIGHT SPEED-OPTIMIZED BOTTOM NAVIGATION BAR
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
