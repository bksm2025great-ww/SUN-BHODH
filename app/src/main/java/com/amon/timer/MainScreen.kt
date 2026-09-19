package com.amon.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.amon.timer.ui.theme.AccentYellow

@Composable
fun MainScreen() {

    // =========================================================================
    // 🟢 MODULE 1: TIMER ENGINE & SESSION STATE (टाइमर और ब्रेक का लॉजिक)
    // (यदि कभी सेशन का समय या ब्रेक बदलना हो, तो सिर्फ इस ब्लॉक को बदलें)
    // =========================================================================
    val focusSeconds = 25 * 60       // 25 मिनट का फ़ोकस सेशन
    val shortBreakSeconds = 5 * 60   // 5 मिनट का शॉर्ट ब्रेक
    val longBreakSeconds = 15 * 60   // 15 मिनट का लॉन्ग ब्रेक

    var initialSeconds by remember { mutableIntStateOf(focusSeconds) }
    var totalSeconds by remember { mutableIntStateOf(focusSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var currentSession by remember { mutableIntStateOf(1) } // 1, 2, या 3
    var isBreak by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && totalSeconds > 0) {
            delay(1000L)
            totalSeconds--
        }
        if (totalSeconds == 0 && isRunning) {
            isRunning = false
            if (!isBreak) {
                // फ़ोकस पूरा -> ब्रेक शुरू
                isBreak = true
                if (currentSession < 3) {
                    initialSeconds = shortBreakSeconds
                    totalSeconds = shortBreakSeconds
                } else {
                    initialSeconds = longBreakSeconds
                    totalSeconds = longBreakSeconds
                }
            } else {
                // ब्रेक पूरा -> अगला फ़ोकस सेशन
                isBreak = false
                if (currentSession < 3) {
                    currentSession++
                } else {
                    currentSession = 1 // चक्र दोबारा शुरू
                }
                initialSeconds = focusSeconds
                totalSeconds = focusSeconds
            }
        }
    }

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val sessionProgress = (initialSeconds - totalSeconds).toFloat() / initialSeconds.toFloat()

    // साइकोलॉजी कलर पैलेट
    val activeColor = when {
        !isBreak -> AccentYellow                       // फ़ोकस मोड: डोपामाइन येलो
        currentSession < 3 -> Color(0xFF34D399)         // शॉर्ट ब्रेक: सेज ग्रीन
        else -> Color(0xFF60A5FA)                       // 3 सेशन बाद लॉन्ग ब्रेक: स्काई ब्लू
    }
    val trackColor = Color(0xFF222228)                  // खाली रिंग का स्लेट डार्क कलर

    // =========================================================================
    // 🟢 MODULE 2: UI LAYOUT & 3-PART RING (स्क्रीन और 3-भागों वाली रिंग)
    // (यदि रिंग की मोटाई या गैप बदलना हो, तो यहाँ बदलाव करें)
    // =========================================================================
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                // 3-पार्ट वाली रिंग का विज़ुअल कैनवास
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    val gap = 12f
                    val sweep = (360f - (gap * 3)) / 3f // प्रत्येक आर्क 108 डिग्री

                    val startAngles = listOf(
                        -90f + (gap / 2f),                 // सेशन 1 (शीर्ष)
                        -90f + (gap / 2f) + sweep + gap,    // सेशन 2
                        -90f + (gap / 2f) + (sweep + gap) * 2 // सेशन 3
                    )

                    // तीनों सेगमेंट्स का बैकग्राउंड ट्रैक
                    startAngles.forEach { startAngle ->
                        drawArc(
                            color = trackColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // सक्रिय और पूर्ण सेगमेंट्स की प्रोग्रेस
                    startAngles.forEachIndexed { index, startAngle ->
                        val segmentIndex = index + 1
                        val fillFraction = when {
                            currentSession > segmentIndex -> 1.0f
                            currentSession == segmentIndex -> if (isBreak) 1.0f else sessionProgress
                            else -> 0.0f
                        }

                        if (fillFraction > 0f) {
                            drawArc(
                                color = activeColor,
                                startAngle = startAngle,
                                sweepAngle = sweep * fillFraction,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // =============================================================
                // 🟢 MODULE 3: CENTER DISPLAY (पौधा और टाइमर डिस्प्ले)
                // (यदि पौधा, टेक्स्ट का साइज़ या सबटाइटल बदलना हो)
                // =============================================================
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val plantEmoji = when (currentSession) {
                        1 -> "🌱"
                        2 -> "🌿"
                        else -> "🌳"
                    }

                    Text(text = plantEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeFormatted,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val statusText = when {
                        !isBreak -> "SESSION $currentSession OF 3"
                        currentSession < 3 -> "SHORT BREAK (5m)"
                        else -> "LONG BREAK (15m)"
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // =================================================================
            // 🟢 MODULE 4: CONTROL BUTTONS (स्मार्ट पॉज़ और रीसेट बटन)
            // (यदि बटन के रंग, नाम या डिज़ाइन में बदलाव करना हो)
            // =================================================================
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (isRunning) "Pause" else "Start",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        isRunning = false
                        currentSession = 1
                        isBreak = false
                        initialSeconds = focusSeconds
                        totalSeconds = focusSeconds
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
