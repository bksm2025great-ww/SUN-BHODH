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
import androidx.compose.material.icons.filled.Lock
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
            // ================= CLEAN HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Groups",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = textMain
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (groups.isEmpty()) {
                // ================= PREMIUM EMPTY STATE CARD (AS IN REFERENCE IMAGE) =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
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
                            // Handshake Icon Box
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(goldAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🤝", fontSize = 26.sp)
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Study with your friends",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textMain,
                                    textAlign = TextAlign.Center
                                )
                                
                                Text(
                                    text = "Study with your friends. Sometimes studying alone is hard—build a companion, track consistency, and support each other.",
                                    fontSize = 13.sp,
                                    color = textMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ================= PROMINENT ACTION BUTTONS =================
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Create Circle Button
                                Button(
                                    onClick = onCreateGroupClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = goldAccent,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = "+ Create Circle",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Join with Code Button
                                OutlinedButton(
                                    onClick = onKeyClick,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, cardBorder)
                                ) {
                                    Text(
                                        text = "🔓 Join with code",
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
                }
            } else {
                // ================= GROUPS LIST =================
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
