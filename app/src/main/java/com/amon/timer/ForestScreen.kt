package com.amon.timer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

    // कलर्स
    val goldColor = Color(0xFFF3C669)
    val cardBg = Color(0xFF1E1E22)
    val boxDarkGray = Color(0xFF2A2A32)
    val textMuted = Color(0xFFA0A0A5)

    // स्टेट्स
    var selectedTab by remember { mutableStateOf("Today") }
    var showDialog by remember { mutableStateOf<FocusSession?>(null) }
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger, selectedTab) {
        allSessions = FocusSessionManager.getAllSessions(context)
    }

    // 🟢 स्मार्ट डेट फ़िल्टर (Today, This Week, All Time)
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. बैकग्राउंड (विंटेज बोर्ड + हरी मखमली घास)
        Image(
            painter = painterResource(id = R.drawable.bg_amon_garden),
            contentDescription = "Amon Garden Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. स्क्रीन की सामग्री
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Amon Garden बोर्ड के नीचे आने के लिए स्पेस
            Spacer(modifier = Modifier.height(115.dp))

            // पारदर्शी कैप्सूल फ़िल्टर टैब्स (Today | This Week | All Time)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.45f)) // 50% ट्रांसपेरेंट लुक
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Today", "This Week", "All Time").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFE2A84B) else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color(0xFF2C1604) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. सात-सात गमलों की स्क्रोल होने वाली ग्रिड
            if (displaySessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No focus sessions yet.\nStart timer to grow plants!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7), // एक कतार में ठीक 7 गमले
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(displaySessions) { session ->
                        // गमला + शैडो कंपोनेंट
                        GardenPotWithShadow(
                            session = session,
                            onClick = { showDialog = session }
                        )
                    }
                }
            }
        }
    }

    // 4. पॉप-अप डायलॉग (जब गमले पर क्लिक करें)
    if (showDialog != null) {
        val isDialogWithered = showDialog!!.earnedTrees == 0

        Dialog(onDismissRequest = { showDialog = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bloomRes = if (isDialogWithered) R.drawable.plant_withered else R.drawable.pot_marigold_1
                    Image(
                        painter = painterResource(id = bloomRes),
                        contentDescription = "Garden Bloom",
                        modifier = Modifier.size(100.dp)
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
// 🌱 शैडो और गमला दिखाने वाला सेल
// -----------------------------------------------------------------------------
@Composable
fun GardenPotWithShadow(
    session: FocusSession,
    onClick: () -> Unit
) {
    // अगर सेशन अधूरा रहा (earnedTrees == 0) तो सूखा पौधा, वरना गेंदा
    val isWithered = session.earnedTrees == 0
    val imageRes = if (isWithered) R.drawable.plant_withered else R.drawable.pot_marigold_1

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clickable { onClick() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // लेयर 1: पौधे के नीचे की हल्की ओवल शैडो
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(8.dp)
                .offset(y = 2.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = 180f
                    ),
                    shape = CircleShape
                )
        )

        // लेयर 2: पौधे की साफ़ इमेज
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = if (isWithered) "Withered Plant" else "Marigold Pot",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

// -----------------------------------------------------------------------------
// 📅 Universal Smart Date Parser (सुरक्षित रखा गया)
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
            set(d.toInt(), m.toInt() - 1, y.toInt(), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    return null
}
