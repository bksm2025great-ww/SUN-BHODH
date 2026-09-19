package com.amon.timer

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun MainScreen() {

    // =========================================================================
    // 🟢 1. STATE & CONTROLS (डेटा और टाइमर का इंजन)
    // =========================================================================
    var selectedSubject by remember { mutableStateOf(PlantRegistry.defaultSubjects.first()) } // डिफ़ॉल्ट: 'All'
    var selectedTab by remember { mutableStateOf("Timer") } // "Timer" या "Stopwatch"
    var selectedPreset by remember { mutableIntStateOf(25) } // 25, 45, या कस्टम टाइम

    // डायल का टाइम (मिनट में) — डिफ़ॉल्ट 25 मिनट
    var dialMinutes by remember { mutableIntStateOf(25) }
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    // टाइमर टिकर इंजन
    LaunchedEffect(isRunning) {
        while (isRunning && totalSeconds > 0) {
            delay(1000L)
            totalSeconds--
        }
        if (totalSeconds == 0 && isRunning) {
            isRunning = false
        }
    }

    val displayMinutes = totalSeconds / 60
    val displaySeconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", displayMinutes, displaySeconds)

    val sessionTotalSeconds = (dialMinutes * 60).coerceAtLeast(1)
    val sessionProgress = ((sessionTotalSeconds - totalSeconds).toFloat() / sessionTotalSeconds.toFloat()).coerceIn(0f, 1f)
    val currentGrowthStage = PlantRegistry.getGrowthStage(sessionProgress)

    // कलर्स (गोल्डन नियॉन थीम)
    val goldColor = Color(0xFFF5A524)
    val glowYellow = Color(0xFFFDE68A)
    val glassBg = Color(0xCC111116)
    val glassBorder = Color(0x33FFFFFF)
    val textMuted = Color(0xFF9CA3AF)

    Scaffold(
        containerColor = Color(0xFF08080B)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // =================================================================
            // 🟢 TOP HEADER
            // =================================================================
            Text(
                text = "Select Time",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            // =================================================================
            // 🟢 HIERARCHY 1: SELECT SUBJECTS (हॉरिजॉन्टल स्क्रॉलिंग कैप्सूल बार)
            // =================================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "SELECT SUBJECTS",
                    color = textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(PlantRegistry.defaultSubjects) { subject ->
                        val isSelected = subject.id == selectedSubject.id
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) goldColor else glassBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) glowYellow else glassBorder,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    selectedSubject = subject
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = subject.name,
                                color = if (isSelected) Color.Black else textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 🟢 HIERARCHY 2: [ TIMER | STOPWATCH ] (सेंटर्ड फ्रॉस्टेड ग्लास कैप्सूल)
            // =================================================================
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(50))
                    .padding(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Timer Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedTab == "Timer") goldColor else Color.Transparent)
                            .clickable { selectedTab = "Timer" }
                    ) {
                        Text(
                            text = "Timer",
                            color = if (selectedTab == "Timer") Color.Black else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Stopwatch Tab
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedTab == "Stopwatch") goldColor else Color.Transparent)
                            .clickable { selectedTab = "Stopwatch" }
                    ) {
                        Text(
                            text = "Stopwatch",
                            color = if (selectedTab == "Stopwatch") Color.Black else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // =================================================================
            // 🟢 HIERARCHY 3: PRESETS (25 POMODORO | 45 FOCUS)
            // =================================================================
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preset 25m
                val is25 = selectedPreset == 25
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(115.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (is25) goldColor else glassBg)
                        .border(1.dp, if (is25) glowYellow else glassBorder, RoundedCornerShape(50))
                        .clickable {
                            if (!isRunning) {
                                selectedPreset = 25
                                dialMinutes = 25
                                totalSeconds = 25 * 60
                            }
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "25",
                            color = if (is25) Color.Black else goldColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Pomodoro",
                            color = if (is25) Color(0xFF1E1E1E) else glowYellow,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Preset 45m
                val is45 = selectedPreset == 45
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(115.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (is45) goldColor else glassBg)
                        .border(1.dp, if (is45) glowYellow else glassBorder, RoundedCornerShape(50))
                        .clickable {
                            if (!isRunning) {
                                selectedPreset = 45
                                dialMinutes = 45
                                totalSeconds = 45 * 60
                            }
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "45",
                            color = if (is45) Color.Black else goldColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Focus",
                            color = if (is45) Color(0xFF1E1E1E) else glowYellow,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =================================================================
            // 🟢 HIERARCHY 4: 100% TRUE CIRCULAR ROTARY DIAL & HERO CORE
            // =================================================================
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(270.dp)
                    .aspectRatio(1f)
                    .pointerInput(isRunning) {
                        if (!isRunning) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val touchX = change.position.x - (size.width / 2f)
                                val touchY = change.position.y - (size.height / 2f)
                                var angle = Math.toDegrees(atan2(touchY.toDouble(), touchX.toDouble())).toFloat()
                                var adjustedAngle = (angle + 90f)
                                if (adjustedAngle < 0f) adjustedAngle += 360f

                                val rawMins = (adjustedAngle / 360f) * 120f
                                val snappedMins = (Math.round(rawMins / 5f) * 5).coerceIn(5, 120)
                                dialMinutes = snappedMins
                                totalSeconds = snappedMins * 60
                                selectedPreset = snappedMins
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val arcSize = Size(radius * 2f, radius * 2f)
                    val topLeft = Offset(center.x - radius, center.y - radius)

                    // 1. बैकग्राउंड डार्क ट्रैक
                    drawCircle(
                        color = Color(0xFF14141A),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // 2. पूरे 120 मिनट के 5-मिनट माइक्रो-डॉट्स
                    val totalDots = 24
                    for (i in 0..totalDots) {
                        val dotMins = i * 5
                        val dotAngle = -90f + (dotMins / 120f) * 360f
                        val rad = Math.toRadians(dotAngle.toDouble())
                        val dotX = center.x + radius * cos(rad).toFloat()
                        val dotY = center.y + radius * sin(rad).toFloat()

                        val isFilled = dotMins <= dialMinutes
                        val isMajor = dotMins % 15 == 0
                        val dotRadius = if (isMajor) 3.5.dp.toPx() else 1.8.dp.toPx()

                        drawCircle(
                            color = if (isFilled) goldColor else Color(0xFF2B2B36),
                            radius = dotRadius,
                            center = Offset(dotX, dotY)
                        )
                    }

                    // 3. सक्रिय आर्च पर वॉर्म गोल्डन नियॉन ग्लो (Halo Effect)
                    val activeSweep = (dialMinutes / 120f) * 360f
                    if (activeSweep > 0f) {
                        drawIntoCanvas { canvas ->
                            val glowPaint = Paint().apply {
                                color = goldColor.copy(alpha = 0.4f).toArgb()
                                setStrokeWidth(strokeWidth + 14.dp.toPx())
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(16.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                            }
                            canvas.nativeCanvas.drawArc(
                                topLeft.x, topLeft.y,
                                topLeft.x + arcSize.width, topLeft.y + arcSize.height,
                                -90f, activeSweep, false, glowPaint
                            )
                        }

                        // सॉलिड कोर गोल्डन आर्च
                        drawArc(
                            color = goldColor,
                            startAngle = -90f,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // 4. ग्लोइंग रोटरी नॉब
                        val knobAngle = -90f + activeSweep
                        val knobRad = Math.toRadians(knobAngle.toDouble())
                        val knobX = center.x + radius * cos(knobRad).toFloat()
                        val knobY = center.y + radius * sin(knobRad).toFloat()

                        drawCircle(
                            color = goldColor.copy(alpha = 0.35f),
                            radius = 16.dp.toPx(),
                            center = Offset(knobX, knobY)
                        )
                        drawCircle(
                            color = goldColor,
                            radius = 9.dp.toPx(),
                            center = Offset(knobX, knobY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(knobX, knobY)
                        )
                    }
                }

                // 🟢 डायल के केंद्र का कोर (The Focus Display)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DEEP FOCUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = textMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = timeFormatted,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "STUDYING ${selectedSubject.name.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = goldColor,
                        letterSpacing = 1.sp
                    )

                    val treeName = selectedSubject.tree.nameEn
                    val stageLabel = when (currentGrowthStage) {
                        TreeGrowthStage.SPROUT -> "Stage 1 🌱"
                        TreeGrowthStage.SAPLING -> "Stage 2 🌿"
                        TreeGrowthStage.DENSE -> "Stage 3 🌳"
                        TreeGrowthStage.MATURE -> "Mature ✨"
                    }

                    Text(
                        text = "$treeName • $stageLabel",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }
            }

            // =================================================================
            // 🟢 HIERARCHY 5: ACTION BUTTONS (PLANT & CANCEL PILLS)
            // =================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Plant / Pause Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(goldColor)
                        .border(1.dp, glowYellow, RoundedCornerShape(50))
                        .clickable {
                            isRunning = !isRunning
                        }
                ) {
                    Text(
                        text = if (isRunning) "Pause" else "Plant",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Cancel / Reset Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(50))
                        .clickable {
                            isRunning = false
                            totalSeconds = dialMinutes * 60
                        }
                ) {
                    Text(
                        text = "Cancel",
                        color = goldColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
