package com.amon.timer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// स्लाइड का डेटा मॉडल
data class WelcomeSlideData(
    val prefix: String,
    val highlight: String,
    val description: String
)

@Composable
fun PreWelcomeScreen(onGetStarted: () -> Unit = {}) {
    WelcomeScreen(onGetStarted = onGetStarted)
}

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit = {}
) {
    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val goldColor = ThemeManager.getAccentColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()

    // 🎯 वे टेक्स्ट जो बारी-बारी एक साथ बदलेंगे
    val slides = remember {
        listOf(
            WelcomeSlideData(
                prefix = "Mastery over",
                highlight = "Deep Focus",
                description = "Train your mind for uninterrupted, distraction-free productivity sessions."
            ),
            WelcomeSlideData(
                prefix = "Mastery over",
                highlight = "Your Time",
                description = "Track every second with monotonic hardware accuracy and beautiful dials."
            ),
            WelcomeSlideData(
                prefix = "Mastery over",
                highlight = "Distractions",
                description = "Stay disciplined, build your daily streak, and conquer your goals."
            )
        )
    }

    var currentSlideIndex by remember { mutableIntStateOf(0) }

    // ⏱️ हर 3 सेकंड में दोनों टेक्स्ट एक साथ बदलेंगे
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200L)
            currentSlideIndex = (currentSlideIndex + 1) % slides.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ---------------------------------------------------------------------
        // 1. टॉप इमेज: बिना किसी पैडिंग के सीधे स्क्रीन के सबसे ऊपर (Full Edge-to-Edge)
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .align(Alignment.TopCenter)
        ) {
            Image(
                // ⚠️ ध्यान दें: अगर आपकी वेलकम इमेज का नाम अलग है (जैसे ic_welcome या welcome_art), 
                // तो यहाँ ic_launcher की जगह अपनी इमेज का नाम लिख लें।
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "Welcome Illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // स्मूथ ग्रेडिएंट: ऊपर साफ़ रहेगा और नीचे आते-आते बैकग्राउंड में घुल जाएगा
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                bgColor.copy(alpha = 0.5f),
                                bgColor
                            )
                        )
                    )
            )
        }

        // ---------------------------------------------------------------------
        // 2. निचला हिस्सा: टेक्स्ट एनीमेशन, डॉट्स और स्टार्ट बटन
        // ---------------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 🔄 दोनों लाइन्स एक साथ ऊपर-नीचे स्लाइड होकर बदलेंगी
            AnimatedContent(
                targetState = currentSlideIndex,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(450, easing = FastOutSlowInEasing)) { height -> height / 2 } +
                            fadeIn(animationSpec = tween(450)))
                        .togetherWith(
                            slideOutVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { height -> -height / 2 } +
                                    fadeOut(animationSpec = tween(350))
                        )
                },
                label = "WelcomeSlideTogether"
            ) { targetIndex ->
                val slide = slides[targetIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // लाइन 1: Mastery over (कम बोल्डनेस)
                    Text(
                        text = slide.prefix,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMuted,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // लाइन 2: Focus / Time / Distractions
                    Text(
                        text = slide.highlight,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = goldColor,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // लाइन 3: छोटा विवरण
                    Text(
                        text = slide.description,
                        fontSize = 13.sp,
                        color = textMuted.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // ⚪ डॉट्स (Slide Indicators)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.indices.forEach { index ->
                    val isSelected = index == currentSlideIndex
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        animationSpec = tween(250),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) goldColor
                                else (if (isDark) Color(0x33FFFFFF) else Color(0x22000000))
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 🚀 मुख्य 'Get Started' बटन
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(48.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(50),
                        spotColor = goldColor,
                        ambientColor = goldColor
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Get Started",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "▶",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
