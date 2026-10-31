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
import com.amon.timer.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreGroupsScreen(
    groups: List<StudyGroup> = MockRepository.sampleGroups,
    onBackClick: () -> Unit = {},
    onGroupClick: (StudyGroup) -> Unit = {},
    onToggleJoinGroup: (String) -> Unit = {}
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredGroups = groups.filter { group ->
        val matchesCategory = selectedCategory == "All" || group.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                group.name.contains(searchQuery, ignoreCase = true) ||
                group.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore Groups",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "Search by group, exam, or state...",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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

            // Category Chips (Horizontal Scroll)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MockRepository.categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
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

            // Results count
            Text(
                text = "${filteredGroups.size} groups found",
                color = textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Group Rows
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredGroups) { group ->
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
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable { onRowClick() },
        shape = RoundedCornerShape(14.dp),
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentSubtle)
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.take(2).uppercase(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    color = textMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
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
                        text = "${group.focusingCount} now",
                        color = focusingGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " · ${group.memberCount} members",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = group.category,
                    color = textMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (group.isJoined) {
                OutlinedButton(
                    onClick = onJoinToggle,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Joined",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Joined", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = onJoinToggle,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(text = "Join", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
