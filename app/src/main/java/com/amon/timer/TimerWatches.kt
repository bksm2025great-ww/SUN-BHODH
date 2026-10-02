package com.amon.timer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * 👑 AMON CENTRAL WATCH CONTAINER
 * तीनों घड़ियों को एक ही जगह कंट्रोल करने वाला मॉड्यूलर हब
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
    hours: Int
) {
    val context = LocalContext.current
    val watchPrefs = remember {
        context.getSharedPreferences("amon_watch_prefs", Context.MODE_PRIVATE)
    }

    // 0: Classic Ring, 1: Rotary Dial, 2: Retro Flip Clock
    var selectedWatchStyle by rememberSaveable {
        mutableIntStateOf(watchPrefs.getInt("selected_watch_style", 0))
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
                            if (totalDragX < -60f && selectedWatchStyle < 2) {
                                selectedWatchStyle++
                                watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
                            } else if (totalDragX > 60f && selectedWatchStyle > 0) {
                                selectedWatchStyle--
                                watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
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
        // ----------------- 3-DOT INDICATOR ( •  ●  • ) -----------------
        if (!isRunning) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                listOf(0, 1, 2).forEach { index ->
                    val isSelected = selectedWatchStyle == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) goldColor else glassBorder)
                            .clickable {
                                selectedWatchStyle = index
                                watchPrefs.edit().putInt("selected_watch_style", index).apply()
                            }
                    )
                }
            }
        }

        // ----------------- 3 WATCH BODIES -----------------
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
                    isRunning = isRunning,
                    goldColor = goldColor,
                    glowYellow = glowYellow,
                    cardBg = cardBg,
                    glassBorder = glassBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    timeFormatted = timeFormatted,
                    hours = hours
                )
            }
        }
    }
}

// =============================================================================
// ⏱️ WATCH STYLE 1: CLASSIC LUXE RING
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

        if (!isRunning) {
            Spacer(modifier = Modifier.height(14.dp))
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
                                onDialMinutesChange(mins)
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

            if (isCustomMode) {
                Spacer(modifier = Modifier.height(12.dp))
                CustomHorizontalSlider(
                    dialMinutes = dialMinutes,
                    onDialMinutesChange = onDialMinutesChange,
                    goldColor = goldColor,
                    glowYellow = glowYellow,
                    cardBg = cardBg,
                    textMain = textMain,
                    textMuted = textMuted
                )
            }
        }
    }
}

// =============================================================================
// 🎛️ WATCH STYLE 2: ROTARY DIAL (0 - 180M + HAPTIC SNAP + ZERO DISTRACTION)
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
    val context = LocalContext.current
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
                                    triggerLightHapticTick(context)
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

                    drawCircle(
                        color = goldColor.copy(alpha = 0.3f),
                        radius = 16.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
                    drawCircle(
                        color = goldColor,
                        radius = 10.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )
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
                Text(text = "👑", fontSize = 26.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormatted,
                    fontSize = if (hours > 0) 36.sp else 43.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain,
                    letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
                )
                if (!isRunning) {
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
                            .clickable {
                                triggerLightHapticTick(context)
                                onDialMinutesChange(mins)
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
            }
        }
    }
}

// =============================================================================
// 📜 WATCH STYLE 3: RETRO DESK FLIP CLOCK
// =============================================================================
@Composable
private fun RetroFlipClockWatch(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    isRunning: Boolean,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    timeFormatted: String,
    hours: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        val parts = timeFormatted.split(":")
        val mainLeft = if (hours > 0) parts[0] else parts[0]
        val mainRight = if (hours > 0) parts[1] else parts[1]
        val suffixSec = if (hours > 0 && parts.size > 2) parts[2] else null

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlipCardBlock(numberStr = mainLeft, cardBg = cardBg, glassBorder = glassBorder, textMain = textMain)
            Text(text = ":", fontSize = 42.sp, fontWeight = FontWeight.Black, color = goldColor)
            FlipCardBlock(numberStr = mainRight, cardBg = cardBg, glassBorder = glassBorder, textMain = textMain)
            if (suffixSec != null) {
                Text(text = ":", fontSize = 42.sp, fontWeight = FontWeight.Black, color = goldColor)
                FlipCardBlock(numberStr = suffixSec, cardBg = cardBg, glassBorder = glassBorder, textMain = textMain)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Desk Flip Mode • Table Stand",
            color = textMuted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )

        if (!isRunning) {
            Spacer(modifier = Modifier.height(18.dp))
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

@Composable
private fun FlipCardBlock(
    numberStr: String,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 86.dp, height = 110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.4.dp, glassBorder, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x33FFFFFF))
        )
        Text(
            text = numberStr,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            color = textMain,
            letterSpacing = (-1).sp
        )
    }
}

// ----------------- CUSTOM SLIDER HELPER -----------------
@Composable
private fun CustomHorizontalSlider(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color
) {
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
                        val stepSensitivity = 18f
                        dragAccumulator -= dragAmount
                        if (abs(dragAccumulator) >= stepSensitivity) {
                            val steps = (dragAccumulator / stepSensitivity).toInt()
                            val nextMins = (dialMinutes + steps * 5f).coerceIn(0f, 180f)
                            if (nextMins != dialMinutes) {
                                onDialMinutesChange(nextMins)
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
                val spacing = 22.dp.toPx()

                for (m in 0..180 step 5) {
                    val x = centerX + ((m - dialMinutes) / 5f) * spacing
                    if (x >= -20f && x <= canvasWidth + 20f) {
                        val distFromCenter = abs(x - centerX)
                        val alpha = (1f - distFromCenter / (canvasWidth / 2f)).coerceIn(0f, 1f)
                        val isMajor30 = (m % 30 == 0)
                        val isMajor15 = (m % 15 == 0)

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

                val needleHeight = 28.dp.toPx()
                drawLine(
                    color = goldColor,
                    start = Offset(centerX, centerY - needleHeight / 2f),
                    end = Offset(centerX, centerY + needleHeight / 2f),
                    strokeWidth = 3.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
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
}

// 📳 HAPTIC BUZZ HELPER
private fun triggerLightHapticTick(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10L)
        }
    } catch (_: Exception) {}
}

