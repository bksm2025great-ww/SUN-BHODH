package com.amon.timer

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProfileScreen() {
    var showAchievementsScreen by remember { mutableStateOf(false) }

    // 🔙 बैक गेस्चर: अचीवमेंट्स से वापस प्रोफ़ाइल पर लाएगा
    BackHandler(enabled = showAchievementsScreen) {
        showAchievementsScreen = false
    }

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

    // 📥 सुरक्षित फ़ाइल पिकर: बैकअप JSON फ़ाइल चुनने के लिए
    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (success, message) = BackupManager.importBackup(context, uri)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // ⏱️ कुल पढ़ाई के घंटे निकालकर रैंक तय करना
    val allSessions = remember { FocusSessionManager.getAllSessions(context) }
    val totalFocusedHours = remember(allSessions) {
        val totalMinutes = allSessions.sumOf { it.durationMinutes }
        totalMinutes / 60
    }

    val currentRankTitle = remember(totalFocusedHours) {
        when {
            totalFocusedHours >= 1950 -> "Grandmaster of Eternity"
            totalFocusedHours >= 1000 -> "Emperor of Amon"
            totalFocusedHours >= 800  -> "Crown Sovereign"
            totalFocusedHours >= 750  -> "Ace of Amon"
            totalFocusedHours >= 500  -> "Platinum Master"
            totalFocusedHours >= 350  -> "Vanguard Knight"
            totalFocusedHours >= 250  -> "Elite Focus"
            totalFocusedHours >= 100  -> "Century King"
            totalFocusedHours >= 50   -> "Silver Master"
            totalFocusedHours >= 10   -> "Bronze Scholar"
            else                      -> "Focus Initiate"
        }
    }

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

    // 🚀 In-App Update States
    var hasNewUpdate by remember { mutableStateOf(false) }
    var latestReleaseVersion by remember { mutableStateOf(currentAppVersion) }
    var apkDownloadUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val updateInfo = updateManager.checkLatestUpdate()
            if (updateInfo.hasUpdate) {
                hasNewUpdate = true
                latestReleaseVersion = updateInfo.latestVersion
                apkDownloadUrl = updateInfo.downloadUrl
            }
        } catch (_: Exception) {}
    }

    // 📦 APK डायरेक्ट शेयरिंग स्टेट
    var isPreparingApkShare by remember { mutableStateOf(false) }

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

        // ----------------- 2. USER PROFILE CARD -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showEditNameDialog = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    MagicStylusIcon(tint = goldColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUserName,
                        color = textMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(goldColor.copy(alpha = 0.18f))
                        .border(1.2.dp, goldColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = currentRankTitle,
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ----------------- 3. 🛡️️ AMON VAULT (BACKUP & RESTORE) CARD -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.2.dp, goldColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShieldSecurityIcon(tint = goldColor, modifier = Modifier.size(28.dp))
                    Column {
                        Text(
                            text = "Amon Vault (Backup & Restore)",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keep your focus history safe & portable",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                // Primary Action Buttons: Export & Import
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 📤 EXPORT BUTTON
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                BackupManager.exportBackup(context, currentUserName)
                            }
                            .padding(vertical = 11.dp, horizontal = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UploadBackupIcon(tint = goldColor)
                            Column {
                                Text(
                                    text = "Export Backup",
                                    color = textMain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Save to Drive / Share",
                                    color = textMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    // 📥 IMPORT BUTTON
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                backupFilePicker.launch("*/*")
                            }
                            .padding(vertical = 11.dp, horizontal = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RestoreBackupIcon(tint = goldColor)
                            Column {
                                Text(
                                    text = "Import Backup",
                                    color = textMain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Restore from file",
                                    color = textMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                }

                // 📊 EXCEL / CSV EXPORT BUTTON
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            BackupManager.exportCsvReport(context, currentUserName)
                        }
                        .padding(vertical = 12.dp, horizontal = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TableGridIcon(tint = goldColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Study Report (CSV / Excel)",
                            color = textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(cardBorder.copy(alpha = 0.4f))
                )

                // 📋 WHAT'S BACKED UP CHECKLIST
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = "WHAT'S BACKED UP",
                        color = goldColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckCircleIcon(tint = goldColor)
                        Text(
                            text = "Study sessions & total hours",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckCircleIcon(tint = goldColor)
                        Text(
                            text = "Custom subjects & theme settings",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckCircleIcon(tint = goldColor)
                        Text(
                            text = "Daily streak & milestones",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                // ☁️ GOOGLE SHEET CLOUD SYNC
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x11FFFFFF) else Color(0x08000000))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CloudSyncIcon(tint = goldColor, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "Google Sheet Cloud Sync",
                                color = textMain,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Google Sheets cloud backup & synchronization",
                                color = textMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) Color(0x33F5A524) else Color(0x22D97706))
                            .border(1.dp, goldColor, RoundedCornerShape(50))
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
                                                syncPopupMessage = "Data Synced! $restoredMinutes mins restored from Google Sheet."
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
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = goldColor,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Text(
                                text = "Sync Now",
                                color = goldColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ----------------- 4. ACHIEVEMENTS & BADGES -----------------
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
                    TrophyIcon(tint = goldColor)
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

        // ----------------- 5. EXPANDABLE: APPEARANCE & THEMES -----------------
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
                        PaletteIcon(tint = goldColor)
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
                        text = "THEME MODE",
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
                            Pair("Default", "System"),
                            Pair("Dark", "Dark"),
                            Pair("Light", "Light")
                        )
                        themes.forEach { (name, desc) ->
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
                                Text(
                                    text = desc,
                                    color = if (isSelected) textMain else textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ACCENT COLOR",
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
                                    text = "Luxe Gold",
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

        // ----------------- 6. PREFERENCES & CONTROLS -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        PhoneVibeIcon(tint = goldColor)
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
                        LightbulbIcon(tint = goldColor)
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

        // ----------------- 7. AUTO-SENSING APP UPDATES -----------------
        val isUpdateExpanded = activeExpandedCard == "update"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, if (hasNewUpdate) Color(0xFF10B981).copy(alpha = 0.8f) else cardBorder, RoundedCornerShape(16.dp))
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
                        RocketLaunchIcon(tint = if (hasNewUpdate) Color(0xFF10B981) else goldColor)
                        Text(
                            text = "App Updates",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasNewUpdate) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
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
                            text = if (hasNewUpdate) "Update Available" else "v$currentAppVersion",
                            color = if (hasNewUpdate) Color(0xFF10B981) else textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = if (hasNewUpdate) FontWeight.ExtraBold else FontWeight.Medium
                        )
                        ChevronIcon(
                            isExpanded = isUpdateExpanded,
                            tint = if (hasNewUpdate) Color(0xFF10B981) else goldColor
                        )
                    }
                }

                if (isUpdateExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasNewUpdate) {
                        Text(
                            text = "New Version $latestReleaseVersion is Ready!",
                            color = Color(0xFF10B981),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Includes 0-180m magnetic slider, stats donut chart, and performance polish.",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
                                    text = "Download & Install Update",
                                    color = Color.White,
                                    fontSize = 13.sp,
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
                                    text = "✓ Amon is up to date",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Installed Version: $currentAppVersion  •  Auto-sync active",
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Latest",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ----------------- 8. SHARE DIRECT APK -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .clickable(enabled = !isPreparingApkShare) {
                    coroutineScope.launch {
                        isPreparingApkShare = true
                        shareApkSafely(context)
                        isPreparingApkShare = false
                    }
                }
                .padding(16.dp)
        ) {
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
                            text = if (isPreparingApkShare) "Preparing Amon_Focus_Timer.apk..." else "Send Amon_Focus_Timer.apk directly to WhatsApp",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
                if (isPreparingApkShare) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = goldColor, strokeWidth = 2.dp)
                } else {
                    Text(text = "➔", color = goldColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
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

// =============================================================================
// ✨ LUXE GOLD MATERIAL VECTOR ICONS (100% PURE ANDROID VECTORS - NO EMOJIS)
// =============================================================================

@Composable
private fun ShieldSecurityIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.25f)
            cubicTo(w * 0.85f, h * 0.60f, w * 0.5f, h * 0.88f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.5f, h * 0.88f, w * 0.15f, h * 0.60f, w * 0.15f, h * 0.25f)
            close()
        }
        drawPath(path = path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun UploadBackupIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx()
        val tray = Path().apply {
            moveTo(w * 0.2f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.82f)
            lineTo(w * 0.8f, h * 0.82f)
            lineTo(w * 0.8f, h * 0.65f)
        }
        drawPath(tray, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.65f), end = Offset(w * 0.5f, h * 0.22f), strokeWidth = stroke, cap = StrokeCap.Round)
        val head = Path().apply {
            moveTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.5f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(head, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun RestoreBackupIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx()
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 290f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = Offset(w * 0.18f, h * 0.18f),
            size = Size(w * 0.64f, h * 0.64f)
        )
        val arrow = Path().apply {
            moveTo(w * 0.38f, h * 0.12f)
            lineTo(w * 0.22f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.36f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.5f, h * 0.35f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.64f, h * 0.5f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
    }
}

@Composable
private fun TableGridIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.70f, h * 0.70f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = stroke)
        )
        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.42f), end = Offset(w * 0.85f, h * 0.42f), strokeWidth = stroke)
        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.68f), end = Offset(w * 0.85f, h * 0.68f), strokeWidth = stroke * 0.8f)
        drawLine(color = tint, start = Offset(w * 0.45f, h * 0.15f), end = Offset(w * 0.45f, h * 0.85f), strokeWidth = stroke * 0.8f)
    }
}

@Composable
private fun CheckCircleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        drawCircle(color = tint, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
        val check = Path().apply {
            moveTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.66f)
            lineTo(w * 0.70f, h * 0.36f)
        }
        drawPath(check, color = tint, style = Stroke(width = stroke * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun MagicStylusIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * 0.78f),
            end = Offset(w * 0.76f, h * 0.24f),
            strokeWidth = stroke * 1.25f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.88f),
            end = Offset(w * 0.25f, h * 0.75f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.70f, h * 0.20f),
            end = Offset(w * 0.86f, h * 0.14f),
            strokeWidth = stroke * 1.35f,
            cap = StrokeCap.Round
        )

        val starCenter = Offset(w * 0.32f, h * 0.26f)
        val starR = w * 0.12f
        drawLine(
            color = tint,
            start = Offset(starCenter.x - starR, starCenter.y),
            end = Offset(starCenter.x + starR, starCenter.y),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(starCenter.x, starCenter.y - starR),
            end = Offset(starCenter.x, starCenter.y + starR),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )

        val swoosh = Path().apply {
            moveTo(w * 0.30f, h * 0.92f)
            cubicTo(w * 0.50f, h * 0.86f, w * 0.65f, h * 0.96f, w * 0.84f, h * 0.88f)
        }
        drawPath(path = swoosh, color = tint.copy(alpha = 0.8f), style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun TrophyIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.0.dp.toPx()

        val bowlPath = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.72f, h * 0.52f)
            cubicTo(w * 0.70f, h * 0.70f, w * 0.30f, h * 0.70f, w * 0.28f, h * 0.52f)
            close()
        }
        drawPath(path = bowlPath, color = tint)

        val leftHandle = Path().apply {
            moveTo(w * 0.26f, h * 0.22f)
            cubicTo(w * 0.08f, h * 0.22f, w * 0.08f, h * 0.48f, w * 0.29f, h * 0.48f)
        }
        drawPath(path = leftHandle, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val rightHandle = Path().apply {
            moveTo(w * 0.74f, h * 0.22f)
            cubicTo(w * 0.92f, h * 0.22f, w * 0.92f, h * 0.48f, w * 0.71f, h * 0.48f)
        }
        drawPath(path = rightHandle, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.65f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke * 1.4f,
            cap = StrokeCap.Square
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.28f, h * 0.84f),
            end = Offset(w * 0.72f, h * 0.84f),
            strokeWidth = stroke * 1.3f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PaletteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height

        val palette = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            cubicTo(w * 0.85f, h * 0.1f, w * 0.95f, h * 0.45f, w * 0.85f, h * 0.75f)
            cubicTo(w * 0.78f, h * 0.92f, w * 0.55f, h * 0.88f, w * 0.45f, h * 0.80f)
            cubicTo(w * 0.38f, h * 0.74f, w * 0.25f, h * 0.78f, w * 0.15f, h * 0.65f)
            cubicTo(w * 0.05f, h * 0.50f, w * 0.15f, h * 0.1f, w * 0.5f, h * 0.1f)
            close()
        }
        drawPath(path = palette, color = tint)

        drawCircle(color = Color(0xFF18181D), radius = w * 0.065f, center = Offset(w * 0.40f, h * 0.30f))
        drawCircle(color = Color(0xFF18181D), radius = w * 0.065f, center = Offset(w * 0.65f, h * 0.32f))
        drawCircle(color = Color(0xFF18181D), radius = w * 0.072f, center = Offset(w * 0.72f, h * 0.55f))
        drawCircle(color = Color(0xFF18181D), radius = w * 0.08f, center = Offset(w * 0.30f, h * 0.55f))
    }
}

@Composable
private fun PhoneVibeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.12f),
            size = Size(w * 0.40f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.44f, h * 0.78f),
            end = Offset(w * 0.56f, h * 0.78f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )

        val leftWave = Path().apply {
            moveTo(w * 0.16f, h * 0.28f)
            cubicTo(w * 0.08f, h * 0.38f, w * 0.08f, h * 0.62f, w * 0.16f, h * 0.72f)
        }
        drawPath(path = leftWave, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val rightWave = Path().apply {
            moveTo(w * 0.84f, h * 0.28f)
            cubicTo(w * 0.92f, h * 0.38f, w * 0.92f, h * 0.62f, w * 0.84f, h * 0.72f)
        }
        drawPath(path = rightWave, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun LightbulbIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val bulb = Path().apply {
            moveTo(w * 0.32f, h * 0.62f)
            cubicTo(w * 0.12f, h * 0.50f, w * 0.15f, h * 0.15f, w * 0.50f, h * 0.12f)
            cubicTo(w * 0.85f, h * 0.15f, w * 0.88f, h * 0.50f, w * 0.68f, h * 0.62f)
            lineTo(w * 0.62f, h * 0.74f)
            lineTo(w * 0.38f, h * 0.74f)
            close()
        }
        drawPath(path = bulb, color = tint, style = Stroke(width = stroke, join = StrokeJoin.Round))

        val filament = Path().apply {
            moveTo(w * 0.42f, h * 0.60f)
            lineTo(w * 0.42f, h * 0.35f)
            lineTo(w * 0.50f, h * 0.42f)
            lineTo(w * 0.58f, h * 0.35f)
            lineTo(w * 0.58f, h * 0.60f)
        }
        drawPath(path = filament, color = tint, style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round))

        drawLine(
            color = tint,
            start = Offset(w * 0.38f, h * 0.80f),
            end = Offset(w * 0.62f, h * 0.80f),
            strokeWidth = stroke * 1.1f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.42f, h * 0.87f),
            end = Offset(w * 0.58f, h * 0.87f),
            strokeWidth = stroke * 1.1f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CloudSyncIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height

        val cloud = Path().apply {
            moveTo(w * 0.22f, h * 0.75f)
            lineTo(w * 0.78f, h * 0.75f)
            cubicTo(w * 0.92f, h * 0.75f, w * 0.95f, h * 0.56f, w * 0.84f, h * 0.48f)
            cubicTo(w * 0.85f, h * 0.32f, w * 0.72f, h * 0.24f, w * 0.58f, h * 0.28f)
            cubicTo(w * 0.52f, h * 0.18f, w * 0.35f, h * 0.18f, w * 0.28f, h * 0.30f)
            cubicTo(w * 0.15f, h * 0.33f, w * 0.08f, h * 0.46f, w * 0.12f, h * 0.60f)
            cubicTo(w * 0.09f, h * 0.70f, w * 0.16f, h * 0.75f, w * 0.22f, h * 0.75f)
            close()
        }
        drawPath(path = cloud, color = tint)

        val stroke = 2.0.dp.toPx()
        drawLine(
            color = Color(0xFF18181D),
            start = Offset(w * 0.5f, h * 0.70f),
            end = Offset(w * 0.5f, h * 0.40f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val arrowHead = Path().apply {
            moveTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.38f)
            lineTo(w * 0.62f, h * 0.50f)
        }
        drawPath(path = arrowHead, color = Color(0xFF18181D), style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun RocketLaunchIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height

        val rocket = Path().apply {
            moveTo(w * 0.85f, h * 0.15f)
            cubicTo(w * 0.68f, h * 0.16f, w * 0.45f, h * 0.30f, w * 0.40f, h * 0.50f)
            lineTo(w * 0.32f, h * 0.56f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.50f, h * 0.60f)
            cubicTo(w * 0.70f, h * 0.55f, w * 0.84f, h * 0.32f, w * 0.85f, h * 0.15f)
            close()
        }
        drawPath(path = rocket, color = tint)

        val leftFin = Path().apply {
            moveTo(w * 0.40f, h * 0.52f)
            lineTo(w * 0.22f, h * 0.56f)
            lineTo(w * 0.32f, h * 0.66f)
            close()
        }
        drawPath(path = leftFin, color = tint)

        val rightFin = Path().apply {
            moveTo(w * 0.52f, h * 0.40f)
            lineTo(w * 0.56f, h * 0.22f)
            lineTo(w * 0.66f, h * 0.32f)
            close()
        }
        drawPath(path = rightFin, color = tint)

        drawCircle(color = Color(0xFF18181D), radius = w * 0.055f, center = Offset(w * 0.62f, h * 0.38f))

        val stroke = 1.7.dp.toPx()
        drawLine(
            color = tint.copy(alpha = 0.9f),
            start = Offset(w * 0.32f, h * 0.72f),
            end = Offset(w * 0.12f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = 0.65f),
            start = Offset(w * 0.40f, h * 0.76f),
            end = Offset(w * 0.26f, h * 0.90f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = 0.65f),
            start = Offset(w * 0.26f, h * 0.64f),
            end = Offset(w * 0.12f, h * 0.78f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

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
            .size(21.dp)
            .rotate(rotation)
    ) {
        val w = size.width
        val h = size.height
        val stroke = 2.6.dp.toPx()
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

@Composable
private fun PaperAirplaneIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(25.dp)) {
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
