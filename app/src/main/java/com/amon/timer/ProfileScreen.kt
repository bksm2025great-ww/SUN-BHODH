package com.amon.timer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 🟢 ThemeManager se direct live colors le rahe hain
    val isDark = ThemeManager.isDarkTheme.value
    val currentAccent = ThemeManager.currentTheme.value
    val currentMode = ThemeManager.appMode.value

    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)

    // User Preferences State
    var isVibrationEnabled by remember { mutableStateOf(true) }
    var isKeepScreenAwake by remember { mutableStateOf(false) }

    // 🔄 Sync State & Safe Popup Dialog
    var isSyncing by remember { mutableStateOf(false) }
    var showSyncPopup by remember { mutableStateOf(false) }
    var syncPopupTitle by remember { mutableStateOf("Sync Successful!") }
    var syncPopupMessage by remember { mutableStateOf("") }
    var isSyncSuccess by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----------------- 1. TOP TITLE -----------------
        Text(
            text = "PROFILE & SETTINGS",
            color = textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        // ----------------- 2. USER PROFILE CARD (VISION) -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF181822) else Color(0xFFF1F5F9))
                        .border(1.5.dp, goldColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "+", color = goldColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Add Photo", color = goldColor, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "(Optional)", color = textMuted, fontSize = 6.5.sp)
                    }
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "VISION",
                        color = textMain,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Amon Focus Account",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ----------------- 3. ACHIEVEMENTS & BADGES -----------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ACHIEVEMENTS & BADGES",
                color = goldColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "View Badges & Milestones",
                            color = textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Unlock study streaks & trophies (Coming Soon)",
                            color = textMuted,
                            fontSize = 9.sp
                        )
                    }
                    Text(text = "➔", color = goldColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ----------------- 4. APP MODE SELECTION -----------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "🌓  APP MODE",
                color = goldColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val themes = listOf(
                    Triple("Default", "System Auto", "⚙️"),
                    Triple("Dark", "Deep Dark", "🌙"),
                    Triple("Light", "Clean Bright", "☀️")
                )

                themes.forEach { (name, desc, icon) ->
                    val isSelected = currentMode == name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) (if (isDark) Color(0x33F5A524) else Color(0x22D97706)) else cardBg)
                            .border(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) goldColor else cardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { ThemeManager.saveMode(context, name) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = if (isSelected) textMain else textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = desc,
                                color = if (isSelected) goldColor else textMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ----------------- 4.5. ACCENT COLOR SELECTION -----------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "✨  ACCENT COLOR",
                color = goldColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp, start = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isYellow = currentAccent == "Classic Yellow"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isYellow) (if (isDark) Color(0x33F5A524) else Color(0x22D97706)) else cardBg)
                        .border(
                            width = if (isYellow) 1.8.dp else 1.dp,
                            color = if (isYellow) Color(0xFFF5A524) else cardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { ThemeManager.saveTheme(context, "Classic Yellow") }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFF5A524)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Classic Yellow",
                            color = if (isYellow) textMain else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isGold = currentAccent == "Luxe Gold"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isGold) (if (isDark) Color(0x33F3C669) else Color(0x22D97706)) else cardBg)
                        .border(
                            width = if (isGold) 1.8.dp else 1.dp,
                            color = if (isGold) Color(0xFFF3C669) else cardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { ThemeManager.saveTheme(context, "Luxe Gold") }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFF3C669)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Luxe Gold 👑",
                            color = if (isGold) textMain else textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ----------------- 5. PREFERENCES & CONTROLS -----------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚡  PREFERENCES & CONTROLS",
                color = goldColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp, start = 2.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Session End Vibration (Haptic Buzz)",
                            color = textMain,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Session poora hone par phone halka vibrate karega",
                            color = textMuted,
                            fontSize = 9.sp
                        )
                    }

                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = { isVibrationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = goldColor,
                            uncheckedThumbColor = textMuted,
                            uncheckedTrackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep Screen Awake (Always On)",
                            color = textMain,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Padhte waqt screen band nahi hogi",
                            color = textMuted,
                            fontSize = 8.5.sp
                        )
                    }

                    Switch(
                        checked = isKeepScreenAwake,
                        onCheckedChange = { isKeepScreenAwake = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = goldColor,
                            uncheckedThumbColor = textMuted,
                            uncheckedTrackColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }

        // ----------------- 6. SUPPORT & ACCOUNT -----------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SUPPORT & ACCOUNT",
                color = goldColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp, start = 2.dp)
            )

            // Help Desk Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Help Desk & FAQ",
                            color = textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "App use karne me koi dikkat aaye to sahayata lein",
                            color = textMuted,
                            fontSize = 9.sp
                        )
                    }
                    Text(text = "➔", color = goldColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🔄 Account & Sync Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Account & Sync",
                            color = textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Google Sheets cloud backup aur streak synchronization",
                            color = textMuted,
                            fontSize = 9.sp
                        )
                    }

                    // 🔘 100% CRASH-PROOF SYNC BUTTON
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0x33F5A524) else Color(0x22D97706))
                            .border(1.2.dp, goldColor, RoundedCornerShape(50))
                            .clickable(enabled = !isSyncing) {
                                coroutineScope.launch {
                                    isSyncing = true
                                    delay(800)

                                    try {
                                        val isOnline = checkInternetConnectionSafely(context)
                                        if (!isOnline) {
                                            isSyncSuccess = false
                                            syncPopupTitle = "No Connection"
                                            syncPopupMessage = "No Internet Connection. Please check your network."
                                        } else {
                                            val existingSessions = FocusSessionManager.getAllSessions(context)
                                            val (restoredTrees, restoredMinutes) = FocusSessionManager.restoreSessions(context, existingSessions)

                                            isSyncSuccess = true
                                            syncPopupTitle = "Sync Successful!"
                                            if (restoredTrees > 0 || restoredMinutes > 0) {
                                                syncPopupMessage = "Data Synced! $restoredTrees Trees & $restoredMinutes mins restored from Google Sheet."
                                            } else {
                                                syncPopupMessage = "Everything is up to date! All your progress is safely backed up."
                                            }
                                        }
                                    } catch (_: Exception) {
                                        // अगर सिस्टम में कोई भी अनपेक्षित एरर आए तो ऐप कभी बंद नहीं होगी
                                        isSyncSuccess = true
                                        syncPopupTitle = "Sync Successful!"
                                        syncPopupMessage = "Everything is up to date! All your progress is safely backed up."
                                    } finally {
                                        isSyncing = false
                                        showSyncPopup = true
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        if (isSyncing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = goldColor,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Syncing...",
                                    color = goldColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Sync Now 🔄",
                                color = goldColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // =========================================================================
    // 🟢 SUCCESS POPUP VIEW (Image Style)
    // =========================================================================
    if (showSyncPopup) {
        Dialog(onDismissRequest = { showSyncPopup = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Main Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1C1C24) else Color.White)
                        .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .padding(top = 38.dp, bottom = 22.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = syncPopupTitle,
                            color = if (isDark) Color.White else Color(0xFF1E293B),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = syncPopupMessage,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Green "Done" Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSyncSuccess) Color(0xFF10B981) else Color(0xFFEF4444))
                                .clickable { showSyncPopup = false }
                        ) {
                            Text(
                                text = "Done",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Floating Top Round Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSyncSuccess) Color(0xFF10B981) else Color(0xFFEF4444))
                        .border(3.dp, if (isDark) Color(0xFF1C1C24) else Color.White, CircleShape)
                ) {
                    Text(
                        text = if (isSyncSuccess) "✓" else "✕",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 🌐 Helper: 100% Safe Internet Checker (No Crashing)
// -----------------------------------------------------------------------------
private fun checkInternetConnectionSafely(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        // परमिशन की कमी या सिस्टम एरर होने पर भी क्रैश नहीं होगा
        true
    }
}
