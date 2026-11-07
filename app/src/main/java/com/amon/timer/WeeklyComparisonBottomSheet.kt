package com.amon.timer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyComparisonBottomSheet(
    selectedPeriod: String, // "Today", "Weekly", "Monthly"
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
                        text = when (selectedPeriod) {
                            "Today" -> "Daily Comparison"
                            "Monthly" -> "Monthly Comparison"
                            else -> "Weekly Comparison"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textMain
                    )
                    Text(
                        text = when (selectedPeriod) {
                            "Today" -> "Today vs Yesterday"
                            "Monthly" -> "This Month vs Last Month"
                            else -> "Drag along curve to inspect days"
                        },
                        fontSize = 11.5.sp,
                        color = textMuted
                    )
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

            if (selectedPeriod == "Today") {
                // 📊 2-PILLAR BAR CHART (Yesterday vs Today)
                TwoPillarDailyComparison(
                    allSessions = allSessions,
                    dateOffset = dateOffset,
                    isDark = isDark,
                    goldColor = goldColor,
                    textMain = textMain,
                    textMuted = textMuted,
                    cardBorder = cardBorder
                )
            } else {
                // 📈 WAVE COMPARISON CHART (Weekly or Monthly)
                WaveComparisonChart(
                    selectedPeriod = selectedPeriod,
                    allSessions = allSessions,
                    dateOffset = dateOffset,
                    isDark = isDark,
                    goldColor = goldColor,
                    textMain = textMain,
                    textMuted = textMuted,
                    cardBorder = cardBorder
                )
            }
        }
    }
}

// =============================================================================
// 📊 2-PILLAR COMPARISON (Yesterday vs Today)
// =============================================================================
@Composable
private fun TwoPillarDailyComparison(
    allSessions: List<FocusSession>,
    dateOffset: Int,
    isDark: Boolean,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    cardBorder: Color
) {
    val todayMins = remember(allSessions, dateOffset) {
        filterSessionsByPeriod(allSessions, "Today", dateOffset).sumOf { it.durationMinutes }
    }
    val yestMins = remember(allSessions, dateOffset) {
        filterSessionsByPeriod(allSessions, "Today", dateOffset - 1).sumOf { it.durationMinutes }
    }

    val maxMins = maxOf(todayMins, yestMins, 60)
    val maxPillarHeight = 160.dp

    val yestHeight = if (yestMins == 0) 6.dp else ((yestMins.toFloat() / maxMins) * maxPillarHeight.value).dp.coerceIn(12.dp, maxPillarHeight)
    val todayHeight = if (todayMins == 0) 6.dp else ((todayMins.toFloat() / maxMins) * maxPillarHeight.value).dp.coerceIn(12.dp, maxPillarHeight)

    val yestAnim = remember { Animatable(0f) }
    val todayAnim = remember { Animatable(0f) }

    LaunchedEffect(todayMins, yestMins) {
        yestAnim.snapTo(0f)
        todayAnim.snapTo(0f)
        yestAnim.animateTo(yestHeight.value, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
        todayAnim.animateTo(todayHeight.value, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDark) Color(0xFF1B1B26) else Color(0xFFF8FAFC))
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Numbers Comparison Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Yesterday", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatMinutes(yestMins), color = Color(0xFF94A3B8), fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today", color = goldColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatMinutes(todayMins), color = textMain, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            // Pillars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxPillarHeight)
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Yesterday Pillar
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(yestAnim.value.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Color(0xFF64748B))
                    )
                }

                // Today Pillar
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(todayAnim.value.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(goldColor)
                    )
                }
            }

            // Difference Banner
            val diff = todayMins - yestMins
            val diffText = if (diff >= 0) "+${formatMinutes(diff)} more than yesterday" else "-${formatMinutes(Math.abs(diff))} less than yesterday"
            Text(
                text = diffText,
                color = if (diff >= 0) Color(0xFF10B981) else Color(0xFFF43F5E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =============================================================================
// 📈 WAVE COMPARISON (Weekly & Monthly)
// =============================================================================
@Composable
private fun WaveComparisonChart(
    selectedPeriod: String,
    allSessions: List<FocusSession>,
    dateOffset: Int,
    isDark: Boolean,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    cardBorder: Color
) {
    val isMonthly = selectedPeriod == "Monthly"
    val daysLabels = if (isMonthly) listOf("W1", "W2", "W3", "W4") else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val thisPeriodMins = remember(allSessions, dateOffset, selectedPeriod) {
        if (isMonthly) {
            val sessions = filterSessionsByPeriod(allSessions, "Monthly", dateOffset)
            calculateWeeksInMonth(sessions)
        } else {
            generateChartBars(filterSessionsByPeriod(allSessions, "Weekly", dateOffset), "Weekly", dateOffset).map { it.minutes }
        }
    }

    val lastPeriodMins = remember(allSessions, dateOffset, selectedPeriod) {
        if (isMonthly) {
            val sessions = filterSessionsByPeriod(allSessions, "Monthly", dateOffset - 1)
            calculateWeeksInMonth(sessions)
        } else {
            generateChartBars(filterSessionsByPeriod(allSessions, "Weekly", dateOffset - 1), "Weekly", dateOffset - 1).map { it.minutes }
        }
    }

    val maxMinutesInBoth = maxOf((thisPeriodMins + lastPeriodMins).maxOrNull() ?: 60, 60)
    val maxHoursCeil = ((maxMinutesInBoth + 59) / 60).coerceAtLeast(3)
    val avgThisPeriodMins = if (thisPeriodMins.isNotEmpty()) (thisPeriodMins.sum().toFloat() / thisPeriodMins.size).toInt() else 0

    val drawAnimProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedPeriod, dateOffset) {
        drawAnimProgress.snapTo(0f)
        drawAnimProgress.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }

    var selectedIndex by remember { mutableIntStateOf(if (isMonthly) 1 else 3) }
    var isScrubbing by remember { mutableStateOf(false) }

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
            Column(
                modifier = Modifier.fillMaxHeight().padding(bottom = 26.dp, end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("${maxHoursCeil}h", fontSize = 10.sp, color = textMuted)
                Text("${(maxHoursCeil * 2) / 3}h", fontSize = 10.sp, color = textMuted)
                Text("${maxHoursCeil / 3}h", fontSize = 10.sp, color = textMuted)
                Text("0h", fontSize = 10.sp, color = textMuted)
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (daysLabels.size - 1).toFloat()
                                val tapped = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, daysLabels.size - 1)
                                selectedIndex = tapped
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isScrubbing = true },
                                onDragEnd = { isScrubbing = false },
                                onDragCancel = { isScrubbing = false }
                            ) { change, _ ->
                                val stepX = size.width / (daysLabels.size - 1).toFloat()
                                val drag = ((change.position.x + stepX / 2f) / stepX).toInt().coerceIn(0, daysLabels.size - 1)
                                selectedIndex = drag
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val maxMins = maxHoursCeil * 60f
                        val progress = drawAnimProgress.value

                        val gridY1 = h * 0.33f
                        val gridY2 = h * 0.66f
                        val gridColor = if (isDark) Color(0x15FFFFFF) else Color(0x15000000)
                        drawLine(gridColor, Offset(0f, gridY1), Offset(w, gridY1), strokeWidth = 1.dp.toPx())
                        drawLine(gridColor, Offset(0f, gridY2), Offset(w, gridY2), strokeWidth = 1.dp.toPx())

                        val stepX = w / (daysLabels.size - 1).toFloat()
                        fun getPoint(idx: Int, mins: Int): Offset {
                            val x = idx * stepX
                            val y = h - ((mins.toFloat() / maxMins) * h).coerceIn(6f, h - 6f)
                            return Offset(x, y)
                        }

                        val thisPoints = thisPeriodMins.mapIndexed { idx, m -> getPoint(idx, m) }
                        val lastPoints = lastPeriodMins.mapIndexed { idx, m -> getPoint(idx, m) }

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

                        val lastMeasure = PathMeasure().apply { setPath(fullLastPath, false) }
                        val animatedLast = Path()
                        lastMeasure.getSegment(0f, lastMeasure.length * progress, animatedLast, true)
                        drawPath(animatedLast, Color(0xFF64748B), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                        val thisMeasure = PathMeasure().apply { setPath(fullThisPath, false) }
                        val animatedThis = Path()
                        thisMeasure.getSegment(0f, thisMeasure.length * progress, animatedThis, true)
                        drawPath(animatedThis, goldColor, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))

                        if (progress >= 0.8f && selectedIndex in thisPoints.indices) {
                            val selX = selectedIndex * stepX
                            val ptThis = thisPoints[selectedIndex]
                            val ptLast = lastPoints[selectedIndex]

                            drawLine(
                                color = goldColor.copy(alpha = if (isScrubbing) 0.85f else 0.5f),
                                start = Offset(selX, 0f),
                                end = Offset(selX, h),
                                strokeWidth = (if (isScrubbing) 2.dp else 1.5.dp).toPx()
                            )

                            if (isScrubbing) drawCircle(goldColor.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = ptThis)

                            drawCircle(Color.White, radius = 5.5.dp.toPx(), center = ptThis)
                            drawCircle(goldColor, radius = 3.8.dp.toPx(), center = ptThis)

                            drawCircle(Color.White, radius = 4.dp.toPx(), center = ptLast)
                            drawCircle(Color(0xFF64748B), radius = 2.5.dp.toPx(), center = ptLast)
                        }
                    }

                    val thisMin = thisPeriodMins.getOrElse(selectedIndex) { 0 }
                    val lastMin = lastPeriodMins.getOrElse(selectedIndex) { 0 }
                    Box(
                        modifier = Modifier
                            .align(if (selectedIndex > daysLabels.size / 2) Alignment.TopStart else Alignment.TopEnd)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xEE1E1E2D) else Color(0xEEFFFFFF))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(daysLabels[selectedIndex], fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textMain)
                            Text("• Current: ${formatMinutes(thisMin)}", fontSize = 10.5.sp, color = goldColor, fontWeight = FontWeight.SemiBold)
                            Text("• Previous: ${formatMinutes(lastMin)}", fontSize = 10.5.sp, color = textMuted)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(26.dp).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    daysLabels.forEachIndexed { index, label ->
                        val isSelected = selectedIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) goldColor.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isSelected) goldColor else textMuted
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Average: ${formatMinutes(avgThisPeriodMins)} • Max: ${formatMinutes(thisPeriodMins.maxOrNull() ?: 0)}",
            fontSize = 11.5.sp,
            color = textMuted,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        )
    }
}

private fun calculateWeeksInMonth(sessions: List<FocusSession>): List<Int> {
    val weeks = MutableList(4) { 0 }
    sessions.forEach { s ->
        parseDateSafelyUniversal(s.date)?.let { d ->
            val cal = Calendar.getInstance().apply { time = d }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val weekIdx = ((day - 1) / 7).coerceIn(0, 3)
            weeks[weekIdx] += s.durationMinutes
        }
    }
    return weeks
}
