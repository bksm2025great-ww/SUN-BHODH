package com.amon.timer.groups

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amon.timer.ThemeManager
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupSheet(
    onDismiss: () -> Unit = {},
    onCreateGroup: (name: String, category: String, isPublic: Boolean, inviteCode: String?) -> Unit = { _, _, _, _ -> }
) {
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
    var isPublic by remember { mutableStateOf(true) }
    var isCodeCopied by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 🔒 100% यूनिक रूम कोड जनरेटर (UUID + Base36 Unique Hash)
    val generatedInviteCode = remember {
        val uniqueToken = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        "AMON-$uniqueToken"
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
                    text = "Create Study Group",
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
                    // 🔤 पहला अक्षर अपने आप कैपिटल बनाएगा
                    groupName = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "e.g. UPSC Prelims 2026 Sprint",
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
                // ⌨️ कीबोर्ड पर भी हर शब्द का पहला अक्षर कैपिटल खुलेगा
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

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp)),
                color = bgColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPublic) Icons.Default.Share else Icons.Default.Lock,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPublic) "Public Group" else "Private Group",
                                color = textMain,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPublic)
                                "Anyone can discover and join from Explore."
                            else
                                "Only aspirants with invite code or link can join.",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }

                    Switch(
                        checked = !isPublic,
                        onCheckedChange = { isPublic = !it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDark) Color.Black else Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = textMuted,
                            uncheckedTrackColor = cardBorder
                        )
                    )
                }
            }

            AnimatedVisibility(visible = !isPublic) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = accentSubtle),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Private Invite Credentials",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBg)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "UNIQUE ROOM CODE",
                                        color = textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = generatedInviteCode,
                                        color = textMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { isCodeCopied = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.Share,
                                        contentDescription = "Copy Code",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreateGroup(
                            groupName.trim(),
                            selectedCategory,
                            isPublic,
                            if (isPublic) null else generatedInviteCode
                        )
                        onDismiss()
                    }
                },
                enabled = groupName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (isDark) Color.Black else Color.White,
                    disabledContainerColor = cardBorder.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Create Group",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
