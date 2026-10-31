package com.amon.timer.groups

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager

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
    val context = LocalContext.current

    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val focusingGreen = Color(0xFF22C55E)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showWeekPickerSheet by remember { mutableStateOf(false) }

    // ⚡ बॉटम शीट को एक ही बार में पूरा खोलने का स्टेट
    val weekSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val availableWeeks = remember {
        listOf(
            WeekPeriod("w_curr", "This Week", "Current", true),
            WeekPeriod("w_prev", "Last Week", "Previous", false)
        )
    }
    var selectedWeek by remember { mutableStateOf(availableWeeks[0]) }

    val members = group.members.ifEmpty { MockRepository.getSampleMembers() }
    val activeMembers = members.filter { it.isCurrentlyFocusing }

    // 🚀 असली शेयर फंक्शन (WhatsApp / Telegram / Messages)
    val triggerShareIntent = {
        val inviteCode = group.inviteCode ?: "AMON-GLOBAL"
        val shareMessage = """
            Hey! Join my study group "${group.name}" on Sun Bodh to track study hours together.
            
            Group Code: $inviteCode
            Join Link: https://sunbodh.app/join/$inviteCode
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Invite Aspirants to ${group.name}")
        context.startActivity(shareIntent)
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
                    // 🔗 स्लीक और वर्किंग Invite चिप
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentSubtle,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                triggerShareIntent()
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = onStartFocusClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isDark) Color.Black else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Focus",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 🌿 अल्ट्रा-कॉम्पैक्ट ग्रुप हेडर
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentSubtle)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.name.take(2).uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = group.name,
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = "${group.memberCount} members · ${if (group.isPublic) "Public" else "Private"}",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (group.category.isNotBlank() && group.category != "All") {
                    Surface(
                        color = cardBorder.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = group.category,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 🎯 कॉम्पैक्ट टैब्स
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = bgColor,
                contentColor = accentColor
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    modifier = Modifier.height(38.dp),
                    text = {
                        Text(
                            text = "Leaderboard",
                            color = if (selectedTabIndex == 0) accentColor else textMuted,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    modifier = Modifier.height(38.dp),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(focusingGreen)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Active (${activeMembers.size})",
                                color = if (selectedTabIndex == 1) accentColor else textMuted,
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                )
            }

            if (selectedTabIndex == 0) {
                // 🗓️ हफ़्ते की लाइन
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

                // 🏆 लीडरबोर्ड लिस्ट
                if (members.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study sessions yet this week.\nStart focus to claim #1 rank!",
                            color = textMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        itemsIndexed(members) { index, member ->
                            LeaderboardRow(
                                rank = index + 1,
                                member = member,
                                isCurrentUser = member.nickname == userNickname,
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
                    }
                }
            } else {
                // 🟢 एक्टिव मेंबर्स लिस्ट
                if (activeMembers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No one is studying right now.\nBe the first to start!",
                            color = textMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        itemsIndexed(activeMembers) { _, member ->
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
            }
        }
    }

    // 📅 हफ़्ते चुनने वाली बॉटम शीट (Full Expanded)
    if (showWeekPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeekPickerSheet = false },
            sheetState = weekSheetState,
            containerColor = cardBg,
            dragHandle = { BottomSheetDefaults.DragHandle(color = cardBorder) }
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
            .clip(RoundedCornerShape(12.dp)),
        border = rowBorder,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = rowBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = rankTextColor,
                    fontWeight = if (rank <= 3 || isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.nickname,
                        color = if (isCurrentUser) accentColor else textMain,
                        fontWeight = if (isCurrentUser || rank <= 3) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.5.sp,
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
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                    fontSize = 13.5.sp
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
                            fontSize = 10.5.sp,
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
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(focusingGreen)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.nickname,
                    color = textMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = member.currentSubject ?: "Deep Focus Sprint",
                    color = textMuted,
                    fontSize = 11.5.sp
                )
            }

            Surface(
                color = focusingGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(focusingGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Studying now",
                        color = focusingGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
