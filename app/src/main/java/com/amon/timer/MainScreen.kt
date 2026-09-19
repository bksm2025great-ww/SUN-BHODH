package com.amon.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    // 🟢 MODULE 1: SUBJECT & TREE SELECTION (विषय और पेड़ का चुनाव)
    // =========================================================================
    var selectedSubject by remember { mutableStateOf(PlantRegistry.defaultSubjects.last()) } // डिफ़ॉल्ट: Physics (Apple Tree)

    // =========================================================================
    // 🟢 MODULE 2: TIMER ENGINE & SESSION STATE (टाइमर का दिमाग)
    // =========================================================================
    val focusSeconds = 25 * 60       // 25 मिनट का फ़ोकस सत्र
    val shortBreakSeconds = 5 * 60   // 5 मिनट का शॉर्ट ब्रेक
    val longBreakSeconds = 15 * 60   // 15 मिनट का लॉन्ग ब्रेक

    var initialSeconds by remember { mutableIntStateOf(focusSeconds) }
    var totalSeconds by remember { mutableIntStateOf(focusSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var currentSession by remember { mutableIntStateOf(1) }
    var isBreak by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && totalSeconds > 0) {
            delay(1000L)
            totalSeconds--
        }
        if (totalSeconds == 0 && isRunning) {
            isRunning = false
            if (!isBreak) {
                isBreak = true
                if (currentSession < 3) {
                    initialSeconds = shortBreakSeconds
                    totalSeconds = shortBreakSeconds
                } else {
                    initialSeconds = longBreakSeconds
                    totalSeconds = longBreakSeconds
                }
            } else {
                isBreak = false
                if (currentSession < 3) {
                    currentSession++
                } else {
                    currentSession = 1
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

    // 4 स्टेज ग्रोथ लॉजिक (SubjectPlant.kt से जुड़ा हुआ)
    val currentGrowthStage = PlantRegistry.getGrowthStage(sessionProgress)

    // कलर्स
    val activeColor = when {
        !isBreak -> AccentYellow
        currentSession < 3 -> Color(0xFF34D399)
        else -> Color(0xFF60A5FA)
    }
    val trackColor = Color(0xFF222228)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // =================================================================
            // 🟢 MODULE 3: TOP SUBJECT SELECTOR (विषय बदलने वाली पट्टी)
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT SUBJECT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PlantRegistry.defaultSubjects) { subject ->
                        val isSelected = subject.id == selectedSubject.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) activeColor else Color(0xFF1E1E24))
                                .clickable {
                                    if (!isRunning) {
                                        selectedSubject = subject
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = subject.name,
                                color = if (isSelected) Color.Black else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 🟢 MODULE 4: 3-PART RING & TREE DISPLAY (रिंग और उगता हुआ पेड़)
            // =================================================================
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    val gap = 12f
                    val sweep = (360f - (gap * 3)) / 3f

                    val startAngles = listOf(
                        -90f + (gap / 2f),
                        -90f + (gap / 2f) + sweep + gap,
                        -90f + (gap / 2f) + (sweep + gap) * 2
                    )

                    // बैकग्राउंड ट्रैक्स
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

                    // प्रोग्रेस आर्क्स
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

                // सेंटर इन्फो: पेड़ का विज़ुअल और स्टेज
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val stageText = when (currentGrowthStage) {
                        TreeGrowthStage.SPROUT -> "🌱 Sprout"
                        TreeGrowthStage.SAPLING -> "🌿 Sapling"
                        TreeGrowthStage.DENSE -> "🌳 ${selectedSubject.tree.nameEn}"
                        TreeGrowthStage.MATURE -> "✨ ${selectedSubject.tree.nameHi} (${selectedSubject.tree.fruitName})"
                    }

                    Text(
                        text = stageText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = activeColor
                    )

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
                        color = Color.Gray
                    )
                }
            }

            // =================================================================
            // 🟢 MODULE 5: BUTTONS (स्टार्ट, पॉज़ और रीसेट)
            // =================================================================
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
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
