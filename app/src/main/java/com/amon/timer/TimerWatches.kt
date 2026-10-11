package com.amon.timer

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.*

/**
 * 👑 AMON CENTRAL WATCH CONTAINER
 * तीनों घड़ियों को एक ही मॉड्यूलर हब में कंट्रोल करता है
 */
@Composable
fun TimerWatchesContainer(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    isCustomMode: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int,
    onTogglePlayPause: () -> Unit = {}
) {
    val context = LocalContext.current
    val watchPrefs = remember {
        context.getSharedPreferences("amon_watch_prefs", Context.MODE_PRIVATE)
    }

    var selectedWatchStyle by rememberSaveable {
        mutableIntStateOf(watchPrefs.getInt("selected_watch_style", 0))
    }

    val onPrevStyle = {
        selectedWatchStyle = if (selectedWatchStyle > 0) selectedWatchStyle - 1 else 2
        watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
    }

    val onNextStyle = {
        selectedWatchStyle = if (selectedWatchStyle < 2) selectedWatchStyle + 1 else 0
        watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(isRunning) {
                if (!isRunning) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDragX < -60f) {
                                onNextStyle()
                            } else if (totalDragX > 60f) {
                                onPrevStyle()
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = { totalDragX = 0f }
                    ) { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                }
            }
    ) {
        // 🕰️ वॉच स्टाइल डिस्प्ले + दोनों तरफ़ खुले शेवरॉन एरो
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = selectedWatchStyle,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "WatchTransition"
            ) { style ->
                when (style) {
                    0 -> ClassicRingWatch(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = onDialMinutesChange,
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
                        isCustomMode = isCustomMode,
                        onCustomModeToggle = onCustomModeToggle,
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

                    1 -> RotaryDialWatch(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = onDialMinutesChange,
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
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

                    2 -> RetroFlipClockWatch(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = onDialMinutesChange,
                        totalSeconds = totalSeconds,
                        isRunning = isRunning,
                        goldColor = goldColor,
                        glowYellow = glowYellow,
                        cardBg = cardBg,
                        glassBorder = glassBorder,
                        textMain = textMain,
                        textMuted = textMuted,
                        isDark = isDark
                    )
                }
            }

            // 🏹 बाएँ और दाएँ खुले शेवरॉन एरो (केवल जब टाइमर रुका हो)
            if (!isRunning) {
                // LEFT ARROW BUTTON
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 2.dp, y = 110.dp)
                ) {
                    WatchNavArrowButton(
                        isLeft = true,
                        goldColor = goldColor,
                        cardBg = cardBg,
                        isDark = isDark,
                        onClick = onPrevStyle
                    )
                }

                // RIGHT ARROW BUTTON
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 110.dp)
                ) {
                    WatchNavArrowButton(
                        isLeft = false,
                        goldColor = goldColor,
                        cardBg = cardBg,
                        isDark = isDark,
                        onClick = onNextStyle
                    )
                }
            }
        }
    }
}

// =============================================================================
// 🏹 SLEEK OPEN-CHEVRON NAVIGATION BUTTONS (हाथ से बने खुले व सुंदर एरो)
// =============================================================================
@Composable
private fun WatchNavArrowButton(
    isLeft: Boolean,
    goldColor: Color,
    cardBg: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isDark) Color(0x331E1E26) else Color(0x66FFFFFF))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x33FFFFFF) else Color(0x44CBD5E1),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        if (isLeft) {
            SleekWideChevronLeft(tint = goldColor)
        } else {
            SleekWideChevronRight(tint = goldColor)
        }
    }
}

@Composable
private fun SleekWideChevronLeft(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.2.dp.toPx()

        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun SleekWideChevronRight(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.2.dp.toPx()

        val path = Path().apply {
            moveTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// =============================================================================
// ⏱ WATCH STYLE 1: CLASSIC LUXE RING
// =============================================================================
@Composable
private fun ClassicRingWatch(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    isCustomMode: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(258.dp)
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
                    color = goldColor.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )

                drawArc(
                    color = goldColor.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                )

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
                FocusTargetIcon(tint = goldColor, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    fontSize = if (hours > 0) 36.sp else 43.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain,
                    letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
                )
            }
        }

        // 🔘 PRESET BUTTONS
        if (!isRunning) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
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
                            .clickable { onDialMinutesChange(mins) }
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
                        .clickable { onCustomModeToggle() }
                ) {
                    Text(
                        text = "Custom",
                        color = if (isCustomMode) Color.White else textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (isCustomMode) {
        Dialog(onDismissRequest = { onCustomModeToggle() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.6.dp, goldColor, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Set Custom Time ⏱️",
                        color = goldColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Slide smoothly to adjust (0 - 180 min)",
                        color = textMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val mins = dialMinutes.toInt()
                    val displayTitle = when {
                        mins == 0 -> "0 min (Stopwatch)"
                        mins >= 60 -> {
                            val h = mins / 60
                            val m = mins % 60
                            if (m > 0) "$mins min ($h h $m m)" else "$mins min ($h h)"
                        }
                        else -> "$mins min"
                    }

                    Text(
                        text = displayTitle,
                        color = textMain,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CustomHorizontalSlider(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = onDialMinutesChange,
                        goldColor = goldColor,
                        cardBg = cardBg,
                        textMain = textMain,
                        textMuted = textMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onCustomModeToggle() },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Done 🎯",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 🎛 WATCH STYLE 2: ROTARY DIAL
// =============================================================================
@Composable
private fun RotaryDialWatch(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int
) {
    var centerOffset by remember { mutableStateOf(Offset.Zero) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(258.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isRunning) {
                        if (!isRunning) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val touchPos = change.position
                                val dx = touchPos.x - centerOffset.x
                                val dy = touchPos.y - centerOffset.y

                                var angleDeg = (atan2(dy, dx) * (180f / PI.toFloat()))
                                angleDeg = (angleDeg + 90f + 360f) % 360f

                                val computedMins = (angleDeg / 360f) * 180f
                                val snappedMins = (round(computedMins / 5f) * 5f).coerceIn(0f, 180f)

                                if (snappedMins != dialMinutes) {
                                    onDialMinutesChange(snappedMins)
                                }
                            }
                        }
                    }
            ) {
                val radius = size.minDimension / 2f - 14.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                centerOffset = center

                drawCircle(
                    color = if (isDark) Color(0xFF1E1E28) else Color(0xFFE2E8F0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 5.dp.toPx())
                )

                if (!isRunning) {
                    val totalMarks = 36
                    for (i in 0 until totalMarks) {
                        val markMins = i * 5
                        val markAngleDeg = (markMins / 180f) * 360f - 90f
                        val rad = markAngleDeg * (PI.toFloat() / 180f)

                        val isMajor = (markMins % 30 == 0)
                        val markLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                        val markStroke = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx()
                        val markAlpha = if (isMajor) 0.8f else 0.35f

                        val startX = center.x + (radius - 2.dp.toPx()) * cos(rad)
                        val startY = center.y + (radius - 2.dp.toPx()) * sin(rad)
                        val endX = center.x + (radius - 2.dp.toPx() - markLen) * cos(rad)
                        val endY = center.y + (radius - 2.dp.toPx() - markLen) * sin(rad)

                        drawLine(
                            color = goldColor.copy(alpha = markAlpha),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = markStroke,
                            cap = StrokeCap.Round
                        )
                    }

                    val sweep = (dialMinutes / 180f) * 360f
                    drawArc(
                        color = goldColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val knobAngleRad = (sweep - 90f) * (PI.toFloat() / 180f)
                    val knobX = center.x + radius * cos(knobAngleRad)
                    val knobY = center.y + radius * sin(knobAngleRad)

                    drawCircle(color = goldColor.copy(alpha = 0.3f), radius = 16.dp.toPx(), center = Offset(knobX, knobY))
                    drawCircle(color = goldColor, radius = 10.dp.toPx(), center = Offset(knobX, knobY))
                    drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(knobX, knobY))
                } else {
                    val maxSeconds = if (initialTotalSeconds > 0) initialTotalSeconds else maxOf(totalSeconds, 1)
                    val sweep = (totalSeconds.toFloat() / maxSeconds.toFloat() * 360f).coerceIn(0f, 360f)

                    drawArc(
                        color = goldColor.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    )

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
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = if (hours > 0) 36.sp else 43.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain,
                    letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
                )
                if (!isRunning) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Rotate Dial",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = goldColor
                    )
                }
            }
        }

        if (!isRunning) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(25f, 45f, 60f).forEach { mins ->
                    val isSelected = dialMinutes == mins
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) goldColor else cardBg)
                            .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                            .clickable { onDialMinutesChange(mins) }
                    ) {
                        Text(
                            text = "${mins.toInt()}m",
                            color = if (isSelected) Color.White else textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 📜 WATCH STYLE 3: RETRO FLIP CLOCK
// =============================================================================
@Composable
private fun RetroFlipClockWatch(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    totalSeconds: Int,
    isRunning: Boolean,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isHourMode = totalSeconds >= 3600

    val topDigitStr = if (isHourMode) {
        String.format("%02d", totalSeconds / 3600)
    } else {
        String.format("%02d", totalSeconds / 60)
    }

    val bottomDigitStr = if (isHourMode) {
        String.format("%02d", (totalSeconds % 3600) / 60)
    } else {
        String.format("%02d", totalSeconds % 60)
    }

    var showTimeInputDialog by remember { mutableStateOf(false) }

    val targetCardWidth = if (isLandscape) {
        if (isRunning) 235.dp else 215.dp
    } else {
        if (isRunning) 265.dp else 245.dp
    }

    val targetCardHeight = if (isLandscape) {
        if (isRunning) 180.dp else 165.dp
    } else {
        if (isRunning) 195.dp else 175.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetCardWidth,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FlipCardWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetCardHeight,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FlipCardHeight"
    )

    val baseFontSize = if (isLandscape) {
        if (isRunning) 150f else 136f
    } else {
        if (isRunning) 160f else 142f
    }
    val topFontSize = if (topDigitStr.length > 2) baseFontSize * 0.70f else baseFontSize
    val bottomFontSize = baseFontSize

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isRunning) (if (isLandscape) 2.dp else 6.dp) else 2.dp)
    ) {
        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isRunning
                ) {
                    showTimeInputDialog = true
                }
            ) {
                AuthenticSplitFlapCard(
                    digit = topDigitStr,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = topFontSize,
                    isDark = isDark
                )

                AuthenticSplitFlapCard(
                    digit = bottomDigitStr,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = bottomFontSize,
                    isDark = isDark
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isRunning) 18.dp else 12.dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isRunning
                ) {
                    showTimeInputDialog = true
                }
            ) {
                AuthenticSplitFlapCard(
                    digit = topDigitStr,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = topFontSize,
                    isDark = isDark
                )

                AuthenticSplitFlapCard(
                    digit = bottomDigitStr,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = bottomFontSize,
                    isDark = isDark
                )
            }
        }

        if (!isRunning) {
            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.75f else 1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(25f, 45f, 60f).forEach { m ->
                    val isSelected = (dialMinutes * 60f).roundToInt() == (m * 60).toInt()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) goldColor else cardBg)
                            .border(1.dp, if (isSelected) glowYellow else glassBorder, RoundedCornerShape(50))
                            .clickable { onDialMinutesChange(m) }
                    ) {
                        Text(
                            text = "${m.toInt()}m",
                            color = if (isSelected) Color.White else textMain,
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
                        .background(cardBg)
                        .border(1.dp, goldColor, RoundedCornerShape(50))
                        .clickable { showTimeInputDialog = true }
                ) {
                    Text(
                        text = "Custom ⌨️",
                        color = goldColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    if (showTimeInputDialog) {
        var inputMins by remember { mutableStateOf((dialMinutes.toInt()).toString()) }

        Dialog(onDismissRequest = { showTimeInputDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, goldColor, RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Set Flip Clock Time 📜",
                        color = goldColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter minutes (1 - 180 min)",
                        color = textMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = inputMins,
                        onValueChange = { inputMins = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = textMain
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            unfocusedBorderColor = glassBorder,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            cursorColor = goldColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 25, 45, 60, 90).forEach { preset ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF222228) else Color(0xFFF1F5F9))
                                    .border(1.dp, glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { inputMins = preset.toString() }
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${preset}m",
                                    color = textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showTimeInputDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = textMuted, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val entered = inputMins.toIntOrNull()?.coerceIn(1, 180)
                                if (entered != null) {
                                    onDialMinutesChange(entered.toFloat())
                                }
                                showTimeInputDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set Time 🎯", color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🎴 MECHANICAL SPLIT-FLAP COMPONENT
 */
@Composable
private fun AuthenticSplitFlapCard(
    digit: String,
    cardWidth: Dp,
    cardHeight: Dp,
    fontSizeSp: Float,
    isDark: Boolean
) {
    var previousDigit by remember { mutableStateOf(digit) }
    var currentDigit by remember { mutableStateOf(digit) }

    val flipAnim = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
            flipAnim.snapTo(0f)
            flipAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }

    val rotation = flipAnim.value * 180f
    val isTopFolded = rotation >= 90f
    val seamGap = 2.dp
    val halfHeight = (cardHeight - seamGap) / 2
    val cornerRadius = 22.dp

    val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = 3.dp,
        bottomEnd = 3.dp
    )
    val bottomShape = RoundedCornerShape(
        topStart = 3.dp,
        topEnd = 3.dp,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )

    Box(
        modifier = Modifier.size(width = cardWidth, height = cardHeight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SplitFlapHalfPlate(
                digit = currentDigit,
                isTop = true,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                halfHeight = halfHeight,
                fontSizeSp = fontSizeSp,
                shape = topShape,
                isDark = isDark
            )

            SplitFlapHalfPlate(
                digit = if (flipAnim.isRunning) previousDigit else currentDigit,
                isTop = false,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                halfHeight = halfHeight,
                fontSizeSp = fontSizeSp,
                shape = bottomShape,
                isDark = isDark
            )
        }

        if (flipAnim.isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(halfHeight)
                    .align(if (isTopFolded) Alignment.BottomCenter else Alignment.TopCenter)
                    .graphicsLayer {
                        rotationX = if (isTopFolded) (180f - rotation) else -rotation
                        cameraDistance = 18 * density
                        transformOrigin = if (isTopFolded) TransformOrigin(0.5f, 0f) else TransformOrigin(0.5f, 1f)
                    }
            ) {
                SplitFlapHalfPlate(
                    digit = if (isTopFolded) currentDigit else previousDigit,
                    isTop = !isTopFolded,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    halfHeight = halfHeight,
                    fontSizeSp = fontSizeSp,
                    shape = if (isTopFolded) bottomShape else topShape,
                    isDark = isDark
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(seamGap)
                .background(if (isDark) Color(0xFF09090C) else Color(0xFFE2E8F0))
                .align(Alignment.Center)
        )
    }
}

/**
 * ✂️ MATHEMATICAL 50-50 HALF-PLATE RENDERER
 */
@Composable
private fun SplitFlapHalfPlate(
    digit: String,
    isTop: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    halfHeight: Dp,
    fontSizeSp: Float,
    shape: Shape,
    isDark: Boolean
) {
    val plateBg = if (isDark) Color(0xFF16161B) else Color(0xFFFFFFFF)
    val plateBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
    val digitTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = halfHeight)
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = shape,
                        spotColor = Color(0x18000000),
                        ambientColor = Color(0x10000000)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(plateBg)
            .border(1.2.dp, plateBorder, shape)
    ) {
        Layout(
            content = {
                Text(
                    text = digit,
                    fontSize = fontSizeSp.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = digitTextColor,
                    letterSpacing = (-4).sp
                )
            }
        ) { measurables, _ ->
            val textPlaceable = measurables.first().measure(Constraints())
            val parentWidthPx = cardWidth.roundToPx()
            val cardHeightPx = cardHeight.roundToPx()
            val halfHeightPx = halfHeight.roundToPx()

            val centerX = (parentWidthPx - textPlaceable.width) / 2
            val centerY = (cardHeightPx - textPlaceable.height) / 2
            val posY = if (isTop) centerY else centerY - halfHeightPx

            layout(parentWidthPx, halfHeightPx) {
                textPlaceable.place(centerX, posY)
            }
        }
    }
}

// =============================================================================
// 🌟 CUSTOM HORIZONTAL SLIDER (ISOLATED INSIDE DIALOG)
// =============================================================================
@Composable
private fun CustomHorizontalSlider(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    goldColor: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color
) {
    var rawMinutes by remember(dialMinutes) { mutableFloatStateOf(dialMinutes) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.2.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                val spacing = 22.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val finalSnapped = (round(rawMinutes / 5f) * 5f).coerceIn(0f, 180f)
                        rawMinutes = finalSnapped
                        onDialMinutesChange(finalSnapped)
                    },
                    onDragCancel = {}
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMinutes = (-dragAmount / spacing) * 5f
                    rawMinutes = (rawMinutes + deltaMinutes).coerceIn(0f, 180f)

                    val currentSnapped = (round(rawMinutes / 5f) * 5f).coerceIn(0f, 180f)
                    if (currentSnapped != dialMinutes) {
                        onDialMinutesChange(currentSnapped)
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val spacing = 22.dp.toPx()

            for (m in 0..180 step 5) {
                val x = centerX + ((m - rawMinutes) / 5f) * spacing
                if (x >= -20f && x <= canvasWidth + 20f) {
                    val distFromCenter = abs(x - centerX)
                    val alpha = (1f - distFromCenter / (canvasWidth / 2f)).coerceIn(0f, 1f)
                    val isMajor30 = (m % 30 == 0)
                    val isMajor15 = (m % 15 == 0)

                    val tickHeight = when {
                        isMajor30 -> 24.dp.toPx()
                        isMajor15 -> 17.dp.toPx()
                        else -> 11.dp.toPx()
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
        }

        // Center Indicator Pin
        Box(
            modifier = Modifier
                .width(3.2.dp)
                .height(30.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(2.dp),
                    spotColor = goldColor,
                    ambientColor = goldColor
                )
                .background(goldColor, RoundedCornerShape(2.dp))
        )
    }
}

// =============================================================================
// ✨ LUXE GOLD MATERIAL VECTOR ICON (TARGET VECTOR)
// =============================================================================
@Composable
private fun FocusTargetIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val stroke = 1.8.dp.toPx()

        drawCircle(
            color = tint.copy(alpha = 0.5f),
            radius = w * 0.44f,
            center = center,
            style = Stroke(width = stroke)
        )

        drawCircle(
            color = tint,
            radius = w * 0.26f,
            center = center,
            style = Stroke(width = stroke * 1.15f)
        )

        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = center
        )

        val crossLen = w * 0.10f
        drawLine(tint, Offset(center.x, center.y - w * 0.44f), Offset(center.x, center.y - w * 0.44f + crossLen), stroke, StrokeCap.Round)
        drawLine(tint, Offset(center.x, center.y + w * 0.44f - crossLen), Offset(center.x, center.y + w * 0.44f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(center.x - w * 0.44f, center.y), Offset(center.x - w * 0.44f + crossLen, center.y), stroke, StrokeCap.Round)
        drawLine(tint, Offset(center.x + w * 0.44f - crossLen, center.y), Offset(center.x + w * 0.44f, center.y), stroke, StrokeCap.Round)
    }
}
