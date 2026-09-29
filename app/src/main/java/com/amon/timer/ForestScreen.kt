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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

    // States
    var selectedTab by remember { mutableStateOf("Today") }
    var showDialog by remember { mutableStateOf<FocusSession?>(null) }
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger, selectedTab) {
        allSessions = FocusSessionManager.getAllSessions(context)
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
                    val d = parseSessionDateUniversal(session.date)
                    if (d != null) {
                        val sessionCal = Calendar.getInstance().apply { time = d }
                        sessionCal.get(Calendar.YEAR) == nowYear && sessionCal.get(Calendar.DAY_OF_YEAR) == nowDayOfYear
                    } else false
                }
            }
            "This Week" -> {
                allSessions.filter { session ->
                    val d = parseSessionDateUniversal(session.date)
                    if (d != null) {
                        !d.before(weekAgoCal.time)
                    } else false
                }
            }
            else -> allSessions
        }
    }

    val successfulTreesCount = displaySessions.count { it.earnedTrees > 0 }
    val motivationSubtitle = when (selectedTab) {
        "Today" -> if (successfulTreesCount == 0) "No blooms grown today yet. Start focusing!" else "You've grown $successfulTreesCount bloom${if (successfulTreesCount > 1) "s" else ""} today, keep it up!"
        "This Week" -> "You've grown $successfulTreesCount bloom${if (successfulTreesCount > 1) "s" else ""} this week, keep it up!"
        else -> "You've grown $successfulTreesCount bloom${if (successfulTreesCount > 1) "s" else ""} in total, keep it up!"
    }

    // पगडंडी के वास्तविक घुमाव के अनुसार मोड़ों के स्लॉट्स (X, Y)
    val pathwaySlots = remember {
        listOf(
            Pair(50.dp, 195.dp),  // Slot 1 (Gate ke paas, Left)
            Pair(215.dp, 290.dp), // Slot 2 (Pahla Mod, Right)
            Pair(65.dp, 430.dp),  // Slot 3 (Dusra Mod, Left)
            Pair(210.dp, 560.dp), // Slot 4 (Teesra Mod, Right)
            Pair(85.dp, 680.dp)   // Slot 5 (Chautha Mod, Left)
        )
    }

    // जब लॉक हो तो 420.dp तक दिखेगा, जब "View More" करेंगे तो पूरा 820.dp रास्ता खुलेगा
    val currentPathwayHeight = if (isExpanded) 820.dp else 440.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. TOP MOTIVATION CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, boxBorderGolden, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Amon Garden",
                        color = goldColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = motivationSubtitle,
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { refreshTrigger++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔄", fontSize = 18.sp)
                }
            }
        }

        // 2. CALENDAR TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF18181B))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Today", "This Week", "All Time").forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color(0x33F3C669) else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 8.dp),
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

        Spacer(modifier = Modifier.height(14.dp))

        // 3. 2.5D GARDEN PATHWAY (Exact Ratio - No Black Void)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentPathwayHeight)
                .clipToBounds()
        ) {
            // बैकग्राउंड पगडंडी
            Image(
                painter = painterResource(id = R.drawable.bg_pathway_meadow),
                contentDescription = "Amon Garden Pathway",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(820.dp),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter
            )

            // केवल वही पौधे और क्यारियां दिखेंगी जहाँ सेशन पूरा हुआ है
            pathwaySlots.forEachIndexed { index, (xPos, yPos) ->
                val session = displaySessions.getOrNull(index)

                if (session != null) {
                    val plantDrawable = getBloomDrawable(session, index)
                    PlantPlot(
                        plantResId = plantDrawable,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = xPos, y = yPos)
                            .clickable {
                                showDialog = session
                            }
                    )
                }
            }

            // लॉक अवस्था में नीचे की खूबसूरत डार्क शेड
            if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    darkBg.copy(alpha = 0.6f),
                                    darkBg
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. VIEW MORE / SHOW LESS BUTTON
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF18181B))
                .border(1.dp, boxBorderGolden, RoundedCornerShape(50))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (isExpanded) "Show Less ⬆" else "View More History ➔",
                color = goldColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 5. POP-UP DIALOG (टैप करने पर विवरण)
    if (showDialog != null) {
        val isDialogWithered = showDialog!!.earnedTrees == 0

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
                    val bloomRes = if (isDialogWithered) R.drawable.tree_withered else getBloomDrawable(showDialog!!, 0)
                    Image(
                        painter = painterResource(id = bloomRes),
                        contentDescription = "Garden Bloom",
                        modifier = Modifier.requiredSize(110.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isDialogWithered) "Withered Session" else "Blooming Flower",
                        color = if (isDialogWithered) Color(0xFFEF4444) else goldColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = showDialog!!.subject,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Date: ${showDialog!!.date}",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isDialogWithered) "Session Incomplete" else "Focused for: ${showDialog!!.durationMinutes} Minutes",
                        color = if (isDialogWithered) Color(0xFFF87171) else goldColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(18.dp))
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
// 🌸 Helper: सही पौधा चुनना
// -----------------------------------------------------------------------------
private fun getBloomDrawable(session: FocusSession, index: Int): Int {
    if (session.earnedTrees == 0) return R.drawable.tree_withered
    val sub = session.subject.trim().lowercase()

    return when {
        sub.contains("lotus") || sub.contains("math") -> R.drawable.plant_lotus
        sub.contains("cherry") || sub.contains("sakura") || sub.contains("english") || sub.contains("art") -> R.drawable.plant_cherry_blossom
        sub.contains("paradise") || sub.contains("bird") || sub.contains("code") || sub.contains("science") -> R.drawable.plant_bird_of_paradise
        sub.contains("iris") || sub.contains("blue") || sub.contains("history") -> R.drawable.plant_iris
        else -> {
            val blooms = listOf(
                R.drawable.plant_lotus,
                R.drawable.plant_cherry_blossom,
                R.drawable.plant_bird_of_paradise,
                R.drawable.plant_iris
            )
            blooms[Math.abs(session.subject.hashCode() + index) % blooms.size]
        }
    }
}

// -----------------------------------------------------------------------------
// 📅 Universal Smart Date Parser
// -----------------------------------------------------------------------------
fun parseSessionDateUniversal(rawDateStr: String): Date? {
    if (rawDateStr.isBlank()) return null

    val clean = rawDateStr
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

    val ymdMatch = Regex("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})").find(clean)
    if (ymdMatch != null) {
        val (y, m, d) = ymdMatch.destructured
        return Calendar.getInstance().apply {
            set(y.toInt(), m.toInt() - 1, d.toInt(), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    val dmyMatch = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})").find(clean)
    if (dmyMatch != null) {
        val (d, m, y) = dmyMatch.destructured
        return Calendar.getInstance().apply {
            set(y.toInt(), m.toInt() - 1, d.toInt(), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    return null
}
