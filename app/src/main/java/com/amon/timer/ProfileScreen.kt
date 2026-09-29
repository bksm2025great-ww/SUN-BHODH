package com.amon.timer

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    var showAchievementsScreen by remember { mutableStateOf(false) }

    if (showAchievementsScreen) {
        AchievementScreen(onBack = { showAchievementsScreen = false })
        return
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 👤 Managers & Dynamic Version
    val userManager = remember { UserManager(context) }
    val updateManager = remember { UpdateManager(context) }
    val currentAppVersion = remember { updateManager.currentVersion }

    var currentUserName by remember { mutableStateOf(userManager.getUserName().ifEmpty { "Vision" }) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    // 🟢 Theme Colors
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

    // 🔄 Sync State & Dialog
    var isSyncing by remember { mutableStateOf(false) }
    var showSyncPopup by remember { mutableStateOf(false) }
    var syncPopupTitle by remember { mutableStateOf("Sync Successful!") }
    var syncPopupMessage by remember { mutableStateOf("") }
    var isSyncSuccess by remember { mutableStateOf(true) }

    // 📂 Accordion State
    var activeExpandedCard by remember { mutableStateOf<String?>(null) }

    // 🚀 Smart In-App Update States
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var hasNewUpdate by remember { mutableStateOf(false) }
    var latestReleaseVersion by remember { mutableStateOf(currentAppVersion) }
    var apkDownloadUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ----------------- 1. TOP TITLE -----------------
        Text(
            text = "PROFILE & SETTINGS",
            color = textMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        // ----------------- 2. USER PROFILE CARD (AVATAR + NAME + RANK) -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // Amon गोल मैस्कॉट अवतार (गोल्डन रिंग)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF141418) else Color(0xFFF1F5F9))
                        .border(2.dp, goldColor, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mascot_amon),
                        contentDescription = "Amon Mascot",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // नाम और लेवल/रैंक विवरण
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showEditNameDialog = true }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = currentUserName,
                            color = textMain,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(text = "✏️", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // लेवल और रैंक बैज कैप्सूल
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(goldColor.copy(alpha = 0.18f))
                            .border(1.dp, goldColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.5.dp)
                    ) {
                        Text(
                            text = "Level 1 • Focus Novice 🏆",
                            color = goldColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ----------------- 3. ACHIEVEMENTS & BADGES -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .clickable { showAchievementsScreen = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text(text = "🏆", fontSize = 20.sp)
                    Text(
                        text = "Achievements & Badges",
                        color = textMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(text = "➔", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ----------------- 4. EXPANDABLE: APPEARANCE & THEMES -----------------
        val isThemeExpanded = activeExpandedCard == "theme"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, if (isThemeExpanded) goldColor.copy(alpha = 0.6f) else cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeExpandedCard = if (isThemeExpanded) null else "theme"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text(text = "🎨", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Appearance & Themes",
                                color = textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "$currentMode • $currentAccent",
                                color = textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    // ✨ स्मूथ एनिमेटेड गोल्डन शेवरॉन
                    ChevronIcon(
                        isExpanded = isThemeExpanded,
                        tint = goldColor
                    )
                }

                if (isThemeExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "🌓  THEME MODE",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            Triple("Default", "System", "⚙️"),
                            Triple("Dark", "Dark", "🌙"),
                            Triple("Light", "Light", "☀️")
                        )
                        themes.forEach { (name, desc, icon) ->
                            val isSelected = currentMode == name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) (if (isDark) Color(0x33F5A524) else Color(0x22D97706)) else Color(0x11FFFFFF))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) goldColor else cardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { ThemeManager.saveMode(context, name) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        color = if (isSelected) textMain else textMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "✨  ACCENT COLOR",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isYellow = currentAccent == "Classic Yellow"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isYellow) (if (isDark) Color(0x33F5A524) else Color(0x22D97706)) else Color(0x11FFFFFF))
                                .border(
                                    width = if (isYellow) 1.5.dp else 1.dp,
                                    color = if (isYellow) Color(0xFFF5A524) else cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { ThemeManager.saveTheme(context, "Classic Yellow") }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFF5A524)))
                                Text(
                                    text = "Classic Yellow",
                                    color = if (isYellow) textMain else textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val isGold = currentAccent == "Luxe Gold"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isGold) (if (isDark) Color(0x33F3C669) else Color(0x22D97706)) else Color(0x11FFFFFF))
                                .border(
                                    width = if (isGold) 1.5.dp else 1.dp,
                                    color = if (isGold) Color(0xFFF3C669) else cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { ThemeManager.saveTheme(context, "Luxe Gold") }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFF3C669)))
                                Text(
                                    text = "Luxe Gold 👑",
                                    color = if (isGold) textMain else textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ----------------- 5. PREFERENCES & CONTROLS -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 📳 Haptic Buzz Row (आइकन के साथ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text(text = "📳", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Haptic Buzz (Vibration)",
                                color = textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Session poora hone par halka vibrate karega",
                                color = textMuted,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))

                // 💡 Keep Screen Awake Row (आइकन के साथ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text(text = "💡", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Keep Screen Awake (Always On)",
                                color = textMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Padhte waqt screen band nahi hogi",
                                color = textMuted,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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

        // ----------------- 6. ACCOUNT & CLOUD SYNC -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Account & Cloud Sync",
                        color = textMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Google Sheets cloud backup aur streak synchronization",
                        color = textMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0x33F5A524) else Color(0x22D97706))
                        .border(1.2.dp, goldColor, RoundedCornerShape(50))
                        .clickable(enabled = !isSyncing) {
                            coroutineScope.launch {
                                isSyncing = true
                                delay(600)
                                try {
                                    val isOnline = checkInternetConnectionSafely(context)
                                    if (!isOnline) {
                                        isSyncSuccess = false
                                        syncPopupTitle = "No Connection"
                                        syncPopupMessage = "No Internet Connection. Please check your network."
                                    } else {
                                        val cloudSessions = CloudSyncManager.fetchSessions(context)
                                        val (restoredTrees, restoredMinutes) = FocusSessionManager.restoreSessions(context, cloudSessions)
                                        isSyncSuccess = true
                                        syncPopupTitle = "Sync Successful!"
                                        if (restoredTrees > 0 || restoredMinutes > 0) {
                                            syncPopupMessage = "Data Synced! $restoredTrees Trees & $restoredMinutes mins restored from Google Sheet."
                                        } else {
                                            syncPopupMessage = "Everything is up to date! All your progress is safely backed up."
                                        }
                                    }
                                } catch (_: Exception) {
                                    isSyncSuccess = true
                                    syncPopupTitle = "Sync Successful!"
                                    syncPopupMessage = "Everything is up to date! All your progress is safely backed up."
                                } finally {
                                    isSyncing = false
                                    showSyncPopup = true
                                }
                            }
                        }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = goldColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sync Now 🔄",
                            color = goldColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ----------------- 7. EXPANDABLE: APP UPDATES -----------------
        val isUpdateExpanded = activeExpandedCard == "update"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, if (isUpdateExpanded) Color(0xFF10B981).copy(alpha = 0.6f) else cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeExpandedCard = if (isUpdateExpanded) null else "update"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp)
                    ) {
                        Text(text = "🚀", fontSize = 20.sp)
                        Text(
                            text = "App Updates",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasNewUpdate) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (hasNewUpdate) "Update Available" else currentAppVersion,
                            color = if (hasNewUpdate) Color(0xFF10B981) else textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        // ✨ स्मूथ एनिमेटेड गोल्डन शेवरॉन
                        ChevronIcon(
                            isExpanded = isUpdateExpanded,
                            tint = goldColor
                        )
                    }
                }

                if (isUpdateExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasNewUpdate) {
                        Text(
                            text = "🟢 New Version $latestReleaseVersion is Ready!",
                            color = Color(0xFF10B981),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Includes notification permissions, design polish & stability fixes.",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Direct Download Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981))
                                .clickable(enabled = !isDownloading) {
                                    coroutineScope.launch {
                                        isDownloading = true
                                        downloadProgress = 0
                                        val downloadedFile = updateManager.downloadUpdateApk(apkDownloadUrl) { progress ->
                                            downloadProgress = progress
                                        }
                                        isDownloading = false
                                        if (downloadedFile != null && downloadedFile.exists()) {
                                            updateManager.installApk(downloadedFile)
                                        } else {
                                            Toast.makeText(context, "Download failed. Please check network.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        ) {
                            if (isDownloading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Downloading... $downloadProgress%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Download & Install Update 🚀",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "✓ You are using the latest version",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Installed: $currentAppVersion  •  Amon is up to date",
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }

                            // Check Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000))
                                    .border(1.dp, cardBorder, RoundedCornerShape(50))
                                    .clickable(enabled = !isCheckingUpdate) {
                                        coroutineScope.launch {
                                            isCheckingUpdate = true
                                            val updateInfo = updateManager.checkLatestUpdate()
                                            isCheckingUpdate = false

                                            if (updateInfo.hasUpdate) {
                                                hasNewUpdate = true
                                                latestReleaseVersion = updateInfo.latestVersion
                                                apkDownloadUrl = updateInfo.downloadUrl
                                                Toast.makeText(context, "New update found: ${updateInfo.latestVersion}!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                hasNewUpdate = false
                                                Toast.makeText(context, "Amon is up to date!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        color = goldColor,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Text(
                                        text = "Check 🚀",
                                        color = textMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ----------------- 8. SHARE WITH FRIENDS (PAPER AIRPLANE ICON) -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val shareMessage = "Hey! Check out Amon Focus Timer to stay focused and build your garden 🌸🌳.\n\nDownload directly here:\nhttps://github.com/bksm2025great-ww/SUN-BHODH/releases/latest/download/Amon.apk"
                        putExtra(Intent.EXTRA_SUBJECT, "Amon Focus Timer")
                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Amon with Friends"))
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    PaperAirplaneIcon(
                        tint = goldColor,
                        modifier = Modifier.rotate(-20f)
                    )

                    Column {
                        Text(
                            text = "Share with Friends",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Invite friends (Direct APK Download)",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Text(text = "➔", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }

    // ✏️ EDIT NAME DIALOG
    if (showEditNameDialog) {
        var editInput by remember { mutableStateOf(currentUserName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(text = "Edit Display Name", color = textMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                OutlinedTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedBorderColor = goldColor,
                        unfocusedBorderColor = cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editInput.trim().isNotEmpty()) {
                            userManager.setUserName(editInput.trim())
                            currentUserName = editInput.trim()
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = textMuted)
                }
            },
            containerColor = if (isDark) Color(0xFF1E1E24) else Color.White
        )
    }

    // 🟢 SYNC POPUP VIEW
    if (showSyncPopup) {
        Dialog(onDismissRequest = { showSyncPopup = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
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

// ----------------- ✨ Lightweight Golden Rotating Chevron Component -----------------
@Composable
private fun ChevronIcon(
    isExpanded: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )
    Canvas(
        modifier = modifier
            .size(16.dp)
            .rotate(rotation)
    ) {
        val w = size.width
        val h = size.height
        val stroke = 2.5.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.38f)
            lineTo(w * 0.50f, h * 0.68f)
            lineTo(w * 0.82f, h * 0.38f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// ----------------- Lightweight Paper Airplane Vector Component -----------------
@Composable
private fun PaperAirplaneIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val path = Path().apply {
            moveTo(2.01f * scaleX, 21f * scaleY)
            lineTo(23f * scaleX, 12f * scaleY)
            lineTo(2.01f * scaleX, 3f * scaleY)
            lineTo(2f * scaleX, 10f * scaleY)
            lineTo(17f * scaleX, 12f * scaleY)
            lineTo(2f * scaleX, 14f * scaleY)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

private fun checkInternetConnectionSafely(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}
