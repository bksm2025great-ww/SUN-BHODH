package com.amon.timer.groups

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    groups: List<StudyGroup> = MockRepository.sampleGroups,
    userNickname: String = MockRepository.CURRENT_USER_NICKNAME,
    onSearchClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onGroupClick: (StudyGroup) -> Unit = {},
    onViewAllSuggested: () -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    onJoinGroup: (String) -> Unit = {},
    onNicknameClick: () -> Unit = {}
) {
    // 🎨 सीधे ThemeManager से कलर्स
    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val focusingGreen = Color(0xFF22C55E)

    val yourGroups = groups.filter { it.isJoined }
    val suggestedGroups = groups.filter { !it.isJoined }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SB",
                                color = if (isDark) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sun Bodh",
                                color = textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = userNickname,
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onNicknameClick() }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search & Explore",
                            tint = textMain
                        )
                    }
                    IconButton(
                        onClick = onCreateGroupClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Group",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header: Your Groups
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Groups (${yourGroups.size})",
                        color = textMain,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pinned on top",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Your groups cards sorted: pinned first
            items(yourGroups.sortedByDescending { it.isPinned }) { group ->
                GroupCard(
                    group = group,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = accentColor,
                    accentSubtle = accentSubtle,
                    focusingGreen = focusingGreen,
                    onCardClick = { onGroupClick(group) },
                    onPinToggle = { onTogglePin(group.id) }
                )
            }

            // Section Header: Suggested Groups
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Suggested Groups",
                        color = textMain,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = onViewAllSuggested,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "View all",
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Suggested groups list
            items(suggestedGroups) { group ->
                SuggestedGroupCard(
                    group = group,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMain = textMain,
                    textMuted = textMuted,
                    accentColor = accentColor,
                    accentSubtle = accentSubtle,
                    isDark = isDark,
                    onJoinClick = { onJoinGroup(group.id) },
                    onCardClick = { onGroupClick(group) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun GroupCard(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    focusingGreen: Color,
    onCardClick: () -> Unit,
    onPinToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentSubtle)
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.name.take(2).uppercase(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            color = textMain,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        if (group.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = accentSubtle,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "GLOBAL",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${group.memberCount} members · ${if (group.isPublic) "Public" else "Private"}",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }

                // Pin toggle button with 48dp minimum touch target
                IconButton(
                    onClick = onPinToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = if (group.isPinned) "Unpin Group" else "Pin Group",
                        tint = if (group.isPinned) accentColor else textMuted.copy(alpha = 0.4f)
                    )
                }
            }

            // Strip: "N focusing now" with green dot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBorder.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(focusingGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${group.focusingCount} focusing now",
                        color = focusingGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = group.category,
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedGroupCard(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    isDark: Boolean,
    onJoinClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentSubtle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.take(2).uppercase(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    color = textMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = "${group.memberCount} members · ${group.category}",
                    color = textMuted,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onJoinClick,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = "Join",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
