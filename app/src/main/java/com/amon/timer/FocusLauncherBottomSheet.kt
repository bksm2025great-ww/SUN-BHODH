package com.amon.timer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusLauncherBottomSheet(
    isDark: Boolean,
    goldColor: Color,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    initialMode: FocusModeType = FocusModeType.TIMER,
    availableSubjects: List<String>,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    onStartFocus: (mode: FocusModeType, durationSeconds: Int, pomodoroConfig: PomodoroConfig?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentMode by remember { mutableStateOf(initialMode) }
    var timerMinutes by remember { mutableIntStateOf(25) }

    // Pomodoro states
    var pomoFocusMinutes by remember { mutableIntStateOf(25) }
    var pomoShortBreak by remember { mutableIntStateOf(5) }
    var pomoLongBreak by remember { mutableIntStateOf(15) }
    var pomoRounds by remember { mutableIntStateOf(4) }
    var pomoAutoStartNext by remember { mutableStateOf(true) }

    val glassBorder = if (isDark) Color(0x2AFFFFFF) else Color(0xFFCBD5E1)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF14141A) else Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0x44FFFFFF) else Color(0x33000000))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launch Focus Session",
                    color = textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                        .clickable { onDismiss() }
                ) {
                    Text("✕", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 3-Mode Selector Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0xFF1B1B24) else Color(0xFFE2E8F0))
                    .padding(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    FocusModeType.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) goldColor else Color.Transparent)
                                .clickable {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    currentMode = mode
                                }
                        ) {
                            Text(
                                text = mode.title,
                                color = if (isSelected) Color.Black else textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Body according to selected mode
            when (currentMode) {
                FocusModeType.TIMER -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (timerMinutes >= 60) "${timerMinutes / 60}h ${timerMinutes % 60}m" else "${timerMinutes}m",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = goldColor
                        )

                        Text("QUICK PICK", fontSize = 11.sp, color = textMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        val quickPicks = listOf(15, 30, 45, 60, 90, 120)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickPicks.chunked(3).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { mins ->
                                        val isSel = timerMinutes == mins
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) goldColor else (if (isDark) Color(0xFF1E1E28) else Color(0xFFF1F5F9)))
                                                .border(1.dp, if (isSel) goldColor else glassBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    FocusSoundManager.triggerHaptic(context, false)
                                                    timerMinutes = mins
                                                }
                                        ) {
                                            Text(
                                                text = if (mins >= 60) "${mins / 60}h" else "${mins}m",
                                                color = if (isSel) Color.Black else textMain,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FocusModeType.STOPWATCH -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "00:00:00", fontSize = 42.sp, fontWeight = FontWeight.Black, color = textMain)
                        Text(text = "Count-up Mode (0 ➔ ∞)", fontSize = 13.sp, color = goldColor, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Starts from zero and counts upward until you end your session.",
                            fontSize = 11.5.sp,
                            color = textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                FocusModeType.POMODORO -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF1C1C26) else Color(0xFFF8FAFC))
                                .border(1.2.dp, goldColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    pomoFocusMinutes = 25
                                    pomoShortBreak = 5
                                    pomoLongBreak = 15
                                    pomoRounds = 4
                                    FocusSoundManager.triggerHaptic(context, false)
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "🍅", fontSize = 24.sp)
                                Column {
                                    Text("Classic Pomodoro", fontWeight = FontWeight.Black, color = textMain, fontSize = 13.5.sp)
                                    Text("25 min focus • 5 min break • 4 rounds (15m long break)", color = textMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        PomodoroStepperRow(
                            label = "Focus",
                            valueText = "$pomoFocusMinutes min",
                            onMinus = { if (pomoFocusMinutes > 5) pomoFocusMinutes -= 5 },
                            onPlus = { if (pomoFocusMinutes < 120) pomoFocusMinutes += 5 },
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        PomodoroStepperRow(
                            label = "Short break",
                            valueText = "$pomoShortBreak min",
                            onMinus = { if (pomoShortBreak > 1) pomoShortBreak -= 1 },
                            onPlus = { if (pomoShortBreak < 30) pomoShortBreak += 1 },
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        PomodoroStepperRow(
                            label = "Long break",
                            valueText = "$pomoLongBreak min",
                            onMinus = { if (pomoLongBreak > 5) pomoLongBreak -= 5 },
                            onPlus = { if (pomoLongBreak < 60) pomoLongBreak += 5 },
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        PomodoroStepperRow(
                            label = "Rounds",
                            valueText = "$pomoRounds",
                            onMinus = { if (pomoRounds > 2) pomoRounds -= 1 },
                            onPlus = { if (pomoRounds < 8) pomoRounds += 1 },
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start next focus by itself", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                Text("After short break, next session starts automatically.", color = textMuted, fontSize = 10.5.sp)
                            }
                            Switch(
                                checked = pomoAutoStartNext,
                                onCheckedChange = { pomoAutoStartNext = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = goldColor, checkedTrackColor = goldColor.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // Subject row chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("STUDY SUBJECT", fontSize = 11.sp, color = textMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSubjectsList = listOf("All") + availableSubjects.filter { !it.equals("All", ignoreCase = true) }
                    items(allSubjectsList) { subj ->
                        val isSel = selectedSubject.equals(subj, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSel) goldColor else (if (isDark) Color(0xFF1E1E28) else Color(0xFFF1F5F9)))
                                .border(1.dp, if (isSel) goldColor else glassBorder, RoundedCornerShape(50))
                                .clickable {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    onSubjectChange(subj)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = subj,
                                color = if (isSel) Color.Black else textMain,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Big Launch Action Button
            Button(
                onClick = {
                    FocusSoundManager.triggerHaptic(context, false)
                    when (currentMode) {
                        FocusModeType.TIMER -> {
                            onStartFocus(FocusModeType.TIMER, timerMinutes * 60, null)
                        }
                        FocusModeType.STOPWATCH -> {
                            onStartFocus(FocusModeType.STOPWATCH, 0, null)
                        }
                        FocusModeType.POMODORO -> {
                            val cfg = PomodoroConfig(
                                focusMinutes = pomoFocusMinutes,
                                shortBreakMinutes = pomoShortBreak,
                                longBreakMinutes = pomoLongBreak,
                                totalRounds = pomoRounds,
                                autoStartNextFocus = pomoAutoStartNext
                            )
                            onStartFocus(FocusModeType.POMODORO, pomoFocusMinutes * 60, cfg)
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Begin Focus Session 🚀",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun PomodoroStepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    goldColor: Color,
    textMain: Color,
    cardBg: Color,
    glassBorder: Color
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, glassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, color = textMain, fontSize = 13.sp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable {
                        FocusSoundManager.triggerHaptic(context, false)
                        onMinus()
                    }
            ) {
                Text("－", color = textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Text(
                text = valueText,
                fontWeight = FontWeight.Black,
                color = goldColor,
                fontSize = 13.sp,
                modifier = Modifier.widthIn(min = 45.dp),
                textAlign = TextAlign.Center
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable {
                        FocusSoundManager.triggerHaptic(context, false)
                        onPlus()
                    }
            ) {
                Text("＋", color = textMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
