package com.amon.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

// एकदम बेसिक लेवल का डेटा स्ट्रक्चर
data class SimpleLevelNode(
    val level: Int,
    val title: String
)

@Composable
fun ForestScreen() {
    val isDark = ThemeManager.isDarkTheme.value

    // शुरुआती डेमो के लिए 10 आसान लेवल्स
    val levels = remember {
        listOf(
            SimpleLevelNode(1, "Forest Tier - Start"),
            SimpleLevelNode(2, "Forest Tier - Step 2"),
            SimpleLevelNode(3, "Forest Tier - Step 3"),
            SimpleLevelNode(4, "Forest Tier - Step 4"),
            SimpleLevelNode(5, "Forest Tier - Step 5"),
            SimpleLevelNode(6, "Wooden Cabin - 1"),
            SimpleLevelNode(7, "Wooden Cabin - 2"),
            SimpleLevelNode(8, "Silver Ice - 1"),
            SimpleLevelNode(9, "Ancient Desert - 1"),
            SimpleLevelNode(10, "Grandmaster Gold")
        )
    }

    // टेस्टिंग के लिए करंट लेवल
    var currentLevel by remember { mutableIntStateOf(3) }

    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF0B0F17), Color(0xFF151C28))
        } else {
            listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))
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
            // टॉप हेडर और टेस्ट कंट्रोल्स
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) Color(0xFF161B26) else Color.White,
                shadowElevation = 4.dp
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
                            text = "BASIC DEMO JOURNEY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Current Level: $currentLevel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }

                    // लेवल घटाने-बढ़ाने के बटन
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentLevel > 1) currentLevel-- },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF4ADE80).copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Decrease",
                                tint = Color(0xFF4ADE80)
                            )
                        }

                        IconButton(
                            onClick = { if (currentLevel < 10) currentLevel++ },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF4ADE80), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // सिंपल लिस्ट व्यू (डेमो के लिए)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(levels) { item ->
                    val isCompleted = item.level < currentLevel
                    val isCurrent = item.level == currentLevel
                    val isUnlocked = item.level <= currentLevel + 1

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCurrent -> Color(0xFF16A34A).copy(alpha = 0.25f)
                                isCompleted -> if (isDark) Color(0xFF1E2638) else Color.White
                                else -> if (isDark) Color(0xFF111622) else Color(0xFFE2E8F0)
                            }
                        ),
                        border = BorderStroke(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = if (isCurrent) Color(0xFF4ADE80) else Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // आइकॉन बॉक्स
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        when {
                                            isCurrent -> Color(0xFF4ADE80)
                                            isCompleted -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.2f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCompleted -> Icons.Default.Check
                                        isCurrent -> Icons.Default.Star
                                        isUnlocked -> Icons.Default.Add
                                        else -> Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.Black else if (isCompleted) Color(0xFF4ADE80) else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Level ${item.level}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }

                            // स्टेटस टेक्स्ट
                            Text(
                                text = when {
                                    isCurrent -> "CURRENT"
                                    isCompleted -> "DONE"
                                    else -> "LOCKED"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color(0xFF4ADE80) else if (isCompleted) Color.Gray else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
