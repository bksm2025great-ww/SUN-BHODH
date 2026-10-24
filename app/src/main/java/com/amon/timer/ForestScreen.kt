package com.amon.timer

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

// --- डेटा मॉडल्स ---
private data class StudyGroup(
    val id: String,
    val name: String,
    val membersCount: String,
    val activeCount: String,
    val isPublic: Boolean,
    val tagIcon: String,
    val tagColor: Color,
    val subtitle: String
)

private data class LeaderboardStudent(
    val rank: Int,
    val name: String,
    val studyDuration: String,
    val isCurrentUser: Boolean = false,
    val avatarLetter: String,
    val avatarBgColor: Color
)

private data class MemberUser(
    val name: String,
    val statusText: String,
    val isActive: Boolean,
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

    // 🔙 बैक बटन फ़िज़िक्स
    BackHandler(enabled = activeGroup != null) {
        activeGroup = null
    }

    val myGroups = remember {
        mutableStateListOf(
            StudyGroup(
                id = "univ_lib",
                name = "Universal Library",
                membersCount = "225k+ members",
                activeCount = "3.8k",
                isPublic = true,
                tagIcon = "🏛️",
                tagColor = primaryAccent,
                subtitle = "Global 24/7 silent focus hub for students worldwide"
            ),
            StudyGroup(
                id = "coding_arena",
                name = "Code & Algorithms",
                membersCount = "14k+ members",
                activeCount = "210",
                isPublic = true,
                tagIcon = "💻",
                tagColor = Color(0xFF0EA5E9),
                subtitle = "DSA, Development & late night deep builds"
            )
        )
    }

    val suggestedGroups = remember {
        mutableStateListOf(
            StudyGroup(
                id = "upsc_prep",
                name = "UPSC & Civil Aspirants",
                membersCount = "48k+ members",
                activeCount = "1.2k",
                isPublic = true,
                tagIcon = "📖",
                tagColor = Color(0xFFF59E0B),
                subtitle = "GS, Answer Writing & Revision streak"
            ),
            StudyGroup(
                id = "exam_warriors",
                name = "CA & Finance Cohort",
                membersCount = "8.5k+ members",
                activeCount = "95",
                isPublic = false,
                tagIcon = "📊",
                tagColor = Color(0xFFEC4899),
                subtitle = "Strict accountability room"
            )
        )
    }

    Box(
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
                    suggestedGroups = suggestedGroups,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    primaryAccent = primaryAccent,
                    liveGreen = liveGreen,
                    onGroupClick = { clickedGroup -> activeGroup = clickedGroup },
                    onCreateGroupClick = { showCreateGroupDialog = true },
                    onJoinGroupClick = { suggestedGroup ->
                        suggestedGroups.remove(suggestedGroup)
                        myGroups.add(suggestedGroup)
                    }
                )
            } else {
                GroupDetailViewScreen(
                    group = group,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    primaryAccent = primaryAccent,
                    liveGreen = liveGreen,
                    onBackClick = { activeGroup = null },
                    onInviteClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my silent study room '${group.name}' on Amon! Let's focus together 🏛️"
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
                        tagIcon = if (isPublic) "🌍" else "🔒",
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
// 1. ग्रुप्स हब स्क्रीन
// ==========================================
@Composable
private fun GroupsListHubScreen(
    myGroups: List<StudyGroup>,
    suggestedGroups: List<StudyGroup>,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    onGroupClick: (StudyGroup) -> Unit,
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: (StudyGroup) -> Unit
) {
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
                    text = "Focus Groups",
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
                        text = "+ Create Group",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "YOUR ROOMS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(myGroups, key = { it.id }) { group ->
            GroupCardItem(
                group = group,
                cardBg = cardBg,
                cardBorder = cardBorder,
                liveGreen = liveGreen,
                onClick = { onGroupClick(group) }
            )
        }

        if (suggestedGroups.isNotEmpty()) {
            item {
                Text(
                    text = "SUGGESTED DISCOVERY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(suggestedGroups, key = { it.id }) { group ->
                SuggestedGroupCardItem(
                    group = group,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    onJoinClick = { onJoinGroupClick(group) },
                    onOpenClick = { onGroupClick(group) }
                )
            }
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
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(group.tagColor.copy(alpha = 0.2f))
                        .border(1.dp, group.tagColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = group.tagIcon, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                    text = "${group.activeCount} focusing right now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = liveGreen
                )
            }
        }
    }
}

@Composable
private fun SuggestedGroupCardItem(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    onJoinClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenClick),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = group.tagIcon, fontSize = 18.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = group.membersCount,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Button(
                onClick = onJoinClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Join", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 2. ग्रुप डिटेल स्क्रीन (50 यूज़र्स + Active / Inactive)
// ==========================================
@Composable
private fun GroupDetailViewScreen(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    onBackClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 🌟 50 रियलिस्टिक स्टूडेंट्स की पूरी लीडरबोर्ड लिस्ट
    val leaderboard50 = remember {
        val names = listOf(
            "Abhinav S.", "Aryan K.", "Neha Sharma", "Rahul Verma", "Rohan Mehra",
            "Priya Nair", "Aditya Joshi", "Tanmay Bhatia", "Dev Sharma", "Ananya Iyer",
            "Harsh Vardhan", "Sneha Roy", "Kartik Rawat", "Bhanu Sharma", "Vikram Singh",
            "Kashish Patel", "Ritvik Deshmukh", "Pooja Hegde", "Ayushmaan Sen", "Manish Tiwari",
            "Divya Saxena", "Kunal Kamra", "Siddharth Rao", "Akshay Patil", "Megha Kapoor",
            "Gaurav Chavan", "Tejas Singh", "Vidhi Jain", "Naveen Jindal", "Saurabh Pant",
            "Kavita Krishnan", "Mohit Suri", "Anjali Aggarwal", "Yashwant Sinha", "Prateek Kuhad",
            "Deepika Soni", "Varun Grover", "Ishaan Khattar", "Alok Nath", "Shruti Haasan",
            "Tarun Mansukhani", "Nidhi Razdan", "Chetan Bhagat", "Sanya Malhotra", "Uday Chopra",
            "Karan Johar", "Zoya Akhtar", "Farhan Roy", "Boman Irani", "Sunny Deol"
        )

        val colors = listOf(
            Color(0xFFE11D48), Color(0xFF2563EB), Color(0xFFD97706), Color(0xFF059669),
            Color(0xFF7C3AED), Color(0xFF0891B2), Color(0xFF4F46E5), Color(0xFFDB2777),
            Color(0xFFEA580C), Color(0xFF16A34A), Color(0xFF9333EA), Color(0xFF0284C7)
        )

        names.mapIndexed { index, studentName ->
            val rank = index + 1
            val isUser = studentName == "Bhanu Sharma"
            val hours = (145 - (index * 2.8)).toInt().coerceAtLeast(3)
            val minutes = (index * 7) % 60
            LeaderboardStudent(
                rank = rank,
                name = studentName,
                studyDuration = if (isUser) "24h 15m" else "${hours}h ${minutes}m",
                isCurrentUser = isUser,
                avatarLetter = studentName.first().toString(),
                avatarBgColor = if (isUser) Color(0xFFFFB800) else colors[index % colors.size]
            )
        }
    }

    // 👥 एक्टिव और इनएक्टिव यूज़र्स का मिला-जुला डेटा
    val allRoomMembers = remember {
        listOf(
            // 🟢 एक्टिव (पढ़ाई कर रहे हैं)
            MemberUser("Humzaa", "10h 56m", true, "H", Color(0xFFEC4899)),
            MemberUser("Samanwita", "10h 55m", true, "S", Color(0xFFF43F5E)),
            MemberUser("AJ", "10h 55m", true, "A", Color(0xFF38BDF8)),
            MemberUser("Nitesh", "10h 55m", true, "N", Color(0xFF60A5FA)),
            MemberUser("Sameer", "10h 55m", true, "S", Color(0xFF4ADE80)),
            MemberUser("Yogesh", "10h 55m", true, "Y", Color(0xFF818CF8)),
            MemberUser("Vidyakulian", "10h 54m", true, "V", Color(0xFFFBBF24)),
            MemberUser("Nirmal", "10h 55m", true, "N", Color(0xFFFDE047)),
            MemberUser("Ananya", "08h 12m", true, "A", Color(0xFFFB7185)),

            // 😴 इनएक्टिव (ब्रेक पर या ऑफ़लाइन)
            MemberUser("Rohan M.", "Break • 15m", false, "R", Color(0xFF475569)),
            MemberUser("Kashish", "Offline • 1h", false, "K", Color(0xFF475569)),
            MemberUser("Dev S.", "Away • 45m", false, "D", Color(0xFF475569)),
            MemberUser("Vikram", "Offline • 3h", false, "V", Color(0xFF475569)),
            MemberUser("Sneha", "Break • 10m", false, "S", Color(0xFF475569)),
            MemberUser("Kartik", "Offline • 2d", false, "K", Color(0xFF475569))
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // टॉप हेडर
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onBackClick),
                shape = CircleShape,
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                Text(
                    text = "+ Invite",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
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
                    text = "Leaderboard (50)",
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
                        text = "Active & Offline",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }

        // टैब 1: LEADERBOARD (50 STUDENTS)
        if (selectedTab == 0) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                text = "Weekly Reset • Ends in 1 day",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Top 50 Ranking",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                        }
                    }

                    items(leaderboard50, key = { it.rank }) { student ->
                        MetallicLeaderboardRow(
                            student = student,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryAccent = primaryAccent
                        )
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }

                // नीचे फ़्लोटिंग स्क्रोल बटन
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clickable {
                            val userIndex = leaderboard50.indexOfFirst { it.isCurrentUser }
                            if (userIndex != -1) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(userIndex + 1)
                                }
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
                                .background(Color(0xFFFFB800)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Scroll to your position (#14)",
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

        // टैब 2: ACTIVE & INACTIVE MEMBERS
        if (selectedTab == 1) {
            Box(modifier = Modifier.fillMaxSize()) {
                val activeList = remember(allRoomMembers) { allRoomMembers.filter { it.isActive } }
                val inactiveList = remember(allRoomMembers) { allRoomMembers.filter { !it.isActive } }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 🟢 हेडर: Live Studying
                    item(span = { GridItemSpan(3) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(liveGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "STUDYING RIGHT NOW (${activeList.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = liveGreen,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // एक्टिव छात्र कार्ड्स
                    items(activeList) { member ->
                        MemberStatusCard(member = member)
                    }

                    // 😴 हेडर: Inactive / Break
                    item(span = { GridItemSpan(3) }) {
                        Text(
                            text = "TAKING A BREAK / OFFLINE (${inactiveList.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
                        )
                    }

                    // इनएक्टिव छात्र कार्ड्स (डल / ग्रेस्केल)
                    items(inactiveList) { member ->
                        MemberStatusCard(member = member)
                    }

                    item(span = { GridItemSpan(3) }) {
                        Spacer(Modifier.height(80.dp))
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
}

// 👑 मैटेलिक कंट्रास्ट बॉर्डर्स
@Composable
private fun MetallicLeaderboardRow(
    student: LeaderboardStudent,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color
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

    val rankTextColor = when (student.rank) {
        1 -> goldColor
        2 -> silverColor
        3 -> bronzeColor
        else -> if (student.isCurrentUser) primaryAccent else Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    fontSize = 14.sp,
                    fontWeight = if (student.isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (student.isCurrentUser) Color.White else Color(0xFFE2E8F0)
                )
                if (student.isCurrentUser) {
                    Text(
                        text = "You",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent
                    )
                }
            }

            Text(
                text = student.studyDuration,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (student.isCurrentUser) primaryAccent else Color(0xFF94A3B8)
            )
        }
    }
}

// 👥 एक्टिव vs इनएक्टिव कार्ड कंपोनेंट
@Composable
private fun MemberStatusCard(member: MemberUser) {
    val cardBackground = if (member.isActive) Color(0xFF121824) else Color(0xFF0C1017)
    val cardBorder = if (member.isActive) Color(0xFF1E2638) else Color(0xFF161C26)
    val avatarBackground = if (member.isActive) member.avatarBgColor else Color(0xFF334155)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (member.isActive) 1f else 0.55f } // इनएक्टिव यूज़र हल्का डल दिखेगा
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(avatarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.avatarLetter,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (member.isActive) Color.White else Color(0xFF94A3B8)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = member.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (member.isActive) Color.White else Color(0xFF94A3B8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = member.statusText,
            fontSize = 10.5.sp,
            color = if (member.isActive) Color(0xFF22C55E) else Color(0xFF64748B),
            fontWeight = if (member.isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 🏛️ CREATE GROUP POPUP DIALOG
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
                    onValueChange = { groupName = it },
                    placeholder = { Text("Room Name (e.g. UPSC Masters)", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
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
                            text = if (isPublic) "Anyone can discover & join" else "Invite-code only",
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
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) {
                                onCreate(groupName.trim(), isPublic)
                            }
                        },
                        enabled = groupName.isNotBlank(),
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
                    ) {
                        Text("Create", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
