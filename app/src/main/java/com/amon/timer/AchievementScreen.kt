package com.amon.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AchievementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 🎨 Theme Colors
    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)

    // 🔄 Load diary sessions
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
    LaunchedEffect(Unit) {
        allSessions = FocusSessionManager.getAllSessions(context)
    }

    // 🏆 Dynamic Badge Calculations
    val totalFocusMinutes = remember(allSessions) { allSessions.sumOf { it.durationMinutes } }
    val totalFocusHours = totalFocusMinutes / 60
    val totalCompletedSessions = allSessions.size

    val badgesList = remember(allSessions, totalFocusHours, totalFocusMinutes) {
        val distinctSubjects = allSessions.map { it.subject.trim().lowercase() }.distinct().size
        val subjectsWith10Hours = allSessions.groupBy { it.subject.trim().lowercase() }
            .filter { it.value.sumOf { s -> s.durationMinutes } >= 600 }.size

        listOf(
            // 👑 CATEGORY 1: TOTAL FOCUS HOURS MILESTONES (10 Sovereign Badges)
            BadgeData("b_10h", "Bronze Scholar", "10 Total Focus Hours", "Accumulate 10 total hours of dedicated focus.", "🥉", totalFocusHours >= 10, "$totalFocusHours/10h"),
            BadgeData("b_50h", "Silver Master", "50 Total Focus Hours", "Accumulate 50 total hours of dedicated focus.", "🥈", totalFocusHours >= 50, "$totalFocusHours/50h"),
            BadgeData("b_100h", "Century King", "100 Total Focus Hours", "Complete 100 total hours of deep focus.", "👑", totalFocusHours >= 100, "$totalFocusHours/100h"),
            BadgeData("b_250h", "Elite Focus", "250 Total Focus Hours", "Reach 250 total hours of dedicated focus.", "🎖️", totalFocusHours >= 250, "$totalFocusHours/250h"),
            BadgeData("b_350h", "Vanguard Knight", "350 Total Focus Hours", "Reach 350 total hours of dedicated focus.", "⚔️", totalFocusHours >= 350, "$totalFocusHours/350h"),
            BadgeData("b_500h", "Platinum Master", "500 Total Focus Hours", "Achieve 500 total hours of deep work.", "💎", totalFocusHours >= 500, "$totalFocusHours/500h"),
            BadgeData("b_750h", "Ace of Amon", "750 Total Focus Hours", "Achieve 750 total hours of focus (Top 1% Club).", "♠️", totalFocusHours >= 750, "$totalFocusHours/750h"),
            BadgeData("b_800h", "Crown Sovereign", "800 Total Focus Hours", "Reach 800 total hours of focused discipline.", "⚜️", totalFocusHours >= 800, "$totalFocusHours/800h"),
            BadgeData("b_1000h", "Emperor of Amon", "1,000 Total Focus Hours", "Cross 1000 total hours of focus (Supreme Sovereign).", "🏛️", totalFocusHours >= 1000, "$totalFocusHours/1000h"),
            BadgeData("b_1950h", "Grandmaster of Eternity", "1,950 Total Focus Hours", "Complete 1950 total hours of focus (Legendary Focus Master).", "🌌", totalFocusHours >= 1950, "$totalFocusHours/1950h"),

            // 🔥 CATEGORY 2: CONSISTENCY & STREAKS (4 Badges)
            BadgeData("b_first_step", "First Step", "First Focus Session", "Successfully complete your first 25-minute focus session.", "🎯", allSessions.any { it.durationMinutes >= 25 }, if (allSessions.any { it.durationMinutes >= 25 }) "Unlocked" else "0/1"),
            BadgeData("b_trio", "Discipline Trio", "3-Day Focus Streak", "Maintain an active daily focus streak for 3 consecutive days.", "🔥", totalCompletedSessions >= 3, "${totalCompletedSessions.coerceAtMost(3)}/3 Sessions"),
            BadgeData("b_titan", "Weekly Titan", "7-Day Focus Streak", "Maintain an unbroken focus streak for 7 consecutive days.", "⚡", totalCompletedSessions >= 7, "${totalCompletedSessions.coerceAtMost(7)}/7 Sessions"),
            BadgeData("b_iron_habit", "Iron Habit", "30-Day Focus Streak", "Achieve an unbroken 30-day focus streak.", "🛡️", totalCompletedSessions >= 30, "${totalCompletedSessions.coerceAtMost(30)}/30 Sessions"),

            // ⏱️ CATEGORY 3: ENDURANCE & TIMING (4 Badges)
            BadgeData("b_hour", "Hour of Power", "60-Min Continuous Focus", "Complete a continuous 60-minute focus session in a single sitting.", "⏱️", allSessions.any { it.durationMinutes >= 60 }, if (allSessions.any { it.durationMinutes >= 60 }) "Unlocked" else "Pending"),
            BadgeData("b_early", "Early Bird Titan", "Dawn Focus Mastery", "Complete a full 60-minute focus session between 4:00 AM and 7:00 AM.", "🌅", allSessions.any { it.durationMinutes >= 60 }, if (allSessions.any { it.durationMinutes >= 60 }) "Unlocked" else "Pending"),
            BadgeData("b_night", "Midnight Marauder", "Night Focus Mastery", "Complete a focus session of 60 minutes or more after 10:00 PM.", "🌙", allSessions.any { it.durationMinutes >= 60 }, if (allSessions.any { it.durationMinutes >= 60 }) "Unlocked" else "Pending"),
            BadgeData("b_monk", "Monk Mode", "120-Min Deep Work", "Complete an uninterrupted 120-minute (2 hours) deep work session in a single sitting.", "🧘‍♂️", allSessions.any { it.durationMinutes >= 120 }, if (allSessions.any { it.durationMinutes >= 120 }) "Unlocked" else "Pending"),

            // 🎨 CATEGORY 4: SPECIAL MASTERY (3 Badges)
            BadgeData("b_iron_will", "Iron Will", "10 Flawless Sessions", "Complete 10 consecutive sessions without a single cancellation or Give Up.", "🧱", totalCompletedSessions >= 10, "${totalCompletedSessions.coerceAtMost(10)}/10"),
            BadgeData("b_polymath", "Polymath", "3 Subjects Exploration", "Complete focus sessions across 3 different subjects within a single day.", "📚", distinctSubjects >= 3, "$distinctSubjects/3 Subjects"),
            BadgeData("b_davinci", "The Da Vinci Mind", "Renaissance Genius", "Log 10+ focus hours each across 8 or more distinct subjects within a single month.", "🎨", subjectsWith10Hours >= 8, "$subjectsWith10Hours/8 Subjects (10h+)")
        )
    }

    val unlockedBadgesCount = badgesList.count { it.isUnlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ----------------- 1. TOP BAR -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
                    .border(1.2.dp, goldColor.copy(alpha = 0.75f), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                BackArrowIcon(tint = goldColor)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "ACHIEVEMENTS",
                    color = textMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Focus Milestones & Sovereign Badges",
                    color = textMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ----------------- 2. HALL OF FAME BANNER -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HALL OF FAME 🏆",
                        color = goldColor,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Complete deep focus milestones to unlock exclusive sovereign badges.",
                        color = textMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(goldColor.copy(alpha = 0.15f))
                        .border(1.2.dp, goldColor, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$unlockedBadgesCount / 21",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------- 3. 21 BADGES LIST -----------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badgesList.forEach { badge ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (badge.isUnlocked) cardBg else (if (isDark) Color(0x18FFFFFF) else Color(0x0A000000)))
                        .border(
                            width = if (badge.isUnlocked) 1.5.dp else 1.dp,
                            color = if (badge.isUnlocked) goldColor.copy(alpha = 0.8f) else cardBorder.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.isUnlocked) goldColor.copy(alpha = 0.15f) else Color(0x11FFFFFF))
                                    .border(
                                        width = 1.dp,
                                        color = if (badge.isUnlocked) goldColor else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            ) {
                                Text(
                                    text = if (badge.isUnlocked) badge.icon else "🔒",
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = badge.title,
                                    color = if (badge.isUnlocked) textMain else textMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = badge.requirement,
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (badge.isUnlocked) goldColor.copy(alpha = 0.18f) else Color(0x11FFFFFF))
                                .border(1.dp, if (badge.isUnlocked) goldColor else cardBorder, RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (badge.isUnlocked) "UNLOCKED 🏆" else badge.progressText,
                                color = if (badge.isUnlocked) goldColor else textMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ----------------- ✨ Golden Back Arrow -----------------
@Composable
private fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.4.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.5f),
            end = Offset(w * 0.85f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.5f),
            end = Offset(w * 0.52f, h * 0.18f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.5f),
            end = Offset(w * 0.52f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// ----------------- Data Model -----------------
private data class BadgeData(
    val id: String,
    val title: String,
    val milestone: String,
    val requirement: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progressText: String
)
