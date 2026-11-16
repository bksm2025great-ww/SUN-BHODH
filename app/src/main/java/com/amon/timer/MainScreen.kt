package com.amon.timer

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    onNavigateToStats: () -> Unit,
    onNavigateToGroups: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }

    // 🎨 कलर पैलेट (डार्क ऑब्सीडियन + वार्म गोल्ड)
    val screenBg = Color(0xFF0A0A0E)
    val cardBg = Color(0xFF131318)
    val goldAccent = Color(0xFFF5A524)
    val textMain = Color(0xFFF8FAFC)
    val textMuted = Color(0xFF94A3B8)

    // 📊 सेशंस और स्ट्रीक स्टेट
    var sessions by remember { mutableStateOf(FocusSessionManager.getAllSessions(context)) }
    val currentStreak = remember(sessions) { calculateStreakDays(sessions) }
    
    // 🔥 स्ट्रीक क्लिक करने पर डायलॉग दिखाने के लिए स्टेट
    var showStreakDialog by remember { mutableStateOf(false) }

    // ⏱️ टाइगर स्टेट्स (डिमोंस्ट्रेशन के लिए बेसिक टाइमर स्टेट)
    var isTimerRunning by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableStateOf(25 * 60) } // 25 minutes
    var currentSubject by remember { mutableStateOf("Coding") }

    // टाइमर लॉजिक
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning && timeLeftSeconds > 0) {
                delay(1000L)
                timeLeftSeconds--
            }
            if (timeLeftSeconds == 0) {
                isTimerRunning = false
                val durationMins = 25L
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                val newSession = FocusSession(
                    id = System.currentTimeMillis(),
                    date = currentDate,
                    subject = currentSubject,
                    durationMinutes = durationMins.toInt()
                )
                
                // 1. लोकल सेव
                FocusSessionManager.saveSession(context, newSession)
                sessions = FocusSessionManager.getAllSessions(context)

                // 2. क्लाउड सिंक (गेस्ट या ऑफलाइन होने पर स्वतः सुरक्षित रूप से बाईपास होगा)
                CloudSyncManager.syncSession(context, currentSubject, durationMins)

                Toast.makeText(context, "Session Completed & Saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🌟 टॉप बार: यूजर नेम, स्ट्रीक फ्लेम और स्टेट्स बटन
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                alignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${userManager.getNickName()} 👋",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                    Text(
                        text = if (userManager.isGuestUser()) "Guest Mode (Offline First)" else "Cloud Synced",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    alignment = Alignment.CenterVertically
                ) {
                    // 🔥 स्ट्रीक फ्लेम इंडिकेटर (अब क्लिक करने पर स्ट्रीक डायलॉग खुलेगा, स्टेट्स पेज नहीं!)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33F5A524)),
                        modifier = Modifier.clickable {
                            showStreakDialog = true // 👈 फिक्स्ड: अब सही जगह स्ट्रीक डायलॉग खुलेगा
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            alignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🔥", fontSize = 14.sp)
                            Text(
                                text = "$currentStreak Days",
                                color = goldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 📊 स्टेट्स बटन (यह अलग से स्टेट्स पेज पर ले जाएगा)
                    Button(
                        onClick = onNavigateToStats,
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Stats 📈", color = textMain, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🎯 सेंटर फोकस टाइमर कार्ड
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Current Focus: $currentSubject",
                        color = textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // टाइमर मिनट्स और सेकंड्स फॉर्मेट
                    val mins = timeLeftSeconds / 60
                    val secs = timeLeftSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", mins, secs),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = goldAccent
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // स्टार्ट / पॉज़ बटन
                    Button(
                        onClick = { isTimerRunning = !isTimerRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTimerRunning) Color(0xFFEF4444) else goldAccent
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .width(160.dp)
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isTimerRunning) "Pause" else "Start Focus",
                            color = if (isTimerRunning) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // नीचे ग्रुप्स या अन्य नेविगेशन बटन
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onNavigateToGroups) {
                    Text(text = "👥 Open Study Circles / Groups", color = goldAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 🔥 स्ट्रीक डिटेल्स डायलॉग (जब यूजर स्ट्रीक फ्लेम पर टैप करेगा)
        if (showStreakDialog) {
            AlertDialog(
                onDismissRequest = { showStreakDialog = false },
                containerColor = cardBg,
                title = {
                    Row(alignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "🔥", fontSize = 22.sp)
                        Text(text = "Study Streak", color = textMain, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "You are on a $currentStreak-day study streak!",
                            color = textMain,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Keep completing at least one session every day to maintain your flame and consistency.",
                            color = textMuted,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Total Sessions Logged: ${sessions.size}",
                            color = goldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStreakDialog = false }) {
                        Text(text = "Awesome", color = goldAccent, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// 📊 सेशंस के आधार पर सटीक स्ट्रीक कैलकुलेट करने वाला फंक्शन (बिल्ड एरर फिक्स)
fun calculateStreakDays(sessions: List<FocusSession>): Int {
    if (sessions.isEmpty()) return 0
    
    val dates = sessions.map { it.date }.filter { it.isNotBlank() }.toSet().sortedDescending()
    if (dates.isEmpty()) return 0

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    var streak = 0
    var expectedDateStr = sdf.format(calendar.time)

    if (!dates.contains(expectedDateStr)) {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        expectedDateStr = sdf.format(calendar.time)
    }

    for (dateStr in dates) {
        if (dateStr == expectedDateStr) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            expectedDateStr = sdf.format(calendar.time)
        } else if (dateStr < expectedDateStr) {
            break
        }
    }
    
    return streak
}
