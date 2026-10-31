package com.amon.timer.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: StudyGroup = MockRepository.sampleGroups[0],
    userNickname: String = MockRepository.CURRENT_USER_NICKNAME,
    onBackClick: () -> Unit = {},
    onInviteClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onStartFocusClick: () -> Unit = {}
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

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showWeekPickerSheet by remember { mutableStateOf(false) }
    var selectedWeek by remember { mutableStateOf(MockRepository.weeks[0]) }

    val members = group.members.ifEmpty { MockRepository.getSampleMembers() }
    val activeMembers = members.filter { it.isCurrentlyFocusing }

    // User's own position in leaderboard
    val userIndex = members.indexOfFirst { it.nickname == userNickname || it.id == "14" }.let {
        if (it >= 0) it else 13
    }
    val userRank = userIndex + 1

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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textMain
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Group Settings",
                            tint = textMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onStartFocusClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Focus",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = bgColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Header item
                item {
                    GroupHeaderSection(
                        group = group,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textMain = textMain,
                        textMuted = textMuted,
                        accentColor = accentColor,
                        accentSubtle = accentSubtle,
                        onInviteClick = onInviteClick
                    )
                }

                // Sticky Tabs: Leaderboard vs Active
                item {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = cardBg,
                        contentColor = accentColor
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            modifier = Modifier.height(48.dp),
                            text = {
                                Text(
                                    text = "Leaderboard",
                                    color = if (selectedTabIndex == 0) accentColor else textMuted,
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            modifier = Modifier.height(48.dp),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(focusingGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Active (${activeMembers.size})",
                                        color = if (selectedTabIndex == 1) accentColor else textMuted,
                                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        )
                    }
                }

                if (selectedTabIndex == 0) {
                    // LEADERBOARD TAB
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, accentColor),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showWeekPickerSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Select Week",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedWeek.label} (${selectedWeek.dateRange})",
                                        color = textMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Weekly Reset: Mon 00:00",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    itemsIndexed(members) { index, member ->
                        val rank = index + 1
                        val isCurrentUser = rank == userRank || member.nickname == userNickname

                        LeaderboardRow(
                            rank = rank,
                            member = member,
                            isCurrentUser = isCurrentUser,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            accentSubtle = accentSubtle,
                            focusingGreen = focusingGreen,
                            isDark = isDark
                        )
                    }
                } else {
                    // ACTIVE TAB
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Members studying right now. Join them to boost your focus streak!",
                                color = textMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    itemsIndexed(activeMembers) { index, member ->
                        ActiveMemberRow(
                            member = member,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            focusingGreen = focusingGreen
                        )
                    }
                }
            }

            // Floating pill "Scroll to your position"
            if (selectedTabIndex == 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(userIndex + 2)
                            }
                        },
                    color = accentColor,
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isDark) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scroll to your position (#$userRank)",
                            color = if (isDark) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Week Picker Bottom Sheet
            if (showWeekPickerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showWeekPickerSheet = false },
                    containerColor = cardBg,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = cardBorder) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Select Leaderboard Week",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        MockRepository.weeks.forEach { week ->
                            val isSelected = week.id == selectedWeek.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else cardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedWeek = week
                                        showWeekPickerSheet = false
                                    },
                                color = if (isSelected) accentSubtle else bgColor
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = week.label,
                                            color = if (isSelected) accentColor else textMain,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = week.dateRange,
                                            color = textMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeaderSection(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    onInviteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accentSubtle)
                        .border(2.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.name.take(2).uppercase(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = cardBorder.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (group.isPublic) Icons.Default.Share else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = textMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (group.isPublic) "Public" else "Private",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${group.memberCount} members",
                            color = textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = accentSubtle,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = group.category,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onInviteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Invite Aspirants",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    rank: Int,
    member: GroupMember,
    isCurrentUser: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    focusingGreen: Color,
    isDark: Boolean
) {
    val rowBorder = when {
        isCurrentUser -> BorderStroke(1.5.dp, accentColor)
        rank == 1 -> BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.65f)) // Gold
        rank == 2 -> BorderStroke(1.5.dp, Color(0xFF94A3B8).copy(alpha = 0.65f)) // Silver
        rank == 3 -> BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.65f)) // Bronze
        else -> BorderStroke(1.dp, cardBorder)
    }

    val rowBackground = when {
        isCurrentUser -> accentSubtle
        rank == 1 -> Color(0xFFF59E0B).copy(alpha = 0.06f)
        rank == 2 -> Color(0xFF94A3B8).copy(alpha = 0.06f)
        rank == 3 -> Color(0xFFD97706).copy(alpha = 0.06f)
        else -> cardBg
    }

    val rankTextColor = when {
        isCurrentUser -> accentColor
        rank == 1 -> Color(0xFFF59E0B)
        rank == 2 -> Color(0xFF94A3B8)
        rank == 3 -> Color(0xFFD97706)
        else -> textMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
        border = rowBorder,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = rowBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = rankTextColor,
                    fontWeight = if (rank <= 3 || isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.nickname,
                        color = if (isCurrentUser) accentColor else textMain,
                        fontWeight = if (isCurrentUser || rank <= 3) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = accentColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "YOU",
                                color = if (isDark) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (member.studyTimeMinutes == 0) member.lastActive else "Last active: ${member.lastActive}",
                    color = textMuted,
                    fontSize = 11.sp
                )
            }

            val hours = member.studyTimeMinutes / 60
            val mins = member.studyTimeMinutes % 60
            val formattedTime = if (member.studyTimeMinutes == 0) "0h 00m" else "${hours}h ${mins.toString().padStart(2, '0')}m"

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedTime,
                    color = if (isCurrentUser) accentColor else textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (member.isCurrentlyFocusing) {
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveMemberRow(
    member: GroupMember,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    focusingGreen: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
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
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(focusingGreen)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.nickname,
                    color = textMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = member.currentSubject ?: "Deep Focus Sprint",
                    color = textMuted,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = focusingGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(focusingGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Studying now",
                        color = focusingGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
