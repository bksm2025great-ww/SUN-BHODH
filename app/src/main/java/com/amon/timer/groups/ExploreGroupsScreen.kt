package com.amon.timer.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var privateGroupResult by remember { mutableStateOf<StudyGroup?>(null) }
    var isSearchingCode by remember { mutableStateOf(false) }

    // 🏷️ डायनामिक कैटेगरीज़ (बिना MockRepository के)
    val availableCategories = remember(groups) {
        val distinct = groups.map { it.category }.filter { it.isNotBlank() && it != "All" }.distinct()
        if (distinct.isEmpty()) {
            listOf("All", "Self Study", "UPSC", "NEET/JEE", "Coding", "College")
        } else {
            listOf("All") + distinct
        }
    }

    // 🔒 1. सामान्य Explore में केवल PUBLIC ग्रुप्स दिखेंगे
    val publicGroups = remember(groups) {
        groups.filter { it.isPublic }
    }

    // 🔍 2. इनवाइट कोड सर्च ट्रिगर (जैसे AMON-AD1523 या कोई कोड)
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length >= 4 && (query.contains("-") || query.startsWith("AMON", ignoreCase = true))) {
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

    // 📋 3. स्क्रीन पर दिखने वाली लिस्ट
    val displayedGroups = remember(searchQuery, selectedCategory, publicGroups, privateGroupResult) {
        if (privateGroupResult != null) {
            listOf(privateGroupResult!!)
        } else {
            publicGroups.filter { group ->
                val matchesCategory = selectedCategory == "All" || group.category.equals(selectedCategory, ignoreCase = true)
                val matchesQuery = searchQuery.isBlank() ||
                        group.name.contains(searchQuery, ignoreCase = true) ||
                        group.category.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore Groups",
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
            // 🔎 सर्च बार: पब्लिक ग्रुप सर्च या प्राइवेट कोड
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = "Search public groups or enter Invite Code...",
                        color = textMuted,
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textMuted,
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

            // 🏷️ कैटेगरी चिप्स
            if (privateGroupResult == null) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableCategories) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = if (isDark) Color.Black else Color.White,
                                containerColor = cardBg,
                                labelColor = textMuted
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) accentColor else cardBorder
                            )
                        )
                    }
                }
            }

            Text(
                text = if (privateGroupResult != null) "Private circle found via invite code" else "${displayedGroups.size} circles found",
                color = if (privateGroupResult != null) Color(0xFFF59E0B) else textMuted,
                fontSize = 11.sp,
                fontWeight = if (privateGroupResult != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (displayedGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No circles found.\nIf this is a private circle, please enter the exact invite code." else "No public groups available.",
                        color = textMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedGroups, key = { it.id }) { group ->
                        ExploreGroupRow(
                            group = group,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textMain = textMain,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            accentSubtle = accentSubtle,
                            focusingGreen = focusingGreen,
                            isDark = isDark,
                            onRowClick = { onGroupClick(group) },
                            onJoinToggle = { onToggleJoinGroup(group.id) }
                        )
                    }
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1
                    )
                    if (!group.isPublic) {
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
                        text = "${group.focusingCount} now · ${group.memberCount} members",
                        color = textMuted,
                        fontSize = 11.sp
                    )
                }

                if (group.category.isNotBlank() && group.category != "All") {
                    Text(
                        text = group.category,
                        color = accentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
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
