package com.amon.timer

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

    // 🌲 30 ghante unlock wala dark forest green shade
    val forestGreenBg = Color(0xFF163323)
    val forestGreenBorder = Color(0xFF4CAF50)

    // States
    var selectedTab by remember { mutableStateOf("Today") }
    var showDialog by remember { mutableStateOf<FocusSession?>(null) }
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }

    // 🔄 Load / Reload data from diary
    LaunchedEffect(refreshTrigger) {
        allSessions = FocusSessionManager.getAllSessions(context)
    }

    // 🧮 Har subject ke kul padhai ke minutes calculator
    val subjectTotalMinutes = remember(allSessions) {
        allSessions.groupBy { it.subject.trim().lowercase() }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
    }

    // 🟢 Smart Date Filter (Today, This Week, All Time)
    val displaySessions = remember(allSessions, selectedTab) {
        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val weekAgoCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (selectedTab) {
            "Today" -> {
                allSessions.filter { session ->
                    val d = parseSessionDate(session.date)
                    if (d != null) {
                        val sessionCal = Calendar.getInstance().apply { time = d }
                        sessionCal.get(Calendar.YEAR) == nowYear && sessionCal.get(Calendar.DAY_OF_YEAR) == nowDayOfYear
                    } else false
                }
            }
            "This Week" -> {
                allSessions.filter { session ->
                    val d = parseSessionDate(session.date)
                    if (d != null) {
                        !d.before(weekAgoCal.time)
                    } else false
                }
            }
            else -> allSessions // "All Time"
        }
    }

    // Motivation Card Subtitle
    val successfulTreesCount = displaySessions.count { it.earnedTrees > 0 }
    val motivationSubtitle = when (selectedTab) {
        "Today" -> if (successfulTreesCount == 0) "No trees grown today yet. Start focusing!" else "You've grown $successfulTreesCount tree${if (successfulTreesCount > 1) "s" else ""} today, keep it up!"
        "This Week" -> "You've grown $successfulTreesCount tree${if (successfulTreesCount > 1) "s" else ""} this week, keep it up!"
        else -> "You've grown $successfulTreesCount tree${if (successfulTreesCount > 1) "s" else ""} in total, keep it up!"
    }

    // 🗺️ Naya Grid Pattern: Base 3,4,3,4,3 (17 slots) aur Expand par 4,3,4,3... judte jayenge
    val gridPattern = remember(isExpanded, displaySessions.size) {
        val basePattern = listOf(3, 4, 3, 4, 3) // 17 slots base
        if (!isExpanded) {
            basePattern
        } else {
            val dynamicList = basePattern.toMutableList()
            var currentCapacity = 17
            val extraRowCycle = listOf(4, 3)
            var cycleIndex = 0

            // Expanded mode me kam se kam 2 extra rows judengi, aur session zyada hone par aur judengi
            val targetCapacity = maxOf(displaySessions.size, 24)
            while (currentCapacity < targetCapacity) {
                val nextCount = extraRowCycle[cycleIndex % extraRowCycle.size]
                dynamicList.add(nextCount)
                currentCapacity += nextCount
                cycleIndex++
            }
            dynamicList
        }
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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                // 🔄 Refresh Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { refreshTrigger++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔄",
                        fontSize = 18.sp
                    )
                }
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

        // ----------------- 3. DIAMOND GRID (3,4,3,4,3 PATTERN) -----------------
        val boxSize = 50.dp
        val treeIconSize = 32.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-10).dp)
        ) {
            var sessionIndex = 0

            gridPattern.forEach { count ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(count) {
                        val session = displaySessions.getOrNull(sessionIndex)
                        sessionIndex++

                        val isWithered = session != null && session.earnedTrees == 0

                        val isMastered = session != null && !isWithered &&
                                ((subjectTotalMinutes[session.subject.trim().lowercase()] ?: 0) >= 1800)

                        val tileBg = when {
                            isWithered -> Color(0xFF241E1E)
                            isMastered -> forestGreenBg
                            else -> boxDarkGray
                        }
                        val tileBorder = when {
                            isWithered -> Color(0xFF7F1D1D)
                            isMastered -> forestGreenBorder
                            else -> boxBorderGolden
                        }

                        Box(
                            modifier = Modifier
                                .size(boxSize)
                                .rotate(45f)
                                .background(tileBg, RoundedCornerShape(12.dp))
                                .border(1.2.dp, tileBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (session != null) {
                                        showDialog = session
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (session != null) {
                                if (isWithered) {
                                    // 🍂 Sukha ped (130dp)
                                    Image(
                                        painter = painterResource(id = R.drawable.tree_withered),
                                        contentDescription = "Withered Tree",
                                        modifier = Modifier
                                            .requiredSize(130.dp)
                                            .rotate(-45f)
                                    )
                                } else if (isMastered) {
                                    // ✨ 30 ghante poore hone par 2.5D ped (130dp)
                                    val isSakura = session.subject.contains("english", ignoreCase = true) ||
                                            session.subject.contains("art", ignoreCase = true) ||
                                            session.subject.contains("cherry", ignoreCase = true)

                                    val treeResId = if (isSakura) R.drawable.tree_sakura else R.drawable.tree_oak

                                    Image(
                                        painter = painterResource(id = treeResId),
                                        contentDescription = "Mastered 2.5D Tree",
                                        modifier = Modifier
                                            .requiredSize(130.dp)
                                            .rotate(-45f)
                                    )
                                } else {
                                    // 🌸 Samanya emoji (0 to 29 ghante)
                                    val plantInfo = getPlantIconAndName(session.subject)
                                    Text(
                                        text = plantInfo.first,
                                        fontSize = treeIconSize.value.sp,
                                        modifier = Modifier.rotate(-45f)
                                    )
                                }
                            } else {
                                // 🌱 Khali slot
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

        // ----------------- 4. VIEW MORE / SHOW LESS BUTTON -----------------
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF18181B))
                .border(1.dp, boxBorderGolden, RoundedCornerShape(50))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isExpanded) "Show Less ⬆" else "View More History ➔",
                color = goldColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // ----------------- 5. POP-UP DIALOG -----------------
    if (showDialog != null) {
        val isDialogWithered = showDialog!!.earnedTrees == 0
        val isDialogMastered = !isDialogWithered &&
                ((subjectTotalMinutes[showDialog!!.subject.trim().lowercase()] ?: 0) >= 1800)

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
                    if (isDialogWithered) {
                        Image(
                            painter = painterResource(id = R.drawable.tree_withered),
                            contentDescription = "Withered Tree",
                            modifier = Modifier.requiredSize(140.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Withered Tree (Session Incomplete)",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isDialogMastered) {
                        val isSakura = showDialog!!.subject.contains("english", ignoreCase = true) ||
                                showDialog!!.subject.contains("art", ignoreCase = true) ||
                                showDialog!!.subject.contains("cherry", ignoreCase = true)

                        val treeResId = if (isSakura) R.drawable.tree_sakura else R.drawable.tree_oak

                        Image(
                            painter = painterResource(id = treeResId),
                            contentDescription = "Mastered Tree",
                            modifier = Modifier.requiredSize(140.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val treeName = if (isSakura) "✨ Magical Cherry Sakura (Mastered)" else "✨ Magical Classic Oak (Mastered)"
                        Text(
                            text = treeName,
                            color = goldColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val plantInfo = getPlantIconAndName(showDialog!!.subject)
                        Text(text = plantInfo.first, fontSize = 50.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plantInfo.second,
                            color = goldColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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
                        text = if (isDialogWithered) "Session Cancelled / Incomplete" else "Focused for: ${showDialog!!.durationMinutes} Minutes",
                        color = if (isDialogWithered) Color(0xFFF87171) else goldColor,
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
// 📅 Smart Date Cleaner & Detector
// -----------------------------------------------------------------------------
private fun parseSessionDate(rawDateStr: String): Date? {
    if (rawDateStr.isBlank()) return null

    var clean = rawDateStr
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("\"", "")
        .replace("'", "")
        .trim()

    clean = clean.replace(Regex("\\s+"), " ")

    // Format A: yyyy-MM-dd
    val ymdMatch = Regex("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})").find(clean)
    if (ymdMatch != null) {
        val (y, m, d) = ymdMatch.destructured
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y.toInt())
        cal.set(Calendar.MONTH, m.toInt() - 1)
        cal.set(Calendar.DAY_OF_MONTH, d.toInt())
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    // Format B: dd/MM/yyyy
    val dmyMatch = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})").find(clean)
    if (dmyMatch != null) {
        val (d, m, y) = dmyMatch.destructured
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y.toInt())
        cal.set(Calendar.MONTH, m.toInt() - 1)
        cal.set(Calendar.DAY_OF_MONTH, d.toInt())
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    // Backup formats
    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-M-d", Locale.getDefault())
    )
    for (fmt in formats) {
        try {
            val d = fmt.parse(clean)
            if (d != null) return d
        } catch (_: Exception) {}
    }
    return null
}

// 🌳 Helper: Subject ke hisaab se emoji aur naam nikaalne wala helper
private fun getPlantIconAndName(subjectName: String): Pair<String, String> {
    val defaultMatch = PlantRegistry.defaultSubjects.find { it.name.equals(subjectName, ignoreCase = true) }
    if (defaultMatch != null) {
        val emoji = when (defaultMatch.tree.id) {
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
