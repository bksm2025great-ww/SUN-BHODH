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
import kotlinx.coroutines.withContext
import java.security.SecureRandom

private enum class GroupsScreenState {
    HOME,
    EXPLORE,
    DETAIL
}

fun generateSecureGroupCode(): String {
    val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    val random = SecureRandom()
    val sb = StringBuilder(8)
    for (i in 0 until 8) {
        sb.append(chars[random.nextInt(chars.length)])
    }
    return "AMON-$sb"
}

@Composable
fun GroupsRootScreen(
    onStartFocus: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }
    val currentUserId = remember { userManager.getUserId() }
    
    var currentScreen by remember { mutableStateOf(GroupsScreenState.HOME) }
    
    val defaultGlobalGroup = remember {
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
    }

    var groupsList by remember { mutableStateOf<List<StudyGroup>>(listOf(defaultGlobalGroup)) }
    var selectedGroup by remember { mutableStateOf(defaultGlobalGroup) }

    var currentNickname: String by remember { 
        mutableStateOf(userManager.getUserName().ifBlank { "Aspirant" }) 
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    // 🔄 क्लाउड से ग्रुप फेच करना और डिफ़ॉल्ट लाइब्रेरी को सुरक्षित रखना
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val cloudGroups = CloudSyncManager.fetchGroups(context)
            withContext(Dispatchers.Main) {
                if (cloudGroups.isNotEmpty()) {
                    groupsList = cloudGroups
                    selectedGroup = cloudGroups.find { it.id == selectedGroup.id } ?: cloudGroups[0]
                } else {
                    // अगर डेटाबेस में Universal Library भी नहीं है, तो क्लाउड पर फिर से बना दो
                    groupsList = listOf(defaultGlobalGroup)
                    selectedGroup = defaultGlobalGroup
                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.createGroupOnCloud(context, defaultGlobalGroup)
                    }
                }
            }
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
                    onTogglePin = { groupId ->
                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) g.copy(isPinned = !g.isPinned) else g
                        }
                    },
                    onJoinGroup = { groupId ->
                        val existing = groupsList.find { it.id == groupId }
                        if (existing != null) {
                            val updated = existing.copy(isJoined = true, memberCount = existing.memberCount + 1)
                            groupsList = groupsList.map { if (it.id == groupId) updated else it }
                            selectedGroup = updated
                            currentScreen = GroupsScreenState.DETAIL
                            Toast.makeText(context, "Joined ${updated.name}!", Toast.LENGTH_SHORT).show()
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            CloudSyncManager.joinGroupOnCloud(context, groupId)
                            val fresh = CloudSyncManager.fetchGroups(context)
                            withContext(Dispatchers.Main) {
                                if (fresh.isNotEmpty()) {
                                    groupsList = fresh
                                    val joinedTarget = fresh.find { it.id == groupId }
                                    if (joinedTarget != null) {
                                        selectedGroup = joinedTarget
                                    }
                                }
                            }
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
                    onInviteClick = { },
                    onSettingsClick = { showSettingsSheet = true }
                )
            }
        }

        if (showCreateSheet) {
            CreateGroupSheet(
                onDismiss = { showCreateSheet = false },
                onCreateGroup = { name, category, isPublic, inviteCode ->
                    val finalCode = if (inviteCode.isNullOrBlank()) generateSecureGroupCode() else inviteCode.trim()

                    val newGroup = StudyGroup(
                        id = "grp_${System.currentTimeMillis()}",
                        name = name.trim(),
                        category = category.trim(),
                        isPublic = isPublic,
                        memberCount = 1,
                        focusingCount = 0,
                        isPinned = false,
                        isJoined = true,
                        isDefault = false,
                        inviteCode = finalCode,
                        members = listOf(
                            GroupMember(id = currentUserId, nickname = currentNickname, role = MemberRole.ADMIN, lastActive = "Just now")
                        )
                    )

                    groupsList = listOf(newGroup) + groupsList
                    selectedGroup = newGroup
                    showCreateSheet = false
                    currentScreen = GroupsScreenState.DETAIL
                    Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.createGroupOnCloud(context, newGroup)
                    }
                }
            )
        }

        if (showSettingsSheet) {
            // 🛡️ केवल असली एडमिन को ही Delete Group दिखेगा (और Universal Library कभी डिलीट नहीं होगी)
            val isUserAdmin = !selectedGroup.isDefault && selectedGroup.members.any { member ->
                member.id == currentUserId && member.role == MemberRole.ADMIN
            }

            GroupSettingsSheet(
                group = selectedGroup,
                isAdmin = isUserAdmin, // ✅ अब सचमुच केवल एडमिन को ही डिलीट बटन मिलेगा
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
                },
                onDeleteGroup = {
                    val groupId = selectedGroup.id
                    groupsList = groupsList.filter { it.id != groupId }
                    selectedGroup = groupsList.firstOrNull() ?: defaultGlobalGroup
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                    Toast.makeText(context, "Group \"${selectedGroup.name}\" deleted permanently", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.deleteGroupOnCloud(groupId)
                    }
                }
            )
        }

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
                            userId = currentUserId,
                            nickname = newName
                        )
                    }
                }
            )
        }
    }
}
