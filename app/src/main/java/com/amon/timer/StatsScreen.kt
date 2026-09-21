package com.amon.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 🎨 Theme Colors (Direct ThemeManager se)
    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)

    // 🎛️ Filter Tabs State ("Today", "Weekly", "Monthly")
    var selectedTab by remember { mutableStateOf("Weekly") }

    // 📅 Date Navigation Offset (0 = Current, -1 = Previous, +1 = Next)
    var dateOffset by remember { mutableIntStateOf(0) }

    // Reset date offset when switching tabs
    LaunchedEffect(selectedTab) {
        dateOffset = 0
    }

    // 📦 Saved Sessions Data
  var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
LaunchedEffect(Unit) {
    allSessions = FocusSessionManager.getAllSessions(context)
}

    // 🗓️ Date Display Text (e.g., "Today", "15 Sep - 21 Sep", "September 2026")
    val dateRangeText = remember(selectedTab, dateOffset) {
        val cal = Calendar.getInstance()
        when (selectedTab) {
            "Today" -> {
                cal.add(Calendar.DAY_OF_YEAR, dateOffset)
                val format = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                if (dateOffset == 0) "Today (${format.format(cal.time)})" else format.format(cal.time)
            }
            "Weekly" -> {
                cal.add(Calendar.WEEK_OF_YEAR, dateOffset)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val startFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                val startDate = startFormat.format(cal.time)
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val endFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                val endDate = endFormat.format(cal.time)
                "$startDate - $endDate"
            }
            else -> { // Monthly
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

    // 👆 Selected Bar Index for Floating Time Pop-up
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    // Reset selected bar when tab or date changes
    LaunchedEffect(selectedTab, dateOffset) {
        selectedBarIndex = null
    }

    // 🧮 Calculations for 4 Summary Cards
    val totalMinutes = remember(filteredSessions) {
        filteredSessions.sumOf { it.durationMinutes }
    }
    val totalHoursStr = remember(totalMinutes) {
        String.format(Locale.getDefault(), "%.1f h", totalMinutes / 60f)
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
        val avgHours = (totalMinutes.toFloat() / daysCount) / 60f
        String.format(Locale.getDefault(), "%.1f h", avgHours)
    }

    // 🔥 30-Minute Streak Calculation Logic
    val streakText = remember(allSessions) {
        calculateStreak(allSessions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ----------------- 1. SCREEN HEADER -----------------
        Text(
            text = "STATS & ANALYTICS",
            color = textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // ----------------- 2. TIME FILTER TABS -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(50))
                .padding(4.dp),
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
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else textMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }

        // ----------------- 3. DATE SWITCHER (PREV / NEXT) -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Arrow
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

            // Current Range Label
            Text(
                text = dateRangeText,
                color = textMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Next Arrow
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

        // ----------------- 4. INTERACTIVE BAR CHART -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Indicator Row: Floating Pop-up Badge or Tap Hint
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    // Floating Time Pop-up
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Text(
                            text = "Tap bar to inspect ☝️",
                            color = textMuted.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    }
                }

                // Chart Bars Area
                if (chartBars.isEmpty()) {
                    // Empty state (Today with no study)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🌱", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No study sessions on this day",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    val maxMinutes = maxOf(chartBars.maxOf { it.minutes }, 60)
                    val maxBarHeight = 130.dp

                    if (selectedTab == "Today") {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            itemsIndexed(chartBars) { index, bar ->
                                val isSelected = selectedBarIndex == index
                                val barHeight = if (bar.minutes == 0) {
                                    3.dp
                                } else {
                                    ((bar.minutes.toFloat() / maxMinutes) * maxBarHeight.value).dp.coerceIn(12.dp, maxBarHeight)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .clickable {
                                            selectedBarIndex = if (selectedBarIndex == index) null else index
                                        }
                                        .padding(bottom = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(if (isSelected) goldColor else (if (isDark) goldColor.copy(alpha = 0.75f) else goldColor))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = bar.title,
                                        color = if (isSelected) goldColor else textMain,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        // Weekly or Monthly
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            chartBars.forEachIndexed { index, bar ->
                                val isSelected = selectedBarIndex == index
                                val isMonthly = selectedTab == "Monthly"
                                val barWidth = if (isMonthly) 5.5.dp else 22.dp

                                val barHeight = if (bar.minutes == 0) {
                                    3.dp
                                } else {
                                    ((bar.minutes.toFloat() / maxMinutes) * maxBarHeight.value).dp.coerceIn(8.dp, maxBarHeight)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .clickable {
                                            selectedBarIndex = if (selectedBarIndex == index) null else index
                                        }
                                        .padding(bottom = 2.dp)
                                ) {
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
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (!isMonthly || (index + 1) == 1 || (index + 1) % 5 == 0) {
                                        Text(
                                            text = bar.title,
                                            color = if (isSelected) goldColor else textMuted,
                                            fontSize = if (isMonthly) 7.5.sp else 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ----------------- 5. 4 SUMMARY CARDS (2x2 GRID) -----------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Total Hours & Completed Sessions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Hours",
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
                    title = "Sessions",
                    value = "${filteredSessions.size}",
                    subtitle = "Trees Planted 🌳",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = goldColor
                )
            }

            // Row 2: Current Streak & Daily Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Current Streak",
                    value = streakText,
                    subtitle = "Min 30m / day",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = goldColor
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 🎴 Reusable Helper Component for 4 Summary Cards
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
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = if (value == "__") textMuted else accentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = textMuted.copy(alpha = 0.8f),
                fontSize = 9.sp
            )
        }
    }
}

// 📊 Helper Data Class for Chart Bars
data class BarItem(
    val title: String,
    val minutes: Int
)

// 🔍 Helper: Filter sessions by period safely
private fun filterSessionsByPeriod(
    sessions: List<FocusSession>,
    period: String,
    offset: Int
): List<FocusSession> {
    val targetCal = Calendar.getInstance()
    return sessions.filter { s ->
        val date = parseDateSafely(s.date)
        if (date == null) false
        else {
            val sessionCal = Calendar.getInstance().apply { time = date }
            when (period) {
                "Today" -> {
                    targetCal.time = Date()
                    targetCal.add(Calendar.DAY_OF_YEAR, offset)
                    sessionCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    sessionCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
                }
                "Weekly" -> {
                    targetCal.time = Date()
                    targetCal.add(Calendar.WEEK_OF_YEAR, offset)
                    targetCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    targetCal.set(Calendar.HOUR_OF_DAY, 0)
                    targetCal.set(Calendar.MINUTE, 0)
                    targetCal.set(Calendar.SECOND, 0)
                    val startMillis = targetCal.timeInMillis

                    targetCal.add(Calendar.DAY_OF_WEEK, 6)
                    targetCal.set(Calendar.HOUR_OF_DAY, 23)
                    targetCal.set(Calendar.MINUTE, 59)
                    targetCal.set(Calendar.SECOND, 59)
                    val endMillis = targetCal.timeInMillis

                    date.time in startMillis..endMillis
                }
                else -> { // Monthly
                    targetCal.time = Date()
                    targetCal.add(Calendar.MONTH, offset)
                    sessionCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    sessionCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH)
                }
            }
        }
    }
}

// 📊 Helper: Generate Bars based on Selected Tab
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
                add(Calendar.WEEK_OF_YEAR, offset)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val result = mutableListOf<BarItem>()
            for (i in 0..6) {
                val currentYear = cal.get(Calendar.YEAR)
                val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                val dayMins = filteredSessions.filter { s ->
                    val d = parseDateSafely(s.date)
                    if (d != null) {
                        val scal = Calendar.getInstance().apply { time = d }
                        scal.get(Calendar.YEAR) == currentYear && scal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                    } else false
                }.sumOf { it.durationMinutes }

                result.add(BarItem(title = days[i], minutes = dayMins))
                cal.add(Calendar.DAY_OF_WEEK, 1)
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
                    val d = parseDateSafely(s.date)
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

// ⏱️ Helper: Format minutes to string e.g. "1h 45m" or "25m"
private fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

// 🛡️ Helper: Parse date safely across multiple formats
private fun parseDateSafely(dateStr: String): Date? {
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    )
    for (fmt in formats) {
        try {
            val d = fmt.parse(dateStr)
            if (d != null) return d
        } catch (_: Exception) {}
    }
    return null
}

// 🔥 Helper: 30-minute daily requirement streak calculator
private fun calculateStreak(sessions: List<FocusSession>): String {
    if (sessions.isEmpty()) return "__"

    val dayMinutesMap = mutableMapOf<String, Int>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    sessions.forEach { s ->
        val d = parseDateSafely(s.date)
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

    return if (streak > 0) "$streak Days 🔥" else "__"
}
