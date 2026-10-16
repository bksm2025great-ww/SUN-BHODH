package com.amon.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// सीढ़ी या स्टेप का डेटा मॉडल
data class JourneyStep(
    val level: Int,
    val offsetFactor: Float // ज़िग-जैग के लिए बाएँ या दाएँ खिसकाने का मान (-1.0 से 1.0)
)

@Composable
fun ForestScreen() {
    val isDark = ThemeManager.isDarkTheme.value

    // कुल 50-60 लेवल्स का ज़िग-जैग रास्ता
    val totalLevels = 60
    val stepsList = remember {
        val list = mutableListOf<JourneyStep>()
        for (i in 1..totalLevels) {
            // ज़िग-जैग पैटर्न (बाएँ, बीच, दाएँ, बीच)
            val align = when (i % 4) {
                1 -> -0.55f
                2 -> 0.0f
                3 -> 0.55f
                else -> 0.0f
            }
            list.add(JourneyStep(level = i, offsetFactor = align))
        }
        list
    }

    // टेस्टिंग के लिए वर्तमान लेवल
    var currentLevel by remember { mutableIntStateOf(4) }

    fun changeLevel(newLevel: Int) {
        currentLevel = newLevel.coerceIn(1, totalLevels)
    }

    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF121212), Color(0xFF1E1E1E))
        } else {
            listOf(Color(0xFFF0F2F5), Color(0xFFE4E6EB))
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // टॉप हेडर और लेवल जंप कंट्रोल पैनल (टेस्टिंग के लिए)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF1F242D) else Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PUZZLE JOURNEY PATH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Level $currentLevel of $totalLevels",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    // लेवल घटाने और बढ़ाने के बटन (+ / -)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { changeLevel(currentLevel - 1) },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Decrease Level",
                                tint = Color(0xFFEF4444)
                            )
                        }

                        IconButton(
                            onClick = { changeLevel(currentLevel + 1) },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF4ADE80), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Level",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ज़िग-जैग सीढ़ी नुमा पाथ (नीचे से ऊपर की तरफ चढ़ता हुआ)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 30.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                reverseLayout = true // नीचे से शुरू होकर ऊपर की तरफ जाएगा
            ) {
                items(stepsList) { step ->
                    val isCompleted = step.level < currentLevel
                    val isCurrent = step.level == currentLevel
                    val isUnlocked = step.level <= currentLevel + 2

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ((step.offsetFactor + 1f) * 65.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // सीढ़ी का एक स्टेप (क्यूब कार्ड)
                        Card(
                            modifier = Modifier
                                .size(width = 100.dp, height = 70.dp)
                                .clickable {
                                    if (isUnlocked) {
                                        changeLevel(step.level)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCurrent -> Color(0xFF22C55E) // वर्तमान लेवल (हरा)
                                    isCompleted -> Color(0xFF15803D) // पार किया हुआ लेवल (गहरा हरा)
                                    isUnlocked -> if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0) // खुला हुआ अगला लेवल
                                    else -> if (isDark) Color(0xFF1A202C) else Color(0xFFCBD5E1) // लॉक लेवल
                                }
                            ),
                            border = BorderStroke(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = if (isCurrent) Color.White else Color.Transparent
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isCurrent) 8.dp else 2.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isCompleted -> {
                                        // पूरा हो चुका स्टेप (सफेद टिक)
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    isCurrent -> {
                                        // वर्तमान स्टेप जिसपर प्यादा / स्टार है
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Current Pawn",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "${step.level}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    isUnlocked -> {
                                        // खुला हुआ नंबर
                                        Text(
                                            text = "${step.level}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF1E293B)
                                        )
                                    }
                                    else -> {
                                        // लॉक किया हुआ स्टेप
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
