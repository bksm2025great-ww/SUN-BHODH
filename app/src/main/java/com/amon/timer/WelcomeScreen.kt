package com.amon.timer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit
) {
    val darkBg = Color(0xFF090D14)
    val purpleAccent = Color(0xFF8B5CF6)

    // 🌟 Word roller ke 4 words
    val words = remember { listOf("Focus", "Consistency", "Discipline", "Goal") }
    var currentWordIndex by remember { mutableIntStateOf(0) }

    // ⚡ 1.3 second delay
    LaunchedEffect(Unit) {
        while (true) {
            delay(1300)
            currentWordIndex = (currentWordIndex + 1) % words.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🖼️ 1. HERO IMAGE (Top 62% screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.35f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.welcome_hero),
                    contentDescription = "Amon Study Scene",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, darkBg.copy(alpha = 0.85f), darkBg)
                            )
                        )
                )
            }

            // ✍️ 2. CONTENT & ROTATING WORD LOADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "Master your",
                            color = Color(0xFFE2E8F0),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(8.dp))

                        // 🔒 150dp Box taaki text vibrate na kare
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .width(150.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            AnimatedContent(
                                targetState = words[currentWordIndex],
                                transitionSpec = {
                                    (slideInVertically(animationSpec = tween(350)) { height -> height } + fadeIn(tween(350)))
                                        .togetherWith(
                                            slideOutVertically(animationSpec = tween(350)) { height -> -height } + fadeOut(tween(350))
                                        )
                                },
                                label = "word_roller_anim"
                            ) { word ->
                                Text(
                                    text = word,
                                    color = purpleAccent,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Build deep study discipline, protect your focus streak, and transform your daily learning journey.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }

                // 🚀 3. ACTION BUTTON ("Let's Begin")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 26.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF5A524), Color(0xFFFFC043))
                                )
                            )
                            .clickable { onStartClick() }
                    ) {
                        Text(
                            text = "Let's Begin ➔",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
