package com.amon.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.amon.timer.ui.theme.AccentYellow

// 🟢 START: [MAIN_SCREEN_COMPONENT]
@Composable
fun MainScreen() {
    // 🟢 START: [TIMER_STATE_LOGIC]
    // टाइमर का समय (25 मिनट = 1500 सेकंड) और चालू/बंद स्थिति
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    // बैकग्राउंड में हर सेकंड समय कम करने का इंजन
    LaunchedEffect(isRunning) {
        while (isRunning && totalSeconds > 0) {
            delay(1000L)
            totalSeconds--
        }
        if (totalSeconds == 0) {
            isRunning = false
        }
    }

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    // 🔴 END: [TIMER_STATE_LOGIC]

    // 🟢 START: [MAIN_UI_LAYOUT]
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // गोल टाइमर डायल
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(260.dp)
                    .border(width = 6.dp, color = AccentYellow, shape = CircleShape)
                    .padding(24.dp)
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // कंट्रोल बटन्स (Start, Pause, Reset)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentYellow,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (isRunning) "Pause" else "Start",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        isRunning = false
                        totalSeconds = 25 * 60
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
    // 🔴 END: [MAIN_UI_LAYOUT]
}
// 🔴 END: [MAIN_SCREEN_COMPONENT]
