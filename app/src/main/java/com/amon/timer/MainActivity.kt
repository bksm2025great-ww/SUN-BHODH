package com.amon.timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ui.theme.AmonTheme
import kotlinx.coroutines.delay

// 📱 App ki 3 stages
private enum class AppScreenState {
    SPLASH,  // 1.5 second ka Angel of Time intro
    AUTH,    // Welcome + Login / Guest (sirf first time user ke liye)
    MAIN     // Main Timer & Forest page
}

// 🟢 START: [MAIN_ACTIVITY_ENTRY]
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⏰ Charo daily reminders (05:30 AM, 10:00 AM, 04:30 PM, 08:00 PM) schedule karna
        AmonReminderManager.scheduleAllReminders(this)

        setContent {
            AmonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F12)
                ) {
                    val userManager = remember { UserManager(this@MainActivity) }
                    var currentScreen by remember { mutableStateOf(AppScreenState.SPLASH) }

                    // 1.5 second ka splash delay aur check
                    LaunchedEffect(Unit) {
                        delay(1500)
                        currentScreen = if (userManager.isAccountSetupDone()) {
                            AppScreenState.MAIN
                        } else {
                            AppScreenState.AUTH
                        }
                    }

                    // Smooth transition animation
                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 400),
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreenState.SPLASH -> {
                                SplashScreenContent()
                            }
                            AppScreenState.AUTH -> {
                                AuthScreen(
                                    onAuthComplete = {
                                        currentScreen = AppScreenState.MAIN
                                    }
                                )
                            }
                            AppScreenState.MAIN -> {
                                MainScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✨ 1.5 second wali Amon Splash Screen (Angel of Time)
@Composable
private fun SplashScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Mukhya Logo: AMON
            Text(
                text = "AMON",
                color = Color(0xFFF5A524),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Sub-title: Angel of Time - LOTM
            Text(
                text = "Angel of Time - LOTM",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(46.dp))

            // 3. Classic font: STAY FOCUSED & GROW
            Text(
                text = "S T A Y   F O C U S E D\n&\nG R O W",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                letterSpacing = 3.sp
            )
        }
    }
}
// 🔴 END: [MAIN_ACTIVITY_ENTRY]
