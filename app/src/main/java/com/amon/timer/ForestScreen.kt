package com.amon.timer

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

// --- डेटा मॉडल्स ---
private enum class RoomIconType {
    LIBRARY,
    CODE,
    CIVIL,
    FINANCE,
    CUSTOM
}

private data class StudyGroup(
    val id: String,
    val name: String,
    val membersCount: String,
    val activeCount: String,
    val isPublic: Boolean,
    val isCreatedByMe: Boolean = false,
    val iconType: RoomIconType,
    val tagColor: Color,
    val subtitle: String
)

data class LeaderboardStudent(
    val id: String,
    val rank: Int,
    val name: String,
    val studyDuration: String,
    val isCurrentUser: Boolean = false,
    val isActiveNow: Boolean = true,
    val avatarLetter: String,
    val avatarBgColor: Color
)

data class ActiveStudent(
    val id: String,
    val name: String,
    val liveFocusTime: String,
    val avatarLetter: String,
    val avatarBgColor: Color
)

@Composable
fun ForestScreen() {
    val context = LocalContext.current

    val darkBg = Color(0xFF090D14)
    val cardBg = Color(0xFF121824)
    val cardBorder = Color(0xFF1E2638)
    val primaryAccent = Color(0xFF644DFF)
    val liveGreen = Color(0xFF22C55E)

    var activeGroup by remember { mutableStateOf<StudyGroup?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // 🔙 बैक बटन हैंडलर
    BackHandler(enabled = activeGroup != null) {
        activeGroup = null
    }

    // केवल मुख्य Universal Library रूम
    val myGroups = remember {
        mutableStateListOf(
            StudyGroup(
                id = "univ_lib",
                name = "Universal Library",
                membersCount = "Global Hub",
                activeCount = "Live",
                isPublic = true,
                isCreatedByMe = false,
                iconType = RoomIconType.LIBRARY,
                tagColor = primaryAccent,
                subtitle = "Global 24/7 silent focus hub for students worldwide"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        AnimatedContent(
            targetState = activeGroup,
            label = "screen_transition"
        ) { group ->
            if (group == null) {
                GroupsListHubScreen(
                    myGroups = myGroups,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    primaryAccent = primaryAccent,
                    liveGreen = liveGreen,
                    onGroupClick = { clickedGroup -> activeGroup = clickedGroup },
                    onCreateGroupClick = { showCreateGroupDialog = true }
                )
            } else {
                GroupDetailViewScreen(
                    group = group,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    primaryAccent = primaryAccent,
                    liveGreen = liveGreen,
                    onBackClick = { activeGroup = null },
                    onLeaveGroup = {
                        myGroups.remove(group)
                        activeGroup = null
                    },
                    onDeleteGroup = {
                        myGroups.remove(group)
                        activeGroup = null
                    },
                    onInviteClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my silent study room '${group.name}' on Sun Bodh! Let's focus together 🏛️"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Invite friends to ${group.name}"))
                    }
                )
            }
        }

        if (showCreateGroupDialog) {
            CreateGroupDialog(
                primaryAccent = primaryAccent,
                onDismiss = { showCreateGroupDialog = false },
                onCreate = { name, isPublic ->
                    val newRoom = StudyGroup(
                        id = "group_${System.currentTimeMillis()}",
                        name = name,
                        membersCount = "1 member",
                        activeCount = "1",
                        isPublic = isPublic,
                        isCreatedByMe = true,
                        iconType = RoomIconType.CUSTOM,
                        tagColor = primaryAccent,
                        subtitle = if (isPublic) "Public community room" else "Private study circle"
                    )
                    myGroups.add(0, newRoom)
                    showCreateGroupDialog = false
                }
            )
        }
    }
}

// ==========================================
// 1. ग्रुप्स हब स्क्रीन (सर्च बार + यूनिवर्सल रूम)
// ==========================================
@Composable
private fun GroupsListHubScreen(
    myGroups: List<StudyGroup>,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    onGroupClick: (StudyGroup) -> Unit,
    onCreateGroupClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredGroups = remember(searchQuery, myGroups) {
        if (searchQuery.isBlank()) {
            myGroups
        } else {
            myGroups.filter {
                it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.subtitle.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Focus Rooms",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primaryAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable(onClick = onCreateGroupClick)
                ) {
                    Text(
                        text = "+ Create Room",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 🔍 सर्च बार
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search rooms or topics...", color = Color(0xFF64748B), fontSize = 13.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryAccent,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = primaryAccent,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        item {
            Text(
                text = "GLOBAL STUDY ROOM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(filteredGroups, key = { it.id }) { group ->
            GroupCardItem(
                group = group,
                cardBg = cardBg,
                cardBorder = cardBorder,
                liveGreen = liveGreen,
                onClick = { onGroupClick(group) }
            )
        }

        item {
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun GroupCardItem(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    liveGreen: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(group.tagColor.copy(alpha = 0.18f))
                        .border(1.dp, group.tagColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CleanCategoryVectorIcon(
                        type = group.iconType,
                        tint = group.tagColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (group.isCreatedByMe) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E2638)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${group.membersCount} • ${if (group.isPublic) "Public" else "Private"}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = "›",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090D14))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(liveGreen)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Live focus leaderboard enabled",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = liveGreen
                )
            }
        }
    }
}

// ==========================================
// 2. ग्रुप डिटेल स्क्रीन (रियल-डेटा रेडी)
// ==========================================
@Composable
private fun GroupDetailViewScreen(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    onBackClick: () -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onInviteClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showMenuDropdown by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<LeaderboardStudent?>(null) }

    val isUniversalLibrary = group.id == "univ_lib"

    // 🛡️ 100% शुद्ध और खाली लिस्ट - इसमें सिर्फ़ Supabase का असली डेटा आएगा
    val activeLeaderboard = remember { mutableStateListOf<LeaderboardStudent>() }
    val onlyActiveMembers = remember { mutableStateListOf<ActiveStudent>() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                shape = CircleShape,
                color = cardBg,
                border = BorderStroke(1.2.dp, cardBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CleanBackArrowVectorIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${group.membersCount} • ${if (group.isPublic) "Public" else "Private"}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF16202E),
                border = BorderStroke(1.dp, Color(0xFF223046)),
                modifier = Modifier.clickable(onClick = onInviteClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CleanShareVectorIcon(tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Invite",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Box {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { showMenuDropdown = true },
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        WhatsAppThreeDotsIcon(tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                    }
                }

                DropdownMenu(
                    expanded = showMenuDropdown,
                    onDismissRequest = { showMenuDropdown = false },
                    modifier = Modifier
                        .background(Color(0xFF141A24))
                        .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(12.dp)),
                    offset = DpOffset(x = 0.dp, y = 6.dp)
                ) {
                    if (isUniversalLibrary) {
                        DropdownMenuItem(
                            text = { Text("Global Silent Hub", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                            onClick = { showMenuDropdown = false },
                            leadingIcon = {
                                CleanCategoryVectorIcon(RoomIconType.LIBRARY, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        )
                    } else if (group.isCreatedByMe) {
                        DropdownMenuItem(
                            text = { Text("Delete Room", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenuDropdown = false
                                showDeleteConfirmDialog = true
                            },
                            leadingIcon = {
                                CleanTrashVectorIcon(tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Leave Room", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenuDropdown = false
                                showLeaveConfirmDialog = true
                            },
                            leadingIcon = {
                                CleanLeaveVectorIcon(tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }

        // टैब स्विचर
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101520))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 0) cardBg else Color.Transparent)
                    .border(1.dp, if (selectedTab == 0) cardBorder else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Leaderboard (${activeLeaderboard.size})",
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTab == 0) Color.White else Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 1) cardBg else Color.Transparent)
                    .border(1.dp, if (selectedTab == 1) cardBorder else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(liveGreen)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Active (${onlyActiveMembers.size})",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }

        // टैब 1: LEADERBOARD (खाली होने पर साफ़ संदेश)
        if (selectedTab == 0) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeLeaderboard.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = primaryAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏆", fontSize = 26.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No Rankings Yet This Week",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Be the first one on the leaderboard! Complete a 5+ minute focus session to claim rank #1.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Weekly Reset • Active Ranking",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Top Ranking",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryAccent
                                )
                            }
                        }

                        items(activeLeaderboard, key = { it.id }) { student ->
                            MetallicLeaderboardRow(
                                student = student,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primaryAccent = primaryAccent,
                                liveGreen = liveGreen,
                                canAdminKick = group.isCreatedByMe && !student.isCurrentUser,
                                onKickClick = { memberToKick = student }
                            )
                        }

                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }

                    // रैंक पर स्क्रॉल करने का बटन तभी दिखेगा जब छात्र खुद लिस्ट में हो
                    val currentUserIndex = activeLeaderboard.indexOfFirst { it.isCurrentUser }
                    if (currentUserIndex != -1) {
                        val currentStudent = activeLeaderboard[currentUserIndex]
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(currentUserIndex + 1)
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E2638),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(primaryAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentStudent.avatarLetter,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Scroll to your position (#${currentStudent.rank})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(text = "↓", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        // टैब 2: ACTIVE छात्र
        if (selectedTab == 1) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (onlyActiveMembers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            color = liveGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, liveGreen.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌱", fontSize = 26.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Silent Room is Quiet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Nobody is studying in this room right now. Start your focus timer and lead the room!",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(onlyActiveMembers, key = { it.id }) { member ->
                            ActiveStudentGridCard(member = member)
                        }

                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }

                Button(
                    onClick = { /* स्टार्ट टाइमर */ },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Start Focus",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showLeaveConfirmDialog) {
        SimpleConfirmDialog(
            title = "Leave Room?",
            message = "Are you sure you want to leave '${group.name}'?",
            confirmText = "Leave",
            confirmColor = Color(0xFFEF4444),
            onDismiss = { showLeaveConfirmDialog = false },
            onConfirm = {
                showLeaveConfirmDialog = false
                onLeaveGroup()
            }
        )
    }

    if (showDeleteConfirmDialog) {
        SimpleConfirmDialog(
            title = "Delete Room Permanently?",
            message = "This room was created by you. Deleting it will permanently remove it for all members.",
            confirmText = "Delete Room",
            confirmColor = Color(0xFFEF4444),
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeleteGroup()
            }
        )
    }

    if (memberToKick != null) {
        val student = memberToKick!!
        SimpleConfirmDialog(
            title = "Remove Member?",
            message = "Remove '${student.name}' from '${group.name}'?",
            confirmText = "Remove Member",
            confirmColor = Color(0xFFEF4444),
            onDismiss = { memberToKick = null },
            onConfirm = {
                activeLeaderboard.remove(student)
                onlyActiveMembers.removeAll { it.name == student.name }
                memberToKick = null
            }
        )
    }
}

// 👑 मेटैलिक लीडरबोर्ड रो
@Composable
private fun MetallicLeaderboardRow(
    student: LeaderboardStudent,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    canAdminKick: Boolean = false,
    onKickClick: () -> Unit = {}
) {
    val goldColor = Color(0xFFFFD700)
    val silverColor = Color(0xFFCBD5E1)
    val bronzeColor = Color(0xFFCD7F32)

    val rowBorder = when {
        student.isCurrentUser -> BorderStroke(1.2.dp, primaryAccent)
        student.rank == 1 -> BorderStroke(1.2.dp, goldColor.copy(alpha = 0.85f))
        student.rank == 2 -> BorderStroke(1.1.dp, silverColor.copy(alpha = 0.70f))
        student.rank == 3 -> BorderStroke(1.1.dp, bronzeColor.copy(alpha = 0.75f))
        else -> BorderStroke(1.dp, cardBorder)
    }

    val rowBg = when {
        student.isCurrentUser -> primaryAccent.copy(alpha = 0.16f)
        student.rank == 1 -> Color(0xFF1A170A)
        student.rank == 2 -> Color(0xFF141822)
        student.rank == 3 -> Color(0xFF1A130F)
        else -> cardBg
    }

    val rankTextColor = when {
        student.isCurrentUser -> primaryAccent
        student.rank == 1 -> goldColor
        student.rank == 2 -> silverColor
        student.rank == 3 -> bronzeColor
        else -> Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 1f }
            .then(
                if (canAdminKick) Modifier.clickable(onClick = onKickClick) else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        color = rowBg,
        border = rowBorder
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${student.rank}",
                    fontSize = if (student.rank <= 3) 16.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rankTextColor
                )
            }

            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(student.avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.avatarLetter,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(liveGreen)
                        .border(1.5.dp, rowBg, CircleShape)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 14.sp,
                    fontWeight = if (student.isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (student.isCurrentUser) Color.White else Color(0xFFE2E8F0)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (student.isCurrentUser) {
                        Text(
                            text = "You • ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent
                        )
                    }
                    Text(
                        text = "Active ranker",
                        fontSize = 11.sp,
                        color = liveGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = student.studyDuration,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (student.isCurrentUser) primaryAccent else Color(0xFF94A3B8)
            )

            if (canAdminKick) {
                Spacer(Modifier.width(8.dp))
                CleanRemoveUserVectorIcon(tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun ActiveStudentGridCard(member: ActiveStudent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF121824))
            .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(member.avatarBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.avatarLetter,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = member.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = member.liveFocusTime,
            fontSize = 11.sp,
            color = Color(0xFF22C55E),
            fontWeight = FontWeight.Medium
        )
    }
}

// 🏛️ क्रिएट रूम डायलॉग
@Composable
private fun CreateGroupDialog(
    primaryAccent: Color,
    onDismiss: () -> Unit,
    onCreate: (name: String, isPublic: Boolean) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF121824),
            border = BorderStroke(1.dp, Color(0xFF1E2638)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Study Room",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { input ->
                        groupName = input.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                    },
                    placeholder = { Text("Room Name (e.g. Focus Lounge)", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryAccent,
                        unfocusedBorderColor = Color(0xFF1E2638),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = primaryAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090D14))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isPublic) "Public Room" else "Private Room",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPublic) "Anyone can discover & join" else "Invite-only room",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryAccent,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E2638)
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(enabled = groupName.isNotBlank()) {
                                if (groupName.isNotBlank()) {
                                    onCreate(groupName.trim(), isPublic)
                                }
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = if (groupName.isNotBlank()) primaryAccent.copy(alpha = 0.88f) else Color(0xFF1E2638),
                        border = BorderStroke(
                            1.1.dp,
                            if (groupName.isNotBlank()) primaryAccent else Color(0xFF334155)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Create",
                                color = if (groupName.isNotBlank()) Color.White else Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ⚠️ कॉमन कन्फर्मेशन डायलॉग
@Composable
private fun SimpleConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121824),
            border = BorderStroke(1.dp, Color(0xFF1E2638)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) {
                        Text(confirmText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 🎨 CLEAN CANVAS VECTOR ICONS (Zero Heavy Libs)
// =============================================================================

@Composable
private fun WhatsAppThreeDotsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = 1.6.dp.toPx()
        drawCircle(color = tint, radius = r, center = Offset(w * 0.5f, h * 0.22f))
        drawCircle(color = tint, radius = r, center = Offset(w * 0.5f, h * 0.50f))
        drawCircle(color = tint, radius = r, center = Offset(w * 0.5f, h * 0.78f))
    }
}

@Composable
private fun CleanShareVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.4.dp.toPx()

        val r = w * 0.14f
        val p1 = Offset(w * 0.76f, h * 0.24f)
        val p2 = Offset(w * 0.26f, h * 0.50f)
        val p3 = Offset(w * 0.76f, h * 0.76f)

        drawLine(color = tint, start = p2, end = p1, strokeWidth = strokeW)
        drawLine(color = tint, start = p2, end = p3, strokeWidth = strokeW)

        drawCircle(color = tint, radius = r, center = p1)
        drawCircle(color = tint, radius = r, center = p2)
        drawCircle(color = tint, radius = r, center = p3)
    }
}

@Composable
private fun CleanBackArrowVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeW = 2.2.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(color = tint, start = Offset(w * 0.90f, h * 0.5f), end = Offset(w * 0.15f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.5f), end = Offset(w * 0.50f, h * 0.15f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.5f), end = Offset(w * 0.50f, h * 0.85f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
private fun CleanTrashVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()

        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.30f), end = Offset(w * 0.85f, h * 0.30f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.38f, h * 0.20f), end = Offset(w * 0.62f, h * 0.20f), strokeWidth = strokeW, cap = StrokeCap.Round)

        val binPath = Path().apply {
            moveTo(w * 0.24f, h * 0.30f)
            lineTo(w * 0.30f, h * 0.85f)
            cubicTo(w * 0.30f, h * 0.90f, w * 0.35f, h * 0.90f, w * 0.40f, h * 0.90f)
            lineTo(w * 0.60f, h * 0.90f)
            cubicTo(w * 0.65f, h * 0.90f, w * 0.70f, h * 0.90f, w * 0.70f, h * 0.85f)
            lineTo(w * 0.76f, h * 0.30f)
        }
        drawPath(binPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}

@Composable
private fun CleanLeaveVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()

        val doorPath = Path().apply {
            moveTo(w * 0.55f, h * 0.18f)
            lineTo(w * 0.20f, h * 0.18f)
            lineTo(w * 0.20f, h * 0.82f)
            lineTo(w * 0.55f, h * 0.82f)
        }
        drawPath(doorPath, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))

        drawLine(color = tint, start = Offset(w * 0.40f, h * 0.50f), end = Offset(w * 0.85f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.85f, h * 0.50f), end = Offset(w * 0.68f, h * 0.35f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.85f, h * 0.50f), end = Offset(w * 0.68f, h * 0.65f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
private fun CleanRemoveUserVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.6.dp.toPx()

        drawCircle(color = tint, radius = w * 0.22f, center = Offset(w * 0.40f, h * 0.35f), style = Stroke(strokeW))

        val bodyPath = Path().apply {
            moveTo(w * 0.12f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.62f, w * 0.30f, h * 0.60f, w * 0.40f, h * 0.60f)
            cubicTo(w * 0.50f, h * 0.60f, w * 0.65f, h * 0.62f, w * 0.68f, h * 0.82f)
        }
        drawPath(bodyPath, color = tint, style = Stroke(strokeW, cap = StrokeCap.Round))

        drawLine(color = tint, start = Offset(w * 0.72f, h * 0.45f), end = Offset(w * 0.92f, h * 0.45f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
private fun CleanCategoryVectorIcon(type: RoomIconType, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.6.dp.toPx()

        when (type) {
            RoomIconType.LIBRARY -> {
                val book = Path().apply {
                    moveTo(w * 0.5f, h * 0.80f)
                    cubicTo(w * 0.35f, h * 0.72f, w * 0.20f, h * 0.74f, w * 0.12f, h * 0.78f)
                    lineTo(w * 0.12f, h * 0.28f)
                    cubicTo(w * 0.20f, h * 0.24f, w * 0.35f, h * 0.22f, w * 0.5f, h * 0.30f)
                    cubicTo(w * 0.65f, h * 0.22f, w * 0.80f, h * 0.24f, w * 0.88f, h * 0.28f)
                    lineTo(w * 0.88f, h * 0.78f)
                    cubicTo(w * 0.80f, h * 0.74f, w * 0.65f, h * 0.72f, w * 0.5f, h * 0.80f)
                    close()
                }
                drawPath(book, color = tint, style = Stroke(width = strokeW))
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.30f), end = Offset(w * 0.5f, h * 0.80f), strokeWidth = strokeW)
            }
            RoomIconType.CODE -> {
                drawLine(color = tint, start = Offset(w * 0.36f, h * 0.30f), end = Offset(w * 0.18f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.50f), end = Offset(w * 0.36f, h * 0.70f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.64f, h * 0.30f), end = Offset(w * 0.82f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.82f, h * 0.50f), end = Offset(w * 0.64f, h * 0.70f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.56f, h * 0.24f), end = Offset(w * 0.44f, h * 0.76f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            RoomIconType.CIVIL -> {
                drawLine(color = tint, start = Offset(w * 0.20f, h * 0.22f), end = Offset(w * 0.80f, h * 0.22f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.22f), end = Offset(w * 0.32f, h * 0.78f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.22f), end = Offset(w * 0.50f, h * 0.78f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.68f, h * 0.22f), end = Offset(w * 0.68f, h * 0.78f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.20f, h * 0.78f), end = Offset(w * 0.80f, h * 0.78f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            RoomIconType.FINANCE -> {
                drawLine(color = tint, start = Offset(w * 0.25f, h * 0.75f), end = Offset(w * 0.25f, h * 0.50f), strokeWidth = strokeW * 1.5f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.75f), end = Offset(w * 0.50f, h * 0.32f), strokeWidth = strokeW * 1.5f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.75f, h * 0.75f), end = Offset(w * 0.75f, h * 0.18f), strokeWidth = strokeW * 1.5f, cap = StrokeCap.Round)
            }
            RoomIconType.CUSTOM -> {
                val shield = Path().apply {
                    moveTo(w * 0.50f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.30f)
                    lineTo(w * 0.82f, h * 0.58f)
                    cubicTo(w * 0.82f, h * 0.76f, w * 0.50f, h * 0.86f, w * 0.50f, h * 0.86f)
                    cubicTo(w * 0.50f, h * 0.86f, w * 0.18f, h * 0.76f, w * 0.18f, h * 0.58f)
                    lineTo(w * 0.18f, h * 0.30f)
                    close()
                }
                drawPath(shield, color = tint, style = Stroke(width = strokeW))
            }
        }
    }
}
