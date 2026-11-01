package com.amon.timer.groups

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsSheet(
    group: StudyGroup,
    isAdmin: Boolean = true,
    onDismiss: () -> Unit = {},
    onInvite: () -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit = {},
    onReportGroup: () -> Unit = {},
    onLeaveGroup: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkTheme.value
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val dangerColor = Color(0xFFEF4444)

    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 🚀 1. शेयर शीट (WhatsApp, Telegram आदि)
    fun launchShareSheet() {
        val shareMessage = """
            Hey! Join my study group "${group.name}" on Sun Bhodh.
            Use Invite Code: ${group.inviteCode}
            Let's focus together! 📚✨
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Invite Aspirants"))
    }

    // 📩 2. असली रिपोर्ट ईमेल इंटेंट
    fun launchReportEmail() {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@sunbhodh.app")) // अपना सपोर्ट ईमेल यहाँ डाल सकते हैं
                putExtra(Intent.EXTRA_SUBJECT, "Report Group: ${group.name} (${group.id})")
                putExtra(Intent.EXTRA_TEXT, "Hi Team,\n\nI want to report this group due to inappropriate content or spam.\n\nDetails:\n")
            }
            context.startActivity(Intent.createChooser(emailIntent, "Send Report"))
        } catch (e: Exception) {
            // अगर ईमेल ऐप न मिले
        }
    }

    // ⚠️ ग्रुप छोड़ने से पहले पक्का करने वाला डायलॉग
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
            // Header
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
                        text = group.name,
                        color = textMuted,
                        fontSize = 12.sp
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

            // 1. Invite Aspirants (एक्टिव)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        onInvite()
                        launchShareSheet()
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
                            text = "Code: ${group.inviteCode} · Tap to share",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Report Group (सीधा ईमेल रिपोर्ट)
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

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Leave Group Button (कन्फ़र्मेशन के साथ)
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
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = dangerColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Leave Group",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
