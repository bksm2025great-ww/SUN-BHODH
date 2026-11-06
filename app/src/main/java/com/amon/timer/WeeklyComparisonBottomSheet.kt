package com.amon.timer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyComparisonBottomSheet(
    allSessions: List<FocusSession>,
    dateOffset: Int,
    isDark: Boolean,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    cardBorder: Color,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // This Week और Last Week का दिन-वार डेटा (Minutes)
    val thisWeekDaysMins = remember(allSessions, dateOffset) {
        val bars = generateChartBars(filterSessionsByPeriod(allSessions, "Weekly", dateOffset), "Weekly", dateOffset)
        bars.map { it.minutes }
    }
    val lastWeekDaysMins = remember(allSessions, dateOffset) {
        val bars = generateChartBars(filterSessionsByPeriod(allSessions, "Weekly", dateOffset - 1), "Weekly", dateOffset - 1)
        bars.map { it.minutes }
    }

    // Y-Axis Dynamic Scale
    val maxMinutesInBoth = maxOf(
        (thisWeekDaysMins + lastWeekDaysMins).maxOrNull() ?: 60,
        60
    )
    val maxHoursCeil = ((maxMinutesInBoth + 59) / 60).coerceAtLeast(3)
    val avgThisWeekMins = (thisWeekDaysMins.sum().toFloat() / 7f).toInt()

    // ✨ 1. ENTRANCE LINE DRAW & FADE ANIMATION
    val drawAnimProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        drawAnimProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }

    // 🖐️ 2. SCRUB & DRAG INTERACTION STATE
    var selectedDayIndex by remember { mutableIntStateOf(3) } // Default Thursday
    var isScrubbing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF13131C) else Color(0xFFFFFFFF),
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Comparison",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textMain
                    )
                    Text(
                        text = "Drag along curve to inspect days",
                        fontSize = 11.5.sp,
                        color = if (isScrubbing) goldColor else textMuted,
                        fontWeight = if (isScrubbing) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Legend Indicators & Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(goldColor))
                        Text("This Week", fontSize = 11.sp, color = textMain, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                        Text("Last Week", fontSize = 11.sp, color = textMuted, fontWeight = FontWeight.SemiBold)
                    }
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
            }

            // 🌟 WAVE CHART BOX (Canvas with Line-Draw + Gradient Fill + Touch-Scrub)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF1B1B26) else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Y-Axis Labels
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = 26.dp, end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("${maxHoursCeil}h", fontSize = 10.sp, color = textMuted)
                        Text("${(maxHoursCeil * 2) / 3}h", fontSize = 10.sp, color = textMuted)
                        Text("${maxHoursCeil / 3}h", fontSize = 10.sp, color = textMuted)
                        Text("0h", fontSize = 10.sp, color = textMuted)
                    }

                    // Main Chart Interactive Area
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                // 👆 TOUCH & DRAG DETECTOR (उंगली से ड्रैग और टैप करने के लिए)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val stepX = size.width / 6f
                                        val tappedIndex = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, 6)
                                        selectedDayIndex = tappedIndex
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isScrubbing = true },
                                        onDragEnd = { isScrubbing = false },
                                        onDragCancel = { isScrubbing = false }
                                    ) { change, _ ->
                                        val stepX = size.width / 6f
                                        val dragIndex = ((change.position.x + stepX / 2f) / stepX).toInt().coerceIn(0, 6)
                                        selectedDayIndex = dragIndex
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val maxMins = maxHoursCeil * 60f
                                val progress = drawAnimProgress.value

                                // Background Subtle Grids
                                val gridY1 = h * 0.33f
                                val gridY2 = h * 0.66f
                                val gridColor = if (isDark) Color(0x15FFFFFF) else Color(0x15000000)
                                drawLine(gridColor, Offset(0f, gridY1), Offset(w, gridY1), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(0f, gridY2), Offset(w, gridY2), strokeWidth = 1.dp.toPx())

                                val stepX = w / 6f
                                fun getPoint(index: Int, mins: Int): Offset {
                                    val x = index * stepX
                                    val y = h - ((mins.toFloat() / maxMins) * h).coerceIn(6f, h - 6f)
                                    return Offset(x, y)
                                }

                                val thisPoints = thisWeekDaysMins.mapIndexed { idx, m -> getPoint(idx, m) }
                                val lastPoints = lastWeekDaysMins.mapIndexed { idx, m -> getPoint(idx, m) }

                                // 1. FULL PATH CREATION (Smooth Cubic Spline)
                                val fullLastPath = Path().apply {
                                    moveTo(lastPoints[0].x, lastPoints[0].y)
                                    for (i in 0 until lastPoints.size - 1) {
                                        val p0 = lastPoints[i]
                                        val p1 = lastPoints[i + 1]
                                        cubicTo((p0.x + p1.x) / 2f, p0.y, (p0.x + p1.x) / 2f, p1.y, p1.x, p1.y)
                                    }
                                }

                                val fullThisPath = Path().apply {
                                    moveTo(thisPoints[0].x, thisPoints[0].y)
                                    for (i in 0 until thisPoints.size - 1) {
                                        val p0 = thisPoints[i]
                                        val p1 = thisPoints[i + 1]
                                        cubicTo((p0.x + p1.x) / 2f, p0.y, (p0.x + p1.x) / 2f, p1.y, p1.x, p1.y)
                                    }
                                }

                                // 2. 🌈 GRADIENT AREA FILL (नीचे से ऊपर स्मूथ शेड)
                                if (progress > 0.05f) {
                                    val areaPath = Path().apply {
                                        addPath(fullThisPath)
                                        lineTo(w * progress, h)
                                        lineTo(0f, h)
                                        close()
                                    }
                                    drawPath(
                                        path = areaPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                goldColor.copy(alpha = 0.28f * progress),
                                                goldColor.copy(alpha = 0.08f * progress),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = h
                                        )
                                    )
                                }

                                // 3. ✍️ ANIMATED PATH DRAWING (पेंसिल की तरह लाइन ड्रॉ होना)
                                val lastMeasure = PathMeasure().apply { setPath(fullLastPath, false) }
                                val animatedLastPath = Path()
                                lastMeasure.getSegment(0f, lastMeasure.length * progress, animatedLastPath, true)
                                drawPath(animatedLastPath, color = Color(0xFF64748B), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                                val thisMeasure = PathMeasure().apply { setPath(fullThisPath, false) }
                                val animatedThisPath = Path()
                                thisMeasure.getSegment(0f, thisMeasure.length * progress, animatedThisPath, true)
                                drawPath(animatedThisPath, color = goldColor, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))

                                // 4. 📍 VERTICAL PROJECTION LINE & GLOWING SCRUB DOTS
                                if (progress >= 0.8f) {
                                    val selX = selectedDayIndex * stepX
                                    val currentThisPoint = thisPoints[selectedDayIndex]
                                    val currentLastPoint = lastPoints[selectedDayIndex]

                                    // Vertical Projection Line
                                    drawLine(
                                        color = goldColor.copy(alpha = if (isScrubbing) 0.85f else 0.5f),
                                        start = Offset(selX, 0f),
                                        end = Offset(selX, h),
                                        strokeWidth = (if (isScrubbing) 2.dp else 1.5.dp).toPx()
                                    )

                                    // Glow Ring on Touch
                                    if (isScrubbing) {
                                        drawCircle(goldColor.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = currentThisPoint)
                                    }

                                    // This Week Dot (Gold)
                                    drawCircle(Color.White, radius = 5.5.dp.toPx(), center = currentThisPoint)
                                    drawCircle(goldColor, radius = 3.8.dp.toPx(), center = currentThisPoint)

                                    // Last Week Dot (Grey)
                                    drawCircle(Color.White, radius = 4.dp.toPx(), center = currentLastPoint)
                                    drawCircle(Color(0xFF64748B), radius = 2.5.dp.toPx(), center = currentLastPoint)
                                }
                            }

                            // 💬 FLOATING TOOLTIP BOX (उंगली के साथ लाइव डेटा बदलेगा)
                            val thisMin = thisWeekDaysMins.getOrElse(selectedDayIndex) { 0 }
                            val lastMin = lastWeekDaysMins.getOrElse(selectedDayIndex) { 0 }
                            Box(
                                modifier = Modifier
                                    .align(if (selectedDayIndex > 3) Alignment.TopStart else Alignment.TopEnd)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xEE1E1E2D) else Color(0xEEFFFFFF))
                                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = days[selectedDayIndex],
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textMain
                                    )
                                    Text(
                                        text = "• This Week: ${formatMinutes(thisMin)}",
                                        fontSize = 10.5.sp,
                                        color = goldColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "• Last Week: ${formatMinutes(lastMin)}",
                                        fontSize = 10.5.sp,
                                        color = textMuted
                                    )
                                }
                            }
                        }

                        // X-Axis Days Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            days.forEachIndexed { index, day ->
                                val isSelected = selectedDayIndex == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) goldColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { selectedDayIndex = index }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isSelected) goldColor else textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary Average Note
            Text(
                text = "Daily Average: ${formatMinutes(avgThisWeekMins)} • Max Day: ${formatMinutes(thisWeekDaysMins.maxOrNull() ?: 0)}",
                fontSize = 11.5.sp,
                color = textMuted,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
