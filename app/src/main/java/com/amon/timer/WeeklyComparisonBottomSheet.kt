package com.amon.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    // Y-Axis Max Hours (Dynamic Screen-Time calculation)
    val maxMinutesInBoth = maxOf(
        (thisWeekDaysMins + lastWeekDaysMins).maxOrNull() ?: 60,
        60
    )
    val maxHoursCeil = ((maxMinutesInBoth + 59) / 60).coerceAtLeast(3) // कम से कम 3h का स्केल
    val avgThisWeekMins = (thisWeekDaysMins.sum().toFloat() / 7f).toInt()

    // इंटरैक्शन स्टेट: कौन सा दिन चुना गया है (डिफ़ॉल्ट रूप से गुरुवार - 3)
    var selectedDayIndex by remember { mutableIntStateOf(3) }

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
                        text = "This Week vs Last Week",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }

                // Legend Indicators & Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(goldColor))
                        Text("This Week", fontSize = 11.sp, color = textMain, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

            // 🌟 WAVE CHART BOX (Canvas with Dynamic Y-Axis)
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
                    // Left Side: Y-Axis Hours (Screen-Time Style)
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

                    // Main Chart Canvas & Touch Handler
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val maxMins = maxHoursCeil * 60f

                                // Grid Lines
                                val gridY1 = h * 0.33f
                                val gridY2 = h * 0.66f
                                drawLine(
                                    if (isDark) Color(0x15FFFFFF) else Color(0x15000000),
                                    Offset(0f, gridY1),
                                    Offset(w, gridY1),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    if (isDark) Color(0x15FFFFFF) else Color(0x15000000),
                                    Offset(0f, gridY2),
                                    Offset(w, gridY2),
                                    strokeWidth = 1.dp.toPx()
                                )

                                val stepX = w / 6f

                                fun getPoint(index: Int, mins: Int): Offset {
                                    val x = index * stepX
                                    val y = h - ((mins.toFloat() / maxMins) * h).coerceIn(4f, h - 4f)
                                    return Offset(x, y)
                                }

                                val thisPoints = thisWeekDaysMins.mapIndexed { idx, m -> getPoint(idx, m) }
                                val lastPoints = lastWeekDaysMins.mapIndexed { idx, m -> getPoint(idx, m) }

                                // 1. Draw Last Week Wave (Smooth Grey)
                                val lastPath = Path().apply {
                                    moveTo(lastPoints[0].x, lastPoints[0].y)
                                    for (i in 0 until lastPoints.size - 1) {
                                        val p0 = lastPoints[i]
                                        val p1 = lastPoints[i + 1]
                                        cubicTo(
                                            (p0.x + p1.x) / 2f, p0.y,
                                            (p0.x + p1.x) / 2f, p1.y,
                                            p1.x, p1.y
                                        )
                                    }
                                }
                                drawPath(
                                    path = lastPath,
                                    color = Color(0xFF64748B),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // 2. Draw This Week Wave (Smooth Gold Glowing)
                                val thisPath = Path().apply {
                                    moveTo(thisPoints[0].x, thisPoints[0].y)
                                    for (i in 0 until thisPoints.size - 1) {
                                        val p0 = thisPoints[i]
                                        val p1 = thisPoints[i + 1]
                                        cubicTo(
                                            (p0.x + p1.x) / 2f, p0.y,
                                            (p0.x + p1.x) / 2f, p1.y,
                                            p1.x, p1.y
                                        )
                                    }
                                }
                                drawPath(
                                    path = thisPath,
                                    color = goldColor,
                                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // 3. Vertical Projection Line on Selected Day
                                val selX = selectedDayIndex * stepX
                                drawLine(
                                    color = goldColor.copy(alpha = 0.6f),
                                    start = Offset(selX, 0f),
                                    end = Offset(selX, h),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                // Highlight Dots on the line
                                drawCircle(Color.White, radius = 5.dp.toPx(), center = thisPoints[selectedDayIndex])
                                drawCircle(goldColor, radius = 3.5.dp.toPx(), center = thisPoints[selectedDayIndex])

                                drawCircle(Color.White, radius = 4.dp.toPx(), center = lastPoints[selectedDayIndex])
                                drawCircle(Color(0xFF64748B), radius = 2.5.dp.toPx(), center = lastPoints[selectedDayIndex])
                            }

                            // 💬 FLOATING TOOLTIP BOX (Positioned Dynamically)
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

                        // X-Axis Days Row (Clickable)
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
