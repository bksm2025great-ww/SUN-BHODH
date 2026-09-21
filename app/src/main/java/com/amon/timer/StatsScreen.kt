package com.amon.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)

    // 🎛️ Filter Tabs State ("Today", "Weekly", "Monthly")
    var selectedTab by remember { mutableStateOf("Weekly") }

    // 📅 Date Navigation Offset (0 = Current, -1 = Previous, +1 = Next)
    var dateOffset by remember { mutableIntStateOf(0) }

    // Reset date offset when switching tabs
    LaunchedEffect(selectedTab) {
        dateOffset = 0
    }

    // 📦 Saved Sessions Data
    val allSessions = remember(context) {
        FocusSessionManager.getAllSessions(context)
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
                    .clickable { if (dateOffset < 0) dateOffset++ }, // Can't go to future
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = if (dateOffset < 0) goldColor else textMuted.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            }
        }

        // ----------------- 4. BAR CHART CONTAINER (STEP 2 PLACEHOLDER) -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📊", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$selectedTab Chart Canvas",
                    color = goldColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step 2 me chart ke khambhe yahan banyenge",
                    color = textMuted,
                    fontSize = 10.sp
                )
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
                    value = "0.0 h", // Connected with data in Step 2
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
                    value = "${allSessions.size}",
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
                    value = streakText, // Shows "__" or "X Days 🔥"
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
                    value = "0.0 h", // Connected with data in Step 2
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

// 🔥 Helper: 30-minute daily requirement streak calculator
private fun calculateStreak(sessions: List<FocusSession>): String {
    if (sessions.isEmpty()) return "__"

    // Group study minutes by date (format: "yyyy-MM-dd" or similar date string)
    val dayMinutesMap = mutableMapOf<String, Int>()
    sessions.forEach { s ->
        dayMinutesMap[s.date] = (dayMinutesMap[s.date] ?: 0) + s.durationMinutes
    }

    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var streak = 0

    // Check today first
    val todayStr = sdf.format(cal.time)
    val todayMins = dayMinutesMap[todayStr] ?: 0
    if (todayMins >= 30) {
        streak++
    }

    // Check backwards day by day
    while (true) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dateStr = sdf.format(cal.time)
        val mins = dayMinutesMap[dateStr] ?: 0
        if (mins >= 30) {
            streak++
        } else {
            break // Streak broken!
        }
    }

    return if (streak > 0) "$streak Days 🔥" else "__"
}
