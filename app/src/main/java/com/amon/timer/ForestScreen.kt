package com.amon.timer

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

// --- डमी डेटा मॉडल्स ---
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

private data class ActiveStudent(
    val name: String,
    val liveFocusTime: String,
    val avatarLetter: String,
    val avatarBgColor: Color
)

@Composable
fun ForestScreen() {
    // बैकग्राउंड और थीम कलर्स
    val darkBg = Color(0xFF090D14)
    val cardBg = Color(0xFF121824)
    val cardBorder = Color(0xFF1E2638)
    val primaryAccent = Color(0xFF644DFF) // आपका इलेक्ट्रिक वॉयलेट
    val liveGreen = Color(0xFF22C55E)

    // नेविगेशन स्टेट: कौन सा ग्रुप खुला हुआ है (null मतलब ग्रुप्स की लिस्ट दिखेगी)
    var activeGroup by remember { mutableStateOf<StudyGroup?>(null) }

    // डमी ग्रुप्स की सूची
    val myGroups = remember {
        listOf(
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
        listOf(
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
                // पेज 1: ग्रुप्स की मुख्य सूची (Hub)
                GroupsListHubScreen(
                    myGroups = myGroups,
                    suggestedGroups = suggestedGroups,
                    darkBg = darkBg,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    primaryAccent = primaryAccent,
                    liveGreen = liveGreen,
                    onGroupClick = { clickedGroup ->
                        activeGroup = clickedGroup
                    }
                )
            } else {
                // पेज 2: ग्रुप के अंदर का पेज (Leaderboard + Active Tabs)
                GroupDetailViewScreen(
                    group = group,
                    darkBg = darkBg,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    primaryAccent = primaryAccent,
                    liveGreen = liveGreen,
                    onBackClick = { activeGroup = null }
                )
            }
        }
    }
}

// ==========================================
// 1. ग्रुप्स हब स्क्रीन (Groups List View)
// ==========================================
@Composable
private fun GroupsListHubScreen(
    myGroups: List<StudyGroup>,
    suggestedGroups: List<StudyGroup>,
    darkBg: Color,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    onGroupClick: (StudyGroup) -> Unit
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
                    modifier = Modifier.clickable { /* डमी क्रिएट ग्रुप */ }
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

        // Your Groups हेडर
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

        // यूज़र के जॉइन किए हुए ग्रुप्स
        items(myGroups) { group ->
            GroupCardItem(
                group = group,
                cardBg = cardBg,
                cardBorder = cardBorder,
                liveGreen = liveGreen,
                onClick = { onGroupClick(group) }
            )
        }

        // Suggested Groups हेडर
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

        // सजेस्टेड ग्रुप्स
        items(suggestedGroups) { group ->
            SuggestedGroupCardItem(
                group = group,
                cardBg = cardBg,
                cardBorder = cardBorder,
                primaryAccent = primaryAccent,
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ग्रुप अवतार आइकॉन
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

            // लाइव फ़ोकस इंडिकेटर स्ट्रिप
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
    primaryAccent: Color,
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
                onClick = onClick,
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
// 2. ग्रुप डिटेल स्क्रीन (Leaderboard & Active)
// ==========================================
@Composable
private fun GroupDetailViewScreen(
    group: StudyGroup,
    darkBg: Color,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color,
    liveGreen: Color,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Leaderboard, 1 = Active

    // डमी लीडरबोर्ड डेटा
    val leaderboardData = remember {
        listOf(
            LeaderboardStudent(1, "Abhinav S.", "142h 30m", false, "A", Color(0xFFE11D48)),
            LeaderboardStudent(2, "Aryan K.", "139h 15m", false, "A", Color(0xFF2563EB)),
            LeaderboardStudent(3, "Neha Sharma", "135h 40m", false, "N", Color(0xFFD97706)),
            LeaderboardStudent(4, "Rahul Verma", "118h 10m", false, "R", Color(0xFF059669)),
            LeaderboardStudent(5, "Rohan Mehra", "95h 25m", false, "R", Color(0xFF7C3AED)),
            LeaderboardStudent(6, "Bhanu Sharma", "14h 25m", true, "B", Color(0xFFFFB800)), // You!
            LeaderboardStudent(7, "Vikram Singh", "12h 10m", false, "V", Color(0xFF0D9488)),
            LeaderboardStudent(8, "Kashish P.", "8h 50m", false, "K", Color(0xFFDB2777))
        )
    }

    // डमी एक्टिव (लाइव) छात्र
    val activeMembers = remember {
        listOf(
            ActiveStudent("Humzaa", "10h 56m", "H", Color(0xFFEC4899)),
            ActiveStudent("Samanwita", "10h 55m", "S", Color(0xFFF43F5E)),
            ActiveStudent("AJ", "10h 55m", "A", Color(0xFF38BDF8)),
            ActiveStudent("Nitesh", "10h 55m", "N", Color(0xFF60A5FA)),
            ActiveStudent("Sameer", "10h 55m", "S", Color(0xFF4ADE80)),
            ActiveStudent("Yogesh", "10h 55m", "Y", Color(0xFF818CF8)),
            ActiveStudent("Vidyakulian", "10h 54m", "V", Color(0xFFFBBF24)),
            ActiveStudent("Nirmal", "10h 55m", "N", Color(0xFFFDE047))
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // टॉप नेविगेशन बार
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
                modifier = Modifier.clickable { /* इनवाइट फ़ीचर */ }
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

        // टैब स्विचर (Leaderboard vs Active)
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
                    .border(
                        1.dp,
                        if (selectedTab == 0) cardBorder else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Leaderboard",
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
                    .border(
                        1.dp,
                        if (selectedTab == 1) cardBorder else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
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
                        text = "Active (${group.activeCount})",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }

        // टैब 1: LEADERBOARD CONTENT
        if (selectedTab == 0) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // टाइमर और लीग पट्टी
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
                                text = "Top 10 League",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                        }
                    }

                    items(leaderboardData) { student ->
                        LeaderboardRowItem(
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

                // नीचे तैरता हुआ पिल बटन: "Scroll to your position"
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clickable { /* यूज़र की रैंक पर स्क्रोल */ },
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
                            text = "Scroll to your position (#6)",
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

        // टैब 2: ACTIVE (LIVE ROOM GRID)
        if (selectedTab == 1) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(activeMembers) { member ->
                        ActiveMemberGridCard(member = member)
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }

                // नीचे बड़ा "Start Focus" बटन
                Button(
                    onClick = { /* टाइमर शुरू करें */ },
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

// लीडरबोर्ड की एक कतार (Row)
@Composable
private fun LeaderboardRowItem(
    student: LeaderboardStudent,
    cardBg: Color,
    cardBorder: Color,
    primaryAccent: Color
) {
    // अगर यूज़र खुद है तो अलग वॉयलेट हाइलाइट शेड
    val rowBg = if (student.isCurrentUser) primaryAccent.copy(alpha = 0.18f) else cardBg
    val rowBorder = if (student.isCurrentUser) primaryAccent else cardBorder

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = rowBg,
        border = BorderStroke(1.dp, rowBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // रैंक नंबर या मेडल
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                when (student.rank) {
                    1 -> Text("🥇", fontSize = 16.sp)
                    2 -> Text("🥈", fontSize = 16.sp)
                    3 -> Text("🥉", fontSize = 16.sp)
                    else -> Text(
                        text = "${student.rank}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (student.isCurrentUser) primaryAccent else Color(0xFF64748B)
                    )
                }
            }

            // छात्र का अवतार
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

            // छात्र का नाम
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

            // कुल पढ़ाई का समय
            Text(
                text = student.studyDuration,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (student.isCurrentUser) primaryAccent else Color(0xFF94A3B8)
            )
        }
    }
}

// एक्टिव ग्रिड का एक छात्र कार्ड
@Composable
private fun ActiveMemberGridCard(member: ActiveStudent) {
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
