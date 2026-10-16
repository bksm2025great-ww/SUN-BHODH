package com.amon.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// शतरंज के हर लेवल का डेटा मॉडल
data class ChessRankTier(
    val level: Int,
    val title: String,
    val hindiTitle: String,
    val symbol: String,
    val minElo: Int,
    val maxElo: Int,
    val accentColor: Color
)

@Composable
fun ForestScreen() {
    val isDark = ThemeManager.isDarkTheme.value

    // ♟️ 6 शतरंज रैंक्स की लिस्ट
    val ranks = remember {
        listOf(
            ChessRankTier(1, "Pawn", "प्यादा", "♟", 400, 800, Color(0xFF94A3B8)),
            ChessRankTier(2, "Knight", "घोड़ा", "♞", 800, 1200, Color(0xFF4ADE80)),
            ChessRankTier(3, "Bishop", "ऊंट", "♝", 1200, 1600, Color(0xFF38BDF8)),
            ChessRankTier(4, "Rook", "हाथी", "♜", 1600, 2000, Color(0xFFA855F7)),
            ChessRankTier(5, "Queen", "वज़ीर", "♛", 2000, 2400, Color(0xFFF43F5E)),
            ChessRankTier(6, "Grandmaster", "राजा (King)", "♚", 2400, 3000, Color(0xFFFACC15))
        )
    }

    // 🎯 यूज़र का ELO स्कोर (टेस्टिंग के लिए 450 से शुरू)
    var currentElo by remember { mutableIntStateOf(450) }
    var lastChangeText by remember { mutableStateOf("Ready to Focus!") }
    var lastChangeColor by remember { mutableStateOf(Color(0xFF94A3B8)) }

    // नंबर बदलने का स्मूथ एनिमेशन
    val animatedElo by animateIntAsState(
        targetValue = currentElo,
        animationSpec = tween(durationMillis = 500),
        label = "eloAnim"
    )

    // अभी की रैंक पता करना
    val currentRank = ranks.lastOrNull { currentElo >= it.minElo } ?: ranks.first()
    val nextRank = ranks.firstOrNull { it.minElo > currentElo }

    // प्रोग्रेस बार का प्रतिशत (0.0 से 1.0)
    val rawProgress = if (nextRank != null) {
        (currentElo - currentRank.minElo).toFloat() / (nextRank.minElo - currentRank.minElo).toFloat()
    } else {
        1f
    }.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 600),
        label = "progressAnim"
    )

    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF0B0F17), Color(0xFF151C28))
        } else {
            listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ════════════════════════════════════════════════════════
        // 1. ऊपरी मुख्य रैंक कार्ड (HERO ELO CARD)
        // ════════════════════════════════════════════════════════
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E2638) else Color.White
                ),
                border = BorderStroke(1.5.dp, currentRank.accentColor.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FOCUS ARENA RATING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentRank.accentColor,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // चमकता हुआ गोल शतरंज बैज
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(currentRank.accentColor.copy(alpha = 0.15f))
                            .border(3.dp, currentRank.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentRank.symbol,
                            fontSize = 50.sp,
                            color = currentRank.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // रैंक का नाम
                    Text(
                        text = "Level ${currentRank.level}: ${currentRank.title} (${currentRank.hindiTitle})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    // बड़ा ELO नंबर
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "$animatedElo",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = currentRank.accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ELO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // लास्ट एक्शन मैसेज (+40 या -15 का फीडबैक)
                    Text(
                        text = lastChangeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = lastChangeColor,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    // प्रोग्रेस बार
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(50)),
                        color = currentRank.accentColor,
                        trackColor = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // अगली रैंक की जानकारी
                    val nextInfo = if (nextRank != null) {
                        "अगली रैंक (${nextRank.symbol} ${nextRank.title}) के लिए ${nextRank.minElo - currentElo} ELO और चाहिए"
                    } else {
                        "👑 आप सर्वोच्च Grandmaster रैंक पर हैं!"
                    }
                    Text(
                        text = nextInfo,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ════════════════════════════════════════════════════════
        // 2. लाइव डोपामिन टेस्ट कंसोल (SIMULATOR BUTTONS)
        // ════════════════════════════════════════════════════════
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF151C28) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🧪 टेस्टिंग पैनल (दबाकर डोपामिन और लेवल चेक करें):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // जीत का बटन (+25 ELO)
                        Button(
                            onClick = {
                                currentElo = (currentElo + 25).coerceAtMost(3000)
                                lastChangeText = "🔥 +25 ELO! शानदार फोकस सेशन पूरा!"
                                lastChangeColor = Color(0xFF4ADE80)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("+25 Win", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // हार मानने का बटन (-10 ELO)
                        Button(
                            onClick = {
                                currentElo = (currentElo - 10).coerceAtLeast(100)
                                lastChangeText = "⚠️ -10 ELO! सेशन अधूरा छोड़ने का जुर्माना!"
                                lastChangeColor = Color(0xFFF43F5E)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("-10 GiveUp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // बड़ा जंप बटन (+200 ELO रैंक टेस्ट के लिए)
                        Button(
                            onClick = {
                                currentElo = if (currentElo >= 2500) 450 else currentElo + 200
                                lastChangeText = "⚡ +200 ELO Boost (रैंक टेस्ट)!"
                                lastChangeColor = Color(0xFFFACC15)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("+200 Jump", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════
        // 3. स्टेप-बाय-स्टेप रैंक सीढ़ी (THE 6 CHESS LEVELS)
        // ════════════════════════════════════════════════════════
        item {
            Text(
                text = "🏆 शतरंज रैंक पथ (Rank Progression)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        itemsIndexed(ranks) { _, tier ->
            val isUnlocked = currentElo >= tier.minElo
            val isCurrent = currentRank.level == tier.level

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrent -> tier.accentColor.copy(alpha = 0.16f)
                        isUnlocked -> if (isDark) Color(0xFF1E2638) else Color.White
                        else -> if (isDark) Color(0xFF111622) else Color(0xFFE2E8F0).copy(alpha = 0.6f)
                    }
                ),
                border = BorderStroke(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = when {
                        isCurrent -> tier.accentColor
                        isUnlocked -> tier.accentColor.copy(alpha = 0.4f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // बाईं तरफ शतरंज का मोहरा आइकॉन
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) tier.accentColor.copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUnlocked) tier.symbol else "🔒",
                            fontSize = 26.sp,
                            color = if (isUnlocked) tier.accentColor else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // बीच में रैंक का नाम और ELO रेंज
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level ${tier.level}: ${tier.title} (${tier.hindiTitle})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) {
                                if (isDark) Color.White else Color.Black
                            } else Color.Gray
                        )
                        Text(
                            text = "आवश्यक रेटिंग: ${tier.minElo} ELO",
                            fontSize = 12.sp,
                            color = if (isCurrent) tier.accentColor else Color.Gray
                        )
                    }

                    // दाईं तरफ स्टेटस बैज (Current / Unlocked / Locked)
                    val statusBadge = when {
                        isCurrent -> "🔥 CURRENT"
                        isUnlocked -> "✅ PAR"
                        else -> "LOCKED"
                    }
                    Text(
                        text = statusBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isCurrent -> tier.accentColor
                            isUnlocked -> Color(0xFF4ADE80)
                            else -> Color.Gray
                        }
                    )
                }
            }
        }
    }
}
