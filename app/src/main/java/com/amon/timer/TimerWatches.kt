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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    selectedSubjectName: String = "Focus",
    onTogglePlayPause: () -> Unit = {},
    showPresets: Boolean = true
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val watchPrefs = remember {
        context.getSharedPreferences("amon_watch_prefs", Context.MODE_PRIVATE)
    }

    var selectedWatchStyle by rememberSaveable {
        val saved = try {
            watchPrefs.getInt("selected_watch_style", 0)
        } catch (_: Exception) {
            0
        }
        mutableIntStateOf(saved)
    }

    val onPrevStyle = {
        selectedWatchStyle = if (selectedWatchStyle > 0) selectedWatchStyle - 1 else 2
        try {
            watchPrefs.edit().putInt("selected_watch_style", selectedWatchStyle).apply()
        } catch (_: Exception) {}
    }

    val onNextStyle = {
        selectedWatchStyle = if (selectedWatchStyle < 2) selectedWatchStyle + 1 else 0
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

                    1 -> RotaryDialWatch(
                        dialMinutes = dialMinutes,
                        onDialMinutesChange = onDialMinutesChange,
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

                    2 -> RetroFlipClockWatch(
                        totalSeconds = totalSeconds,
                        isRunning = isRunning,
                        onCustomModeToggle = onCustomModeToggle,
                        subjectName = selectedSubjectName,
                        goldColor = goldColor,
                        isDark = isDark
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
// 🏹 PURE FLOATING CHEVRON BUTTON
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
// ⏱ WATCH STYLE 1: CLASSIC LUXE RING
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
// 🎛 WATCH STYLE 2: ROTARY DIAL
// =============================================================================
@Composable
private fun RotaryDialWatch(
    dialMinutes: Float,
    onDialMinutesChange: (Float) -> Unit,
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
                                val snappedMins = (round(computedMins / 5f) * 5f).coerceIn(5f, 180f)

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
                    style = Stroke(width = 3.8.dp.toPx())
                )

                if (!isRunning) {
                    val totalMarks = 36
                    for (i in 1..totalMarks) {
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

                    val sweep = (dialMinutes.coerceIn(5f, 180f) / 180f) * 360f
                    drawArc(
                        color = goldColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val knobAngleRad = (sweep - 90f) * (PI.toFloat() / 180f)
                    val knobX = center.x + radius * cos(knobAngleRad)
                    val knobY = center.y + radius * sin(knobAngleRad)

                    drawCircle(color = goldColor.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(knobX, knobY))
                    drawCircle(color = goldColor, radius = 9.dp.toPx(), center = Offset(knobX, knobY))
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(knobX, knobY))
                } else {
                    val maxSeconds = if (initialTotalSeconds > 0) initialTotalSeconds else maxOf(totalSeconds, 1)
                    val sweep = (totalSeconds.toFloat() / maxSeconds.toFloat() * 360f).coerceIn(0f, 360f)

                    drawArc(
                        color = goldColor.copy(alpha = 0.20f),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 8.8.dp.toPx(), cap = StrokeCap.Round)
                    )

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
    }
}

// =============================================================================
// 📜 WATCH STYLE 3: RETRO FLIP CLOCK (UNIFIED WITH BOTTOM SHEET)
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
                    onClick = onCustomModeToggle // 🎯 अब यह सीधे वही शेयर्ड बॉटम शीट खोलेगा
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
                    onClick = onCustomModeToggle // 🎯 अब यह सीधे वही शेयर्ड बॉटम शीट खोलेगा
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

/**
 * 🎴 MECHANICAL SPLIT-FLAP COMPONENT
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

        // 🌟 स्प्लिट सीम लाइन: लाइट थीम में बैकग्राउंड जैसा साफ़ रंग (#F8FAFC) ताकि कोई भूरा पैच न दिखे
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
// 🌟 CUSTOM HORIZONTAL SLIDER
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
