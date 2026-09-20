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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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
    var selectedTab by remember { mutableStateOf("All Time") }
    var showDialog by remember { mutableStateOf<FocusSession?>(null) }
    var allSessions by remember { mutableStateOf(listOf<FocusSession>()) }

    // Load data
    LaunchedEffect(Unit) {
        allSessions = FocusSessionManager.getSessions(context)
    }

    // Filter data based on tab
    val displaySessions = when (selectedTab) {
        "Today" -> allSessions.takeLast(3) // Example filtering
        "This Week" -> allSessions.takeLast(7)
        else -> allSessions
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
                    text = "You've grown ${allSessions.size} trees, keep it up!",
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
        // Hum diamond effect ke liye boxes ko 45 degree ghuma (rotate) rahe hain
        val boxSize = 50.dp
        val treeIconSize = 32.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-10).dp) // Thoda overlap jisse 3D lage
        ) {
            // Row patterns to make a big diamond shape (2, 3, 4, 3, 2 boxes)
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
                                .rotate(45f) // Dibbe ko tircha karke diamond banaya
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
                            // Agar data hai toh ped ugega
                            if (session != null) {
                                Text(
                                    text = "🌲",
                                    fontSize = treeIconSize.value.sp,
                                    modifier = Modifier.rotate(-45f) // Ped ko sidha rakhne ke liye wapas ghumaya
                                )
                            } else {
                                // Khali zameen ka nishaan (chhota beej)
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
                .clickable { /* Future me list khulegi */ }
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

    // ----------------- 5. POP-UP DIALOG (JAB PED PAR TOUCH KAREN) -----------------
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
                    Text(text = "🌲", fontSize = 50.sp)
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
