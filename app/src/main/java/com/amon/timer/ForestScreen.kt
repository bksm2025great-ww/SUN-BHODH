package com.amon.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ForestScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Colors (Luxe Gold Theme)
    val goldColor = Color(0xFFF3C669)
    val darkBg = Color(0xFF121214)
    val cardBg = Color(0xFF1E1E22)
    val boxDarkGray = Color(0xFF2A2A32)
    val boxBorderGolden = Color(0x33F3C669)
    val textMuted = Color(0xFFA0A0A5)

    // States
    var selectedTab by remember { mutableStateOf("Today") }
    var showDialog by remember { mutableStateOf<FocusSession?>(null) }
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }

    // Load data from diary
    LaunchedEffect(Unit) {
        allSessions = FocusSessionManager.getAllSessions(context)
    }

    // 🟢 स्मार्ट डेट फ़िल्टर: सिर्फ़ असली पेड़ (> 0) और चुनी हुई तारीख के हिसाब से
    val displaySessions = remember(allSessions, selectedTab) {
        val treeOnlySessions = allSessions.filter { it.earnedTrees > 0 }
        val now = Calendar.getInstance()

        when (selectedTab) {
            "Today" -> {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                treeOnlySessions.filter { session ->
                    val d = parseSessionDate(session.date)
                    if (d != null) {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d) == todayStr
                    } else {
                        session.date.startsWith(todayStr)
                    }
                }
            }
            "This Week" -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val sevenDaysAgo = cal.time
                treeOnlySessions.filter { session ->
                    val d = parseSessionDate(session.date)
                    if (d != null) {
                        !d.before(sevenDaysAgo)
                    } else false
                }
            }
            else -> treeOnlySessions // "All Time"
        }
    }

    // ऊपर का मोटिवेशनल संदेश
    val motivationSubtitle = when (selectedTab) {
        "Today" -> if (displaySessions.isEmpty()) "No trees grown today yet. Start focusing!" else "You've grown ${displaySessions.size} tree${if (displaySessions.size > 1) "s" else ""} today, keep it up!"
        "This Week" -> "You've grown ${displaySessions.size} tree${if (displaySessions.size > 1) "s" else ""} this week, keep it up!"
        else -> "You've grown ${displaySessions.size} tree${if (displaySessions.size > 1) "s" else ""} in total, keep it up!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ----------------- 1. TOP MOTIVATION CARD -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, boxBorderGolden, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Your Amon Forest",
                    color = goldColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = motivationSubtitle,
                    color = textMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ----------------- 2. CALENDAR TABS -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF18181B))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("Today", "This Week", "All Time")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color(0x33F3C669) else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) goldColor else textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ----------------- 3. DIAMOND GRID (3D LOOK) -----------------
        val boxSize = 50.dp
        val treeIconSize = 32.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-10).dp)
        ) {
            val gridPattern = listOf(2, 3, 4, 3, 2)
            var sessionIndex = 0

            gridPattern.forEach { count ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(count) {
                        val session = displaySessions.getOrNull(sessionIndex)
                        sessionIndex++

                        Box(
                            modifier = Modifier
                                .size(boxSize)
                                .rotate(45f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(boxDarkGray)
                                .border(1.2.dp, boxBorderGolden, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (session != null) {
                                        showDialog = session
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (session != null) {
                                val plantInfo = getPlantIconAndName(session.subject)
                                Text(
                                    text = plantInfo.first,
                                    fontSize = treeIconSize.value.sp,
                                    modifier = Modifier.rotate(-45f)
                                )
                            } else {
                                Text(
                                    text = "🌱",
                                    fontSize = 14.sp,
                                    modifier = Modifier.rotate(-45f).alpha(0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ----------------- 4. VIEW MORE BUTTON -----------------
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF18181B))
                .border(1.dp, boxBorderGolden, RoundedCornerShape(50))
                .clickable { /* Future me full history view */ }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "View More History ➔",
                color = goldColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // ----------------- 5. POP-UP DIALOG -----------------
    if (showDialog != null) {
        Dialog(onDismissRequest = { showDialog = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, goldColor, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val plantInfo = getPlantIconAndName(showDialog!!.subject)
                    Text(text = plantInfo.first, fontSize = 50.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plantInfo.second,
                        color = goldColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = showDialog!!.subject,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Date: ${showDialog!!.date}",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Focused for: ${showDialog!!.durationMinutes} Minutes",
                        color = goldColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = boxDarkGray)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 📅 Helper: Tariq ko sahi se parse karne wala function
// -----------------------------------------------------------------------------
private fun parseSessionDate(dateStr: String): Date? {
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    )
    for (fmt in formats) {
        try {
            val d = fmt.parse(dateStr)
            if (d != null) return d
        } catch (_: Exception) {}
    }
    return null
}

// 🌳 Subject ke hisaab se unique plant aur naam nikaalne wala helper function
private fun getPlantIconAndName(subjectName: String): Pair<String, String> {
    val defaultMatch = PlantRegistry.defaultSubjects.find { it.name.equals(subjectName, ignoreCase = true) }
    if (defaultMatch != null) {
        val emoji = when (defaultMatch.tree.id) {
            "cherry" -> "🌸" // English (Cherry Blossom)
            "lemon" -> "🍋"  // Math (Lemon Tree)
            "apple" -> "🍎"  // Physics (Apple Tree)
            "coconut" -> "🌴"// Geography (Coconut Tree)
            "mango" -> "🥭"  // Art & Culture (Mango Tree)
            "banyan" -> "🌳" // All (Banyan Tree)
            "bael" -> "🌿"   // Hindi (Bael Tree)
            "kiwi" -> "🥝"   // Science (Kiwi Tree)
            "walnut" -> "🌰" // Polity (Walnut Tree)
            "orange" -> "🍊" // Economics (Orange Tree)
            "starfruit" -> "⭐" // Current Affairs (Starfruit)
            "peach" -> "🍑"  // Psychology (Peach Tree)
            "olive" -> "🫒"  // History (Olive Tree)
            "fig" -> "🪴"    // Biology (Fig Tree)
            "pomegranate" -> "🌱" // Chemistry (Pomegranate)
            else -> "🌲"
        }
        return Pair(emoji, "${defaultMatch.tree.nameEn} (${defaultMatch.tree.nameHi})")
    }

    val vaultIndex = Math.abs(subjectName.hashCode()) % PlantRegistry.reservedTreeVault.size
    val reservedTree = PlantRegistry.reservedTreeVault[vaultIndex]
    val vaultEmoji = when (reservedTree.id) {
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
    return Pair(vaultEmoji, "${reservedTree.nameEn} (${reservedTree.nameHi})")
}
