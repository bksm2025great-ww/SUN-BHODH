package com.amon.timer.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

private enum class GroupsScreenState {
    HOME,
    EXPLORE,
    DETAIL
}

@Composable
fun GroupsRootScreen(
    onStartFocus: () -> Unit = {}
) {
    // स्क्रीन की वर्तमान स्थिति (State)
    var currentScreen by remember { mutableStateOf(GroupsScreenState.HOME) }
    var selectedGroup by remember { mutableStateOf(MockRepository.sampleGroups[0]) }
    var currentNickname by remember { mutableStateOf(MockRepository.CURRENT_USER_NICKNAME) }

    // शीट्स और डायलॉग्स की विजिबिलिटी
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    // फ़ोन का बैक बटन संभालने के लिए BackHandler
    BackHandler(enabled = currentScreen != GroupsScreenState.HOME) {
        currentScreen = GroupsScreenState.HOME
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // मुख्य स्क्रीन्स की अदला-बदली
        when (currentScreen) {
            GroupsScreenState.HOME -> {
                GroupsHomeScreen(
                    userNickname = currentNickname,
                    onSearchClick = { currentScreen = GroupsScreenState.EXPLORE },
                    onCreateGroupClick = { showCreateSheet = true },
                    onGroupClick = { group ->
                        selectedGroup = group
                        currentScreen = GroupsScreenState.DETAIL
                    },
                    onViewAllSuggested = { currentScreen = GroupsScreenState.EXPLORE },
                    onNicknameClick = { showNicknameDialog = true },
                    onTogglePin = { /* Pin toggle logic */ },
                    onJoinGroup = { /* Join group logic */ }
                )
            }

            GroupsScreenState.EXPLORE -> {
                ExploreGroupsScreen(
                    onBackClick = { currentScreen = GroupsScreenState.HOME },
                    onGroupClick = { group ->
                        selectedGroup = group
                        currentScreen = GroupsScreenState.DETAIL
                    },
                    onToggleJoinGroup = { /* Join logic */ }
                )
            }

            GroupsScreenState.DETAIL -> {
                GroupDetailScreen(
                    group = selectedGroup,
                    userNickname = currentNickname,
                    onBackClick = { currentScreen = GroupsScreenState.HOME },
                    onInviteClick = { /* Share invite intent */ },
                    onSettingsClick = { showSettingsSheet = true },
                    onStartFocusClick = onStartFocus
                )
            }
        }

        // 1. ग्रुप बनाने की बॉटम शीट
        if (showCreateSheet) {
            CreateGroupSheet(
                onDismiss = { showCreateSheet = false },
                onCreateGroup = { name, category, isPublic, inviteCode ->
                    showCreateSheet = false
                }
            )
        }

        // 2. ग्रुप सेटिंग्स की बॉटम शीट
        if (showSettingsSheet) {
            GroupSettingsSheet(
                group = selectedGroup,
                onDismiss = { showSettingsSheet = false },
                onLeaveGroup = {
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                }
            )
        }

        // 3. निकनेम बदलने का डायलॉग
        if (showNicknameDialog) {
            NicknameDialog(
                currentNickname = currentNickname,
                onDismiss = { showNicknameDialog = false },
                onSaveNickname = { newName ->
                    currentNickname = newName
                    showNicknameDialog = false
                }
            )
        }
    }
}
