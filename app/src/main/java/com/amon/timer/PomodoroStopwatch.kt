package com.amon.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// =============================================================================
// 🍅 1. POMODORO SESSION TRACKER (BADA GOLA / CHHOTA GOLA)
// =============================================================================
@Composable
fun PomodoroSessionTracker(
    totalSessions: Int,
    currentSessionIndex: Int,
    isBreakActive: Boolean,
    isRunning: Boolean,
    goldColor: Color,
    glowYellow: Color,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_session")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isDark) Color(0xFF14141E) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(50))
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSessions) {
                val isCompleted = i < currentSessionIndex
                val isCurrent = i == currentSessionIndex
                val isUpcoming = i > currentSessionIndex

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp)
                ) {
                    when {
                        isCompleted -> {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(goldColor)
                            )
                        }
                        isCurrent -> {
                            if (isBreakActive) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(CircleShape)
                                        .background(goldColor.copy(alpha = if (isRunning) pulseAlpha * 0.4f else 0.3f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                            }
                        }
                        isUpcoming -> {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .border(1.6.dp, if (isDark) Color(0xFF38384A) else Color(0xFF94A3B8), CircleShape)
                            )
                        }
                    }
                }

                if (i < totalSessions) {
                    val isBreakCurrent = (i == currentSessionIndex && isBreakActive)
                    val isBreakDone = (i < currentSessionIndex)

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.8.dp)
                                .background(if (isBreakDone) goldColor.copy(alpha = 0.5f) else (if (isDark) Color(0xFF262636) else Color(0xFFCBD5E1)))
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isBreakCurrent) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isBreakCurrent -> Color(0xFF10B981)
                                        isBreakDone -> goldColor.copy(alpha = 0.6f)
                                        else -> if (isDark) Color(0xFF343446) else Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.8.dp)
                                .background(if (isBreakDone) goldColor.copy(alpha = 0.5f) else (if (isDark) Color(0xFF262636) else Color(0xFFCBD5E1)))
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// ⚙️ 2. POMODORO SETTINGS MODAL BOTTOM SHEET
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsBottomSheet(
    isDark: Boolean,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    focusMinutes: Int,
    shortBreakMinutes: Int,
    longBreakMinutes: Int,
    totalSessions: Int,
    autoStartBreak: Boolean,
    autoStartFocus: Boolean,
    onFocusMinutesChange: (Int) -> Unit,
    onShortBreakMinutesChange: (Int) -> Unit,
    onLongBreakMinutesChange: (Int) -> Unit,
    onSessionsChange: (Int) -> Unit,
    onAutoStartBreakChange: (Boolean) -> Unit,
    onAutoStartFocusChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPillTab by remember { mutableIntStateOf(0) }
    var showEnterMinutesDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF14141E) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color(0x44FFFFFF) else Color(0x33000000))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pomodoro Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        .clickable { onDismiss() }
                ) {
                    Text("✕", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF1E1E2A) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                val tabWidth = maxWidth / 3
                val animatedPillOffset by animateDpAsState(
                    targetValue = tabWidth * selectedPillTab,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                    label = "pill_glide"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedPillOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(goldColor)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    val tabs = listOf(
                        Triple("Focus", "${focusMinutes}m", 0),
                        Triple("Break", "${shortBreakMinutes}m", 1),
                        Triple("Long", "${longBreakMinutes}m", 2)
                    )

                    tabs.forEach { (name, timeStr, index) ->
                        val isSelected = selectedPillTab == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedPillTab = index }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else textMuted
                            )
                            Text(
                                text = timeStr,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.Black else textMain,
                                modifier = Modifier.clickable {
                                    selectedPillTab = index
                                    showEnterMinutesDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val (currentVal, minVal, maxVal, ticks) = when (selectedPillTab) {
                    0 -> Quadruple(focusMinutes.toFloat(), 1f, 90f, listOf(5, 25, 60, 90))
                    1 -> Quadruple(shortBreakMinutes.toFloat(), 1f, 15f, listOf(1, 5, 10, 15))
                    else -> Quadruple(longBreakMinutes.toFloat(), 5f, 45f, listOf(5, 15, 30, 45))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedPillTab) {
                            0 -> "Focus Duration"
                            1 -> "Short Break"
                            else -> "Long Break"
                        },
                        fontWeight = FontWeight.Bold,
                        color = textMain,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${currentVal.toInt()} min",
                        fontWeight = FontWeight.Black,
                        color = goldColor,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = currentVal,
                    onValueChange = { newVal ->
                        when (selectedPillTab) {
                            0 -> onFocusMinutesChange(newVal.toInt())
                            1 -> onShortBreakMinutesChange(newVal.toInt())
                            else -> onLongBreakMinutesChange(newVal.toInt())
                        }
                    },
                    valueRange = minVal..maxVal,
                    colors = SliderDefaults.colors(thumbColor = goldColor, activeTrackColor = goldColor)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ticks.forEach { tick ->
                        Text(
                            text = "${tick}m",
                            color = if (currentVal.toInt() == tick) goldColor else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                when (selectedPillTab) {
                                    0 -> onFocusMinutesChange(tick)
                                    1 -> onShortBreakMinutesChange(tick)
                                    else -> onLongBreakMinutesChange(tick)
                                }
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Focus sessions before Long Break", fontWeight = FontWeight.Bold, color = textMain, fontSize = 13.sp)
                    Text("$totalSessions", fontWeight = FontWeight.Black, color = goldColor, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(2, 3, 4, 5, 6).forEach { num ->
                        val isSelected = totalSessions == num
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) goldColor else (if (isDark) Color(0xFF22222E) else Color(0xFFE2E8F0)))
                                .border(1.dp, if (isSelected) glowYellow else (if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)), CircleShape)
                            .clickable { onSessionsChange(num) }
                        ) {
                            Text(
                                text = "$num",
                                color = if (isSelected) Color.Black else textMain,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoStartBreakChange(!autoStartBreak) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-start Break after Focus", fontSize = 12.5.sp, color = textMain, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = autoStartBreak,
                        onCheckedChange = onAutoStartBreakChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = goldColor, checkedTrackColor = goldColor.copy(alpha = 0.4f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoStartFocusChange(!autoStartFocus) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-start Focus after Break", fontSize = 12.5.sp, color = textMain, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = autoStartFocus,
                        onCheckedChange = onAutoStartFocusChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = goldColor, checkedTrackColor = goldColor.copy(alpha = 0.4f))
                    )
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("Save & Apply", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }

    if (showEnterMinutesDialog) {
        val currentMinutes = when (selectedPillTab) {
            0 -> focusMinutes
            1 -> shortBreakMinutes
            else -> longBreakMinutes
        }
        var inputMinutesText by remember { mutableStateOf(currentMinutes.toString()) }

        Dialog(onDismissRequest = { showEnterMinutesDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF181824) else Color(0xFFFFFFFF))
                    .border(1.5.dp, goldColor, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enter Minutes (Max 120)",
                            color = goldColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                                .clickable { showEnterMinutesDialog = false }
                        ) {
                            Text("✕", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputMinutesText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            val num = digitsOnly.toIntOrNull()
                            inputMinutesText = when {
                                num == null -> digitsOnly
                                num > 120 -> "120"
                                else -> digitsOnly
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = textMain
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            cursorColor = goldColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(68.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val finalMin = inputMinutesText.toIntOrNull()?.coerceIn(1, 120) ?: 25
                            when (selectedPillTab) {
                                0 -> onFocusMinutesChange(finalMin)
                                1 -> onShortBreakMinutesChange(finalMin.coerceAtMost(30))
                                else -> onLongBreakMinutesChange(finalMin.coerceAtMost(60))
                            }
                            showEnterMinutesDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// ⏱️ 3. STOPWATCH DISPLAY (HOME TAB READOUT)
// =============================================================================
@Composable
fun StopwatchPrecisionDisplay(
    swMainTime: String,
    swMilliText: String,
    goldColor: Color,
    textMain: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "STOPWATCH PRECISION",
            color = goldColor,
            fontSize = 11.5.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = swMainTime,
                color = textMain,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = swMilliText,
                color = goldColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

// =============================================================================
// 🎯 4. ADAPTIVE FOCUS CONTROLS (PAUSE / RESUME / SWITCH TO BREAK / END SESSION)
// =============================================================================
@Composable
fun PomodoroFocusControls(
    isRunning: Boolean,
    isPaused: Boolean,
    isBreakActive: Boolean,
    goldColor: Color,
    glowYellow: Color,
    cardBg: Color,
    glassBorder: Color,
    textMain: Color,
    textMuted: Color,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSwitchToBreakClick: () -> Unit,
    onResumeFocusFromBreak: () -> Unit,
    onEndSessionClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // ☕ Subtle Chip: Break par switch karne ka option (Sirf tab dikhega jab pause ho)
        if (isPaused && !isBreakActive) {
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, glassBorder),
                modifier = Modifier.clickable { onSwitchToBreakClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = "☕", fontSize = 12.sp)
                    Text(
                        text = "Switch to Break",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Main Bottom Action Buttons (Sleek 2-Pill Row)
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                // State 1: Break chal raha hai (Left = End Session, Right = Resume Focus 🎯)
                isBreakActive -> {
                    OutlinedButton(
                        onClick = onEndSessionClick,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, glassBorder),
                        modifier = Modifier
                            .weight(0.75f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "End Session",
                            color = textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    Button(
                        onClick = onResumeFocusFromBreak,
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Resume Focus 🎯",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp
                        )
                    }
                }

                // State 2: Focus Paused hai (Left = End Session, Right = Resume 🎯)
                isPaused -> {
                    OutlinedButton(
                        onClick = onEndSessionClick,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, glassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "End Session",
                            color = textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onResumeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Resume 🎯",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp
                        )
                    }
                }

                // State 3: Focus chal raha hai (Running)
                else -> {
                    Button(
                        onClick = onPauseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, glassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⏸", color = textMain, fontSize = 14.sp)
                            Text(
                                text = "Pause",
                                color = textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onEndSessionClick,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, glassBorder),
                        modifier = Modifier
                            .weight(0.75f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "End Session",
                            color = textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}
