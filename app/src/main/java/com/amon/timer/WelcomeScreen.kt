package com.amon.timer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

data class WelcomeSlideItem(
    val prefix: String,
    val highlightWord: String,
    val highlightColor: Color,
    val subtitle: String
)

@Composable
fun PreWelcomeScreen(
    onStartClick: () -> Unit = {},
    onGetStarted: () -> Unit = {}
) {
    WelcomeScreen(onStartClick = onStartClick, onGetStarted = onGetStarted)
}

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit = {},
    onGetStarted: () -> Unit = {}
) {
    val finalAction = {
        onStartClick()
        onGetStarted()
    }

    val goldColor = ThemeManager.getAccentColor()
    val bgColor = Color(0xFF090A0F)

    val slides = remember {
        listOf(
            WelcomeSlideItem(
                prefix = "Master your ",
                highlightWord = "Goal",
                highlightColor = Color(0xFFA855F7),
                subtitle = "Build deep study discipline, protect your focus streak, and transform your daily learning journey."
            ),
            WelcomeSlideItem(
                prefix = "Master your ",
                highlightWord = "Focus",
                highlightColor = Color(0xFFEAB308),
                subtitle = "Eliminate distractions, enter deep flow states, and accomplish more in every study session."
            ),
            WelcomeSlideItem(
                prefix = "Master your ",
                highlightWord = "Time",
                highlightColor = Color(0xFF38BDF8),
                subtitle = "Track precision sessions with intuitive watches and build unbroken consistency."
            )
        )
    }

    var currentSlideIndex by remember { mutableIntStateOf(0) }

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
        // 1. टॉप इमेज: बिना किसी पैडिंग के ऊपर स्टेटस बार तक (Full Edge-to-Edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .align(Alignment.TopCenter)
        ) {
            Image(
                painter = painterResource(id = R.drawable.welcome_hero),
                contentDescription = "Welcome Hero Illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

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

        // 2. निचला हिस्सा: टेक्स्ट एनीमेशन और Let's Begin बटन
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
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
                label = "MasterYourSlideTogether"
            ) { targetIndex ->
                val slide = slides[targetIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = slide.prefix,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = slide.highlightWord,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = slide.highlightColor,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = slide.subtitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = finalAction,
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(52.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(50),
                        spotColor = goldColor,
                        ambientColor = goldColor
                    )
            ) {
                Text(
                    text = "Let's Begin ➔",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}
