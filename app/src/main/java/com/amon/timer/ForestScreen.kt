
package com.amon.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private data class Biome(
    val name: String,
    val emoji: String,
    val startLevel: Int,
    val endLevel: Int,
    val dark: Color,
    val light: Color
)

@Composable
fun ForestScreen() {
    var level by remember { mutableIntStateOf(7) }
    var xp by remember { mutableIntStateOf(65) }
    var selectedBiome by remember { mutableIntStateOf(0) }

    val biomes = remember {
        listOf(
            Biome("Forest", "🌲", 1, 50,
                Color(0xFF123D2A), Color(0xFF54A66B)),
            Biome("Desert", "🏜️", 51, 100,
                Color(0xFF704214), Color(0xFFE6AE52)),
            Biome("Ice", "❄️", 101, 150,
                Color(0xFF164E63), Color(0xFF67C9E8)),
            Biome("Volcano", "🌋", 151, 200,
                Color(0xFF641E16), Color(0xFFFF7043))
        )
    }

    val biome = biomes[selectedBiome]
    val isDark = selectedBiome == 0 || selectedBiome == 2 ||
            selectedBiome == 3

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            if (isDark) Color(0xFF101916) else Color(0xFFF3F0E7),
            if (isDark) Color(0xFF080D0B) else Color(0xFFE3E8DC)
        )
    )

    val mainText = if (isDark) Color(0xFFF1F5F2) else Color(0xFF1F2933)
    val mutedText = if (isDark) Color(0xFFB0BDB5) else Color(0xFF66736B)
    val cardColor = if (isDark) Color(0xFF18221D) else Color(0xFFFFFFFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOUR JOURNEY",
                        color = mutedText,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Focus World",
                        color = mainText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = biome.dark
                ) {
                    Text(
                        text = biome.emoji,
                        fontSize = 30.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // XP card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardColor,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENT LEVEL",
                                fontSize = 11.sp,
                                color = mutedText,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "$level",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = biome.light
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "✦ $xp XP",
                                fontSize = 20.sp,
                                color = biome.light,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Demo rewards",
                                color = mutedText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(mutedText.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((xp % 100) / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(biome.dark, biome.light)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${xp % 100}/100 XP to next reward",
                        fontSize = 12.sp,
                        color = mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Biome selector
            Text(
                text = "EXPEDITIONS",
                fontSize = 12.sp,
                color = mutedText,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                biomes.forEachIndexed { index, item ->
                    val selected = selectedBiome == index

                    Column(
                        modifier = Modifier
                            .width(112.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selected) item.dark
                                else cardColor
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) item.light
                                else mutedText.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { selectedBiome = index }
                            .padding(12.dp)
                    ) {
                        Text(item.emoji, fontSize = 25.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.name,
                            color = if (selected) Color.White else mainText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${item.startLevel}-${item.endLevel}",
                            color = if (selected) item.light else mutedText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Level path
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = biome.emoji + "  ${biome.name} Path",
                        color = mainText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Complete levels to explore further",
                        color = mutedText,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "LEVELS",
                    color = biome.light,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Simple zigzag level map
            for (rowIndex in 0 until 4) {
                val rowLevels = (0 until 3).map { col ->
                    biome.startLevel + rowIndex * 3 +
                            if (rowIndex % 2 == 0) col else (2 - col)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowLevels.forEachIndexed { position, tileLevel ->
                        val completed = tileLevel < level &&
                                selectedBiome == 0
                        val current = tileLevel == level &&
                                selectedBiome == 0
                        val unlocked = tileLevel <= level + 1 &&
                                selectedBiome == 0

                        val tileColor = when {
                            completed -> biome.dark
                            current -> biome.light
                            unlocked -> cardColor
                            else -> mutedText.copy(alpha = 0.12f)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(78.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (current) 62.dp else 54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(tileColor)
                                    .border(
                                        width = if (current) 2.dp else 1.dp,
                                        color = when {
                                            current -> biome.light
                                            completed -> biome.light.copy(alpha = 0.7f)
                                            else -> mutedText.copy(alpha = 0.25f)
                                        },
                                        shape = RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        completed -> "✓"
                                        current -> "★"
                                        unlocked -> "$tileLevel"
                                        else -> "🔒"
                                    },
                                    color = when {
                                        completed -> Color.White
                                        current -> Color(0xFF172016)
                                        else -> mainText
                                    },
                                    fontSize = if (current) 22.sp else 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Level $tileLevel",
                                fontSize = 10.sp,
                                color = mutedText
                            )
                        }
                    }
                }

                if (rowIndex < 3) {
                    Text(
                        text = if (rowIndex % 2 == 0) "↘  ·  ·  ↙"
                        else "↗  ·  ·  ↖",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = biome.light.copy(alpha = 0.8f),
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Milestone preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = biome.dark
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏆", fontSize = 34.sp)

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Next Milestone",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )

                        Text(
                            text = when {
                                level < 50 -> "Reach Level 50"
                                level < 100 -> "Reach Level 100"
                                level < 150 -> "Reach Level 150"
                                else -> "Master Explorer"
                            },
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Keep going, one session at a time.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Demo controls
            Text(
                text = "DEMO CONTROLS",
                color = mutedText,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        xp += 25
                        if (xp >= 100) {
                            xp -= 100
                            level = (level + 1).coerceAtMost(200)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = biome.dark
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+25 XP")
                }

                Button(
                    onClick = {
                        level = (level + 1).coerceAtMost(200)
                        xp = 0
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = biome.light
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Next Level", color = Color(0xFF102016))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    level = 7
                    xp = 65
                    selectedBiome = 0
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardColor,
                    contentColor = mainText
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Reset Demo")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Prototype only • Progress is not saved",
                modifier = Modifier.fillMaxWidth(),
                color = mutedText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
