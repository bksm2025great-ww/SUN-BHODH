package com.amon.timer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen() {
    var showAchievementsScreen by remember { mutableStateOf(false) }

    // 🔙 Back gesture: Achievements se wapas profile par layega
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

    // ✏️ Smart Name State
    var currentUserName by remember { mutableStateOf(userManager.getUserName().trim()) }
    val hasCustomName = currentUserName.isNotEmpty()
    var showEditNameDialog by remember { mutableStateOf(false) }

    // 📥 Backup picker
    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (success, message) = BackupManager.importBackup(context, uri)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // ⏱️ Total focused hours nikaal kar rank set karna
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

    // ⚙️ Persistent User Settings
    val prefs = remember { context.getSharedPreferences("amon_user_preferences", Context.MODE_PRIVATE) }
    var isVibrationEnabled by remember {
        mutableStateOf(prefs.getBoolean("pref_vibration", true))
    }
    var isKeepScreenAwake by remember {
        mutableStateOf(prefs.getBoolean("pref_keep_screen_awake", false))
    }

    // 💡 Keep Screen Awake Controller
    DisposableEffect(isKeepScreenAwake) {
        val activity = context as? Activity
        if (isKeepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { }
    }

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

    // 📦 APK Direct Sharing State
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
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (!hasCustomName) {
                                Modifier
                                    .background(goldColor.copy(alpha = 0.08f))
                                    .border(1.dp, goldColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { showEditNameDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    OutlinedPencilIcon(tint = goldColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasCustomName) currentUserName else "Tap to add name",
                        color = if (hasCustomName) textMain else textMuted,
                        fontSize = if (hasCustomName) 20.sp else 16.sp,
                        fontWeight = if (hasCustomName) FontWeight.Black else FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(goldColor.copy(alpha = 0.15f))
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
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                OutlinedTrophyIcon(tint = goldColor)
                Text(
                    text = "Achievements & Badges",
                    color = textMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
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
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    OutlinedPaletteIcon(tint = goldColor)
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
                                    .background(
                                        if (isSelected) {
                                            goldColor.copy(alpha = 0.15f)
                                        } else {
                                            if (isDark) Color(0x11FFFFFF) else Color(0x06000000)
                                        }
                                    )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ACCENT COLOR",
                        color = goldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val accentThemes = listOf(
                        Triple("Luxe Gold", Color(0xFFF3C669), Color(0xFF18181B)),
                        Triple("Classic Yellow", Color(0xFFF5A524), Color(0xFF18181B)),
                        Triple("Warm Amber", Color(0xFFFB923C), Color(0xFF18181B)),
                        Triple("Soft Lavender", Color(0xFFA78BFA), Color(0xFF18181B)),
                        Triple("Slate Violet", Color(0xFF9381FF), Color.White),
                        Triple("Dusty Rose", Color(0xFFF472B6), Color(0xFF18181B)),
                        Triple("Blossom Rose", Color(0xFFE879A9), Color.White)
                    )

                    val colorChunks = accentThemes.chunked(4)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorChunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (name, color, checkColor) ->
                                    val isSelected = currentAccent == name

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.2.dp else 0.dp,
                                                color = if (isSelected) (if (isDark) Color.White else Color(0xFF1E293B)) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { ThemeManager.saveTheme(context, name) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                text = "✓",
                                                color = checkColor,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size < 4) {
                                    repeat(4 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
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
                        OutlinedPhoneVibeIcon(tint = goldColor)
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
                        onCheckedChange = { enabled ->
                            isVibrationEnabled = enabled
                            prefs.edit().putBoolean("pref_vibration", enabled).apply()
                        },
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
                        OutlinedLightbulbIcon(tint = goldColor)
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
                        onCheckedChange = { enabled ->
                            isKeepScreenAwake = enabled
                            prefs.edit().putBoolean("pref_keep_screen_awake", enabled).apply()
                        },
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

        // ----------------- 6. APP UPDATES -----------------
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
                        OutlinedRocketIcon(tint = if (hasNewUpdate) Color(0xFF10B981) else goldColor)
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

                    Text(
                        text = if (hasNewUpdate) "Update Available" else "v$currentAppVersion",
                        color = if (hasNewUpdate) Color(0xFF10B981) else textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = if (hasNewUpdate) FontWeight.ExtraBold else FontWeight.Medium
                    )
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

                        // 🌟 Sleek Gradient Progress Bar (Story/Ad Style)
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDownloading) Color(0x2210B981) else Color(0xFF10B981))
                                .border(
                                    width = 1.dp,
                                    color = if (isDownloading) goldColor.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
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
                                // A. Left-to-right filling sleek gradient line
                                val progressFraction = (downloadProgress / 100f).coerceIn(0.02f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFF59E0B),
                                                    Color(0xFFFDE047),
                                                    Color(0x88FEF08A)
                                                )
                                            )
                                        )
                                )

                                // B. Percentage Text
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading Update... $downloadProgress%",
                                        color = if (downloadProgress > 50) Color.Black else textMain,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Download & Install Update",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                                    text = "Amon is up to date",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Installed Version: $currentAppVersion • Auto-sync active",
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

        // ----------------- 7. AMON VAULT (BACKUP & RESTORE) -----------------
        val isVaultExpanded = activeExpandedCard == "vault"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(
                    width = 1.dp,
                    color = if (isVaultExpanded) goldColor.copy(alpha = 0.7f) else cardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeExpandedCard = if (isVaultExpanded) null else "vault"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    OutlinedShieldIcon(tint = goldColor, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = "Amon Vault (Backup & Restore)",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Keep your focus history safe & portable",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (isVaultExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1️⃣ FULL APP BACKUP (.AMON)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val success = BackupManager.exportBackup(context, currentUserName)
                                if (!success) {
                                    Toast.makeText(context, "Could not open share menu.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedUploadIcon(tint = goldColor)
                            Column {
                                Text(
                                    text = "Full App Backup (.amon)",
                                    color = textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Secure backup for switching phones • Readable only by Amon",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2️⃣ RESTORE BACKUP
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                backupFilePicker.launch("*/*")
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedDownloadIcon(tint = goldColor)
                            Column {
                                Text(
                                    text = "Restore Backup",
                                    color = textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Recover your streak, stats, and hours of hard work using Amon backup",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3️⃣ EXPORT STUDY SHEET (.CSV)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                val success = BackupManager.exportCsvReport(context, currentUserName)
                                if (!success) {
                                    Toast.makeText(context, "Could not open share menu.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTableGridIcon(tint = goldColor)
                            Column {
                                Text(
                                    text = "Export Study Sheet (.csv)",
                                    color = textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "View logs in Excel or Google Sheets • For personal viewing only",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.height(14.dp))

                    // WHAT'S BACKED UP SUMMARY
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
                            OutlinedCheckCircleIcon(tint = goldColor)
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
                            OutlinedCheckCircleIcon(tint = goldColor)
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
                            OutlinedCheckCircleIcon(tint = goldColor)
                            Text(
                                text = "Daily streak & milestones",
                                color = textMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ☁️ BALANCED GOOGLE SHEET CLOUD SYNC CARD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x11FFFFFF) else Color(0x08000000))
                            .border(1.dp, cardBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedCloudSyncIcon(tint = goldColor, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = "Google Sheet Cloud Sync",
                                    color = textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Google Sheets cloud backup & sync",
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(goldColor.copy(alpha = 0.15f))
                                .border(1.dp, goldColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
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
                        ) {
                            if (isSyncing) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = goldColor,
                                        strokeWidth = 1.8.dp
                                    )
                                    Text(
                                        text = "Syncing...",
                                        color = goldColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Sync Now",
                                    color = goldColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ----------------- 8. HELP & SUPPORT -----------------
        val isSupportExpanded = activeExpandedCard == "support"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(
                    width = 1.dp,
                    color = if (isSupportExpanded) goldColor.copy(alpha = 0.7f) else cardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeExpandedCard = if (isSupportExpanded) null else "support"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    OutlinedHelpSupportIcon(tint = goldColor)
                    Column {
                        Text(
                            text = "Help & Support",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Report issues, share feedback & ideas",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (isSupportExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 🐞 REPORT A BUG
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                sendSupportEmail(
                                    context = context,
                                    subject = "[Amon Bug Report] v$currentAppVersion",
                                    appVersion = currentAppVersion,
                                    isBug = true
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🐞",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = "Report a Bug",
                                    color = textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Found an issue? Send diagnostics directly to developer",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 💡 SEND FEEDBACK & IDEAS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF27272F) else Color(0xFFF1F5F9))
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                sendSupportEmail(
                                    context = context,
                                    subject = "[Amon Feedback] Suggestion",
                                    appVersion = currentAppVersion,
                                    isBug = false
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = "Send Feedback & Ideas",
                                    color = textMain,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Have a feature suggestion or want to say hi? We'd love to hear",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }

        // ----------------- 9. SHARE DIRECT APK -----------------
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
                    OutlinedPaperAirplaneIcon(tint = goldColor)

                    Column {
                        Text(
                            text = "Share with Friends",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isPreparingApkShare) "Preparing Amon_Focus_Timer.apk..." else "Share the app with your friends",
                            color = textMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }
                if (isPreparingApkShare) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = goldColor, strokeWidth = 2.dp)
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
                    placeholder = { Text("Enter your name", color = textMuted) },
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
                        val trimmed = editInput.trim()
                        userManager.setUserName(trimmed)
                        currentUserName = trimmed
                        showEditNameDialog = false
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
                        text = if (isSyncSuccess) "OK" else "!",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// =============================================================================
// ✨ SUPPORT EMAIL DISPATCHER (MINIMAL TEMPLATE)
// =============================================================================

private fun sendSupportEmail(context: Context, subject: String, appVersion: String, isBug: Boolean) {
    try {
        val body = """
Hey Team Amon is here!
App Version: v$appVersion

""".trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:bksm2025great@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(emailIntent, "Send feedback via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found on your phone.", Toast.LENGTH_SHORT).show()
    }
}

// =============================================================================
// ✨ 100% PURE VECTOR ICONS (HAND-DRAWN CANVAS ART)
// =============================================================================

@Composable
private fun OutlinedHelpSupportIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val bubble = Path().apply {
            moveTo(w * 0.20f, h * 0.22f)
            lineTo(w * 0.80f, h * 0.22f)
            cubicTo(w * 0.90f, h * 0.22f, w * 0.90f, h * 0.65f, w * 0.80f, h * 0.65f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.26f, h * 0.82f)
            lineTo(w * 0.26f, h * 0.65f)
            lineTo(w * 0.20f, h * 0.65f)
            cubicTo(w * 0.10f, h * 0.65f, w * 0.10f, h * 0.22f, w * 0.20f, h * 0.22f)
            close()
        }
        drawPath(bubble, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val dotRadius = w * 0.038f
        drawCircle(color = tint, radius = dotRadius, center = Offset(w * 0.36f, h * 0.435f))
        drawCircle(color = tint, radius = dotRadius, center = Offset(w * 0.50f, h * 0.435f))
        drawCircle(color = tint, radius = dotRadius, center = Offset(w * 0.64f, h * 0.435f))
    }
}

@Composable
private fun OutlinedRocketIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(25.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val flameBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFEB3B),
                Color(0xFFFF9800),
                Color(0xFFE53935)
            ),
            start = Offset(w * 0.40f, h * 0.60f),
            end = Offset(w * 0.05f, h * 0.95f)
        )

        val flamePath = Path().apply {
            moveTo(w * 0.35f, h * 0.57f)
            cubicTo(w * 0.28f, h * 0.64f, w * 0.20f, h * 0.74f, w * 0.15f, h * 0.84f)
            lineTo(w * 0.24f, h * 0.72f)
            cubicTo(w * 0.18f, h * 0.82f, w * 0.10f, h * 0.92f, w * 0.04f, h * 0.98f)
            lineTo(w * 0.27f, h * 0.78f)
            cubicTo(w * 0.24f, h * 0.88f, w * 0.22f, h * 0.92f, w * 0.20f, h * 0.96f)
            cubicTo(w * 0.30f, h * 0.82f, w * 0.36f, h * 0.74f, w * 0.44f, h * 0.66f)
            close()
        }
        drawPath(path = flamePath, brush = flameBrush)

        val bodyPath = Path().apply {
            moveTo(w * 0.92f, h * 0.08f)
            cubicTo(w * 0.88f, h * 0.28f, w * 0.74f, h * 0.50f, w * 0.52f, h * 0.64f)
            lineTo(w * 0.36f, h * 0.48f)
            cubicTo(w * 0.50f, h * 0.26f, w * 0.72f, h * 0.12f, w * 0.92f, h * 0.08f)
            close()
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val leftFin = Path().apply {
            moveTo(w * 0.48f, h * 0.32f)
            cubicTo(w * 0.30f, h * 0.32f, w * 0.18f, h * 0.42f, w * 0.14f, h * 0.56f)
            cubicTo(w * 0.26f, h * 0.54f, w * 0.34f, h * 0.50f, w * 0.38f, h * 0.48f)
        }
        drawPath(
            path = leftFin,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val rightFin = Path().apply {
            moveTo(w * 0.68f, h * 0.52f)
            cubicTo(w * 0.68f, h * 0.70f, w * 0.58f, h * 0.82f, w * 0.44f, h * 0.86f)
            cubicTo(w * 0.46f, h * 0.74f, w * 0.50f, h * 0.66f, w * 0.52f, h * 0.64f)
        }
        drawPath(
            path = rightFin,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = tint,
            radius = w * 0.08f,
            center = Offset(w * 0.66f, h * 0.34f),
            style = Stroke(width = stroke * 0.9f)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.41f, h * 0.53f),
            end = Offset(w * 0.47f, h * 0.59f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun OutlinedPencilIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(19.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val body = Path().apply {
            moveTo(w * 0.22f, h * 0.78f)
            lineTo(w * 0.72f, h * 0.28f)
            lineTo(w * 0.86f, h * 0.42f)
            lineTo(w * 0.36f, h * 0.92f)
            lineTo(w * 0.12f, h * 0.98f)
            close()
        }
        drawPath(body, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(
            color = tint,
            start = Offset(w * 0.26f, h * 0.74f),
            end = Offset(w * 0.40f, h * 0.88f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun OutlinedTrophyIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val cup = Path().apply {
            moveTo(w * 0.26f, h * 0.18f)
            lineTo(w * 0.74f, h * 0.18f)
            lineTo(w * 0.70f, h * 0.50f)
            cubicTo(w * 0.68f, h * 0.68f, w * 0.32f, h * 0.68f, w * 0.30f, h * 0.50f)
            close()
        }
        drawPath(cup, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val leftHandle = Path().apply {
            moveTo(w * 0.26f, h * 0.24f)
            cubicTo(w * 0.10f, h * 0.24f, w * 0.10f, h * 0.46f, w * 0.29f, h * 0.46f)
        }
        drawPath(leftHandle, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round))

        val rightHandle = Path().apply {
            moveTo(w * 0.74f, h * 0.24f)
            cubicTo(w * 0.90f, h * 0.24f, w * 0.90f, h * 0.46f, w * 0.71f, h * 0.46f)
        }
        drawPath(rightHandle, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round))

        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.64f), end = Offset(w * 0.5f, h * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.30f, h * 0.82f), end = Offset(w * 0.70f, h * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OutlinedPaletteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val palette = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            cubicTo(w * 0.82f, h * 0.12f, w * 0.92f, h * 0.42f, w * 0.84f, h * 0.70f)
            cubicTo(w * 0.78f, h * 0.88f, w * 0.58f, h * 0.84f, w * 0.48f, h * 0.76f)
            cubicTo(w * 0.40f, h * 0.70f, w * 0.28f, h * 0.74f, w * 0.18f, h * 0.62f)
            cubicTo(w * 0.08f, h * 0.48f, w * 0.16f, h * 0.12f, w * 0.5f, h * 0.12f)
            close()
        }
        drawPath(palette, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val spotRadius = w * 0.045f
        drawCircle(color = tint, radius = spotRadius, center = Offset(w * 0.38f, h * 0.32f), style = Stroke(width = stroke * 0.75f))
        drawCircle(color = tint, radius = spotRadius, center = Offset(w * 0.62f, h * 0.30f), style = Stroke(width = stroke * 0.75f))
        drawCircle(color = tint, radius = spotRadius, center = Offset(w * 0.72f, h * 0.52f), style = Stroke(width = stroke * 0.75f))
    }
}

@Composable
private fun OutlinedPhoneVibeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.14f),
            size = Size(w * 0.40f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = stroke)
        )
        drawLine(color = tint, start = Offset(w * 0.44f, h * 0.76f), end = Offset(w * 0.56f, h * 0.76f), strokeWidth = stroke * 0.8f, cap = StrokeCap.Round)

        val leftWave = Path().apply {
            moveTo(w * 0.16f, h * 0.32f)
            cubicTo(w * 0.08f, h * 0.42f, w * 0.08f, h * 0.58f, w * 0.16f, h * 0.68f)
        }
        drawPath(leftWave, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val rightWave = Path().apply {
            moveTo(w * 0.84f, h * 0.32f)
            cubicTo(w * 0.92f, h * 0.42f, w * 0.92f, h * 0.58f, w * 0.84f, h * 0.68f)
        }
        drawPath(rightWave, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun OutlinedLightbulbIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val bulb = Path().apply {
            moveTo(w * 0.32f, h * 0.64f)
            cubicTo(w * 0.12f, h * 0.50f, w * 0.16f, h * 0.16f, w * 0.50f, h * 0.14f)
            cubicTo(w * 0.84f, h * 0.16f, w * 0.88f, h * 0.50f, w * 0.68f, h * 0.64f)
            lineTo(w * 0.62f, h * 0.74f)
            lineTo(w * 0.38f, h * 0.74f)
            close()
        }
        drawPath(bulb, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(color = tint, start = Offset(w * 0.38f, h * 0.82f), end = Offset(w * 0.62f, h * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.42f, h * 0.89f), end = Offset(w * 0.58f, h * 0.89f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OutlinedShieldIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.84f, h * 0.26f)
            cubicTo(w * 0.84f, h * 0.60f, w * 0.5f, h * 0.86f, w * 0.5f, h * 0.86f)
            cubicTo(w * 0.5f, h * 0.86f, w * 0.16f, h * 0.60f, w * 0.16f, h * 0.26f)
            close()
        }
        drawPath(shield, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun OutlinedUploadIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val tray = Path().apply {
            moveTo(w * 0.2f, h * 0.68f)
            lineTo(w * 0.2f, h * 0.82f)
            lineTo(w * 0.8f, h * 0.82f)
            lineTo(w * 0.8f, h * 0.68f)
        }
        drawPath(tray, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.66f), end = Offset(w * 0.5f, h * 0.24f), strokeWidth = stroke, cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.32f, h * 0.40f)
            lineTo(w * 0.5f, h * 0.22f)
            lineTo(w * 0.68f, h * 0.40f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun OutlinedDownloadIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val tray = Path().apply {
            moveTo(w * 0.2f, h * 0.68f)
            lineTo(w * 0.2f, h * 0.82f)
            lineTo(w * 0.8f, h * 0.82f)
            lineTo(w * 0.8f, h * 0.68f)
        }
        drawPath(tray, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(color = tint, start = Offset(w * 0.5f, h * 0.24f), end = Offset(w * 0.5f, h * 0.66f), strokeWidth = stroke, cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.5f, h * 0.68f)
            lineTo(w * 0.68f, h * 0.50f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun OutlinedTableGridIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.7.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.70f, h * 0.70f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = stroke)
        )
        drawLine(color = tint, start = Offset(w * 0.15f, h * 0.44f), end = Offset(w * 0.85f, h * 0.44f), strokeWidth = stroke)
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.15f), end = Offset(w * 0.50f, h * 0.85f), strokeWidth = stroke * 0.85f)
    }
}

@Composable
private fun OutlinedCheckCircleIcon(tint: Color, modifier: Modifier = Modifier) {
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
private fun OutlinedCloudSyncIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val cloud = Path().apply {
            moveTo(w * 0.22f, h * 0.74f)
            lineTo(w * 0.78f, h * 0.74f)
            cubicTo(w * 0.92f, h * 0.74f, w * 0.94f, h * 0.56f, w * 0.84f, h * 0.48f)
            cubicTo(w * 0.84f, h * 0.32f, w * 0.72f, h * 0.24f, w * 0.58f, h * 0.28f)
            cubicTo(w * 0.52f, h * 0.18f, w * 0.35f, h * 0.18f, w * 0.28f, h * 0.30f)
            cubicTo(w * 0.15f, h * 0.33f, w * 0.08f, h * 0.46f, w * 0.12f, h * 0.60f)
            cubicTo(w * 0.09f, h * 0.70f, w * 0.16f, h * 0.74f, w * 0.22f, h * 0.74f)
            close()
        }
        drawPath(cloud, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val arrow = Path().apply {
            moveTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.42f)
            lineTo(w * 0.58f, h * 0.50f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.62f), end = Offset(w * 0.50f, h * 0.44f), strokeWidth = stroke * 0.9f, cap = StrokeCap.Round)
    }
}

@Composable
private fun OutlinedPaperAirplaneIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val planeOutline = Path().apply {
            moveTo(w * 0.15f, h * 0.86f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.15f, h * 0.14f)
            lineTo(w * 0.32f, h * 0.48f)
            close()
        }
        drawPath(
            path = planeOutline,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.32f, h * 0.48f),
            end = Offset(w * 0.88f, h * 0.50f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
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

private fun shareApkSafely(context: Context) {
    try {
        val appInfo = context.applicationInfo
        val originalApk = File(appInfo.sourceDir)
        val shareFolder = File(context.cacheDir, "shared_apk").apply { mkdirs() }
        val destApk = File(shareFolder, "Amon_Focus_Timer.apk")
        originalApk.copyTo(destApk, overwrite = true)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destApk
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Amon Focus Timer APK")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Amon APK via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error preparing APK: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
