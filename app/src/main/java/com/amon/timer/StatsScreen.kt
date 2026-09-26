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

    // 🧮 Summary Cards Calculation
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

    // 🔥 Streak Calculation
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
        // 1. SCREEN HEADER
        Text(
            text = "STATS & ANALYTICS",
            color = textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // 2. TIME FILTER TABS (कैप्सूल का साइज़ वही, फ़ॉन्ट 15.sp और टाइट पैडिंग)
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
                        .padding(vertical = 5.dp), // पैडिंग कम की ताकि 15.sp पर भी बाहरी कैप्सूल न फैले
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else textMuted,
                        fontSize = 15.sp, // नया बड़ा फ़ॉन्ट साइज़
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
                fontSize = 13.sp,
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

        // 4. INTERACTIVE BAR CHART
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

                if (chartBars.isEmpty()) {
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

        // 5. 4 SUMMARY CARDS
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

data class BarItem(
    val title: String,
    val minutes: Int
)

// 🔍 अचूक तारीख फ़िल्टर (Weekly: सोमवार से रविवार तक का सॉलिड घेरा)
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
                        val dow = (get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
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
                cal.add(Calendar.DAY_OF_YEAR, 1) // 1 दिन आगे बढ़ाया
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
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

// 🛡️ Bulletproof Universal Date Parser (Google Sheet, ISO, व अन्य फ़ॉर्मेट सपोर्टर)
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
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
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

    // Regex Fallback (12 दोपहर सेट ताकि टाइमज़ोन से दिन न बदले)
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
            set(y.toInt(), m.toInt() - 1, d.toInt(), 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    return null
}

private fun calculateStreak(sessions: List<FocusSession>): String {
    if (sessions.isEmpty()) return "__"

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

    return if (streak > 0) "$streak Days 🔥" else "__"
}
