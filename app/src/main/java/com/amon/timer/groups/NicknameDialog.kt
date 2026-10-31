package com.amon.timer.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amon.timer.ThemeManager

@Composable
fun NicknameDialog(
    currentNickname: String = "Amon_User",
    isFirstTime: Boolean = false,
    onDismiss: () -> Unit = {},
    onSaveNickname: (String) -> Unit = {}
) {
    val isDark = ThemeManager.isDarkTheme.value
    val cardBg = ThemeManager.getCardColor()
    val bgColor = ThemeManager.getBackgroundColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val accentColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
    val accentSubtle = accentColor.copy(alpha = 0.15f)

    var enteredNickname by remember { mutableStateOf(currentNickname) }
    val isValid = enteredNickname.trim().length >= 3

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentSubtle)
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isFirstTime) "Choose Study Nickname" else "Edit Your Nickname",
                    color = textMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your nickname is your identity across all study circles and leaderboards.",
                    color = textMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = enteredNickname,
                    onValueChange = { input ->
                        if (input.length <= 25) {
                            val formatted = input.replace(" ", "_")
                            // 🔤 पहला अक्षर हमेशा कैपिटल
                            enteredNickname = formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Study Nickname", color = textMuted, fontSize = 13.sp) },
                    placeholder = { Text("e.g. Rahul_NEET_AIR1", color = textMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    // ⌨️ कीबोर्ड पर पहला लेटर कैपिटल
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

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tip: Aspirants often include their target exam (e.g., _NEET, _UPSC).",
                    color = textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isFirstTime) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textMuted)
                        ) {
                            Text("Cancel", fontSize = 13.5.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isValid) {
                                onSaveNickname(enteredNickname.trim())
                                onDismiss()
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isDark) Color.Black else Color.White,
                            disabledContainerColor = cardBorder.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isFirstTime) "Start" else "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
