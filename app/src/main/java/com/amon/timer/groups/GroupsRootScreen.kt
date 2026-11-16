package com.amon.timer.groups

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.CloudSyncManager
import com.amon.timer.ThemeManager
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
                    onKeyClick = { currentScreen = GroupsScreenState.JOIN_FLOW },
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
                // ✨ अब यह फंक्शन इसी फाइल के नीचे मौजूद है, इसलिए कोई अनरिज्ॉल्व्ड एरर नहीं आएगा
                JoinGroupFlowInternalScreen(
                    userDefaultNickname = currentNickname,
                    onBackClick = { currentScreen = GroupsScreenState.HOME },
                    onSuccessfullyJoined = { joinedGroup: StudyGroup ->
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

// ✨✨✨ JoinGroupFlowInternalScreen अब इसी फाइल के अंदर मौजूद है (कोई अनरिज्ॉल्व्ड एरर नहीं आएगा)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupFlowInternalScreen(
    userDefaultNickname: String,
    onBackClick: () -> Unit,
    onSuccessfullyJoined: (StudyGroup) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val focusingGreen = Color(0xFF22C55E)

    var currentStep by remember { mutableIntStateOf(1) }
    var inviteCodeInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var foundGroup by remember { mutableStateOf<StudyGroup?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var groupNickname by remember { mutableStateOf(userDefaultNickname) }
    var isJoining by remember { mutableStateOf(false) }

    LaunchedEffect(inviteCodeInput) {
        val clean = inviteCodeInput.trim()
        if (clean.length >= 4) {
            isSearching = true
            errorMessage = ""
            coroutineScope.launch(Dispatchers.IO) {
                val result = CloudSyncManager.fetchGroupByInviteCode(clean)
                withContext(Dispatchers.Main) {
                    isSearching = false
                    foundGroup = result
                    if (result == null && clean.length >= 8) {
                        errorMessage = "Invalid invite code. No circle found."
                    }
                }
            }
        } else {
            foundGroup = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentStep == 1) "Join with code" else "Preview + nickname",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) currentStep = 1 else onBackClick()
                        },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentStep == 1) {
                    Text(text = "Enter invite code", color = textMain, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Ask the group admin for the invitation code.", color = textMuted, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "Invite code", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { 
                                inviteCodeInput = it.uppercase()
                                errorMessage = ""
                            },
                            placeholder = { Text("e.g. A7K9Q2", color = textMuted, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = cardBorder,
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                cursorColor = accentColor
                            )
                        )

                        OutlinedButton(
                            onClick = {
                                val clipData = clipboardManager.primaryClip
                                if (clipData != null && clipData.itemCount > 0) {
                                    val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                                    if (pastedText.isNotBlank()) {
                                        inviteCodeInput = pastedText.trim().uppercase()
                                    }
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Text(text = "Paste", color = textMain, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage, color = Color(0xFFEF4444), fontSize = 11.5.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (foundGroup != null) {
                        val group = foundGroup!!
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentStep = 2 },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, focusingGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(accentSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = group.name.take(1).uppercase(),
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = group.name, color = textMain, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1)
                                        Text(text = "Private · ${group.memberCount} members", color = textMuted, fontSize = 11.5.sp)
                                    }
                                }

                                Text(text = "Group found", color = focusingGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                } else {
                    val group = foundGroup ?: return@Column

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(accentSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = group.name.take(1).uppercase(), color = accentColor, fontWeight = FontWeight.Black, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = group.name, color = textMain, fontSize = 20.sp, fontWeight = FontWeight.Black)

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = cardBorder.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Private",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Daily study together. Be consistent, cheer each other on.",
                            color = textMuted,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${group.memberCount}", color = textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "Members", color = textMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "62h", color = textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "This week", color = textMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${group.focusingCount}", color = focusingGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "Focusing now", color = textMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text(text = "Your nickname in this group", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = groupNickname,
                                onValueChange = { groupNickname = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = textMain,
                                    unfocusedTextColor = textMain,
                                    cursorColor = accentColor
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Only this nickname is shown. Your real name stays hidden.", color = textMuted.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }

            val buttonText = if (currentStep == 1) "Continue" else "Join group"
            val isButtonEnabled = if (currentStep == 1) foundGroup != null else groupNickname.isNotBlank() && !isJoining

            Button(
                onClick = {
                    if (currentStep == 1) {
                        if (foundGroup != null) currentStep = 2
                    } else {
                        val target = foundGroup ?: return@Button
                        isJoining = true
                        coroutineScope.launch(Dispatchers.IO) {
                            GroupCacheManager.recordLocalJoin(context, target.id)
                            CloudSyncManager.joinGroupOnCloud(context, target.id)
                            withContext(Dispatchers.Main) {
                                isJoining = false
                                Toast.makeText(context, "Successfully joined ${target.name}!", Toast.LENGTH_SHORT).show()
                                onSuccessfullyJoined(target)
                            }
                        }
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.4f)
                )
            ) {
                if (isJoining) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text(text = buttonText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
