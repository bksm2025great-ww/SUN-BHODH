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
            isJoined = true,
            isDefault = true,
            inviteCode = "GLOBAL"
        )
    }

    // ⚡ 0 सेकंड में लोकल कैशे से लिस्ट आएगी - कोई 0 Groups या Join बटन का फ़्लिकर नहीं
    val initialGroups = remember {
        val cached = GroupCacheManager.getCachedGroupsList(context)
        if (cached.isNotEmpty()) cached else listOf(defaultGlobalGroup)
    }

    var groupsList by remember { mutableStateOf(initialGroups) }
    var selectedGroup by remember { mutableStateOf(groupsList.firstOrNull() ?: defaultGlobalGroup) }

    var currentNickname: String by remember {
        mutableStateOf(userManager.getUserName().ifBlank { "Aspirant" })
    }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }

    // 🔄 स्मार्ट बैकग्राउंड सिंक: इंटरनेट का डेटा बॉस के लोकल फ़ैसलों को कभी ओवरराइट नहीं करेगा
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val cloudGroups = CloudSyncManager.fetchGroups(context)
            withContext(Dispatchers.Main) {
                if (cloudGroups.isNotEmpty()) {
                    val mergedGroups = GroupCacheManager.mergeWithCloud(context, cloudGroups)
                    groupsList = mergedGroups
                    selectedGroup = mergedGroups.find { it.id == selectedGroup.id } ?: mergedGroups[0]
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
                        GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    },
                    onJoinGroup = { groupId ->
                        GroupCacheManager.recordLocalJoin(context, groupId)

                        val existing = groupsList.find { it.id == groupId }
                        if (existing != null) {
                            val updated = existing.copy(isJoined = true, memberCount = existing.memberCount + 1)
                            groupsList = groupsList.map { if (it.id == groupId) updated else it }
                            selectedGroup = updated
                            GroupCacheManager.saveCachedGroupsList(context, groupsList)
                            currentScreen = GroupsScreenState.DETAIL
                            Toast.makeText(context, "Joined ${updated.name}!", Toast.LENGTH_SHORT).show()
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            CloudSyncManager.joinGroupOnCloud(context, groupId)
                            val fresh = CloudSyncManager.fetchGroups(context)
                            withContext(Dispatchers.Main) {
                                if (fresh.isNotEmpty()) {
                                    val merged = GroupCacheManager.mergeWithCloud(context, fresh)
                                    groupsList = merged
                                    GroupCacheManager.saveCachedGroupsList(context, merged)
                                    val joinedTarget = merged.find { it.id == groupId }
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

                        if (willJoin) {
                            GroupCacheManager.recordLocalJoin(context, groupId)
                        } else {
                            GroupCacheManager.recordLocalLeave(context, groupId)
                        }

                        groupsList = groupsList.map { g ->
                            if (g.id == groupId) {
                                g.copy(
                                    isJoined = willJoin,
                                    memberCount = if (willJoin) g.memberCount + 1 else (g.memberCount - 1).coerceAtLeast(1)
                                )
                            } else g
                        }
                        GroupCacheManager.saveCachedGroupsList(context, groupsList)

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
                    onSettingsClick = { showSettingsSheet = true },
                    onJoinClick = { groupId ->
                        GroupCacheManager.recordLocalJoin(context, groupId)

                        val target = groupsList.find { it.id == groupId }
                        if (target != null) {
                            val updated = target.copy(isJoined = true, memberCount = target.memberCount + 1)
                            groupsList = groupsList.map { if (it.id == groupId) updated else it }
                            selectedGroup = updated
                            GroupCacheManager.saveCachedGroupsList(context, groupsList)
                            Toast.makeText(context, "Joined ${updated.name}!", Toast.LENGTH_SHORT).show()
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            CloudSyncManager.joinGroupOnCloud(context, groupId)
                            val fresh = CloudSyncManager.fetchGroups(context)
                            withContext(Dispatchers.Main) {
                                if (fresh.isNotEmpty()) {
                                    val merged = GroupCacheManager.mergeWithCloud(context, fresh)
                                    groupsList = merged
                                    GroupCacheManager.saveCachedGroupsList(context, merged)
                                    val joinedTarget = merged.find { it.id == groupId }
                                    if (joinedTarget != null) {
                                        selectedGroup = joinedTarget
                                    }
                                }
                            }
                        }
                    }
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

                    GroupCacheManager.recordLocalJoin(context, newGroup.id)
                    groupsList = listOf(newGroup) + groupsList
                    selectedGroup = newGroup
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
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
            val isUserAdmin = !selectedGroup.isDefault && selectedGroup.members.any { member ->
                member.id == currentUserId && member.role == MemberRole.ADMIN
            }

            GroupSettingsSheet(
                group = selectedGroup,
                isAdmin = isUserAdmin,
                onDismiss = { showSettingsSheet = false },
                onInvite = { showSettingsSheet = false },
                onToggleNotifications = { enabled ->
                    groupsList = groupsList.map { g ->
                        if (g.id == selectedGroup.id) g.copy(notificationsEnabled = enabled) else g
                    }
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    val msg = if (enabled) "Notifications turned ON" else "Notifications turned OFF"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onReportGroup = {
                    showSettingsSheet = false
                    Toast.makeText(context, "Group has been reported. We will review it shortly.", Toast.LENGTH_LONG).show()
                },
                onLeaveGroup = {
                    val groupId = selectedGroup.id
                    GroupCacheManager.recordLocalLeave(context, groupId)

                    groupsList = groupsList.map { g ->
                        if (g.id == groupId) g.copy(isJoined = false, isPinned = false) else g
                    }
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                    Toast.makeText(context, "You left ${selectedGroup.name}", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        CloudSyncManager.leaveGroupOnCloud(context, groupId)
                    }
                },
                onDeleteGroup = {
                    val groupId = selectedGroup.id
                    val deletedGroupName = selectedGroup.name
                    GroupCacheManager.recordLocalLeave(context, groupId)

                    groupsList = groupsList.filter { it.id != groupId }
                    selectedGroup = groupsList.firstOrNull() ?: defaultGlobalGroup
                    GroupCacheManager.saveCachedGroupsList(context, groupsList)
                    showSettingsSheet = false
                    currentScreen = GroupsScreenState.HOME
                    Toast.makeText(context, "Group \"$deletedGroupName\" deleted permanently", Toast.LENGTH_SHORT).show()

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
