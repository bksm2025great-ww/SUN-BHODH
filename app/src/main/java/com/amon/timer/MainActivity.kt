package com.amon.timer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amon.timer.ui.theme.AmonTheme
import kotlinx.coroutines.delay

private enum class AppScreenState {
    SPLASH,
    MAIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val maxRefreshMode = currentDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxRefreshMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxRefreshMode.modeId
                    window.attributes = params
                }
            }
        } catch (e: Exception) {
            Log.e("AmonLaunch", "120Hz unlock catch: ${e.localizedMessage}")
        }

        try {
            AmonReminderManager.scheduleAllReminders(this)
        } catch (e: Exception) {
            Log.e("AmonLaunch", "Safe Reminder catch: ${e.localizedMessage}")
        }

        setContent {
            AmonTheme {
                val isDark = ThemeManager.isDarkTheme.value
                val appBgColor = ThemeManager.getBackgroundColor()

                var currentScreen by remember { mutableStateOf(AppScreenState.SPLASH) }

                val currentStatusBarColor = when (currentScreen) {
                    AppScreenState.SPLASH -> Color(0xFF0F0F12)
                    AppScreenState.MAIN -> appBgColor
                }
                val isLightStatusBarIcons = when (currentScreen) {
                    AppScreenState.SPLASH -> false
                    AppScreenState.MAIN -> !isDark
                }

                SideEffect {
                    window.statusBarColor = currentStatusBarColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isLightStatusBarIcons
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = currentStatusBarColor
                ) {
                    LaunchedEffect(Unit) {
                        delay(1000)
                        // 🚀 Offline-First: Skip auth completely and go straight to MAIN screen
                        currentScreen = AppScreenState.MAIN
                    }

                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 400),
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreenState.SPLASH -> SplashScreenContent()
                            AppScreenState.MAIN -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    MainScreen()

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val localCtx = LocalContext.current
                                        var showPermissionDialog by remember {
                                            val isGranted = ContextCompat.checkSelfPermission(
                                                localCtx,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            mutableStateOf(!isGranted)
                                        }

                                        val permissionLauncher = rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.RequestPermission()
                                        ) { _ ->
                                            showPermissionDialog = false
                                        }

                                        if (showPermissionDialog) {
                                            NotificationPermissionDialog(
                                                onAllowClick = {
                                                    showPermissionDialog = false
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                },
                                                onDismiss = {
                                                    showPermissionDialog = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun openOverlayPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun AmonBlockTabSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val bgDark = Color(0xFF121216)
    val cardDark = Color(0xFF1A1A20)
    val goldAccent = Color(0xFFE5B44C)
    val subTextColor = Color(0xFF9CA3AF)

    var isAccessibilityReady by remember {
        mutableStateOf(BlockAppManager.isAccessibilityServiceEnabled(context))
    }
    var isOverlayReady by remember {
        mutableStateOf(hasOverlayPermission(context))
    }
    var isBlockingEnabled by remember {
        mutableStateOf(BlockAppManager.isBlockingEnabled(context))
    }
    var blockSettings by remember {
        mutableStateOf(BlockSettingsManager.loadConfig(context))
    }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember {
        mutableStateOf(BlockAppManager.getLaunchableApps(context))
    }
    var showBlockSettingsDialog by remember { mutableStateOf(false) }

    fun refreshBlockState() {
        isAccessibilityReady = BlockAppManager.isAccessibilityServiceEnabled(context)
        isOverlayReady = hasOverlayPermission(context)
        isBlockingEnabled = BlockAppManager.isBlockingEnabled(context)
        blockSettings = BlockSettingsManager.loadConfig(context)
        installedApps = BlockAppManager.getLaunchableApps(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshBlockState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionsGranted = isAccessibilityReady && isOverlayReady

    if (!permissionsGranted) {
        MainActivityBlockerPreview(
            modifier = modifier.background(bgDark),
            isOverlayReady = isOverlayReady,
            onEnableClick = {
                if (!isOverlayReady) {
                    openOverlayPermissionSettings(context)
                } else {
                    BlockAppManager.setBlockingEnabled(context, true)
                    isBlockingEnabled = true
                    BlockAppManager.openAccessibilitySettings(context)
                }
            }
        )
        return
    }

    val filteredApps = remember(installedApps, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) installedApps
        else installedApps.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    val blockedCount = installedApps.count { it.isBlocked }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Blocker",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$blockedCount apps selected for focus",
                    color = subTextColor,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { showBlockSettingsDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(cardDark)
                    .border(1.dp, Color(0xFF2B2B35), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Block Settings",
                    tint = goldAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isBlockingEnabled) Color(0xFF231F16) else cardDark)
                .border(
                    1.dp,
                    if (isBlockingEnabled) goldAccent.copy(alpha = 0.65f) else Color(0xFF2A2A34),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(goldAccent.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBlockingEnabled) "Focus Shield Active" else "Focus Shield Paused",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBlockingEnabled) {
                            "Blocked apps will show shield during focus"
                        } else {
                            "Turn on to block selected apps during focus"
                        },
                        color = subTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isBlockingEnabled,
                onCheckedChange = { checked ->
                    BlockAppManager.setBlockingEnabled(context, checked)
                    isBlockingEnabled = checked
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF141418),
                    checkedTrackColor = goldAccent,
                    uncheckedThumbColor = Color(0xFFA1A1AA),
                    uncheckedTrackColor = Color(0xFF2E2E3A)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search apps...",
                    color = subTextColor,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = goldAccent
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = goldAccent,
                focusedContainerColor = cardDark,
                unfocusedContainerColor = cardDark,
                focusedBorderColor = goldAccent,
                unfocusedBorderColor = Color(0xFF2A2A34)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val iconBitmap = remember(app.packageName) {
                    app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (app.isBlocked) Color(0xFF221F18) else cardDark)
                        .border(
                            1.dp,
                            if (app.isBlocked) goldAccent.copy(alpha = 0.55f) else Color(0xFF262630),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            BlockAppManager.toggleAppBlocked(context, app.packageName)
                            installedApps = BlockAppManager.getLaunchableApps(context)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = app.appName,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2C2C38))
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    color = goldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = app.appName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Switch(
                        checked = app.isBlocked,
                        onCheckedChange = {
                            BlockAppManager.toggleAppBlocked(context, app.packageName)
                            installedApps = BlockAppManager.getLaunchableApps(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF141418),
                            checkedTrackColor = goldAccent,
                            uncheckedThumbColor = Color(0xFFA1A1AA),
                            uncheckedTrackColor = Color(0xFF2E2E3A)
                        )
                    )
                }
            }
        }
    }

    if (showBlockSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showBlockSettingsDialog = false },
            containerColor = cardDark,
            titleContentColor = Color.White,
            textContentColor = subTextColor,
            title = {
                Text(
                    text = "Block Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Block Notifications During Focus",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Silence distraction alerts during focus",
                                color = subTextColor,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = blockSettings.blockNotificationsDuringFocus,
                            onCheckedChange = { enabled ->
                                val updated = blockSettings.copy(blockNotificationsDuringFocus = enabled)
                                blockSettings = updated
                                BlockSettingsManager.saveConfig(context, updated)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF141418),
                                checkedTrackColor = goldAccent
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Block Apps During Break",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Keep blocked apps restricted during breaks",
                                color = subTextColor,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = blockSettings.blockAppsDuringBreak,
                            onCheckedChange = { enabled ->
                                val updated = blockSettings.copy(blockAppsDuringBreak = enabled)
                                blockSettings = updated
                                BlockSettingsManager.saveConfig(context, updated)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF141418),
                                checkedTrackColor = goldAccent
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockSettingsDialog = false }) {
                    Text(
                        text = "Done",
                        color = goldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun MainActivityBlockerPreview(
    modifier: Modifier = Modifier,
    isOverlayReady: Boolean,
    onEnableClick: () -> Unit
) {
    val cardDark = Color(0xFF1A1A20)
    val goldAccent = Color(0xFFE5B44C)
    val subTextColor = Color(0xFF9CA3AF)

    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            currentStep = (currentStep + 1) % 3
        }
    }

    val stepLabel = when (currentStep) {
        0 -> "1. Allow Accessibility"
        1 -> "2. Select Apps"
        else -> "3. During Focus"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "App Blocker",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Allow Accessibility, pick apps to block,\nand stay focused during sessions",
                color = subTextColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(goldAccent.copy(alpha = 0.14f))
                    .border(1.dp, goldAccent.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = stepLabel,
                    color = goldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(256.dp)
                    .height(356.dp)
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    goldAccent.copy(alpha = 0.26f),
                                    goldAccent.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.maxDimension * 0.65f
                            ),
                            cornerRadius = CornerRadius(34.dp.toPx(), 34.dp.toPx())
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(cardDark)
                        .border(2.dp, Color(0xFF2D2D36), RoundedCornerShape(28.dp))
                        .padding(14.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                        },
                        label = "main_blocker_preview_step",
                        modifier = Modifier.fillMaxSize()
                    ) { step ->
                        when (step) {
                            0 -> MainPreviewStepOne(goldAccent)
                            1 -> MainPreviewStepTwo(goldAccent)
                            else -> MainPreviewStepThree(goldAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val selected = currentStep == idx
                    val width by animateFloatAsState(
                        targetValue = if (selected) 22f else 8f,
                        animationSpec = tween(250),
                        label = "main_dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(if (selected) goldAccent else Color(0xFF3E3E4A))
                            .clickable { currentStep = idx }
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEnableClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goldAccent,
                    contentColor = Color(0xFF141418)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (!isOverlayReady) "Allow Overlay Permission" else "Allow Accessibility",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Trusted by students",
                    color = subTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun MainPreviewStepOne(goldAccent: Color) {
    val toggle by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "step1_toggle"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Accessibility", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("MORE", color = Color(0xFF71717A), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF26231C))
                .border(1.2.dp, goldAccent.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Amon App Blocker",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Blocks selected distracting apps during focus",
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(if (toggle > 0.5f) goldAccent else Color(0xFF3A3A46))
                        .padding(2.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141418))
                    )
                }
            }
        }
    }
}

@Composable
private fun MainPreviewStepTwo(goldAccent: Color) {
    val itemsList = listOf(
        Triple("Social", Color(0xFFE1306C), true),
        Triple("Video", Color(0xFFFF0000), true),
        Triple("Games", Color(0xFF3B82F6), false)
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("Select Apps", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        itemsList.forEach { (label, color, checked) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (checked) Color(0xFF26231C) else Color(0xFF22222A))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(if (checked) goldAccent else Color(0xFF3A3A46))
                        .padding(2.dp),
                    contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (checked) Color(0xFF141418) else Color(0xFFA1A1AA))
                    )
                }
            }
        }
    }
}

@Composable
private fun MainPreviewStepThree(goldAccent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "step3_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "step3_pulse_val"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(126.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = goldAccent.copy(alpha = 0.22f),
                    radius = (size.minDimension * 0.45f) * pulse,
                    center = c,
                    style = Stroke(width = 1.3.dp.toPx())
                )
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
                            )
                        )
                ) {
                    Text("APP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(goldAccent)
                        .border(2.dp, Color(0xFF1A1A20), CircleShape)
                ) {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        val w = size.width
                        val h = size.height
                        drawRoundRect(
                            color = Color(0xFF141418),
                            topLeft = Offset(w * 0.22f, h * 0.46f),
                            size = Size(w * 0.56f, h * 0.42f),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                        val shackle = Path().apply {
                            moveTo(w * 0.32f, h * 0.46f)
                            lineTo(w * 0.32f, h * 0.32f)
                            cubicTo(
                                w * 0.32f, h * 0.16f,
                                w * 0.68f, h * 0.16f,
                                w * 0.68f, h * 0.32f
                            )
                            lineTo(w * 0.68f, h * 0.46f)
                        }
                        drawPath(
                            path = shackle,
                            color = Color(0xFF141418),
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Focus Session Active",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This app is blocked during focus",
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(goldAccent)
        ) {
            Text(
                text = "Return to AMON",
                color = Color(0xFF141418),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NotificationPermissionDialog(
    onAllowClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF5B3BA5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔔", fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Never miss a\nfocus session",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E24),
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Enable notifications to unlock the\nfull timer experience:",
                    fontSize = 14.sp,
                    color = Color(0xFF4A5568),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NotificationBulletItem(text = "Live countdown on lock screen and status bar")
                    NotificationBulletItem(text = "Instant chime when focus block ends")
                    NotificationBulletItem(text = "Smart reminders to protect study streak")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAllowClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B3BA5)
                    )
                ) {
                    Text(
                        text = "Allow Notifications",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maybe Later",
                        color = Color(0xFF5B3BA5),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationBulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            color = Color(0xFF1E1E24),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = Color(0xFF2D3748),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SplashScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AMON",
                color = Color(0xFFF5A524),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Angel of Time - LOTM",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(46.dp))

            Text(
                text = "S T A Y   F O C U S E D\n&\nG R O W",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                letterSpacing = 3.sp
            )
        }
    }
}
