package com.amon.timer.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupSheet(
    onDismiss: () -> Unit = {},
    onCreateGroup: (name: String, category: String, isPublic: Boolean, inviteCode: String?) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val isDark = ThemeManager.isDarkTheme.value
    val cardBg = ThemeManager.getCardColor()
    val bgColor = ThemeManager.getBackgroundColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val accentSubtle = accentColor.copy(alpha = 0.15f)

    var groupName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MockRepository.categories.getOrElse(1) { "General" }) }
    var isCodeCopied by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 🔒 8-character random alphanumeric combo without prefix (matching root screen update)
    val generatedInviteCode = remember {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val random = SecureRandom()
        val sb = StringBuilder(8)
        for (i in 0 until 8) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        sb.toString()
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Study Circle",
                    color = textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                Text(
                    text = "Cancel",
                    color = textMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Group Name",
                color = textMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = groupName,
                onValueChange = { input ->
                    groupName = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. Morning Study Squad",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = accentColor
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = bgColor,
                    unfocusedContainerColor = bgColor,
                    focusedTextColor = textMain,
                    unfocusedTextColor = textMain,
                    cursorColor = accentColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔒 Always Private & Invite Code display card right on the sheet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = accentSubtle),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Private Study Circle",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Only members with this unique 8-character invite code can join.",
                        color = textMuted,
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "UNIQUE INVITE CODE",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = generatedInviteCode,
                                color = textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Invite Code", generatedInviteCode)
                                clipboardManager.setPrimaryClip(clip)
                                isCodeCopied = true
                                Toast.makeText(context, "Invite code copied: $generatedInviteCode", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreateGroup(
                            groupName.trim(),
                            selectedCategory,
                            false, // Always private
                            generatedInviteCode
                        )
                        onDismiss()
                    }
                },
                enabled = groupName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (isDark) Color.Black else Color.White,
                    disabledContainerColor = cardBorder.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Create Circle",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
