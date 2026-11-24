package com.amon.timer

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * AMON CENTRAL WATCH CONTAINER
 * सभी 6 घड़ियों को एक ही मॉड्यूलर हब में कंट्रोल करता है
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
    selectedSubjectName: String = "Focus",
    onTogglePlayPause: () -> Unit = {},
    showPresets: Boolean = true
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val maxStyleIndex = 5

    val watchPrefs = remember {
        context.getSharedPreferences("amon_watch_prefs", Context.MODE_PRIVATE)
    }

    var selectedWatchStyle by rememberSaveable {
        val saved = try {
            watchPrefs.getInt("selected_watch_style", 0).coerceIn(0, maxStyleIndex)
        } catch (_: Exception) {
            0
        }
        mutableIntStateOf(saved)
    }

    val onPrevStyle = {
        selectedWatchStyle = if (selectedWatchStyle > 0) selectedWatchStyle - 1 else maxStyleIndex
        try {
            watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
        } catch (_: Exception) {}
    }

    val onNextStyle = {
        selectedWatchStyle = if (selectedWatchStyle < maxStyleIndex) selectedWatchStyle + 1 else 0
        try {
            watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
        } catch (_: Exception) {}
    }

    val targetArrowY = if (isLandscape) {
        if (selectedWatchStyle == 2) 72.dp else 95.dp
    } else {
        if (selectedWatchStyle == 2) 105.dp else 111.dp
    }

    val animatedArrowY by animateDpAsState(
        targetValue = targetArrowY,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "ArrowSeamGlide"
    )

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
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        goldColor = goldColor,
                        textMain = textMain,
                        isDark = isDark,
                        timeFormatted = timeFormatted,
                        hours = hours
                    )

                    1 -> DynamicSessionAnalogWatch(
                        dialMinutes = dialMinutes,
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        goldColor = goldColor,
                        textMain = textMain,
                        textMuted = textMuted,
                        isDark = isDark,
                        timeFormatted = timeFormatted,
                        hours = hours
                    )

                    2 -> RetroFlipClockWatch(
                        totalSeconds = totalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        subjectName = selectedSubjectName,
                        goldColor = goldColor,
                        isDark = isDark
                    )

                    3 -> CarbonMatrixDigitalWatch(
                        dialMinutes = dialMinutes,
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        goldColor = goldColor,
                        textMain = textMain,
                        textMuted = textMuted,
                        isDark = isDark,
                        timeFormatted = timeFormatted,
                        hours = hours
                    )

                    4 -> SandHourglassTimerWatch(
                        dialMinutes = dialMinutes,
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        goldColor = goldColor,
                        textMain = textMain,
                        isDark = isDark,
                        timeFormatted = timeFormatted,
                        hours = hours
                    )

                    else -> WaterWaveTimerWatch(
                        dialMinutes = dialMinutes,
                        totalSeconds = totalSeconds,
                        initialTotalSeconds = initialTotalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        goldColor = goldColor,
                        textMain = textMain,
                        isDark = isDark,
                        timeFormatted = timeFormatted,
                        hours = hours
                    )
                }
            }

            if (!isRunning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 2.dp, y = animatedArrowY)
                ) {
                    WatchNavArrowButton(
                        isLeft = true,
                        goldColor = goldColor,
                        onClick = onPrevStyle
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = animatedArrowY)
                ) {
                    WatchNavArrowButton(
                        isLeft = false,
                        goldColor = goldColor,
                        onClick = onNextStyle
                    )
                }
            }
        }
    }
}

// =============================================================================
// PURE FLOATING CHEVRON BUTTON
// =============================================================================
@Composable
private fun WatchNavArrowButton(
    isLeft: Boolean,
    goldColor: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
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
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.4.dp.toPx()

        val path = Path().apply {
            moveTo(w * 0.64f, h * 0.22f)
            lineTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.78f)
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
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.4.dp.toPx()

        val path = Path().apply {
            moveTo(w * 0.36f, h * 0.22f)
            lineTo(w * 0.66f, h * 0.50f)
            lineTo(w * 0.36f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// =============================================================================
// WATCH STYLE 0: CLASSIC LUXE RING
// =============================================================================
@Composable
private fun ClassicRingWatch(
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    textMain: Color,
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
                    style = Stroke(width = 3.8.dp.toPx())
                )

                val maxSeconds = if (initialTotalSeconds > 0) initialTotalSeconds else maxOf(totalSeconds, 1)
                val sweep = if (!isRunning) {
                    360f
                } else {
                    (totalSeconds.toFloat() / maxSeconds.toFloat() * 360f).coerceIn(0f, 360f)
                }

                if (isRunning) {
                    drawArc(
                        color = goldColor.copy(alpha = 0.20f),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 8.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    color = goldColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 4.4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        enabled = !isRunning,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCustomModeToggle
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = if (hours > 0) 38.sp else 46.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain,
                    letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
                )
            }
        }
    }
}

// =============================================================================
// WATCH STYLE 1: 12-HOUR STYLE DYNAMIC ANALOG WATCH (SINGLE NEEDLE OVER TIME)
// =============================================================================
@Composable
private fun DynamicSessionAnalogWatch(
    dialMinutes: Float,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int
) {
    val effectiveTotalSeconds = when {
        initialTotalSeconds > 0 -> initialTotalSeconds
        totalSeconds > 0 -> totalSeconds
        else -> (dialMinutes.toInt().coerceAtLeast(1)) * 60
    }

    val topTotalMinutes = ((effectiveTotalSeconds + 59) / 60).coerceAtLeast(1)

    val remainingFraction = if (!isRunning) {
        1f
    } else {
        (totalSeconds.toFloat() / effectiveTotalSeconds.toFloat()).coerceIn(0f, 1f)
    }
    val elapsedFraction = (1f - remainingFraction).coerceIn(0f, 1f)

    val needleAngleDeg = -90f + (elapsedFraction * 360f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(258.dp)
                .padding(vertical = 4.dp)
                .clickable(
                    enabled = !isRunning,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCustomModeToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = if (isDark) Color(0xFF16161F) else Color(0xFFF8FAFC),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = if (isDark) Color(0xFF232330) else Color(0xFFE2E8F0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.5.dp.toPx())
                )

                for (i in 0 until 60) {
                    val angleDeg = (i * 6f) - 90f
                    val rad = angleDeg * (PI.toFloat() / 180f)
                    val isHourMark = (i % 5 == 0)

                    val outerPtRadius = radius - 5.dp.toPx()
                    val innerPtRadius = if (isHourMark) {
                        radius - 14.dp.toPx()
                    } else {
                        radius - 9.dp.toPx()
                    }

                    val startPt = Offset(
                        x = center.x + innerPtRadius * cos(rad),
                        y = center.y + innerPtRadius * sin(rad)
                    )
                    val endPt = Offset(
                        x = center.x + outerPtRadius * cos(rad),
                        y = center.y + outerPtRadius * sin(rad)
                    )

                    drawLine(
                        color = if (isHourMark) {
                            goldColor.copy(alpha = 0.75f)
                        } else {
                            textMuted.copy(alpha = 0.25f)
                        },
                        start = startPt,
                        end = endPt,
                        strokeWidth = if (isHourMark) 2.2.dp.toPx() else 1.1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val sweepRemaining = remainingFraction * 360f
                if (isRunning && sweepRemaining > 0f) {
                    drawArc(
                        color = goldColor.copy(alpha = 0.18f),
                        startAngle = needleAngleDeg,
                        sweepAngle = sweepRemaining,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    color = goldColor,
                    startAngle = needleAngleDeg,
                    sweepAngle = sweepRemaining,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Text(
                text = "$topTotalMinutes",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = goldColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 26.dp)
            )

            Text(
                text = timeFormatted,
                fontSize = if (hours > 0) 26.sp else 32.sp,
                fontWeight = FontWeight.Black,
                color = textMain,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.align(Alignment.Center)
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                rotate(degrees = needleAngleDeg + 90f, pivot = center) {
                    drawLine(
                        color = goldColor,
                        start = Offset(center.x, center.y + 14.dp.toPx()),
                        end = Offset(center.x, center.y - radius * 0.76f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = goldColor,
                    radius = 5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = if (isDark) Color(0xFF121216) else Color.White,
                    radius = 2.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

// =============================================================================
// WATCH STYLE 2: RETRO FLIP CLOCK
// =============================================================================
@Composable
private fun RetroFlipClockWatch(
    totalSeconds: Int,
    isRunning: Boolean,
    onCustomModeToggle: () -> Unit,
    subjectName: String,
    goldColor: Color,
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

    val targetCardWidth = if (isLandscape) {
        if (isRunning) 235.dp else 195.dp
    } else {
        if (isRunning) 260.dp else 185.dp
    }

    val targetCardHeight = if (isLandscape) {
        if (isRunning) 175.dp else 145.dp
    } else {
        if (isRunning) 185.dp else 118.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetCardWidth,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "FlipCardWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetCardHeight,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "FlipCardHeight"
    )

    val baseFontSize = if (isLandscape) {
        if (isRunning) 145f else 115f
    } else {
        if (isRunning) 150f else 95f
    }
    val topFontSize = if (topDigitStr.length > 2) baseFontSize * 0.70f else baseFontSize
    val bottomFontSize = baseFontSize

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isRunning) (if (isLandscape) 2.dp else 6.dp) else 4.dp)
    ) {
        if (isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isRunning,
                    onClick = onCustomModeToggle
                )
            ) {
                AuthenticSplitFlapCard(
                    digit = topDigitStr,
                    badgeText = "• ${subjectName.uppercase()}",
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = topFontSize,
                    isDark = isDark,
                    goldColor = goldColor
                )

                AuthenticSplitFlapCard(
                    digit = bottomDigitStr,
                    badgeText = null,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = bottomFontSize,
                    isDark = isDark,
                    goldColor = goldColor
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isRunning,
                    onClick = onCustomModeToggle
                )
            ) {
                AuthenticSplitFlapCard(
                    digit = topDigitStr,
                    badgeText = "• ${subjectName.uppercase()}",
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = topFontSize,
                    isDark = isDark,
                    goldColor = goldColor
                )

                AuthenticSplitFlapCard(
                    digit = bottomDigitStr,
                    badgeText = null,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = bottomFontSize,
                    isDark = isDark,
                    goldColor = goldColor
                )
            }
        }
    }
}

// =============================================================================
// WATCH STYLE 3: CARBON MATRIX DIGITAL (120 DENSE TICKS)
// =============================================================================
@Composable
private fun CarbonMatrixDigitalWatch(
    dialMinutes: Float,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int
) {
    val effectiveTotalSeconds = when {
        initialTotalSeconds > 0 -> initialTotalSeconds
        totalSeconds > 0 -> totalSeconds
        else -> (dialMinutes.toInt().coerceAtLeast(1)) * 60
    }
    val topTotalMinutes = ((effectiveTotalSeconds + 59) / 60).coerceAtLeast(1)

    val remainingFraction = if (!isRunning) {
        1f
    } else {
        (totalSeconds.toFloat() / effectiveTotalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(258.dp)
                .padding(vertical = 4.dp)
                .clickable(
                    enabled = !isRunning,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCustomModeToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = if (isDark) Color(0xFF14141B) else Color(0xFFF8FAFC),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = if (isDark) Color(0xFF282834) else Color(0xFFCBD5E1),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4.dp.toPx())
                )

                val totalSegments = 120
                val activeSegments = (remainingFraction * totalSegments).roundToInt()

                for (i in 0 until totalSegments) {
                    val angleDeg = (i * 3f) - 90f
                    val rad = angleDeg * (PI.toFloat() / 180f)
                    val isMajor = (i % 10 == 0)
                    val isMedium = (i % 5 == 0)
                    val isLit = !isRunning || i < activeSegments

                    val outerPtRadius = radius - 6.dp.toPx()
                    val innerPtRadius = when {
                        isMajor -> radius - 14.dp.toPx()
                        isMedium -> radius - 11.dp.toPx()
                        else -> radius - 9.dp.toPx()
                    }

                    drawLine(
                        color = if (isLit) {
                            goldColor.copy(
                                alpha = when {
                                    isMajor -> 1f
                                    isMedium -> 0.85f
                                    else -> 0.65f
                                }
                            )
                        } else {
                            textMuted.copy(alpha = if (isDark) 0.18f else 0.15f)
                        },
                        start = Offset(center.x + innerPtRadius * cos(rad), center.y + innerPtRadius * sin(rad)),
                        end = Offset(center.x + outerPtRadius * cos(rad), center.y + outerPtRadius * sin(rad)),
                        strokeWidth = when {
                            isMajor -> 2.4.dp.toPx()
                            isMedium -> 1.6.dp.toPx()
                            else -> 1.1.dp.toPx()
                        },
                        cap = StrokeCap.Butt
                    )
                }

                drawCircle(
                    color = goldColor.copy(alpha = 0.22f),
                    radius = radius - 19.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$topTotalMinutes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = goldColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = timeFormatted,
                    fontSize = if (hours > 0) 31.sp else 39.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = goldColor,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    text = "LEFT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMuted.copy(alpha = 0.85f),
                    letterSpacing = 2.2.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// =============================================================================
// WATCH STYLE 4: SAND HOURGLASS TIMER (NO TEXT INSIDE, TIME + % OUTSIDE BELOW)
// =============================================================================
@Composable
private fun SandHourglassTimerWatch(
    dialMinutes: Float,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    textMain: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int
) {
    val effectiveTotalSeconds = when {
        initialTotalSeconds > 0 -> initialTotalSeconds
        totalSeconds > 0 -> totalSeconds
        else -> (dialMinutes.toInt().coerceAtLeast(1)) * 60
    }

    val remainingFraction = if (!isRunning) {
        1f
    } else {
        (totalSeconds.toFloat() / effectiveTotalSeconds.toFloat()).coerceIn(0f, 1f)
    }
    val elapsedFraction = (1f - remainingFraction).coerceIn(0f, 1f)
    val progressPercent = (remainingFraction * 100f).roundToInt().coerceIn(0, 100)

    val grainAnim = rememberInfiniteTransition(label = "sand_grain_stream")
    val grainPhase by grainAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sand_grain_phase"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = !isRunning,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCustomModeToggle
            )
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val neckY = h * 0.50f
                val topY = h * 0.10f
                val bottomY = h * 0.90f

                val glassPath = Path().apply {
                    moveTo(w * 0.24f, topY)
                    lineTo(w * 0.76f, topY)
                    cubicTo(w * 0.76f, h * 0.34f, centerX + w * 0.05f, neckY - h * 0.05f, centerX + w * 0.03f, neckY)
                    cubicTo(centerX + w * 0.05f, neckY + h * 0.05f, w * 0.76f, h * 0.66f, w * 0.76f, bottomY)
                    lineTo(w * 0.24f, bottomY)
                    cubicTo(w * 0.24f, h * 0.66f, centerX - w * 0.05f, neckY + h * 0.05f, centerX - w * 0.03f, neckY)
                    cubicTo(centerX - w * 0.05f, neckY - h * 0.05f, w * 0.24f, h * 0.34f, w * 0.24f, topY)
                    close()
                }

                drawPath(
                    path = glassPath,
                    color = if (isDark) Color(0xFF161620) else Color(0xFFF8FAFC)
                )

                clipPath(glassPath) {
                    val upperSandHeight = (neckY - (topY + h * 0.08f)) * remainingFraction
                    if (upperSandHeight > 1f) {
                        val sandTopY = neckY - upperSandHeight
                        val upperSandPath = Path().apply {
                            moveTo(w * 0.15f, sandTopY)
                            quadraticTo(centerX, sandTopY - 6.dp.toPx(), w * 0.85f, sandTopY)
                            lineTo(w * 0.85f, neckY)
                            lineTo(w * 0.15f, neckY)
                            close()
                        }
                        drawPath(
                            path = upperSandPath,
                            color = goldColor.copy(alpha = 0.88f)
                        )
                    }

                    val lowerSandHeight = ((bottomY - h * 0.04f) - neckY) * elapsedFraction
                    if (lowerSandHeight > 1f) {
                        val moundTopY = bottomY - lowerSandHeight
                        val lowerSandPath = Path().apply {
                            moveTo(w * 0.15f, bottomY)
                            lineTo(w * 0.15f, moundTopY + 5.dp.toPx())
                            quadraticTo(centerX, moundTopY - 8.dp.toPx(), w * 0.85f, moundTopY + 5.dp.toPx())
                            lineTo(w * 0.85f, bottomY)
                            close()
                        }
                        drawPath(
                            path = lowerSandPath,
                            color = goldColor
                        )
                    }

                    if (isRunning && remainingFraction > 0.01f) {
                        drawLine(
                            color = goldColor.copy(alpha = 0.75f),
                            start = Offset(centerX, neckY),
                            end = Offset(centerX, bottomY - lowerSandHeight.coerceAtLeast(8f)),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        for (g in 0 until 4) {
                            val t = (grainPhase + g * 0.25f) % 1f
                            val gy = neckY + t * (bottomY - neckY - lowerSandHeight.coerceAtLeast(10f))
                            val gx = centerX + sin(t * 12f + g) * 2.5.dp.toPx()
                            drawCircle(
                                color = goldColor,
                                radius = 1.6.dp.toPx(),
                                center = Offset(gx, gy)
                            )
                        }
                    }
                }

                drawPath(
                    path = glassPath,
                    color = goldColor.copy(alpha = 0.72f),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val capWidth = w * 0.58f
                val capHeight = 6.dp.toPx()
                drawRoundRect(
                    color = goldColor.copy(alpha = 0.85f),
                    topLeft = Offset(centerX - capWidth / 2f, topY - capHeight / 2f),
                    size = Size(capWidth, capHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = goldColor.copy(alpha = 0.85f),
                    topLeft = Offset(centerX - capWidth / 2f, bottomY - capHeight / 2f),
                    size = Size(capWidth, capHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = timeFormatted,
            fontSize = if (hours > 0) 34.sp else 40.sp,
            fontWeight = FontWeight.Black,
            color = textMain,
            letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
        )
        Text(
            text = "$progressPercent%",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = goldColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// =============================================================================
// WATCH STYLE 5: WATER WAVE TIMER (NO TEXT INSIDE, TIME + % OUTSIDE BELOW)
// =============================================================================
@Composable
private fun WaterWaveTimerWatch(
    dialMinutes: Float,
    totalSeconds: Int,
    initialTotalSeconds: Int,
    isRunning: Boolean,
    onCustomModeToggle: () -> Unit,
    goldColor: Color,
    textMain: Color,
    isDark: Boolean,
    timeFormatted: String,
    hours: Int
) {
    val effectiveTotalSeconds = when {
        initialTotalSeconds > 0 -> initialTotalSeconds
        totalSeconds > 0 -> totalSeconds
        else -> (dialMinutes.toInt().coerceAtLeast(1)) * 60
    }

    val remainingFraction = if (!isRunning) {
        1f
    } else {
        (totalSeconds.toFloat() / effectiveTotalSeconds.toFloat()).coerceIn(0f, 1f)
    }
    val progressPercent = (remainingFraction * 100f).roundToInt().coerceIn(0, 100)

    val waveTransition = rememberInfiniteTransition(label = "water_wave_anim")
    val waveShift by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val bubbleShift by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_phase"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = !isRunning,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCustomModeToggle
            )
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 8.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                val vesselPath = Path().apply {
                    addOval(
                        Rect(
                            left = center.x - radius,
                            top = center.y - radius,
                            right = center.x + radius,
                            bottom = center.y + radius
                        )
                    )
                }

                drawCircle(
                    color = if (isDark) Color(0xFF15151E) else Color(0xFFF8FAFC),
                    radius = radius,
                    center = center
                )

                clipPath(vesselPath) {
                    val topEdgeY = center.y - radius
                    val bottomEdgeY = center.y + radius
                    val waterBaseY = bottomEdgeY - (radius * 2f * remainingFraction)
                    val amp = if (isRunning) 5.5.dp.toPx() else 2.5.dp.toPx()

                    val backWavePath = Path().apply {
                        val stepPx = 8f
                        var x = center.x - radius
                        moveTo(x, bottomEdgeY)
                        while (x <= center.x + radius + stepPx) {
                            val normX = ((x - (center.x - radius)) / (radius * 2f)) * (2f * PI).toFloat()
                            val y = waterBaseY - 3.dp.toPx() + sin(normX + waveShift * 0.7f + 1.2f) * (amp * 0.8f)
                            lineTo(x, y)
                            x += stepPx
                        }
                        lineTo(center.x + radius, bottomEdgeY)
                        close()
                    }
                    drawPath(
                        path = backWavePath,
                        color = goldColor.copy(alpha = 0.35f)
                    )

                    val frontWavePath = Path().apply {
                        val stepPx = 8f
                        var x = center.x - radius
                        moveTo(x, bottomEdgeY)
                        while (x <= center.x + radius + stepPx) {
                            val normX = ((x - (center.x - radius)) / (radius * 2f)) * (2f * PI).toFloat()
                            val y = waterBaseY + cos(normX + waveShift) * amp
                            lineTo(x, y)
                            x += stepPx
                        }
                        lineTo(center.x + radius, bottomEdgeY)
                        close()
                    }
                    drawPath(
                        path = frontWavePath,
                        color = goldColor.copy(alpha = 0.72f)
                    )

                    for (b in 0 until 5) {
                        val phase = (bubbleShift + b * 0.2f) % 1f
                        val bx = center.x - radius * 0.55f + ((b * 37) % 100) / 100f * (radius * 1.1f)
                        val by = bottomEdgeY - phase * (radius * 1.7f)
                        if (by >= topEdgeY + 10f) {
                            drawCircle(
                                color = if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f),
                                radius = (2.dp + (b % 2).dp).toPx(),
                                center = Offset(bx, by)
                            )
                        }
                    }
                }

                drawCircle(
                    color = goldColor.copy(alpha = 0.75f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = timeFormatted,
            fontSize = if (hours > 0) 34.sp else 40.sp,
            fontWeight = FontWeight.Black,
            color = textMain,
            letterSpacing = if (hours > 0) (-0.5).sp else 0.sp
        )
        Text(
            text = "$progressPercent%",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = goldColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * MECHANICAL SPLIT-FLAP COMPONENT
 */
@Composable
private fun AuthenticSplitFlapCard(
    digit: String,
    badgeText: String? = null,
    cardWidth: Dp,
    cardHeight: Dp,
    fontSizeSp: Float,
    isDark: Boolean,
    goldColor: Color
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
    val cornerRadius = 18.dp

    val topShape = RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomStart = 2.5.dp,
        bottomEnd = 2.5.dp
    )
    val bottomShape = RoundedCornerShape(
        topStart = 2.5.dp,
        topEnd = 2.5.dp,
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
                badgeText = badgeText,
                isTop = true,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                halfHeight = halfHeight,
                fontSizeSp = fontSizeSp,
                shape = topShape,
                isDark = isDark,
                goldColor = goldColor
            )

            SplitFlapHalfPlate(
                digit = if (flipAnim.isRunning) previousDigit else currentDigit,
                badgeText = null,
                isTop = false,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                halfHeight = halfHeight,
                fontSizeSp = fontSizeSp,
                shape = bottomShape,
                isDark = isDark,
                goldColor = goldColor
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
                    badgeText = if (!isTopFolded) badgeText else null,
                    isTop = !isTopFolded,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    halfHeight = halfHeight,
                    fontSizeSp = fontSizeSp,
                    shape = if (isTopFolded) bottomShape else topShape,
                    isDark = isDark,
                    goldColor = goldColor
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(seamGap)
                .background(if (isDark) Color(0xFF09090C) else Color(0xFFF8FAFC))
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun SplitFlapHalfPlate(
    digit: String,
    badgeText: String? = null,
    isTop: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    halfHeight: Dp,
    fontSizeSp: Float,
    shape: Shape,
    isDark: Boolean,
    goldColor: Color
) {
    val plateBg = if (isDark) Color(0xFF1B1B1E) else Color(0xFFFAF8F5)
    val plateBorder = if (isDark) Color(0x22FFFFFF) else Color(0x1F333333)
    val digitTextColor = if (isDark) Color(0xFFCECECF) else Color(0xFF333333)

    Box(
        modifier = Modifier
            .size(width = cardWidth, height = halfHeight)
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 3.dp,
                        shape = shape,
                        spotColor = Color(0x15000000),
                        ambientColor = Color(0x0C000000)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(plateBg)
            .border(1.2.dp, plateBorder, shape)
    ) {
        if (isTop && badgeText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = badgeText,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) goldColor.copy(alpha = 0.85f) else Color(0xFF78716C),
                    letterSpacing = 1.4.sp
                )
            }
        }

        Layout(
            content = {
                Text(
                    text = digit,
                    fontSize = fontSizeSp.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = digitTextColor,
                    letterSpacing = (-3).sp
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
// CUSTOM HORIZONTAL SLIDER
// =============================================================================
@Composable
fun CustomHorizontalSlider(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
    goldColor: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    isDark: Boolean = ThemeManager.isDarkTheme.value
) {
    var rawMinutes by remember(dialMinutes) { mutableFloatStateOf(dialMinutes.coerceIn(5f, 180f)) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF13131A) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                val spacing = 22.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val finalSnapped = (round(rawMinutes / 5f) * 5f).coerceIn(5f, 180f)
                        rawMinutes = finalSnapped
                        onDialMinutesChange(finalSnapped)
                    },
                    onDragCancel = {}
                ) { change, dragAmount ->
                    change.consume()
                    val deltaMinutes = (-dragAmount / spacing) * 5f
                    rawMinutes = (rawMinutes + deltaMinutes).coerceIn(5f, 180f)

                    val currentSnapped = (round(rawMinutes / 5f) * 5f).coerceIn(5f, 180f)
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

            for (m in 5..180 step 5) {
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

                    val tickColor = if (isDark) {
                        if (isMajor30 || isMajor15) textMain.copy(alpha = alpha * 0.9f)
                        else textMuted.copy(alpha = alpha * 0.6f)
                    } else {
                        if (isMajor30 || isMajor15) Color(0xFF64748B).copy(alpha = alpha * 0.9f)
                        else Color(0xFF94A3B8).copy(alpha = alpha * 0.65f)
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

        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(32.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(2.dp),
                    spotColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0x55000000),
                    ambientColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0x33000000)
                )
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
                .border(
                    width = if (isDark) 0.5.dp else 1.dp,
                    color = if (isDark) Color(0x44FFFFFF) else Color(0x33000000),
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
