package com.amon.timer.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsSheet(
    group: StudyGroup = MockRepository.sampleGroups[0],
    isAdmin: Boolean = true,
    onDismiss: () -> Unit = {},
    onInvite: () -> Unit = {},
    onRemoveMember: (String) -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit = {},
    onReportGroup: () -> Unit = {},
    onLeaveGroup: () -> Unit = {}
) {
    // 🎨 सीधे ThemeManager से कलर्स
    val isDark = ThemeManager.isDarkTheme.value
    val cardBg = ThemeManager.getCardColor()
    val bgColor = ThemeManager.getBackgroundColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val destructiveColor = Color(0xFFEF4444) // Semantic Red

    var notificationsOn by remember { mutableStateOf(group.notificationsEnabled) }
    var showMembersList by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = cardBorder) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Group Settings",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = group.name,
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action 1: Invite Aspirants
            SettingsActionRow(
                icon = Icons.Default.Share,
                title = "Invite Aspirants",
                subtitle = "Share group link or invite code",
                tint = accentColor,
                cardBorder = cardBorder,
                bgColor = bgColor,
                textMain = textMain,
                textMuted = textMuted,
                onClick = onInvite
            )

            // Action 2: Members management
            SettingsActionRow(
                icon = Icons.Default.Person,
                title = "Members (${group.members.size.ifZero(MockRepository.getSampleMembers().size)})",
                subtitle = if (isAdmin) "Admin control: Tap to manage and remove" else "View all participants",
                tint = textMain,
                cardBorder = cardBorder,
                bgColor = bgColor,
                textMain = textMain,
                textMuted = textMuted,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = textMuted
                    )
                },
                onClick = { showMembersList = !showMembersList }
            )

            // Expandable Members List
            if (showMembersList) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    color = bgColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        val membersToDisplay = group.members.ifEmpty { MockRepository.getSampleMembers() }
                        membersToDisplay.take(6).forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(accentSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.nickname.take(2).uppercase(),
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = member.nickname,
                                            color = textMain,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (member.role == MemberRole.ADMIN) "Group Admin" else "Member",
                                            color = textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isAdmin && member.role != MemberRole.ADMIN) {
                                    TextButton(
                                        onClick = { onRemoveMember(member.id) },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = destructiveColor
                                        )
                                    ) {
                                        Text(
                                            text = "Remove",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action 3: Notifications Switch
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                color = bgColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Study Notifications",
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Daily focus reminders & milestone alerts",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = notificationsOn,
                        onCheckedChange = {
                            notificationsOn = it
                            onToggleNotifications(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDark) Color.Black else Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = textMuted,
                            uncheckedTrackColor = cardBorder
                        )
                    )
                }
            }

            // Action 4: Report Group
            SettingsActionRow(
                icon = Icons.Default.Warning,
                title = "Report Group",
                subtitle = "Flag spam or inappropriate content",
                tint = textMuted,
                cardBorder = cardBorder,
                bgColor = bgColor,
                textMain = textMain,
                textMuted = textMuted,
                onClick = onReportGroup
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action 5: Leave Group (Red)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, destructiveColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onLeaveGroup() },
                color = destructiveColor.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Leave Group",
                        tint = destructiveColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Leave Group",
                        color = destructiveColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun Int.ifZero(fallback: Int): Int = if (this == 0) fallback else this

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    cardBorder: Color,
    bgColor: Color,
    textMain: Color,
    textMuted: Color,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            trailingContent?.invoke()
        }
    }
}
