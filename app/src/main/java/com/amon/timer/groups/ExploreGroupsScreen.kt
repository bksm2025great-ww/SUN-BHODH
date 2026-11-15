package com.amon.timer.groups

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.CloudSyncManager
import com.amon.timer.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreGroupsScreen(
    groups: List<StudyGroup> = emptyList(),
    onBackClick: () -> Unit = {},
    onGroupClick: (StudyGroup) -> Unit = {},
    onToggleJoinGroup: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val focusingGreen = Color(0xFF22C55E)

    // 🛑 पब्लिक सर्च हटाने के बाद अब यह सिर्फ "Invite Code" इनपुट के लिए है
    var inviteCodeQuery by remember { mutableStateOf("") }
    var privateGroupResult by remember { mutableStateOf<StudyGroup?>(null) }
    var isSearchingCode by remember { mutableStateOf(false) }
    var isGhostModeActive by remember { mutableStateOf(false) } // 👻 Ghost Mode

    // 🔍 सिर्फ इनवाइट कोड के जरिए प्राइवेट ग्रुप ढूंढना
    LaunchedEffect(inviteCodeQuery) {
        val query = inviteCodeQuery.trim()
        if (query.length >= 4) {
            isSearchingCode = true
            coroutineScope.launch {
                val found = CloudSyncManager.fetchGroupByInviteCode(query)
                privateGroupResult = found
                isSearchingCode = false
            }
        } else {
            privateGroupResult = null
            isSearchingCode = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Join Study Group",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
                    // 👻 Ghost Mode Toggle Button in TopBar
                    IconButton(
                        onClick = { isGhostModeActive = !isGhostModeActive }
                    ) {
                        Icon(
                            imageVector = if (isGhostModeActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ghost Mode",
                            tint = if (isGhostModeActive) accentColor else textMuted
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
                .padding(16.dp)
        ) {
            // 🏷️ निर्देश कार्ड: सिर्फ इनवाइट कोड से जुड़ें
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 Private Study Circle",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "यह ऐप पूरी तरह से प्राइवेट है। कोई भी पब्लिक ग्रुप यहाँ नहीं दिखेगा। अपने दोस्त से इनवाइट कोड लेकर नीचे दर्ज करें।",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔑 इनवाइट कोड दर्ज करने का बॉक्स (सिर्फ यही रहेगा)
            OutlinedTextField(
                value = inviteCodeQuery,
                onValueChange = { inviteCodeQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter Invite Code (e.g. AMON-XXXX)",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Invite Code",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (isSearchingCode) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = accentColor
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = textMain,
                    unfocusedTextColor = textMain,
                    cursorColor = accentColor
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isGhostModeActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0x22FFC107),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "👻 Ghost Mode एक्टिव है: आपके दोस्त आपकी एक्टिविटी नहीं देख पाएंगे।",
                        color = Color(0xFFFFC107),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = if (privateGroupResult != null) "Group Found via Invite Code:" else "Enter code above to find your friends' circle",
                color = textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 📋 यदि इनवाइट कोड से ग्रुप मिल जाता है तो उसे दिखाएँ
            if (privateGroupResult != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ExploreGroupRow(
                            group = privateGroupResult!!,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            accentSubtle = accentSubtle,
                            focusingGreen = focusingGreen,
                            isDark = isDark,
                            onRowClick = { onGroupClick(privateGroupResult!!) },
                            onJoinToggle = { onToggleJoinGroup(privateGroupResult!!.id) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    alignment = Alignment.Center
                ) {
                    Text(
                        text = if (inviteCodeQuery.isBlank()) "कोई ग्रुप प्रदर्शित नहीं है।\nकृपया इनवाइट कोड दर्ज करें।" else "इनवाइट कोड से कोई ग्रुप नहीं मिला। सही कोड दर्ज करें।",
                        color = textMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreGroupRow(
    group: StudyGroup,
    cardBg: Color,
    cardBorder: Color,
    textMain: Color,
    textMuted: Color,
    accentColor: Color,
    accentSubtle: Color,
    focusingGreen: Color,
    isDark: Boolean,
    onRowClick: () -> Unit,
    onJoinToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onRowClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentSubtle)
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                alignment = Alignment.Center
            ) {
                Text(
                    text = group.name.take(2).uppercase(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0x22F59E0B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Private",
                            color = Color(0xFFF59E0B),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
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
                        text = "${group.focusingCount} active now · ${group.memberCount} members",
                        color = textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (group.isJoined) {
                OutlinedButton(
                    onClick = onJoinToggle,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Joined",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Joined", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onJoinToggle,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text(text = "Join", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
