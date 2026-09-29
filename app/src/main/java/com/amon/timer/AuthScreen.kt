package com.amon.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onAuthComplete: () -> Unit
) {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    val scrollState = rememberScrollState()

    // ✨ Amon Luxe Dark & Gold Theme Colors
    val goldColor = Color(0xFFF3C669)
    val goldSecondary = Color(0xFFE5A93C)
    val darkBg = Color(0xFF0F0F12)
    val cardBg = Color(0xEE18181D)
    val boxDarkGray = Color(0xFF24242B)
    val boxBorderGolden = Color(0x44F3C669)
    val textMuted = Color(0xFFA0A0A8)

    // Form States
    var username by remember { 
        mutableStateOf(
            val saved = userManager.getUserName()
            if (saved == "Guest" || saved == "Amon User") "" else saved
        ) 
    }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showAdminDialog by remember { mutableStateOf(false) }

    // 🔔 Android Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onAuthComplete()
    }

    val proceedToApp = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAuthComplete()
        }
    }

    // 🛡️ Banned Characters Check for Google Sheet Safety
    val bannedChars = listOf('\\', '/', '?', '*', ':', '[', ']')

    val handleLogin = {
        val cleanName = username.trim()
        val cleanPass = password.trim()

        if (cleanName.isBlank()) {
            errorMessage = "कृपया अपना यूज़रनेम दर्ज करें!"
        } else if (cleanName.length < 3) {
            errorMessage = "यूज़रनेम कम से कम 3 अक्षरों का होना चाहिए!"
        } else if (cleanName.any { it in bannedChars }) {
            errorMessage = "नाम में ये चिन्ह नहीं हो सकते: \\ / ? * : [ ]"
        } else if (cleanPass.length < 6) {
            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए!"
        } else {
            // डेटा को लोकल फ़ोन मेमोरी में सुरक्षित सेव करना
            userManager.setUserName(cleanName)
            userManager.setPassword(cleanPass)
            userManager.setGuestUser(false)
            proceedToApp()
        }
    }

    val handleGuestLogin = {
        userManager.setUserName("Guest")
        userManager.setPassword("guest123")
        userManager.setGuestUser(true)
        proceedToApp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        // ----------------- 0. FLOATING STUDY ICONS (LIGHTWEIGHT & ZERO LAG) -----------------
        Text("📖", fontSize = 42.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 40.dp).alpha(0.12f))
        Text("⏱️", fontSize = 38.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 28.dp, top = 55.dp).alpha(0.12f))
        Text("✏️", fontSize = 36.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp, top = 160.dp).alpha(0.10f))
        Text("💡", fontSize = 40.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp, top = 140.dp).alpha(0.12f))
        Text("🧠", fontSize = 36.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 90.dp).alpha(0.10f))
        Text("📚", fontSize = 40.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 100.dp).alpha(0.12f))

        // ----------------- MAIN SCROLLABLE CONTENT -----------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ----------------- 1. BRAND HEADER -----------------
                Text(
                    text = "👑 AMON",
                    color = goldColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Time of Angel",
                    color = textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ----------------- 2. PREMIUM FLOATING CARD -----------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(cardBg)
                        .border(1.2.dp, boxBorderGolden, RoundedCornerShape(26.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "LOGIN",
                            color = goldColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // --- USERNAME INPUT ---
                        OutlinedTextField(
                            value = username,
                            onValueChange = { 
                                username = it
                                if (errorMessage.isNotEmpty()) errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Text("👤", fontSize = 16.sp)
                            },
                            placeholder = { Text("Username", color = textMuted, fontSize = 14.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = goldColor,
                                unfocusedBorderColor = boxDarkGray,
                                focusedContainerColor = boxDarkGray,
                                unfocusedContainerColor = boxDarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = goldColor
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // --- PASSWORD INPUT (6 CHARACTERS) ---
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                if (errorMessage.isNotEmpty()) errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Text("🔒", fontSize = 16.sp)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(if (passwordVisible) "👁️" else "🙈", fontSize = 15.sp)
                                }
                            },
                            placeholder = { Text("Password (6 characters)", color = textMuted, fontSize = 14.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = goldColor,
                                unfocusedBorderColor = boxDarkGray,
                                focusedContainerColor = boxDarkGray,
                                unfocusedContainerColor = boxDarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = goldColor
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Error Message
                        if (errorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // --- PRIMARY ACTION: LOGIN BUTTON ---
                        Button(
                            onClick = { handleLogin() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                        ) {
                            Text(
                                text = "LOGIN",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // 📐 Spacing Reduced to 10.dp for Tight, Symmetrical Look
                        Spacer(modifier = Modifier.height(10.dp))

                        // --- SECONDARY ACTION: COMPACT GUEST BUTTON (HALF WEIGHT) ---
                        Button(
                            onClick = { handleGuestLogin() },
                            modifier = Modifier
                                .width(200.dp)
                                .height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = boxDarkGray),
                            border = androidx.compose.foundation.BorderStroke(1.dp, boxBorderGolden),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Continue as Guest",
                                color = goldColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // --- ADMIN SUPPORT / FORGOT PASSWORD LINK ---
                        Text(
                            text = "Forgot Password? Contact Admin",
                            color = textMuted,
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { showAdminDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ----------------- 3. STEPPED LADDER MOTTO FOOTER -----------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Step 1: Left Aligned
                Text(
                    text = "🌱 1. Cultivate Consistency",
                    color = goldColor.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                // Step 2: Center Aligned
                Text(
                    text = "⚡ 2. Master Deep Focus",
                    color = goldSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                // Step 3: Right Aligned
                Text(
                    text = "🌸 3. Build Unbroken Habits",
                    color = goldColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }

        // ----------------- ADMIN SUPPORT DIALOG -----------------
        if (showAdminDialog) {
            AlertDialog(
                onDismissRequest = { showAdminDialog = false },
                title = {
                    Text(text = "Admin Support 🛡️", color = goldColor, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        text = "अगर आप अपना पासवर्ड भूल गए हैं, तो कृपया एडमिन से संपर्क करें।\n\nएडमिन आपकी 'Users_List' शीट से पुष्टि करके आपका पासवर्ड रीसेट कर देंगे।",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAdminDialog = false }) {
                        Text("ठीक है", color = goldColor, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = cardBg,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}
