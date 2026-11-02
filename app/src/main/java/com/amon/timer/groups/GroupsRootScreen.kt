package com.amon.timer.groups

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.amon.timer.CloudSyncManager
import com.amon.timer.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }
    
    var currentScreen by remember { mutableStateOf(GroupsScreenState.HOME) }
    
    // 🌟 असली स्टेट जो क्लाउड से ग्रुप्स सिंक करके रखती है
    var groupsList by remember { mutableStateOf<List<StudyGroup>>(emptyList()) }
    var selectedGroup by remember { 
        mutableStateOf(
            StudyGroup(
                id = "universal_lib",
                name = "Universal Library",
                category = "Global",
                isPublic = true,
                memberCount = 1,
                focusingCount = 0,
                isPinned = true,
                isJoined = false,
                isDefault = true,
                inviteCode = "GLOBAL"
            )
        ) 
    }
    // ✅ MockRepository.CURRENT_USER_NICKNAME हटाकर सेफ़ फ़ॉलबैक दिया गया
    var currentNickname by remember { 
        mutableStateOf(userManager.getUserName().ifBlank { "Aspirant" }) 
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    // 🚀 सुप्राबेस से ग्रुप्स फेच करना
    LaunchedEffect(Unit) {
        val cloudGroups = CloudSyncManager.fetchGroups(context)
        if (cloudGroups.isNotEmpty()) {
            groupsList = cloudGroups
            selectedGroup = cloudGroups[0]
        } else {
            groupsList = listOf(selectedGroup)
        }
    }

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
                    // ➕ जॉइन ग्रुप (सुप्राबेस सिंक के साथ)
                    onJoinGroup = { groupId ->
                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) g.copy(isJoined = true, memberCount = g.memberCount + 1) else g
                        }
                        coroutineScope.launch(Dispatchers.IO) {
                            CloudSyncManager.joinGroupOnCloud(context, groupId)
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
                        val target = groupsList.find { it.id == groupId }
                        val willJoin = target?.isJoined != true

                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) {
                                g.copy(
                                    isJoined = willJoin,
                                    memberCount = if (willJoin) g.memberCount + 1 else (g.memberCount - 1).coerceAtLeast(1)
                                )
                            } else g
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            if (willJoin) {
                                CloudSyncManager.joinGroupOnCloud(context, groupId)
                            } else {
                                CloudSyncManager.leaveGroupOnCloud(context, groupId)
                            }
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

        // ➕ नया ग्रुप बनाने का पूरा फ़ंक्शन (सुप्राबेस में परमानेंट सेव)
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
                            GroupMember(id = userManager.getUserId(), nickname = currentNickname, role = MemberRole.ADMIN, lastActive = "Just now")
                        )
                    )

                    // 1. तुरंत UI में दिखाएँ
                    groupsList = listOf(newGroup) + groupsList
                    selectedGroup = newGroup
                    showCreateSheet = false
                    currentScreen = GroupsScreenState.DETAIL
                    Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()

                    // 2. सुप्राबेस में परमानेंट इन्सर्ट करें
                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.createGroupOnCloud(context, newGroup)
                    }
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
                // 🚪 लीव ग्रुप फ़ंक्शन (सुप्राबेस से डिलीट)
                onLeaveGroup = {
                    val groupId = selectedGroup.id
                    groupsList = groupsList.map { g ->
                        if (g.id == groupId) g.copy(isJoined = false, isPinned = false) else g
                    }
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                    Toast.makeText(context, "You left ${selectedGroup.name}", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.leaveGroupOnCloud(context, groupId)
                    }
                }
            )
        }

        // ✏️ निकनेम बदलने का डायलॉग (सुप्राबेस प्रोफ़ाइल अपडेट के साथ)
        if (showNicknameDialog) {
            NicknameDialog(
                currentNickname = currentNickname,
                onDismiss = { showNicknameDialog = false },
                onSaveNickname = { newName ->
                    currentNickname = newName
                    userManager.setUserName(newName)
                    showNicknameDialog = false
                    Toast.makeText(context, "Nickname updated to $newName", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.registerUserOnCloud(
                            context = context,
                            userId = userManager.getUserId(),
                            nickname = newName
                        )
                    }
                }
            )
        }
    }
}
