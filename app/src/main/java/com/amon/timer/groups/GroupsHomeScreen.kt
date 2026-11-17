package com.amon.timer.groups

import android.content.Context
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.CloudSyncManager
import com.amon.timer.ThemeManager
import com.amon.timer.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 🔑 Clean Vector Key Icon
@Composable
fun GroupKeyIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        val headRadius = w * 0.22f
        val headCenter = Offset(w * 0.30f, h * 0.5f)

        drawCircle(
            color = tint,
            radius = headRadius,
            center = headCenter,
            style = Stroke(width = stroke)
        )

        drawLine(
            color = tint,
            start = Offset(headCenter.x + headRadius, h * 0.5f),
            end = Offset(w * 0.88f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.68f, h * 0.5f),
            end = Offset(w * 0.68f, h * 0.72f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.84f, h * 0.5f),
            end = Offset(w * 0.84f, h * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

// 💾 Groups Cache Extensions
fun GroupCacheManager.saveCachedGroupsList(context: Context, groups: List<StudyGroup>) {
    val serialized = groups.joinToString("###") { g ->
        "${g.id}::${g.name}::${g.category}::${g.isPublic}::${g.memberCount}::${g.focusingCount}::${g.isPinned}::${g.isJoined}::${g.isDefault}::${g.inviteCode}"
    }
    context.getSharedPreferences("amon_group_cache", Context.MODE_PRIVATE)
        .edit().putString("cache_home_groups", serialized).apply()
}

fun GroupCacheManager.getCachedGroupsList(context: Context): List<StudyGroup> {
    val serialized = context.getSharedPreferences("amon_group_cache", Context.MODE_PRIVATE)
        .getString("cache_home_groups", null) ?: return emptyList()
    if (serialized.isBlank()) return emptyList()
    return try {
        serialized.split("###").mapNotNull { row ->
            val parts = row.split("::")
            if (parts.size >= 10) {
                StudyGroup(
                    id = parts[0],
                    name = parts[1],
                    category = parts[2],
                    isPublic = parts[3].toBoolean(),
                    memberCount = parts[4].toIntOrNull() ?: 1,
                    focusingCount = parts[5].toIntOrNull() ?: 0,
                    isPinned = parts[6].toBoolean(),
                    isJoined = parts[7].toBoolean(),
                    isDefault = parts[8].toBoolean(),
                    inviteCode = parts.getOrNull(9)?.ifBlank { "" } ?: ""
                )
            } else null
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun GroupCacheManager.clearCachedGroupsList(context: Context) {
    try {
        context.getSharedPreferences("amon_group_cache", Context.MODE_PRIVATE)
            .edit().remove("cache_home_groups").apply()
    } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    groups: List<StudyGroup> = emptyList(),
    userNickname: String = "",
    onCreateGroupClick: () -> Unit = {},
    onKeyClick: () -> Unit = {},
    onGroupClick: (StudyGroup) -> Unit = {},
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
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)

    val initialGroups = remember {
        GroupCacheManager.getCachedGroupsList(context)
    }

    var liveGroups by remember { mutableStateOf(if (groups.isNotEmpty()) groups else initialGroups) }

    LaunchedEffect(groups) {
        liveGroups = groups
        if (groups.isNotEmpty()) {
            GroupCacheManager.saveCachedGroupsList(context, groups)
        } else {
            GroupCacheManager.clearCachedGroupsList(context)
        }
    }

    val effectiveNickname = if (userNickname.isNotBlank()) userNickname else userManager.getUserName().ifBlank { "Aspirant" }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val freshGroups = CloudSyncManager.fetchGroups(context)
            withContext(Dispatchers.Main) {
                if (freshGroups.isNotEmpty()) {
                    liveGroups = freshGroups
                    GroupCacheManager.saveCachedGroupsList(context, freshGroups)
                }
            }
        }
    }

    val yourGroups = liveGroups.filter { it.isJoined }

    Scaffold(
        topBar = {
            // 4. हेडर साफ़ करना: "Study Profile" हटाकर सीधे "Groups" टाइटल रखना
            TopAppBar(
                title = {
                    Text(
                        text = "Groups",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    // सिर्फ एक साफ़-सुथरा '+' बटन (डेटा क्लटर हटाने के लिए)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Study Circles",
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 3. इमोजी की जगह साफ़ मिनिमलिस्ट आइकन बॉक्स
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accentSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                GroupKeyIcon(tint = accentColor, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Build Your Inner Study Circle",
                                color = textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 5. लंबा पैराग్రాफ़ हटाकर एक साफ़ और प्रोफेशनल लाइन
                            Text(
                                text = "Study together. Stay consistent.",
                                color = textMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            // 2. नकली डेटा (Alex 14 Days Streak) पूरी तरह से हटा दिया गया है.

                            Spacer(modifier = Modifier.height(20.dp))

                            // 5. बटन्स (Create Circle मुख्य भरा हुआ बटन, Join हल्का आउटलाइन बटन)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onCreateGroupClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = if (isDark) Color.Black else Color.White
                                    )
                                ) {
                                    Text(
                                        text = "Create Circle",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // 1. सिंगल लाइन 'Join' बटन (बिना इमोजी के, जो कभी दो लाइन में नहीं टूटेगा)
                                OutlinedButton(
                                    onClick = onKeyClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, cardBorder),
                                    colors = OutlinedButtonDefaults.outlinedButtonColors(
                                        contentColor = textMain
                                    )
                                ) {
                                    Text(
                                        text = "Join",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
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
                        onJoinClick = { onJoinGroup(group.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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
                        text = "${group.memberCount} members · Private Circle",
                        color = textMuted,
                        fontSize = 11.5.sp
                    )
                }

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
                        textMain
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
