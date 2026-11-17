package com.amon.timer.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.CloudSyncManager
import com.amon.timer.ThemeManager
import com.amon.timer.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: StudyGroup = StudyGroup(id = "default", name = "Study Group"),
    userNickname: String = "",
    onBackClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onJoinClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val accentSubtle = accentColor.copy(alpha = 0.16f)
    val focusingGreen = Color(0xFF22C55E)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)

    var showWeekPickerSheet by remember { mutableStateOf(false) }
    val weekSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availableWeeks = remember {
        listOf(
            WeekPeriod("w_curr", "This Week", "Current", true),
            WeekPeriod("w_prev", "Last Week", "Previous", false)
        )
    }
    var selectedWeek by remember { mutableStateOf(availableWeeks[0]) }

    val cachedMembers: List<GroupMember> = remember(group.id) {
        GroupCacheManager.getCachedLeaderboard(context, group.id)
    }

    var leaderboardMembers: List<GroupMember> by remember {
        mutableStateOf(if (group.members.isNotEmpty()) group.members else cachedMembers)
    }

    LaunchedEffect(selectedWeek.id, group.id) {
        coroutineScope.launch(Dispatchers.IO) {
            val freshMembers: List<GroupMember> = CloudSyncManager.fetchGroupLeaderboard(
                context = context,
                groupId = group.id,
                isCurrentWeek = selectedWeek.isCurrent
            )
            if (freshMembers.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    leaderboardMembers = freshMembers
                }
                if (selectedWeek.isCurrent) {
                    GroupCacheManager.saveLeaderboard(context, group.id, freshMembers)
                }
            }
        }
    }

    val displayMemberCount = remember(leaderboardMembers, group.memberCount) {
        if (leaderboardMembers.isNotEmpty()) leaderboardMembers.size else maxOf(group.memberCount, 1)
    }

    // 📋 Generates and copies the unique invite code to clipboard
    val copyInviteCodeToClipboard = {
        val inviteCode = group.inviteCode.ifBlank { "AMON-GROUP" }
        val clip = ClipData.newPlainText("Invite Code", inviteCode)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Invite code copied: $inviteCode", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group.name,
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textMain
                        )
                    }
                },
                actions = {
                    // Invite Button -> Copies Code directly
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentSubtle,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                copyInviteCodeToClipboard()
                                onInviteClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Invite",
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Invite",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = textMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.name.take(2).uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = group.name,
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$displayMemberCount members · Private Study Circle",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Week Selector Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showWeekPickerSheet = true }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedWeek.label,
                        color = textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "Reset: Mon 00:00",
                    color = textMuted.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            if (leaderboardMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No study sessions yet for ${selectedWeek.label.lowercase()}.",
                        color = textMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = leaderboardMembers,
                        key = { _, m -> m.id }
                    ) { index, member ->
                        LeaderboardRow(
                            rank = index + 1,
                            member = member,
                            isCurrentUser = userNickname.isNotBlank() && member.nickname.equals(userNickname, ignoreCase = true),
                            textMain = textMain,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            accentSubtle = accentSubtle,
                            focusingGreen = focusingGreen,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }

    if (showWeekPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeekPickerSheet = false },
            sheetState = weekSheetState,
            containerColor = if (isDark) Color(0xFF1E1E24) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Select Leaderboard Week",
                    color = textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                availableWeeks.forEach { week ->
                    val isSelected = week.id == selectedWeek.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedWeek = week
                                showWeekPickerSheet = false
                            },
                        color = if (isSelected) accentSubtle else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = week.label,
                                color = if (isSelected) accentColor else textMain,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    rank: Int,
    member: GroupMember,
    isCurrentUser: Boolean,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    focusingGreen: Color,
    isDark: Boolean
) {
    val colorGold = Color(0xFFF59E0B)
    val colorSilver = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)
    val colorBronze = Color(0xFFD97706)

    val itemContentColor = when (rank) {
        1 -> colorGold
        2 -> colorSilver
        3 -> colorBronze
        else -> textMain
    }

    val rankTextColor = when (rank) {
        1 -> colorGold
        2 -> colorSilver
        3 -> colorBronze
        else -> textMuted
    }

    val rowBackground = if (isCurrentUser) accentSubtle else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "#$rank",
                color = rankTextColor,
                fontWeight = if (rank <= 3) FontWeight.Black else FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 📝 Long Nickname Handling with Ellipsis & Weight constraint
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = member.nickname,
                    color = itemContentColor,
                    fontWeight = if (rank <= 3 || isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "You",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥 ${member.streak} Days Streak",
                    color = Color(0xFFFF9800),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val hours = member.studyTimeMinutes / 60
        val mins = member.studyTimeMinutes % 60
        val formattedTime = if (member.studyTimeMinutes == 0) "0h 00m" else "${hours}h ${mins.toString().padStart(2, '0')}m"

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = formattedTime,
                color = itemContentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            if (member.isCurrentlyFocusing) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(focusingGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live",
                        color = focusingGreen,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
