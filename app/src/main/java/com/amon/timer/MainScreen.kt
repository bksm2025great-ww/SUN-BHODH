package com.amon.timer

import android.content.Intent
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun MainScreen() {
    val context = LocalContext.current

    // Navigation Tab State (0: Home, 1: Forest, 2: Stats, 3: Profile)
    var currentNavIndex by remember { mutableIntStateOf(0) }

    // Colors (Amon Dark Neon Gold Theme)
    val goldColor = Color(0xFFF5A524)
    val glowYellow = Color(0xFFFDE68A)
    val glassBg = Color(0xCC111116)
    val glassBorder = Color(0x33FFFFFF)
    val textMuted = Color(0xFF9CA3AF)

    Scaffold(
        containerColor = Color(0xFF08080B),
        bottomBar = {
            AmonCurvedBottomBar(
                selectedIndex = currentNavIndex,
                onTabSelected = { newIndex -> currentNavIndex = newIndex }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentNavIndex) {
                0 -> HomeTimerTab(context, goldColor, glowYellow, glassBg, glassBorder, textMuted)
                1 -> ComingSoonTab(title = "Forest", iconRes = R.drawable.ic_nav_forest, desc = "Ugaye hue pedon ka bageecha jald hi aayega")
                2 -> ComingSoonTab(title = "Stats", iconRes = R.drawable.ic_nav_stats, desc = "Daily & Weekly focus analytics jald hi aayega")
                3 -> ComingSoonTab(title = "Profile", iconRes = R.drawable.ic_nav_profile, desc = "Badges, Themes & Settings jald hi aayenge")
            }
        }
    }
}

// =============================================================================
// 🟢 1. HOME TAB (TIMER HERO SCREEN)
// =============================================================================
@Composable
fun HomeTimerTab(
    context: android.content.Context,
    goldColor: Color,
    glowYellow: Color,
    glassBg: Color,
    glassBorder: Color,
    textMuted: Color
) {
    var selectedSubject by remember { mutableStateOf(PlantRegistry.defaultSubjects.first()) }
    var selectedTab by remember { mutableStateOf("Timer") }
    var selectedPreset by remember { mutableIntStateOf(25) }
    var isSoundOn by remember { mutableStateOf(false) }
    var dialMinutes by remember { mutableIntStateOf(25) }

    val isRunning = TimerService.isTimerRunning.value
    val totalSeconds = TimerService.remainingSeconds.intValue

    val displayMinutes = totalSeconds / 60
    val displaySeconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", displayMinutes, displaySeconds)

    val sessionTotalSeconds = (dialMinutes * 60).coerceAtLeast(1)
    val sessionProgress = ((sessionTotalSeconds - totalSeconds).toFloat() / sessionTotalSeconds.toFloat()).coerceIn(0f, 1f)
    val currentGrowthStage = PlantRegistry.getGrowthStage(sessionProgress)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BAR: AMON MASCOT + STREAK BADGE + SOUND TOGGLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Top-Left: Amon Mascot (Squircle) + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, goldColor, RoundedCornerShape(12.dp))
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
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            // Top-Right: Sound toggle + Streak Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sound Toggle Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSoundOn) goldColor else glassBg)
                        .border(1.dp, if (isSoundOn) glowYellow else glassBorder, RoundedCornerShape(50))
                        .clickable { isSoundOn = !isSoundOn }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isSoundOn) "🔊 On" else "🔈 Sound",
                        color = if (isSoundOn) Color.Black else goldColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Streak Badge Capsule
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33F5A524))
                        .border(1.2.dp, goldColor, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 10.sp)
                        Text(
                            text = "5 DAYS",
                            color = glowYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // SUBJECTS ROW
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SELECT SUBJECTS",
                color = textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

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
                            .background(if (isSelected) goldColor else glassBg)
                            .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                            .clickable {
                                if (!isRunning) selectedSubject = subject
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subject.name,
                            color = if (isSelected) Color.Black else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // [ TIMER | STOPWATCH ]
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(50))
                .background(glassBg)
                .border(1.dp, glassBorder, RoundedCornerShape(50))
                .padding(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (selectedTab == "Timer") goldColor else Color.Transparent)
                        .clickable { selectedTab = "Timer" }
                ) {
                    Text(
                        text = "Timer",
                        color = if (selectedTab == "Timer") Color.Black else textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(if (selectedTab == "Stopwatch") goldColor else Color.Transparent)
                        .clickable { selectedTab = "Stopwatch" }
                ) {
                    Text(
                        text = "Stopwatch",
                        color = if (selectedTab == "Stopwatch") Color.Black else textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // PRESETS (25 | 45)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val is25 = selectedPreset == 25
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(115.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (is25) goldColor else glassBg)
                    .border(1.dp, if (is25) glowYellow else glassBorder, RoundedCornerShape(50))
                    .clickable {
                        if (!isRunning) {
                            selectedPreset = 25
                            dialMinutes = 25
                            TimerService.remainingSeconds.intValue = 25 * 60
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "25", color = if (is25) Color.Black else goldColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(text = "Pomodoro", color = if (is25) Color(0xFF1E1E1E) else glowYellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            val is45 = selectedPreset == 45
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(115.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (is45) goldColor else glassBg)
                    .border(1.dp, if (is45) glowYellow else glassBorder, RoundedCornerShape(50))
                    .clickable {
                        if (!isRunning) {
                            selectedPreset = 45
                            dialMinutes = 45
                            TimerService.remainingSeconds.intValue = 45 * 60
                        }
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "45", color = if (is45) Color.Black else goldColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(text = "Focus", color = if (is45) Color(0xFF1E1E1E) else glowYellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ROTARY DIAL
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val dynamicDialSize = min(maxWidth, maxHeight) * 0.95f

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dynamicDialSize)
                    .aspectRatio(1f)
                    .pointerInput(isRunning) {
                        if (!isRunning) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val touchX = change.position.x - (size.width / 2f)
                                val touchY = change.position.y - (size.height / 2f)
                                var angle = Math.toDegrees(atan2(touchY.toDouble(), touchX.toDouble())).toFloat()
                                var adjustedAngle = (angle + 90f)
                                if (adjustedAngle < 0f) adjustedAngle += 360f

                                val rawMins = (adjustedAngle / 360f) * 120f
                                val snappedMins = (Math.round(rawMins / 5f) * 5).coerceIn(5, 120)
                                dialMinutes = snappedMins
                                TimerService.remainingSeconds.intValue = snappedMins * 60
                                selectedPreset = snappedMins
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val baseTrackWidth = 10.dp.toPx()
                    val radius = (size.minDimension - baseTrackWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    val topLeft = Offset(center.x - radius, center.y - radius)

                    drawCircle(color = Color(0xFF14141A), radius = radius, center = center, style = Stroke(width = baseTrackWidth))

                    val totalDots = 24
                    for (i in 0..totalDots) {
                        val dotMins = i * 5
                        val dotAngle = -90f + (dotMins / 120f) * 360f
                        val rad = Math.toRadians(dotAngle.toDouble())
                        val dotX = center.x + radius * cos(rad).toFloat()
                        val dotY = center.y + radius * sin(rad).toFloat()

                        val isFilled = dotMins <= dialMinutes
                        val isMajor = dotMins % 15 == 0
                        val dotRadius = if (isMajor) 3.dp.toPx() else 1.6.dp.toPx()

                        drawCircle(
                            color = if (isFilled) goldColor else Color(0xFF2B2B36),
                            radius = dotRadius,
                            center = Offset(dotX, dotY)
                        )
                    }

                    val activeSweep = (dialMinutes / 120f) * 360f
                    if (activeSweep > 0f) {
                        drawIntoCanvas { canvas ->
                            val glowPaint = Paint().apply {
                                color = goldColor.copy(alpha = 0.35f).toArgb()
                                setStrokeWidth(baseTrackWidth + 12.dp.toPx())
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawArc(
                                topLeft.x, topLeft.y,
                                topLeft.x + arcSize.width, topLeft.y + arcSize.height,
                                -90f, activeSweep, false, glowPaint
                            )
                        }

                        drawArc(
                            color = goldColor,
                            startAngle = -90f,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = baseTrackWidth, cap = StrokeCap.Round)
                        )

                        val knobAngle = -90f + activeSweep
                        val knobRad = Math.toRadians(knobAngle.toDouble())
                        val knobX = center.x + radius * cos(knobRad).toFloat()
                        val knobY = center.y + radius * sin(knobRad).toFloat()

                        drawCircle(color = goldColor.copy(alpha = 0.35f), radius = 14.dp.toPx(), center = Offset(knobX, knobY))
                        drawCircle(color = goldColor, radius = 8.dp.toPx(), center = Offset(knobX, knobY))
                        drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(knobX, knobY))
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "DEEP FOCUS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = textMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = timeFormatted, fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-1).sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "STUDYING ${selectedSubject.name.uppercase()}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = goldColor, letterSpacing = 1.sp)

                    val treeName = selectedSubject.tree.nameEn
                    val stageLabel = when (currentGrowthStage) {
                        TreeGrowthStage.SPROUT -> "Stage 1 🌱"
                        TreeGrowthStage.SAPLING -> "Stage 2 🌿"
                        TreeGrowthStage.DENSE -> "Stage 3 🌳"
                        TreeGrowthStage.MATURE -> "Mature ✨"
                    }
                    Text(text = "$treeName • $stageLabel", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                }
            }
        }

        // ACTION BUTTONS (PLANT / CANCEL)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(46.dp)
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
                Text(text = if (isRunning) "Pause" else "Plant", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                    .clickable {
                        val intent = Intent(context, TimerService::class.java).apply {
                            action = TimerService.ACTION_STOP
                        }
                        context.startService(intent)
                        TimerService.remainingSeconds.intValue = dialMinutes * 60
                    }
            ) {
                Text(text = "Cancel", color = goldColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =============================================================================
// 🟢 2. COMING SOON TAB (FOREST, STATS, PROFILE KE LIYE)
// =============================================================================
@Composable
fun ComingSoonTab(title: String, iconRes: Int, desc: String) {
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
                .background(Color(0xEE111116))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
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
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
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
// 🟢 3. CURVED WAVE BOTTOM NAVIGATION BAR (AAPKE 4 ICONS + SHARP GOLD BORDER)
// =============================================================================
@Composable
fun AmonCurvedBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabItems = listOf(
        Triple("Home", R.drawable.ic_nav_home, 0),
        Triple("Forest", R.drawable.ic_nav_forest, 1),
        Triple("Stats", R.drawable.ic_nav_stats, 2),
        Triple("Profile", R.drawable.ic_nav_profile, 3)
    )

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "notch_slide"
    )

    val goldColor = Color(0xFFF5A524)
    val inactiveColor = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        // BACKGROUND CURVED DOCK CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barHeight = 64.dp.toPx()
            val startY = size.height - barHeight
            val tabWidth = size.width / 4f

            // Notch center animated position
            val notchCenterX = (animatedIndex + 0.5f) * tabWidth
            val notchRadius = 34.dp.toPx()
            val shoulderWidth = 14.dp.toPx()

            val path = Path().apply {
                moveTo(0f, startY)
                val left = notchCenterX - notchRadius
                val right = notchCenterX + notchRadius

                // Left flat line to notch
                lineTo(left - shoulderWidth, startY)

                // Pure Half-Circle socket dip
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

                // Right flat line to edge
                lineTo(size.width, startY)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            // Fill dock body (Frosted glass look)
            drawPath(path = path, color = Color(0xEE111116))
            // Top rim glass accent line
            drawPath(path = path, color = Color(0x33FFFFFF), style = Stroke(width = 1.2.dp.toPx()))
        }

        // TABS & FLOATING BUBBLE
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / 4

            // FLOATING YELLOW LINE CIRCLE (ACTIVE TAB) - NO GLOW, PURE SHARP BORDER
            val bubbleSize = 54.dp
            val bubbleX = (tabWidth * animatedIndex) + (tabWidth / 2) - (bubbleSize / 2)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = bubbleX, y = 2.dp)
                    .size(bubbleSize)
                    .clip(CircleShape)
                    .background(Color(0xFF08080B))
                    .border(2.5.dp, goldColor, CircleShape)
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

            // 4 TABS ROW (INACTIVE ICONS & LABELS)
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
