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

    // 🔒 यूनिवर्सल/ग्लोबल ग्रुप हमेशा के लिए हटा दिया गया है (अब सिर्फ प्राइवेट स्टडी सर्कल)
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

    // 🔄 स्मार्ट बैकग्राउंड सिंक: केवल प्राइवेट ग्रुप्स और यूजर के जॉइन किए हुए सर्कल्स सिंक होंगे
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
                // इस स्टेट की अब जरूरत नहीं है क्योंकि सर्च/एक्सप्लोर हटा दिया गया है, पर कंपैटिबिलिटी के लिए होम भेज सकते हैं
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
                        isPublic = false, // हमेशा प्राइवेट रहेगा
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
                    val msg = if (enabled) "Notifications turned ON" else "Notifications turned OFF"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onReportGroup = {
                    showSettingsSheet = false
                    Toast.makeText(context, "Group has been reported. We will review it shortly.", Toast.LENGTH_LONG).show()
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
                    val deletedGroupName = currentGroup.name
                    GroupCacheManager.recordLocalLeave(context, groupId)

                    groupsList = groupsList.filter { it.id != groupId }
                    selectedGroup = groupsList.firstOrNull()
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
