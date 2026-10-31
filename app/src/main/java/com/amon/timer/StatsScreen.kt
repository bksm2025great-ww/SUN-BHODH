package com.amon.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 🎨 Theme Colors
    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)

    // Filter Tabs State
    var selectedTab by remember { mutableStateOf("Weekly") }
    var dateOffset by remember { mutableIntStateOf(0) }
    var showStreakSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        dateOffset = 0
    }

    // 📦 Saved Sessions
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
    LaunchedEffect(selectedTab, dateOffset) {
        allSessions = FocusSessionManager.getAllSessions(context)
    }

    // 🗓️ Date Display Text
    val dateRangeText = remember(selectedTab, dateOffset) {
        val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
        when (selectedTab) {
            "Today" -> {
                cal.add(Calendar.DAY_OF_YEAR, dateOffset)
                val format = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                if (dateOffset == 0) "Today (${format.format(cal.time)})" else format.format(cal.time)
            }
            "Weekly" -> {
                cal.add(Calendar.WEEK_OF_YEAR, dateOffset)
                val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                cal.add(Calendar.DAY_OF_YEAR, -dayOfWeek)
                val startFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                val startDate = startFormat.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val endFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                val endDate = endFormat.format(cal.time)
                "$startDate - $endDate"
            }
            else -> {
                cal.add(Calendar.MONTH, dateOffset)
                val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                format.format(cal.time)
            }
        }
    }

    // 🔍 Filter Sessions for selected Tab and Date Offset
    val filteredSessions = remember(allSessions, selectedTab, dateOffset) {
        filterSessionsByPeriod(allSessions, selectedTab, dateOffset)
    }

    // 📊 Chart Bars Data Generator
    val chartBars = remember(filteredSessions, selectedTab, dateOffset) {
        generateChartBars(filteredSessions, selectedTab, dateOffset)
    }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(selectedTab, dateOffset) {
        selectedBarIndex = null
    }

    // 🧮 Summary Calculations
    val totalMinutes = remember(filteredSessions) {
        filteredSessions.sumOf { it.durationMinutes }
    }
    val totalHoursStr = remember(totalMinutes) {
        formatMinutes(totalMinutes)
    }
    val dailyAvgStr = remember(totalMinutes, selectedTab, dateOffset) {
        val daysCount = when (selectedTab) {
            "Today" -> 1
            "Weekly" -> 7
            else -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, dateOffset)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }
        val avgMinutes = Math.round(totalMinutes.toFloat() / daysCount).toInt()
        formatMinutes(avgMinutes)
    }

    // 🔥 Live Streak Number
    val streakDaysCount = remember(allSessions) {
        calculateStreakCount(allSessions)
    }

    // 🎯 Weekly Goal Target (Default 21h = 1260 mins)
    val weeklyGoalTargetMinutes = 21 * 60
    val weeklyGoalProgress = remember(totalMinutes) {
        (totalMinutes.toFloat() / weeklyGoalTargetMinutes.toFloat()).coerceIn(0f, 1f)
    }

    // 🥧 Donut Chart Data
    val subjectMinutesList = remember(filteredSessions) {
        filteredSessions.groupBy { it.subject }
            .map { (subject, sessions) ->
                Pair(subject, sessions.sumOf { it.durationMinutes })
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
    }
    val totalSubjectMinutes = remember(subjectMinutesList) {
        subjectMinutesList.sumOf { it.second }
    }

    val subjectColors = listOf(
        Color(0xFFF59E0B),
        Color(0xFF38BDF8),
        Color(0xFFA855F7),
        Color(0xFF10B981),
        Color(0xFFF43F5E),
        Color(0xFF6366F1),
        Color(0xFFEC4899),
        Color(0xFF14B8A6),
        Color(0xFFEAB308),
        Color(0xFF8B5CF6)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. SCREEN HEADER
        Text(
            text = "STATS & ANALYTICS",
            color = textMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // 2. TIME FILTER TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(50))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Today", "Weekly", "Monthly").forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) goldColor else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else textMuted,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. DATE SWITCHER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .clickable { dateOffset-- },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "◀", color = goldColor, fontSize = 12.sp)
            }

            Text(
                text = dateRangeText,
                color = textMain,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .clickable { if (dateOffset < 0) dateOffset++ },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = if (dateOffset < 0) goldColor else textMuted.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }

        // 🎯 3.1 WEEKLY GOAL RING CARD (सिर्फ़ वीकली टैब में साफ़ रिंग)
        if (selectedTab == "Weekly") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WEEKLY GOAL",
                            color = textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Week goal 21h",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatMinutes(totalMinutes)} achieved (${(weeklyGoalProgress * 100).toInt()}%)",
                            color = if (weeklyGoalProgress >= 1f) Color(0xFF10B981) else goldColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier.size(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 5.dp.toPx()
                            val radius = (size.minDimension - stroke) / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            // Track
                            drawCircle(
                                color = if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
                                radius = radius,
                                center = center,
                                style = Stroke(width = stroke)
                            )
                            // Progress
                            drawArc(
                                color = goldColor,
                                startAngle = -90f,
                                sweepAngle = weeklyGoalProgress * 360f,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2f, radius * 2f),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${(weeklyGoalProgress * 100).toInt()}%",
                            color = textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. INTERACTIVE BAR CHART
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(246.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedTab) {
                            "Today" -> "STUDY BY SUBJECT"
                            "Weekly" -> "STUDY TIME (7 DAYS)"
                            else -> "MONTHLY ACTIVITY"
                        },
                        color = textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    if (selectedBarIndex != null && selectedBarIndex!! in chartBars.indices) {
                        val bar = chartBars[selectedBarIndex!!]
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(goldColor.copy(alpha = 0.15f))
                                .border(1.dp, goldColor, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${bar.title}: ${formatMinutes(bar.minutes)}",
                                color = goldColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Text(
                            text = "Tap bar to inspect ☝️",
                            color = textMuted.copy(alpha = 0.6f),
                            fontSize = 10.5.sp
                        )
                    }
                }

                if (chartBars.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study sessions in this period",
                            color = textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val maxMinutes = maxOf(chartBars.maxOf { it.minutes }, 60)
                    val maxBarHeight = 125.dp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (selectedTab == "Today") {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(maxBarHeight),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                itemsIndexed(chartBars) { index, bar ->
                                    val isSelected = selectedBarIndex == index
                                    val barHeight = if (bar.minutes == 0) 3.dp else {
                                        ((bar.minutes.toFloat() / maxMinutes) * maxBarHeight.value).dp.coerceIn(12.dp, maxBarHeight)
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(if (isSelected) goldColor else goldColor.copy(alpha = 0.75f))
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.dp,
                                                    color = if (isSelected) Color.White else Color.Transparent,
                                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                                )
                                                .clickable { selectedBarIndex = if (selectedBarIndex == index) null else index }
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(maxBarHeight)
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                chartBars.forEachIndexed { index, bar ->
                                    val isSelected = selectedBarIndex == index
                                    val isMonthly = selectedTab == "Monthly"
                                    val barWidth = if (isMonthly) 5.5.dp else 22.dp
                                    val barHeight = if (bar.minutes == 0) 3.dp else {
                                        ((bar.minutes.toFloat() / maxMinutes) * maxBarHeight.value).dp.coerceIn(8.dp, maxBarHeight)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                if (bar.minutes == 0) {
                                                    if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1)
                                                } else if (isSelected) {
                                                    goldColor
                                                } else {
                                                    goldColor.copy(alpha = 0.8f)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected && bar.minutes > 0) 1.2.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                            )
                                            .clickable { selectedBarIndex = if (selectedBarIndex == index) null else index }
                                    )
                                }
                            }
                        }

                        // Base ground line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                        )

                        // Date Labels
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .padding(top = 4.dp)
                        ) {
                            if (selectedTab == "Today") {
                                LazyRow(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    itemsIndexed(chartBars) { index, bar ->
                                        val isSelected = selectedBarIndex == index
                                        Box(
                                            modifier = Modifier.width(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bar.title,
                                                color = if (isSelected) goldColor else textMain,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    chartBars.forEachIndexed { index, bar ->
                                        val isSelected = selectedBarIndex == index
                                        val isMonthly = selectedTab == "Monthly"
                                        val barWidth = if (isMonthly) 5.5.dp else 22.dp

                                        Box(
                                            modifier = Modifier.width(barWidth),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!isMonthly || (index + 1) == 1 || (index + 1) % 5 == 0) {
                                                Text(
                                                    text = bar.title,
                                                    color = if (isSelected) goldColor else textMuted,
                                                    fontSize = if (isMonthly) 9.sp else 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. 4 SUMMARY CARDS (Clean, Flat, Minimalist Style)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Time",
                    value = totalHoursStr,
                    subtitle = "Focus Time",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = goldColor
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Sessions",
                    value = "${filteredSessions.size}",
                    subtitle = "Completed",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = goldColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStreakSheet = true },
                    title = "Current Streak",
                    value = if (streakDaysCount > 0) "$streakDaysCount Days" else "__",
                    subtitle = "Tap to view streak & calendar",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = goldColor,
                    flameContent = if (streakDaysCount > 0) {
                        {
                            StreakFlame(
                                streakDays = streakDaysCount,
                                isLarge = false,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else null
                )

                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Daily Avg",
                    value = dailyAvgStr,
                    subtitle = "Based on $selectedTab",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = goldColor
                )
            }
        }

        // 6. SUBJECT BREAKDOWN (Multi-Color Donut Chart)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBJECT BREAKDOWN",
                        color = textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "By $selectedTab",
                        color = goldColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (subjectMinutesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No subject data for this period",
                            color = textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(176.dp)
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 24.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            var currentStartAngle = -90f
                            subjectMinutesList.forEachIndexed { index, (_, mins) ->
                                val sweepAngle = (mins.toFloat() / totalSubjectMinutes) * 360f
                                val color = subjectColors[index % subjectColors.size]

                                drawArc(
                                    color = color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2f, radius * 2f),
                                    style = Stroke(width = strokeWidth)
                                )
                                currentStartAngle += sweepAngle
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formatMinutes(totalSubjectMinutes),
                                color = textMain,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TOTAL FOCUS",
                                color = textMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjectMinutesList.forEachIndexed { index, (subject, mins) ->
                            val color = subjectColors[index % subjectColors.size]
                            val percentage = Math.round((mins.toFloat() / totalSubjectMinutes) * 100)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = subject,
                                        color = textMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formatMinutes(mins),
                                        color = goldColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "($percentage%)",
                                        color = textMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // =========================================================================
    // 🔥 STREAK JOURNAL BOTTOM SHEET
    // =========================================================================
    if (showStreakSheet) {
        StreakJournalBottomSheet(
            streakDays = streakDaysCount,
            allSessions = allSessions,
            isDark = isDark,
            goldColor = goldColor,
            textMain = textMain,
            textMuted = textMuted,
            cardBorder = cardBorder,
            onDismiss = { showStreakSheet = false }
        )
    }
}

// =============================================================================
// 🔥 STREAK JOURNAL BOTTOM SHEET (Clean, Soft Accent Tint)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakJournalBottomSheet(
    streakDays: Int,
    allSessions: List<FocusSession>,
    isDark: Boolean,
    goldColor: Color,
    textMain: Color,
    textMuted: Color,
    cardBorder: Color,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var calendarMonthOffset by remember { mutableIntStateOf(0) }

    val flameTierName = remember(streakDays) {
        when {
            streakDays <= 0 -> "Resting Flame"
            streakDays in 1..6 -> "Classic Ember • 7-Day Target"
            streakDays in 7..20 -> "Volcanic Lava • 21-Day Target"
            streakDays in 21..49 -> "Solar Gold • 50-Day Target"
            streakDays in 50..99 -> "Electric Mystic • 100-Day Target"
            streakDays in 100..199 -> "Cyan Plasma • 200-Day Target"
            else -> "Cosmic Nebula • Master Tier"
        }
    }

    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val cal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        val dow = (get(Calendar.DAY_OF_WEEK) + 5) % 7
        add(Calendar.DAY_OF_YEAR, -dow)
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val weekStatus = remember(allSessions) {
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 0..6) {
            val key = sdf.format(cal.time)
            val mins = allSessions.filter { s ->
                parseDateSafelyUniversal(s.date)?.let { sdf.format(it) == key } ?: false
            }.sumOf { it.durationMinutes }
            list.add(Pair(key, mins))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Soft Tint of Accent Color
    val softAccentFill = goldColor.copy(alpha = 0.16f)

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
                .padding(bottom = 28.dp)
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
                    text = "STREAK JOURNAL",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
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

            // Top Flame Showcase Card (केवल यहाँ फ़्लेम आइकन दिखेगा)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(softAccentFill)
                    .border(1.2.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$streakDays",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = textMain
                            )
                            Text(
                                text = "days active",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = goldColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = flameTierName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted
                        )
                    }

                    // Single Large Flame
                    StreakFlame(
                        streakDays = streakDays,
                        isLarge = true,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            // THIS WEEK'S TRACK
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF1B1B26) else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "THIS WEEK'S TRACK (MIN 30M / DAY)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = textMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekStatus.forEachIndexed { index, pair ->
                        val (_, mins) = pair
                        val isGoalMet = mins >= 30
                        val dayTitle = weekDays[index]
                        val isPrevMet = index > 0 && weekStatus[index - 1].second >= 30
                        val isNextMet = index < 6 && weekStatus[index + 1].second >= 30

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(
                                            if (isGoalMet && isPrevMet) {
                                                goldColor.copy(alpha = 0.40f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dayTitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGoalMet) goldColor else textMuted
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isGoalMet) {
                                                softAccentFill
                                            } else {
                                                if (isDark) Color(0x22FFFFFF) else Color(0x11000000)
                                            }
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isGoalMet) goldColor else cardBorder,
                                            shape = CircleShape
                                        )
                                ) {
                                    if (isGoalMet) {
                                        Text(text = "✓", fontSize = 13.sp, fontWeight = FontWeight.Black, color = goldColor)
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(textMuted.copy(alpha = 0.4f))
                                        )
                                    }
                                }

                                Text(
                                    text = if (mins > 0) "${mins}m" else "0m",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isGoalMet) textMain else textMuted
                                )
                            }

                            if (index < 6) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(
                                            if (isGoalMet && isNextMet) {
                                                goldColor.copy(alpha = 0.40f)
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // MONTHLY CALENDAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF1B1B26) else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val activeCalendar = remember(calendarMonthOffset) {
                    Calendar.getInstance().apply {
                        add(Calendar.MONTH, calendarMonthOffset)
                    }
                }
                val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(activeCalendar.time)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                            .clickable(enabled = calendarMonthOffset > -11) {
                                calendarMonthOffset--
                            }
                    ) {
                        Text(
                            text = "◀",
                            color = if (calendarMonthOffset > -11) goldColor else textMuted.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = currentMonthName.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = textMain
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                            .clickable(enabled = calendarMonthOffset < 0) {
                                calendarMonthOffset++
                            }
                    ) {
                        Text(
                            text = "▶",
                            color = if (calendarMonthOffset < 0) goldColor else textMuted.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMuted,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val monthCal = remember(calendarMonthOffset) {
                    Calendar.getInstance().apply {
                        add(Calendar.MONTH, calendarMonthOffset)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                val startDayOfWeek = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val dayMinutesMap = remember(allSessions, calendarMonthOffset) {
                    val map = mutableMapOf<String, Int>()
                    allSessions.forEach { s ->
                        parseDateSafelyUniversal(s.date)?.let { d ->
                            val key = sdf.format(d)
                            map[key] = (map[key] ?: 0) + s.durationMinutes
                        }
                    }
                    map
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    var dayCounter = 1
                    for (week in 0..5) {
                        if (dayCounter > daysInMonth) break
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (dow in 0..6) {
                                if (week == 0 && dow < startDayOfWeek || dayCounter > daysInMonth) {
                                    Spacer(modifier = Modifier.width(36.dp))
                                } else {
                                    val currentDay = dayCounter
                                    val testCal = Calendar.getInstance().apply {
                                        add(Calendar.MONTH, calendarMonthOffset)
                                        set(Calendar.DAY_OF_MONTH, currentDay)
                                    }
                                    val dateKey = sdf.format(testCal.time)
                                    val mins = dayMinutesMap[dateKey] ?: 0
                                    val isGoalMet = mins >= 30

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isGoalMet) {
                                                    softAccentFill
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isGoalMet) 1.5.dp else 0.dp,
                                                color = if (isGoalMet) goldColor else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Text(
                                            text = "$currentDay",
                                            fontSize = 12.sp,
                                            fontWeight = if (isGoalMet) FontWeight.Black else FontWeight.Medium,
                                            color = if (isGoalMet) goldColor else textMain
                                        )
                                    }
                                    dayCounter++
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Rule Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x0A000000))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Rule: Complete minimum 30 minutes of focus per day to maintain your streak flame.",
                    fontSize = 11.sp,
                    color = textMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// SUMMARY CARD COMPOSABLE (Cleaner, No Extra Heavy Borders)
// =============================================================================
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    flameContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = textMuted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (flameContent != null) {
                    flameContent()
                }
                Text(
                    text = value,
                    color = if (value == "__") textMuted else textMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = textMuted.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

data class BarItem(
    val title: String,
    val minutes: Int
)

// 🔍 Period Filter
private fun filterSessionsByPeriod(
    sessions: List<FocusSession>,
    period: String,
    offset: Int
): List<FocusSession> {
    return sessions.filter { s ->
        val date = parseDateSafelyUniversal(s.date)
        if (date == null) false
        else {
            val sessionCal = Calendar.getInstance().apply { time = date }
            when (period) {
                "Today" -> {
                    val targetCal = Calendar.getInstance().apply {
                        time = Date()
                        add(Calendar.DAY_OF_YEAR, offset)
                    }
                    sessionCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                            sessionCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
                }
                "Weekly" -> {
                    val startCal = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.MONDAY
                        time = Date()
                        add(Calendar.WEEK_OF_YEAR, offset)
                        val dow = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                        add(Calendar.DAY_OF_YEAR, -dow)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = startCal.timeInMillis
                        add(Calendar.DAY_OF_YEAR, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    date.time in startCal.timeInMillis..endCal.timeInMillis
                }
                else -> {
                    val targetCal = Calendar.getInstance().apply {
                        time = Date()
                        add(Calendar.MONTH, offset)
                    }
                    sessionCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                            sessionCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH)
                }
            }
        }
    }
}

private fun generateChartBars(
    filteredSessions: List<FocusSession>,
    period: String,
    offset: Int
): List<BarItem> {
    return when (period) {
        "Today" -> {
            filteredSessions
                .groupBy { it.subject }
                .map { (subject, list) ->
                    BarItem(title = subject, minutes = list.sumOf { it.durationMinutes })
                }
        }
        "Weekly" -> {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                add(Calendar.WEEK_OF_YEAR, offset)
                val dow = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                add(Calendar.DAY_OF_YEAR, -dow)
            }
            val result = mutableListOf<BarItem>()
            for (i in 0..6) {
                val currentYear = cal.get(Calendar.YEAR)
                val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                val dayMins = filteredSessions.filter { s ->
                    val d = parseDateSafelyUniversal(s.date)
                    if (d != null) {
                        val scal = Calendar.getInstance().apply { time = d }
                        scal.get(Calendar.YEAR) == currentYear && scal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                    } else false
                }.sumOf { it.durationMinutes }

                result.add(BarItem(title = days[i], minutes = dayMins))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            result
        }
        else -> {
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, offset)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH)

            val result = mutableListOf<BarItem>()
            for (day in 1..maxDays) {
                val dayMins = filteredSessions.filter { s ->
                    val d = parseDateSafelyUniversal(s.date)
                    if (d != null) {
                        val scal = Calendar.getInstance().apply { time = d }
                        scal.get(Calendar.YEAR) == currentYear &&
                                scal.get(Calendar.MONTH) == currentMonth &&
                                scal.get(Calendar.DAY_OF_MONTH) == day
                    } else false
                }.sumOf { it.durationMinutes }

                result.add(BarItem(title = "$day", minutes = dayMins))
            }
            result
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0m"
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

private fun parseDateSafelyUniversal(dateStr: String): Date? {
    if (dateStr.isBlank()) return null

    val clean = dateStr
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("\"", "")
        .replace("'", "")
        .replace(",", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    clean.toLongOrNull()?.let { millis ->
        if (millis > 1000000000000L) return Date(millis)
    }

    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("MMMM dd yyyy", Locale.ENGLISH)
    )

    for (fmt in formats) {
        try {
            val d = fmt.parse(clean)
            if (d != null) return d
        } catch (_: Exception) {}
    }

    val ymdMatch = Regex("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})").find(clean)
    if (ymdMatch != null) {
        val (y, m, d) = ymdMatch.destructured
        return Calendar.getInstance().apply {
            set(y.toInt(), m.toInt() - 1, d.toInt(), 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    val dmyMatch = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})").find(clean)
    if (dmyMatch != null) {
        val (d, m, y) = dmyMatch.destructured
        return Calendar.getInstance().apply {
            set(d.toInt(), m.toInt() - 1, y.toInt(), 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    return null
}

private fun calculateStreakCount(sessions: List<FocusSession>): Int {
    if (sessions.isEmpty()) return 0

    val dayMinutesMap = mutableMapOf<String, Int>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    sessions.forEach { s ->
        val d = parseDateSafelyUniversal(s.date)
        if (d != null) {
            val key = sdf.format(d)
            dayMinutesMap[key] = (dayMinutesMap[key] ?: 0) + s.durationMinutes
        }
    }

    val cal = Calendar.getInstance()
    var streak = 0

    val todayKey = sdf.format(cal.time)
    val todayMins = dayMinutesMap[todayKey] ?: 0
    if (todayMins >= 30) {
        streak++
    }

    while (true) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dateKey = sdf.format(cal.time)
        val mins = dayMinutesMap[dateKey] ?: 0
        if (mins >= 30) {
            streak++
        } else {
            break
        }
    }

    return streak
}
