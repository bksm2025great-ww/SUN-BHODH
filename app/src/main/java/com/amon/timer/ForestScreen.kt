package com.amon.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// 6 महा-थीम्स (हर 10 लेवल पर बदलेंगी)
enum class JourneyTheme(val title: String, val primaryColor: Color, val secondaryColor: Color, val tileColor: Color) {
    FOREST("1. Forest Sanctuary", Color(0xFF16A34A), Color(0xFF15803D), Color(0xFF22C55E)),
    WOOD("2. Wooden Cabin", Color(0xFFB45309), Color(0xFF92400E), Color(0xFFD97706)),
    ICE("3. Silver Ice Realm", Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF38BDF8)),
    DESERT("4. Ancient Desert", Color(0xFFD97706), Color(0xFFB45309), Color(0xFFFBBF24)),
    VOLCANO("5. Magma Volcano", Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFFEF4444)),
    GOLD("6. Grandmaster Gold", Color(0xFFCA8A04), Color(0xFFA16207), Color(0xFFFACC15))
}

data class JourneyNode(
    val level: Int,
    val theme: JourneyTheme,
    val alignment: Float // -1.0 (Left), 0.0 (Center), 1.0 (Right) के लिए जिग-जैग ऑफसेट
)

@Composable
fun ForestScreen() {
    val isDark = ThemeManager.isDarkTheme.value

    // कुल 60 लेवल्स जनरेट करना (10-10 के बंडल में 6 थीम)
    val totalLevels = 60
    val nodes = remember {
        val list = mutableListOf<JourneyNode>()
        for (i in 1..totalLevels) {
            val themeIndex = ((i - 1) / 10).coerceIn(0, 5)
            val theme = JourneyTheme.entries[themeIndex]
            // जिग-जैग पैटर्न: बाएं, बीच, दाएं, बीच
            val align = when (i % 4) {
                1 -> -0.65f
                2 -> 0.0f
                3 -> 0.65f
                else -> 0.0f
            }
            list.add(JourneyNode(level = i, theme = theme, alignment = align))
        }
        list
    }

    // यूज़र का वर्तमान लेवल (टेस्टिंग के लिए 3 से शुरू)
    var currentLevel by remember { mutableIntStateOf(3) }
    var unlockedTierForPopup by remember { mutableIntStateOf(0) }

    val currentTheme = ((currentLevel - 1) / 10).let { JourneyTheme.entries[it.coerceIn(0, 5)] }

    // लेवल बदलने का फंक्शन (डायरेक्ट क्लिक या + / - बटन से)
    fun navigateToLevel(targetLevel: Int) {
        val clamped = targetLevel.coerceIn(1, totalLevels)
        val oldLevel = currentLevel
        currentLevel = clamped

        // अगर कोई नया बंडल (जैसे 10, 20, 30...) पार किया, तो बधाई पॉप-अप दिखाओ
        if (clamped > oldLevel && clamped % 10 == 0) {
            unlockedTierForPopup = clamped
        }
    }

    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF0B0F17), Color(0xFF151C28), Color(0xFF0B0F17))
        } else {
            listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFF1F5F9))
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            // ════════════════════════════════════════════════════════
            // 1. टॉप हेडर (थीम का नाम और क्विक लेवल जंप पैनल)
            // ════════════════════════════════════════════════════════
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF161B26) else Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PUZZLE JOURNEY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.tileColor,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = currentTheme.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    // + / - लेवल कंट्रोल (टेस्टिंग के लिए)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { navigateToLevel(currentLevel - 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(currentTheme.tileColor.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Prev Level",
                                tint = currentTheme.tileColor
                            )
                        }

                        Text(
                            text = "Lvl $currentLevel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { navigateToLevel(currentLevel + 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(currentTheme.tileColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Next Level",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════
            // 2. 3D जिग-जैग पाथ (CHESS.COM STYLE PUZZLE JOURNEY)
            // ════════════════════════════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 30.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                reverseLayout = true // नीचे से ऊपर की तरफ रास्ता चढ़ेगा
            ) {
                items(nodes) { node ->
                    val isCompleted = node.level < currentLevel
                    val isCurrent = node.level == currentLevel
                    val isUnlocked = node.level <= currentLevel + 2 // आगे के 2 लेवल एक्सप्लोर करने के लिए खुले हैं

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ((node.alignment + 1f) * 60.dp)), // जिग-जैग पोजीशनिंग
                        contentAlignment = Alignment.Center
                    ) {
                        // साइड में डेकोरेशन / शॉप पीस (बिना किसी इमोजी के प्योर वेक्टर ब्लॉक)
                        if (node.level % 3 == 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = if (node.alignment < 0) 90.dp else -90.dp)
                                    .size(36.dp)
                                    .background(node.theme.primaryColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .border(1.dp, node.theme.tileColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(node.theme.tileColor, CircleShape)
                                )
                            }
                        }

                        // मुख्य 3D क्यूब / स्टेप बटन
                        Card(
                            modifier = Modifier
                                .size(width = 90.dp, height = 75.dp)
                                .clickable {
                                    // यूज़र किसी भी खुले या पिछले लेवल पर क्लिक करके डायरेक्ट जा सकता है
                                    if (isUnlocked) {
                                        navigateToLevel(node.level)
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCurrent -> node.theme.tileColor
                                    isCompleted -> node.theme.secondaryColor
                                    else -> if (isDark) Color(0xFF1E2638) else Color(0xFFCBD5E1)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isCurrent) 3.dp else 1.5.dp,
                                color = if (isCurrent) Color.White else node.theme.tileColor
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 8.dp else 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    when {
                                        isCompleted -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        isCurrent -> {
                                            // वर्तमान लेवल पर घूमने वाला/चमकता प्यादा (शतरंज का मोहरा)
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.size(20.dp)) {
                                                    drawRect(
                                                        color = node.theme.tileColor,
                                                        topLeft = Offset(size.width * 0.35f, size.height * 0.2f),
                                                        size = Size(size.width * 0.3f, size.height * 0.6f),
                                                        cornerRadius = CornerRadius(4f, 4f)
                                                    )
                                                }
                                            }
                                        }
                                        isUnlocked -> {
                                            Text(
                                                text = "${node.level}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDark) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                        else -> {
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

        // ════════════════════════════════════════════════════════
        // 3. टियर / थीम कंप्लीशन सेलिब्रेशन पॉप-अप
        // ════════════════════════════════════════════════════════
        if (unlockedTierForPopup > 0) {
            val tierName = when (unlockedTierForPopup) {
                10 -> "Forest Tier Completed!"
                20 -> "Wooden Cabin Tier Completed!"
                30 -> "Silver Ice Tier Completed!"
                40 -> "Ancient Desert Tier Completed!"
                50 -> "Magma Volcano Tier Completed!"
                60 -> "Grandmaster Gold Tier Completed!"
                else -> "Milestone Reached!"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { unlockedTierForPopup = 0 },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B26)),
                    border = BorderStroke(2.5.dp, Color(0xFFFFD700))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "MAGNIFICENT!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tierName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { unlockedTierForPopup = 0 },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color(0xFF0F172A))
                        ) {
                            Text("CONTINUE JOURNEY", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
