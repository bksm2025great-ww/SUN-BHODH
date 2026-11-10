package com.amon.timer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.Dialog

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

    // 🎯 Drum Scroll Time Picker Dialog State
    var showDrumTimeDialog by remember { mutableStateOf(false) }
    var isEditingPomodoroFocus by remember { mutableStateOf(false) }

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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ⏱️ 25m Text पर क्लिक करते ही Drum Picker खुलेगा
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    FocusSoundManager.triggerHaptic(context, false)
                                    isEditingPomodoroFocus = false
                                    showDrumTimeDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (timerMinutes >= 60) {
                                    val h = timerMinutes / 60
                                    val m = timerMinutes % 60
                                    if (m > 0) "${h}h ${m}m" else "${h}h"
                                } else "${timerMinutes}m",
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black,
                                color = goldColor
                            )
                            Text(
                                text = "Tap to set custom time ⏱️",
                                fontSize = 11.sp,
                                color = textMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text("QUICK PICK", fontSize = 11.sp, color = textMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        // 6 Quick Picks: 15m, 30m, 45m | 1h, 1.5h, Custom
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 30, 45).forEach { mins ->
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
                                            text = "${mins}m",
                                            color = if (isSel) Color.Black else textMain,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Row 2: 1h, 1.5h, Custom ⏱️
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val is1h = timerMinutes == 60
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (is1h) goldColor else (if (isDark) Color(0xFF1E1E28) else Color(0xFFF1F5F9)))
                                        .border(1.dp, if (is1h) goldColor else glassBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            timerMinutes = 60
                                        }
                                ) {
                                    Text("1h", color = if (is1h) Color.Black else textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                val is90m = timerMinutes == 90
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (is90m) goldColor else (if (isDark) Color(0xFF1E1E28) else Color(0xFFF1F5F9)))
                                        .border(1.dp, if (is90m) goldColor else glassBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            timerMinutes = 90
                                        }
                                ) {
                                    Text("1.5h", color = if (is90m) Color.Black else textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // 🎯 CUSTOM BUTTON
                                val isCustom = timerMinutes !in listOf(15, 30, 45, 60, 90)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCustom) goldColor else (if (isDark) Color(0xFF1E1E28) else Color(0xFFF1F5F9)))
                                        .border(1.dp, if (isCustom) goldColor else glassBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            FocusSoundManager.triggerHaptic(context, false)
                                            isEditingPomodoroFocus = false
                                            showDrumTimeDialog = true
                                        }
                                ) {
                                    Text(
                                        text = "Custom ⏱️",
                                        color = if (isCustom) Color.Black else goldColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                FocusModeType.STOPWATCH -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "00:00:00", fontSize = 42.sp, fontWeight = FontWeight.Black, color = textMain)
                        Text(text = "Count-up Mode (0 ➔ ∞)", fontSize = 13.sp, color = goldColor, fontWeight = FontWeight.Bold)
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

                        // Stepper: Focus Time (With direct Drum Picker tap)
                        PomodoroStepperRow(
                            label = "Focus",
                            valueText = "$pomoFocusMinutes min",
                            onMinus = { if (pomoFocusMinutes > 5) pomoFocusMinutes -= 5 },
                            onPlus = { if (pomoFocusMinutes < 180) pomoFocusMinutes += 5 },
                            onTextClick = {
                                isEditingPomodoroFocus = true
                                showDrumTimeDialog = true
                            },
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        // Stepper: Short Break
                        PomodoroStepperRow(
                            label = "Short break",
                            valueText = "$pomoShortBreak min",
                            onMinus = { if (pomoShortBreak > 1) pomoShortBreak -= 1 },
                            onPlus = { if (pomoShortBreak < 30) pomoShortBreak += 1 },
                            onTextClick = {},
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        // Stepper: Long Break
                        PomodoroStepperRow(
                            label = "Long break",
                            valueText = "$pomoLongBreak min",
                            onMinus = { if (pomoLongBreak > 5) pomoLongBreak -= 5 },
                            onPlus = { if (pomoLongBreak < 60) pomoLongBreak += 5 },
                            onTextClick = {},
                            goldColor = goldColor,
                            textMain = textMain,
                            cardBg = if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9),
                            glassBorder = glassBorder
                        )

                        // Stepper: Rounds
                        PomodoroStepperRow(
                            label = "Rounds",
                            valueText = "$pomoRounds",
                            onMinus = { if (pomoRounds > 2) pomoRounds -= 1 },
                            onPlus = { if (pomoRounds < 8) pomoRounds += 1 },
                            onTextClick = {},
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
                            Text("Start next focus by itself", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
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

    // 🥁 DRUM SCROLL TIME PICKER DIALOG (Hours & Minutes Wheel)
    if (showDrumTimeDialog) {
        val initialMins = if (isEditingPomodoroFocus) pomoFocusMinutes else timerMinutes
        DrumScrollTimePickerDialog(
            initialMinutes = initialMins,
            isDark = isDark,
            goldColor = goldColor,
            textMain = textMain,
            textMuted = textMuted,
            onConfirm = { selectedMins ->
                if (isEditingPomodoroFocus) {
                    pomoFocusMinutes = selectedMins
                } else {
                    timerMinutes = selectedMins
                }
                showDrumTimeDialog = false
            },
            onDismiss = { showDrumTimeDialog = false }
        )
    }
}

@Composable
fun PomodoroStepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onTextClick: () -> Unit,
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
                modifier = Modifier
                    .widthIn(min = 50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onTextClick() },
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

// =============================================================================
// 🥁 DRUM SCROLL WHEEL PICKER (HOURS + MINUTES)
// =============================================================================
@Composable
fun DrumScrollTimePickerDialog(
    initialMinutes: Int,
    isDark: Boolean,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialH = (initialMinutes / 60).coerceIn(0, 8)
    val initialM = (initialMinutes % 60)

    var selectedHour by remember { mutableIntStateOf(initialH) }
    var selectedMin by remember { mutableIntStateOf(initialM) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF161622) else Color.White)
                .border(1.2.dp, goldColor, RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set Custom Time",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = goldColor
                )

                // Selected Time Display
                val totalM = selectedHour * 60 + selectedMin
                Text(
                    text = if (selectedHour > 0) "${selectedHour}h ${selectedMin}m" else "${selectedMin}m",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain
                )

                // 2 Drum Wheels: Hours and Minutes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Wheel (0 - 8 hours)
                    DrumWheelColumn(
                        items = (0..8).map { "$it h" },
                        initialIndex = initialH,
                        onItemSelected = { selectedHour = it },
                        goldColor = goldColor,
                        textMain = textMain,
                        textMuted = textMuted,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = goldColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes Wheel (0 - 55 step 5 mins)
                    val minuteOptions = (0..55 step 5).toList()
                    val mIndex = minuteOptions.indexOfFirst { it >= initialM }.coerceAtLeast(0)
                    DrumWheelColumn(
                        items = minuteOptions.map { "$it m" },
                        initialIndex = mIndex,
                        onItemSelected = { selectedMin = minuteOptions[it] },
                        goldColor = goldColor,
                        textMain = textMain,
                        textMuted = textMuted,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", color = textMuted, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val finalMinutes = (selectedHour * 60 + selectedMin).coerceIn(5, 480)
                            onConfirm(finalMinutes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Set Time 🎯", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DrumWheelColumn(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onItemSelected(listState.firstVisibleItemIndex.coerceIn(0, items.size - 1))
    }

    Box(
        modifier = modifier.height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(goldColor.copy(alpha = 0.15f))
                .border(1.2.dp, goldColor, RoundedCornerShape(10.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(vertical = 46.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = listState.firstVisibleItemIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = if (isSelected) 18.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) goldColor else textMuted.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
