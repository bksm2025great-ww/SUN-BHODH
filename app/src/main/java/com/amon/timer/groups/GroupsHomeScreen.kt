package com.amon.timer.groups

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.CloudSyncManager
import com.amon.timer.ThemeManager
import com.amon.timer.UserManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    groups: List<StudyGroup> = emptyList(),
    userNickname: String = "",
    onSearchClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onGroupClick: (StudyGroup) -> Unit = {},
    onViewAllSuggested: () -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    onJoinGroup: (String) -> Unit = {},
    onNicknameClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }

    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val focusingGreen = Color(0xFF22C55E)

    // 🚀 लाइव डेटा स्टेट: Supabase से आने वाले ग्रुप्स
    var liveGroups by remember { mutableStateOf<List<StudyGroup>>(groups) }
    var isLoading by remember { mutableStateOf(groups.isEmpty()) }

    val effectiveNickname = if (userNickname.isNotBlank()) userNickname else userManager.getUserName()

    // 🔄 डेटा रीलोड फंक्शन: Supabase से ताज़ा ग्रुप्स खींचने के लिए
    fun reloadGroups() {
        coroutineScope.launch {
            val freshGroups = CloudSyncManager.fetchGroups(context)
            if (freshGroups.isNotEmpty() || liveGroups.isEmpty()) {
                liveGroups = freshGroups
            }
            isLoading = false
        }
    }

    // ⚡ स्क्रीन खुलते ही / टैब पर आते ही ऑटो-फ़ेच ट्रिगर होगा
    LaunchedEffect(Unit) {
        reloadGroups()
    }

    val userInitials = remember(effectiveNickname) {
        val parts = effectiveNickname.trim().split(" ", "_", "-").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.isNotEmpty() -> parts[0].take(1).uppercase()
            else -> "SB"
        }
    }

    // 🎯 फ़िल्टर: जॉइन किए हुए ग्रुप्स और बाकी उपलब्ध ग्रुप्स
    val yourGroups = liveGroups.filter { it.isJoined }
    val discoverGroups = liveGroups.filter { !it.isJoined }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNicknameClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userInitials,
                                color = if (isDark) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = effectiveNickname,
                                color = textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Study Profile",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search & Explore",
                            tint = textMain,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onCreateGroupClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Group",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        if (isLoading && liveGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- SECTION 1: जुड़े हुए ग्रुप्स (YOUR JOINED GROUPS) ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Groups (${yourGroups.size})",
                            color = textMain,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (yourGroups.isNotEmpty()) {
                            Text(
                                text = "Pinned on top",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (yourGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You haven't joined any groups yet.\nExplore below or create one!",
                                color = textMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(yourGroups.sortedByDescending { it.isPinned }, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            cardBg = cardBg,
                            textMain = textMain,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            accentSubtle = accentSubtle,
                            focusingGreen = focusingGreen,
                            onCardClick = { onGroupClick(group) },
                            onPinToggle = { onTogglePin(group.id) },
                            onJoinClick = {
                                coroutineScope.launch {
                                    onJoinGroup(group.id)
                                    CloudSyncManager.joinGroupOnCloud(context, group.id)
                                    reloadGroups()
                                }
                            }
                        )
                    }
                }

                // --- SECTION 2: बाकी उपलब्ध ग्रुप्स (DISCOVER / JOIN) ---
                if (discoverGroups.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Explore & Join Groups",
                            color = textMain,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(discoverGroups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            cardBg = cardBg,
                            textMain = textMain,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            accentSubtle = accentSubtle,
                            focusingGreen = focusingGreen,
                            onCardClick = { onGroupClick(group) },
                            onPinToggle = { onTogglePin(group.id) },
                            onJoinClick = {
                                coroutineScope.launch {
                                    onJoinGroup(group.id)
                                    CloudSyncManager.joinGroupOnCloud(context, group.id)
                                    reloadGroups()
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group: StudyGroup,
    cardBg: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    focusingGreen: Color,
    onCardClick: () -> Unit,
    onPinToggle: () -> Unit,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.name.take(2).uppercase(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${group.memberCount} members · ${if (group.isPublic) "Public" else "Private"}",
                        color = textMuted,
                        fontSize = 11.5.sp
                    )
                }

                // 🎯 अगर जॉइन नहीं है तो साफ़ "Join" बटन दिखेगा, अगर जॉइन है तो पिन स्टार
                if (!group.isJoined) {
                    Button(
                        onClick = onJoinClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Join",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                } else {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Pin Group",
                            tint = if (group.isPinned) Color(0xFFF59E0B) else textMuted.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(textMuted.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(focusingGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${group.focusingCount} focusing now",
                        color = focusingGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (group.category.isNotBlank() && group.category != "All") {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = group.category,
                            color = textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
