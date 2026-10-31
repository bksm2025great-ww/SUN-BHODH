package com.amon.timer.groups

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

private enum class GroupsScreenState {
    HOME,
    EXPLORE,
    DETAIL
}

@Composable
fun GroupsRootScreen(
    onStartFocus: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(GroupsScreenState.HOME) }
    
    // 🌟 असली स्टेट जो हर बदलाव को तुरंत UI में दिखाती है
    var groupsList by remember { mutableStateOf(MockRepository.sampleGroups) }
    var selectedGroup by remember { mutableStateOf(groupsList[0]) }
    var currentNickname by remember { mutableStateOf(MockRepository.CURRENT_USER_NICKNAME) }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentScreen != GroupsScreenState.HOME) {
        currentScreen = GroupsScreenState.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            GroupsScreenState.HOME -> {
                GroupsHomeScreen(
                    groups = groupsList,
                    userNickname = currentNickname,
                    onSearchClick = { currentScreen = GroupsScreenState.EXPLORE },
                    onCreateGroupClick = { showCreateSheet = true },
                    onGroupClick = { group ->
                        selectedGroup = group
                        currentScreen = GroupsScreenState.DETAIL
                    },
                    onViewAllSuggested = { currentScreen = GroupsScreenState.EXPLORE },
                    onNicknameClick = { showNicknameDialog = true },
                    // ⭐ स्टार टॉगल
                    onTogglePin = { groupId ->
                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) g.copy(isPinned = !g.isPinned) else g
                        }
                    },
                    // ➕ जॉइन ग्रुप
                    onJoinGroup = { groupId ->
                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) g.copy(isJoined = true, memberCount = g.memberCount + 1) else g
                        }
                    }
                )
            }

            GroupsScreenState.EXPLORE -> {
                ExploreGroupsScreen(
                    groups = groupsList,
                    onBackClick = { currentScreen = GroupsScreenState.HOME },
                    onGroupClick = { group ->
                        selectedGroup = group
                        currentScreen = GroupsScreenState.DETAIL
                    },
                    onToggleJoinGroup = { groupId ->
                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) {
                                val newJoined = !g.isJoined
                                g.copy(
                                    isJoined = newJoined,
                                    memberCount = if (newJoined) g.memberCount + 1 else (g.memberCount - 1).coerceAtLeast(1)
                                )
                            } else g
                        }
                    }
                )
            }

            GroupsScreenState.DETAIL -> {
                GroupDetailScreen(
                    group = selectedGroup,
                    userNickname = currentNickname,
                    onBackClick = { currentScreen = GroupsScreenState.HOME },
                    onInviteClick = { /* Share handled inside GroupDetailScreen */ },
                    onSettingsClick = { showSettingsSheet = true }
                )
            }
        }

        // ➕ नया ग्रुप बनाने का पूरा फंक्शन
        if (showCreateSheet) {
            CreateGroupSheet(
                onDismiss = { showCreateSheet = false },
                onCreateGroup = { name, category, isPublic, inviteCode ->
                    val newGroup = StudyGroup(
                        id = "grp_${System.currentTimeMillis()}",
                        name = name,
                        category = category,
                        isPublic = isPublic,
                        memberCount = 1,
                        focusingCount = 0,
                        isPinned = false,
                        isJoined = true,
                        isDefault = false,
                        inviteCode = inviteCode,
                        members = listOf(
                            GroupMember(id = "user_me", nickname = currentNickname, role = MemberRole.ADMIN, lastActive = "Just now")
                        )
                    )
                    groupsList = listOf(newGroup) + groupsList
                    selectedGroup = newGroup
                    showCreateSheet = false
                    currentScreen = GroupsScreenState.DETAIL
                    Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // ⚙️ सेटिंग्स शीट (Leave Group और Notifications के साथ)
        if (showSettingsSheet) {
            GroupSettingsSheet(
                group = selectedGroup,
                isAdmin = true,
                onDismiss = { showSettingsSheet = false },
                onInvite = { showSettingsSheet = false },
                onToggleNotifications = { enabled ->
                    groupsList = groupsList.map { g ->
                        if (g.id == selectedGroup.id) g.copy(notificationsEnabled = enabled) else g
                    }
                    val msg = if (enabled) "Notifications turned ON" else "Notifications turned OFF"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onReportGroup = {
                    showSettingsSheet = false
                    Toast.makeText(context, "Group has been reported. We will review it shortly.", Toast.LENGTH_LONG).show()
                },
                // 🚪 लीव ग्रुप फंक्शन
                onLeaveGroup = {
                    if (selectedGroup.isDefault) {
                        Toast.makeText(context, "You cannot leave the default Global Library.", Toast.LENGTH_SHORT).show()
                    } else {
                        groupsList = groupsList.map { g ->
                            if (g.id == selectedGroup.id) g.copy(isJoined = false, isPinned = false) else g
                        }
                        showSettingsSheet = false
                        currentScreen = GroupsScreenState.HOME
                        Toast.makeText(context, "You left ${selectedGroup.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // ✏️ निकनेम बदलने का डायलॉग
        if (showNicknameDialog) {
            NicknameDialog(
                currentNickname = currentNickname,
                onDismiss = { showNicknameDialog = false },
                onSaveNickname = { newName ->
                    currentNickname = newName
                    showNicknameDialog = false
                    Toast.makeText(context, "Nickname updated to $newName", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
