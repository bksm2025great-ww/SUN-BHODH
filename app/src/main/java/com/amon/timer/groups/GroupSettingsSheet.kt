package com.amon.timer.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import com.amon.timer.ThemeManager
import com.amon.timer.UserManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsSheet(
    group: StudyGroup,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit = {},
    onInvite: () -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit = {},
    onReportGroup: () -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onDeleteGroup: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkTheme.value
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val accentSubtle = accentColor.copy(alpha = 0.15f)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val dangerColor = Color(0xFFEF4444)

    val userManager = remember { UserManager(context) }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var showInviteCodeView by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val inviteCode = group.inviteCode.ifBlank { "N/A" }

    val copyInviteCodeToClipboard = {
        val clip = ClipData.newPlainText("Invite Code", inviteCode)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Invite code copied: $inviteCode", Toast.LENGTH_SHORT).show()
    }

    fun launchReportEmail() {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("bksm2025great@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Report Group: ${group.name} (${group.id})")
                putExtra(Intent.EXTRA_TEXT, "Hi Team,\n\nI want to report this group due to inappropriate content or spam.\n\nDetails:\n")
            }
            context.startActivity(Intent.createChooser(emailIntent, "Send Report"))
        } catch (_: Exception) {}
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            containerColor = cardBg,
            title = {
                Text(
                    text = "Leave Group?",
                    color = textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to leave \"${group.name}\"? You can join back anytime using the group code.",
                    color = textMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirmDialog = false
                        GroupCacheManager.clearGroupCache(context, group.id)
                        onLeaveGroup()
                    }
                ) {
                    Text("Leave", color = dangerColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Cancel", color = textMain)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = cardBg,
            title = {
                Text(
                    text = "Delete Group Permanently?",
                    color = dangerColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${group.name}\"? All members will be removed and this cannot be undone.",
                    color = textMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        GroupCacheManager.clearGroupCache(context, group.id)
                        onDeleteGroup()
                    }
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = textMain)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = cardBorder) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            if (showInviteCodeView) {
                // ================= INVITE CODE BOTTOM SHEET VIEW =================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showInviteCodeView = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = textMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Invite Aspirants",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Share this unique code with friends to let them join \"${group.name}\".",
                        color = textMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Code Display Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = accentSubtle,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "GROUP INVITE CODE",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = inviteCode,
                                color = textMain,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            copyInviteCodeToClipboard()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Invite Code",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

            } else {
                // ================= MAIN SETTINGS VIEW =================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Group Settings",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${group.name} ${if (isAdmin) "· (Admin)" else ""}",
                            color = if (isAdmin) accentColor else textMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isAdmin) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🎯 Invite Aspirants Card -> Opens Invite Code View Inside Sheet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            onInvite()
                            showInviteCodeView = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invite Aspirants",
                                color = textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Code: $inviteCode · Tap to view code",
                                color = textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Report Group Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            onReportGroup()
                            launchReportEmail()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Report Group",
                                color = textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Flag spam or inappropriate content via email",
                                color = textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // 🛑 Universal Library (Default group) को कोई लीव या डिलीट नहीं कर सकता
                if (!group.isDefault) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Leave Group Action (सभी मेंबर्स के लिए)
                    OutlinedButton(
                        onClick = { showLeaveConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        border = BorderStroke(1.2.dp, dangerColor.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = dangerColor,
                            containerColor = dangerColor.copy(alpha = 0.06f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = dangerColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Leave Group",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Delete Group Action (केवल ग्रुप एडमिन के लिए)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dangerColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete Group",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
