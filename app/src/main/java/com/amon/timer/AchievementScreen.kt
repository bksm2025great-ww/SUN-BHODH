package com.amon.timer

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
import androidx.compose.ui.graphics.Color
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

    // 🔄 Load diary sessions & calculate subject minutes
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
    LaunchedEffect(Unit) {
        allSessions = FocusSessionManager.getAllSessions(context)
    }

    val subjectStats = remember(allSessions) {
        val minutesMap = allSessions.groupBy { it.subject.trim() }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }

        // Default subjects aur diary se aaye subjects ko jodein
        val allSubjectNames = (PlantRegistry.defaultSubjects.map { it.name } + minutesMap.keys).distinct()

        allSubjectNames.map { subjectName ->
            val totalMins = minutesMap[subjectName] ?: 0
            val plant = getTreeDetails(subjectName)
            SubjectAchievement(
                name = subjectName,
                totalMinutes = totalMins,
                treeName = plant.nameEn,
                emoji = plant.emoji
            )
        }.sortedByDescending { it.totalMinutes }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ----------------- 1. TOP BAR (BACK BUTTON & TITLE) -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                    .border(1.dp, cardBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", color = textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "ACHIEVEMENTS",
                    color = textMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Unlock Magical Trees through deep focus",
                    color = textMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ----------------- 2. HEADER BANNER CARD -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAGICAL TREES UNLOCK",
                        color = goldColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Master a subject to evolve its tree into a Magical Tree (30 Hours Dedicated Focus Goal).",
                    color = textMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ----------------- 3. SUBJECT TREE PROGRESS CARDS -----------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            subjectStats.forEach { item ->
                val hours = item.totalMinutes / 60
                val minsRemainder = item.totalMinutes % 60
                val isMastered = item.totalMinutes >= 1800 // 30 hours
                val progress = (item.totalMinutes.toFloat() / 1800f).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(
                            width = 1.dp,
                            color = if (isMastered) goldColor.copy(alpha = 0.8f) else cardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = item.emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        color = textMain,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isMastered) "Tree: ${item.treeName} • Mastered" else "Tree: ${item.treeName} • Growing",
                                        color = if (isMastered) goldColor else textMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isMastered) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            // Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isMastered) goldColor.copy(alpha = 0.2f) else Color(0x11FFFFFF))
                                    .border(1.dp, if (isMastered) goldColor else cardBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isMastered) "✨ MASTERED!" else "🔒 IN PROGRESS",
                                    color = if (isMastered) goldColor else textMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = goldColor,
                            trackColor = if (isDark) Color(0xFF2A2A32) else Color(0xFFE2E8F0),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress Numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${hours}h ${minsRemainder}m / 30h",
                                color = textMain,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            val percentage = (progress * 100).toInt()
                            Text(
                                text = "$percentage%",
                                color = if (isMastered) goldColor else textMuted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isMastered) {
                            val remainingMinutes = 1800 - item.totalMinutes
                            val remHours = remainingMinutes / 60
                            val remMins = remainingMinutes % 60
                            val remText = if (remHours > 0) "${remHours}h ${remMins}m" else "${remMins}m"

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⏳ $remText left to evolve into Magical Tree",
                                color = textMuted,
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ----------------- Data Model & Helper -----------------
private data class SubjectAchievement(
    val name: String,
    val totalMinutes: Int,
    val treeName: String,
    val emoji: String
)

private data class PlantSummary(val emoji: String, val nameEn: String)

private fun getTreeDetails(subjectName: String): PlantSummary {
    val match = PlantRegistry.defaultSubjects.find { it.name.equals(subjectName, ignoreCase = true) }
    if (match != null) {
        val emoji = when (match.tree.id) {
            "cherry" -> "🌸"
            "lemon" -> "🍋"
            "apple" -> "🍎"
            "coconut" -> "🌴"
            "mango" -> "🥭"
            "banyan" -> "🌳"
            "bael" -> "🌿"
            "kiwi" -> "🥝"
            "walnut" -> "🌰"
            "orange" -> "🍊"
            "starfruit" -> "⭐"
            "peach" -> "🍑"
            "olive" -> "🫒"
            "fig" -> "🪴"
            "pomegranate" -> "🌱"
            else -> "🌳"
        }
        return PlantSummary(emoji, match.tree.nameEn)
    }

    val vaultIndex = Math.abs(subjectName.hashCode()) % PlantRegistry.reservedTreeVault.size
    val reserved = PlantRegistry.reservedTreeVault[vaultIndex]
    val emoji = when (reserved.id) {
        "banana" -> "🍌"
        "guava" -> "🍈"
        "papaya" -> "🥭"
        "plum" -> "🫐"
        "pear" -> "🍐"
        "jackfruit" -> "🍈"
        "cashew" -> "🥜"
        "custard_apple" -> "🍏"
        "apricot" -> "🍑"
        "lychee" -> "🍓"
        else -> "🌳"
    }
    return PlantSummary(emoji, reserved.nameEn)
}
