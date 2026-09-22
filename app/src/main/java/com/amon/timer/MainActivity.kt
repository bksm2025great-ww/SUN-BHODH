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

// 🟢 START: [MAIN_ACTIVITY_ENTRY]
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F12)
                ) {
                    // 1.5 सेकंड का टाइमर (Splash State)
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(1500) // ठीक 1.5 सेकंड का ठहराव
                        showSplash = false
                    }

                    // स्मूथ बदलाव (Fade Animation)
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 400),
                        label = "SplashTransition"
                    ) { isSplashActive ->
                        if (isSplashActive) {
                            SplashScreenContent()
                        } else {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }
}

// ✨ 1.5 सेकंड वाली Amon Splash Screen (Angel of Time)
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
            // 1. मुख्य लोगो: AMON
            Text(
                text = "AMON",
                color = Color(0xFFF5A524),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. सब-टाइटल: Angel of Time - LOTM (तिरछा / Italic)
            Text(
                text = "Angel of Time - LOTM",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            )

            // 3-4 लाइन का खाली स्पेस
            Spacer(modifier = Modifier.height(46.dp))

            // 3. क्लासिक फॉन्ट: STAY FOCUSED & GROW
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
