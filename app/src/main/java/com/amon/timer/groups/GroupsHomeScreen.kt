package com.amon.timer.groups

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager

@Composable
fun GroupsHomeScreen(
    groups: List<StudyGroup>,
    userNickname: String,
    onCreateGroupClick: () -> Unit,
    onKeyClick: () -> Unit,
    onGroupClick: (StudyGroup) -> Unit,
    onNicknameClick: () -> Unit,
    onTogglePin: (String) -> Unit,
    onJoinGroup: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkTheme.value
    val screenBg = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldAccent = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0xFFCBD5E1)
    val slimBorderStroke = 0.8.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= 4. CLEAN HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Groups", // 👈 साफ़ टाइटल
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Key / Join via code button
                    IconButton(
                        onClick = onKeyClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(slimBorderStroke, cardBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Join with Code",
                            tint = goldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Create group button
                    IconButton(
                        onClick = onCreateGroupClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(slimBorderStroke, cardBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Group",
                            tint = goldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (groups.isEmpty()) {
                // ================= 2, 3 & 5. CLEAN EMPTY STATE =================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(slimBorderStroke, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(goldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 20.sp)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Build Your Inner Study Circle",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMain,
                                textAlign = TextAlign.Center
                            )
                            
                            // 5. मिनिमलिस्ट लाइन
                            Text(
                                text = "Study together. Stay consistent.",
                                fontSize = 13.sp,
                                color = textMuted,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 2. सारा नकली डेटा (Alex 14 Days Streak) हटा दिया गया है

                        Spacer(modifier = Modifier.height(4.dp))

                        // ================= 1 & 5. BUTTONS (SINGLE LINE, NO OUTREACH ERROR) =================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Create Circle Button
                            Button(
                                onClick = onCreateGroupClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = goldAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = "Create Circle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Secondary Join Button (OutlinedButtonDefaults एरर पूरी तरह फिक्स)
                            OutlinedButton(
                                onClick = onKeyClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Text(
                                    text = "Join",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = textMain
                                )
                            }
                        }
                    }
                }
            } else {
                // ग्रुप्स लिस्ट दिखने पर
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groups) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGroupClick(group) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(slimBorderStroke, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(goldAccent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = group.name.take(1).uppercase(),
                                            color = goldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = group.name,
                                            color = textMain,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${group.memberCount} members • ${group.focusingCount} focusing",
                                            color = textMuted,
                                            fontSize = 12.sp
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
