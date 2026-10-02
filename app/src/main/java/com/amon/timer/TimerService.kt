package com.amon.timer

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    onTogglePlayPause: () -> Unit = {}
) {
    val context = LocalContext.current
    val watchPrefs = remember {
        context.getSharedPreferences("amon_watch_prefs", Context.MODE_PRIVATE)
    }

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
                    textMuted = textMuted,
                    onTogglePlayPause = onTogglePlayPause
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

            if (isCustomMode) {
                Spacer(modifier = Modifier.height(12.dp))
                CustomHorizontalSlider(
                    dialMinutes = dialMinutes,
                    onDialMinutesChange = onDialMinutesChange,
                    goldColor = goldColor,
                    cardBg = cardBg,
                    textMain = textMain,
                    textMuted = textMuted
                )
            }
        }
    }
}

// =============================================================================
// 🎛 WATCH STYLE 2: ROTARY DIAL (100% साइलेंट & मिनिमल)
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
            }
        }
    }
}

// =============================================================================
// 📜 WATCH STYLE 3: PURE 3D SPLIT-FLAP (टाइट बॉक्स + 1-स्वाइप लॉक + कीबोर्ड इनपुट)
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
    textMuted: Color,
    onTogglePlayPause: () -> Unit
) {
    val view = LocalView.current
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60

    val minStr = String.format("%02d", mins)
    val secStr = if (isRunning) String.format("%02d", secs) else "00"

    // 🌟 1-स्वाइप लॉक: उंगली के एक पूरे झटके पर केवल 1 नंबर बदलेगा
    var hasSwipedInCurrentGesture by remember { mutableStateOf(false) }
    var showTimeInputDialog by remember { mutableStateOf(false) }

    // 🌟 कॉम्पैक्ट टाइट डायमेंशन्स (खाली गैप पूरी तरह ख़त्म)
    val animatedWidth by animateDpAsState(
        targetValue = if (isRunning) 265.dp else 248.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FlipCardWidth"
    )
    val animatedHeight by animateDpAsState(
        targetValue = if (isRunning) 166.dp else 150.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FlipCardHeight"
    )

    val baseFontSize = if (isRunning) 115f else 105f
    val minFontSize = if (minStr.length > 2) baseFontSize * 0.72f else baseFontSize
    val secFontSize = baseFontSize

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isRunning) 6.dp else 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isRunning) 16.dp else 12.dp)
        ) {
            // 👆 MINUTES CARD: 1-स्वाइप लॉक + टैप-टू-टाइप + रनिंग में टैप-टू-पॉज़
            Box(
                modifier = Modifier
                    .clickable {
                        if (isRunning) {
                            onTogglePlayPause()
                        } else {
                            showTimeInputDialog = true
                        }
                    }
                    .pointerInput(isRunning) {
                        if (!isRunning) {
                            detectVerticalDragGestures(
                                onDragStart = { hasSwipedInCurrentGesture = false },
                                onDragEnd = { hasSwipedInCurrentGesture = false },
                                onDragCancel = { hasSwipedInCurrentGesture = false }
                            ) { change, dragAmount ->
                                change.consume()
                                if (!hasSwipedInCurrentGesture) {
                                    if (dragAmount < -24f) { // स्वाइप ऊपर -> +1 मिनट
                                        val nextMins = (dialMinutes + 1f).coerceIn(1f, 180f)
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onDialMinutesChange(nextMins)
                                        hasSwipedInCurrentGesture = true
                                    } else if (dragAmount > 24f) { // स्वाइप नीचे -> -1 मिनट
                                        val nextMins = (dialMinutes - 1f).coerceIn(1f, 180f)
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onDialMinutesChange(nextMins)
                                        hasSwipedInCurrentGesture = true
                                    }
                                }
                            }
                        }
                    }
            ) {
                AuthenticSplitFlapCard(
                    digit = minStr,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = minFontSize
                )
            }

            // ⏱ SECONDS CARD: सेटअप में '00' स्थिर, रनिंग में टैप-टू-पॉज़
            Box(
                modifier = Modifier.clickable {
                    if (isRunning) {
                        onTogglePlayPause()
                    } else {
                        showTimeInputDialog = true
                    }
                }
            ) {
                AuthenticSplitFlapCard(
                    digit = secStr,
                    cardWidth = animatedWidth,
                    cardHeight = animatedHeight,
                    fontSizeSp = secFontSize
                )
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
                listOf(25f, 45f, 60f).forEach { m ->
                    val isSelected = (dialMinutes * 60f).roundToInt() == (m * 60).toInt()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) goldColor else Color(0xFF18181C))
                            .border(1.dp, if (isSelected) glowYellow else Color(0x33FFFFFF), RoundedCornerShape(50))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onDialMinutesChange(m)
                            }
                    ) {
                        Text(
                            text = "${m.toInt()}m",
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ⌨️ कीबोर्ड डायलॉग: मनचाहा टाइम सीधे टाइप करने के लिए
    if (showTimeInputDialog) {
        var inputMinutesText by remember { mutableStateOf(dialMinutes.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showTimeInputDialog = false },
            containerColor = Color(0xFF141419),
            titleContentColor = goldColor,
            textContentColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Set Focus Time",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = goldColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter minutes (1 to 180):",
                        fontSize = 13.sp,
                        color = textMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputMinutesText,
                        onValueChange = { newStr ->
                            if (newStr.length <= 3 && newStr.all { it.isDigit() }) {
                                inputMinutesText = newStr
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = inputMinutesText.toIntOrNull()
                        if (parsed != null && parsed in 1..180) {
                            onDialMinutesChange(parsed.toFloat())
                        }
                        showTimeInputDialog = false
                    }
                ) {
                    Text("OK", color = goldColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeInputDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * 🎴 असली स्प्लिट-फ्लैप मैकेनिज्म (50-50 कट & शुद्ध मैकेनिकल फोल्ड)
 */
@Composable
private fun AuthenticSplitFlapCard(
    digit: String,
    cardWidth: Dp,
    cardHeight: Dp,
    fontSizeSp: Float
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
        // 1. स्थिर बैकग्राउंड पत्ते
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
                shape = topShape
            )

            SplitFlapHalfPlate(
                digit = if (flipAnim.isRunning) previousDigit else currentDigit,
                isTop = false,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                halfHeight = halfHeight,
                fontSizeSp = fontSizeSp,
                shape = bottomShape
            )
        }

        // 2. हवा में फ्लिप होने वाला पत्ता
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
                    shape = if (isTopFolded) bottomShape else topShape
                )
            }
        }

        // 3. बीच की 2px बारीक सीम-लाइन
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(seamGap)
                .background(Color.Black)
                .align(Alignment.Center)
        )
    }
}

/**
 * ✂️ गणितीय रूप से सटीक 50-50 कट रेंडरर
 */
@Composable
private fun SplitFlapHalfPlate(
    digit: String,
    isTop: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    halfHeight: Dp,
    fontSizeSp: Float,
    shape: Shape
) {
    Box(
        modifier = Modifier
            .size(width = cardWidth, height = halfHeight)
            .clip(shape)
            .background(Color(0xFF16161B))
            .border(1.2.dp, Color(0x22FFFFFF), shape)
    ) {
        Layout(
            content = {
                Text(
                    text = digit,
                    fontSize = fontSizeSp.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFFF1F5F9),
                    letterSpacing = (-2).sp
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
// 🌟 1:1 स्मूथ कस्टम स्लाइडर (सफ़ेद सुई + हार्डवेयर शैडो)
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = sliderDisplayTitle,
                color = textMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
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
                        .height(40.dp)
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
                }

                Box(
                    modifier = Modifier
                        .width(3.2.dp)
                        .height(28.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(2.dp),
                            spotColor = Color.White,
                            ambientColor = Color.White
                        )
                        .background(Color.White, RoundedCornerShape(2.dp))
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
