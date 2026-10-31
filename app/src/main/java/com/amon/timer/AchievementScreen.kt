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

    val maxSingleSessionMinutes = remember(allSessions) {
        allSessions.maxOfOrNull { it.durationMinutes } ?: 0
    }

    val badgesList = remember(allSessions, totalFocusHours, totalFocusMinutes, maxSingleSessionMinutes) {
        val distinctSubjects = allSessions.map { it.subject.trim().lowercase() }.distinct().size
        val subjectsWith10Hours = allSessions.groupBy { it.subject.trim().lowercase() }
            .filter { it.value.sumOf { s -> s.durationMinutes } >= 600 }.size

        listOf(
            // 👑 CATEGORY 1: TOTAL FOCUS HOURS MILESTONES (10 Sovereign Badges)
            BadgeData("b_10h", "Bronze Scholar", "10 Total Focus Hours", "Accumulate 10 total hours of dedicated focus.", "🥉", totalFocusHours >= 10, "$totalFocusHours / 10h", (totalFocusHours.toFloat() / 10f).coerceIn(0f, 1f)),
            BadgeData("b_50h", "Silver Master", "50 Total Focus Hours", "Accumulate 50 total hours of dedicated focus.", "🥈", totalFocusHours >= 50, "$totalFocusHours / 50h", (totalFocusHours.toFloat() / 50f).coerceIn(0f, 1f)),
            BadgeData("b_100h", "Century King", "100 Total Focus Hours", "Complete 100 total hours of deep focus.", "👑", totalFocusHours >= 100, "$totalFocusHours / 100h", (totalFocusHours.toFloat() / 100f).coerceIn(0f, 1f)),
            BadgeData("b_250h", "Elite Focus", "250 Total Focus Hours", "Reach 250 total hours of dedicated focus.", "🎖️", totalFocusHours >= 250, "$totalFocusHours / 250h", (totalFocusHours.toFloat() / 250f).coerceIn(0f, 1f)),
            BadgeData("b_350h", "Vanguard Knight", "350 Total Focus Hours", "Reach 350 total hours of dedicated focus.", "⚔️", totalFocusHours >= 350, "$totalFocusHours / 350h", (totalFocusHours.toFloat() / 350f).coerceIn(0f, 1f)),
            BadgeData("b_500h", "Platinum Master", "500 Total Focus Hours", "Achieve 500 total hours of deep work.", "💎", totalFocusHours >= 500, "$totalFocusHours / 500h", (totalFocusHours.toFloat() / 500f).coerceIn(0f, 1f)),
            BadgeData("b_750h", "Ace of Amon", "750 Total Focus Hours", "Achieve 750 total hours of focus (Top 1% Club).", "♠️", totalFocusHours >= 750, "$totalFocusHours / 750h", (totalFocusHours.toFloat() / 750f).coerceIn(0f, 1f)),
            BadgeData("b_800h", "Crown Sovereign", "800 Total Focus Hours", "Reach 800 total hours of focused discipline.", "⚜️", totalFocusHours >= 800, "$totalFocusHours / 800h", (totalFocusHours.toFloat() / 800f).coerceIn(0f, 1f)),
            BadgeData("b_1000h", "Emperor of Amon", "1,000 Total Focus Hours", "Cross 1000 total hours of focus (Supreme Sovereign).", "🏛️", totalFocusHours >= 1000, "$totalFocusHours / 1000h", (totalFocusHours.toFloat() / 1000f).coerceIn(0f, 1f)),
            BadgeData("b_1950h", "Grandmaster of Eternity", "1,950 Total Focus Hours", "Complete 1950 total hours of focus (Legendary Focus Master).", "🌌", totalFocusHours >= 1950, "$totalFocusHours / 1950h", (totalFocusHours.toFloat() / 1950f).coerceIn(0f, 1f)),

            // 🔥 CATEGORY 2: CONSISTENCY & STREAKS (4 Badges)
            BadgeData("b_first_step", "First Step", "First Focus Session", "Successfully complete your first 25-minute focus session.", "🎯", allSessions.any { it.durationMinutes >= 25 }, "${maxSingleSessionMinutes.coerceAtMost(25)} / 25 min", (maxSingleSessionMinutes.toFloat() / 25f).coerceIn(0f, 1f)),
            BadgeData("b_trio", "Discipline Trio", "3-Day Focus Streak", "Maintain an active daily focus streak for 3 consecutive days.", "🔥", totalCompletedSessions >= 3, "${totalCompletedSessions.coerceAtMost(3)} / 3 Sessions", (totalCompletedSessions.toFloat() / 3f).coerceIn(0f, 1f)),
            BadgeData("b_titan", "Weekly Titan", "7-Day Focus Streak", "Maintain an unbroken focus streak for 7 consecutive days.", "⚡", totalCompletedSessions >= 7, "${totalCompletedSessions.coerceAtMost(7)} / 7 Sessions", (totalCompletedSessions.toFloat() / 7f).coerceIn(0f, 1f)),
            BadgeData("b_iron_habit", "Iron Habit", "30-Day Focus Streak", "Achieve an unbroken 30-day focus streak.", "🛡️", totalCompletedSessions >= 30, "${totalCompletedSessions.coerceAtMost(30)} / 30 Sessions", (totalCompletedSessions.toFloat() / 30f).coerceIn(0f, 1f)),

            // ⏱️ CATEGORY 3: ENDURANCE & TIMING (4 Badges)
            BadgeData("b_hour", "Hour of Power", "60-Min Continuous Focus", "Complete a continuous 60-minute focus session in a single sitting.", "⏱️", allSessions.any { it.durationMinutes >= 60 }, "${maxSingleSessionMinutes.coerceAtMost(60)} / 60 min", (maxSingleSessionMinutes.toFloat() / 60f).coerceIn(0f, 1f)),
            BadgeData("b_early", "Early Bird Titan", "Dawn Focus Mastery", "Complete a full 60-minute focus session between 4:00 AM and 7:00 AM.", "🌅", allSessions.any { it.durationMinutes >= 60 }, if (allSessions.any { it.durationMinutes >= 60 }) "60 / 60 min" else "0 / 60 min", if (allSessions.any { it.durationMinutes >= 60 }) 1f else 0f),
            BadgeData("b_night", "Midnight Marauder", "Night Focus Mastery", "Complete a focus session of 60 minutes or more after 10:00 PM.", "🌙", allSessions.any { it.durationMinutes >= 60 }, if (allSessions.any { it.durationMinutes >= 60 }) "60 / 60 min" else "0 / 60 min", if (allSessions.any { it.durationMinutes >= 60 }) 1f else 0f),
            BadgeData("b_monk", "Monk Mode", "120-Min Deep Work", "Complete an uninterrupted 120-minute (2 hours) deep work session in a single sitting.", "🧘‍♂️", allSessions.any { it.durationMinutes >= 120 }, "${maxSingleSessionMinutes.coerceAtMost(120)} / 120 min", (maxSingleSessionMinutes.toFloat() / 120f).coerceIn(0f, 1f)),

            // 🎨 CATEGORY 4: SPECIAL MASTERY (3 Badges)
            BadgeData("b_iron_will", "Iron Will", "10 Flawless Sessions", "Complete 10 consecutive sessions without a single cancellation or Give Up.", "🧱", totalCompletedSessions >= 10, "${totalCompletedSessions.coerceAtMost(10)} / 10 Sessions", (totalCompletedSessions.toFloat() / 10f).coerceIn(0f, 1f)),
            BadgeData("b_polymath", "Polymath", "3 Subjects Exploration", "Complete focus sessions across 3 different subjects within a single day.", "📚", distinctSubjects >= 3, "$distinctSubjects / 3 Subjects", (distinctSubjects.toFloat() / 3f).coerceIn(0f, 1f)),
            BadgeData("b_davinci", "The Da Vinci Mind", "Renaissance Genius", "Log 10+ focus hours each across 8 or more distinct subjects within a single month.", "🎨", subjectsWith10Hours >= 8, "$subjectsWith10Hours / 8 Subjects", (subjectsWith10Hours.toFloat() / 8f).coerceIn(0f, 1f))
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            badgesList.forEach { badge ->
                if (badge.isUnlocked) {
                    // 🌟 UNLOCKED BADGE: Full Rich Gold Appearance
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.5.dp, goldColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(goldColor.copy(alpha = 0.15f))
                                        .border(1.2.dp, goldColor, CircleShape)
                                ) {
                                    Text(
                                        text = badge.icon,
                                        fontSize = 20.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = badge.title,
                                        color = textMain,
                                        fontSize = 14.5.sp,
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
                                    .background(goldColor.copy(alpha = 0.18f))
                                    .border(1.dp, goldColor, RoundedCornerShape(50))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "UNLOCKED 🏆",
                                    color = goldColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // 🔒 LOCKED BADGE: Clean, Subtle, No Clutter, With Slim Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0x14FFFFFF) else Color(0x06000000))
                            .border(1.dp, cardBorder.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = badge.title,
                                    color = textMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = badge.progressText,
                                    color = textMuted.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = badge.requirement,
                                color = textMuted.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 📊 Thin, Clean Progress Bar
                            LinearProgressIndicator(
                                progress = { badge.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = goldColor.copy(alpha = 0.75f),
                                trackColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000)
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
    val progressText: String,
    val progressFraction: Float = 0f
)
