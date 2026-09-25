package com.amon.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun AuthScreen(
    onAuthComplete: () -> Unit
) {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    val scrollState = rememberScrollState()

    // Luxe Gold Theme Colors
    val goldColor = Color(0xFFF3C669)
    val darkBg = Color(0xFF121214)
    val cardBg = Color(0xFF1E1E22)
    val boxDarkGray = Color(0xFF2A2A32)
    val boxBorderGolden = Color(0x33F3C669)
    val textMuted = Color(0xFFA0A0A5)

    // Form State (Sirf Username)
    var username by remember { mutableStateOf(userManager.getUserName().ifEmpty { "Vision" }) }
    var errorMessage by remember { mutableStateOf("") }

    // 🔔 Android ka Asli Default Permission Popup Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // User chahe Allow kare ya Don't allow, bina kisi crash ke aage badhein
        onAuthComplete()
    }

    val completeSetupAndProceed = {
        val cleanName = username.trim().ifEmpty { "Vision" }
        userManager.setUserName(cleanName)
        // SharedPreferences ki safety taaki purana logic isAccountSetupDone par na atke
        userManager.setPassword("active")
        userManager.setBirthday("01/01/2000")
        userManager.setGuestUser(false)

        // Android 13+ me notification permission ka default popup trigger karna
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAuthComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Text(
                text = "Welcome to Amon",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ----------------- 2. INPUT CARD (ONLY NAME) -----------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, boxBorderGolden, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "What should we call you?",
                        color = goldColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            if (errorMessage.isNotEmpty()) errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Apna naam likhein (e.g. Vision)", color = textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = goldColor,
                            unfocusedBorderColor = boxDarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = goldColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ----------------- 3. GOLDEN ACTION BUTTON -----------------
            Button(
                onClick = {
                    if (username.isBlank()) {
                        errorMessage = "Kripya apna naam likhein!"
                    } else {
                        completeSetupAndProceed()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = goldColor)
            ) {
                Text(
                    text = "Start Journey 🚀",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------- 4. GUEST / SKIP BUTTON -----------------
            TextButton(
                onClick = {
                    userManager.setUserName("Guest")
                    userManager.setPassword("active")
                    userManager.setBirthday("01/01/2000")
                    userManager.setGuestUser(true)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onAuthComplete()
                    }
                }
            ) {
                Text(
                    text = "Skip & Continue as Guest",
                    color = textMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
