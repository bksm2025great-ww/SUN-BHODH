package com.amon.timer.groups

import android.content.Context
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
    DETAIL,
    JOIN_FLOW
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

    val initialGroups = remember {
        val cached = GroupCacheManager.getCachedGroupsList(context)
        if (cached.isNotEmpty()) cached else emptyList()
    }

    var groupsList by remember { mutableStateOf(initialGroups) }
    var selectedGroup by remember { mutableStateOf(groupsList.firstOrNull()) }

    var currentNickname: String by remember {
        mutableStateOf(userManager.getUserName().ifBlank { "Aspirant" })
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val cloudGroups = CloudSyncManager.fetchGroups(context)
            withContext(Dispatchers.Main) {
                if (cloudGroups.isNotEmpty()) {
                    val mergedGroups = GroupCacheManager.mergeWithCloud(context, cloudGroups)
                    groupsList = mergedGroups
                    if (selectedGroup != null) {
                        selectedGroup = mergedGroups.find { it.id == selectedGroup?.id } ?: mergedGroups.firstOrNull()
                    }
                    GroupCacheManager.saveCachedGroupsList(context, mergedGroups)
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
                    onCreateGroupClick = { showCreateSheet = true },
                    onKeyClick = { currentScreen = GroupsScreenState.JOIN_FLOW }, // ✨ अब यह सही से काम करेगा
                    onGroupClick = { group ->
                        selectedGroup = group
                        currentScreen = GroupsScreenState.DETAIL
                    },
                    onNicknameClick = { showNicknameDialog = true },
                    onTogglePin = { groupId ->
                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) g.copy(isPinned = !g.isPinned) else g
                        }
                        GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    },
                    onJoinGroup = { groupId ->
                        // direct join fallback
                    }
                )
            }

            GroupsScreenState.JOIN_FLOW -> {
                // ✨ सही नाम JoinGroupFlowInternalScreen का उपयोग किया गया है
                JoinGroupFlowInternalScreen(
                    userDefaultNickname = currentNickname,
                    onBackClick = { currentScreen = GroupsScreenState.HOME },
                    onSuccessfullyJoined = { joinedGroup ->
                        val updated = joinedGroup.copy(isJoined = true)
                        groupsList = if (groupsList.any { it.id == updated.id }) {
                            groupsList.map { if (it.id == updated.id) updated else it }
                        } else {
                            listOf(updated) + groupsList
                        }
                        selectedGroup = updated
                        GroupCacheManager.saveCachedGroupsList(context, groupsList)
                        currentScreen = GroupsScreenState.DETAIL
                    }
                )
            }

            GroupsScreenState.EXPLORE -> {
                currentScreen = GroupsScreenState.HOME
            }

            GroupsScreenState.DETAIL -> {
                if (selectedGroup != null) {
                    GroupDetailScreen(
                        group = selectedGroup!!,
                        userNickname = currentNickname,
                        onBackClick = { currentScreen = GroupsScreenState.HOME },
                        onInviteClick = { },
                        onSettingsClick = { showSettingsSheet = true },
                        onJoinClick = { groupId ->
                            // join logic
                        }
                    )
                }
            }
        }

        if (showCreateSheet) {
            CreateGroupSheet(
                onDismiss = { showCreateSheet = false },
                onCreateGroup = { name, category, _, inviteCode ->
                    val finalCode = if (inviteCode.isNullOrBlank()) generateSecureGroupCode() else inviteCode.trim()

                    val newGroup = StudyGroup(
                        id = "grp_${System.currentTimeMillis()}",
                        name = name.trim(),
                        category = category.trim(),
                        isPublic = false,
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

                    GroupCacheManager.recordLocalJoin(context, newGroup.id)
                    groupsList = listOf(newGroup) + groupsList
                    selectedGroup = newGroup
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    showCreateSheet = false
                    currentScreen = GroupsScreenState.DETAIL
                    Toast.makeText(context, "Private Group created successfully!", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.createGroupOnCloud(context, newGroup)
                    }
                }
            )
        }

        if (showSettingsSheet && selectedGroup != null) {
            val currentGroup = selectedGroup!!
            val isUserAdmin = !currentGroup.isDefault && currentGroup.members.any { member ->
                member.id == currentUserId && member.role == MemberRole.ADMIN
            }

            GroupSettingsSheet(
                group = currentGroup,
                isAdmin = isUserAdmin,
                onDismiss = { showSettingsSheet = false },
                onInvite = { showSettingsSheet = false },
                onToggleNotifications = { enabled ->
                    groupsList = groupsList.map { g ->
                        if (g.id == currentGroup.id) g.copy(notificationsEnabled = enabled) else g
                    }
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    Toast.makeText(context, if (enabled) "Notifications ON" else "Notifications OFF", Toast.LENGTH_SHORT).show()
                },
                onReportGroup = {
                    showSettingsSheet = false
                    Toast.makeText(context, "Group reported.", Toast.LENGTH_LONG).show()
                },
                onLeaveGroup = {
                    val groupId = currentGroup.id
                    GroupCacheManager.recordLocalLeave(context, groupId)
                    groupsList = groupsList.map { g ->
                        if (g.id == groupId) g.copy(isJoined = false, isPinned = false) else g
                    }
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                    Toast.makeText(context, "You left ${currentGroup.name}", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.leaveGroupOnCloud(context, groupId)
                    }
                },
                onDeleteGroup = {
                    val groupId = currentGroup.id
                    groupsList = groupsList.filter { it.id != groupId }
                    selectedGroup = groupsList.firstOrNull()
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                    Toast.makeText(context, "Group deleted", Toast.LENGTH_SHORT).show()
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
                        CloudSyncManager.registerUserOnCloud(context, currentUserId, newName)
                    }
                }
            )
        }
    }
}
