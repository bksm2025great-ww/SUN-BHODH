package com.amon.timer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun PreferencesScreen(onBack: () -> Unit) {
    // 🔙 Back gesture
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

    // 💡 Screen Awake Controller
    DisposableEffect(isKeepScreenAwake) {
        val activity = context as? Activity
        if (isKeepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { }
    }

    // 🔔 Notifications Toggle & Permission Launcher
    var isNotificationsEnabled by remember {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        mutableStateOf(prefs.getBoolean("pref_notifications", isGranted))
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationsEnabled = granted
        prefs.edit().putBoolean("pref_notifications", granted).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----------------- 1. TOP HEADER WITH BACK BUTTON -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, cardBorder, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                OutlinedArrowBackIcon(tint = textMain)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Preferences & Controls",
                    color = textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Themes, display, and alerts",
                    color = textMuted,
                    fontSize = 11.5.sp
                )
            }
        }

        // ----------------- 2. SECTION: APPEARANCE & THEMES -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedPaletteIcon(tint = goldColor)
                    Text(
                        text = "Appearance & Themes",
                        color = textMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(14.dp))

                // THEME MODE SELECTOR
                Text(
                    text = "THEME MODE",
                    color = goldColor,
                    fontSize = 11.5.sp,
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

                Spacer(modifier = Modifier.height(18.dp))

                // ACCENT COLOR SELECTOR
                Text(
                    text = "ACCENT COLOR",
                    color = goldColor,
                    fontSize = 11.5.sp,
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

        // ----------------- 3. SECTION: CONTROLS & TOGGLES -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "CONTROLS & HARDWARE",
                    color = goldColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // SWITCH 1: KEEP SCREEN AWAKE (NOW WITH SUN AWAKE ICON)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedSunAwakeIcon(tint = goldColor)
                        Column {
                            Text(
                                text = "Keep Screen Awake",
                                color = textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Screen will stay on during timer",
                                color = textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
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

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cardBorder.copy(alpha = 0.5f)))

                // SWITCH 2: HAPTIC BUZZ (VIBRATION)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedPhoneVibeIcon(tint = goldColor)
                        Column {
                            Text(
                                text = "Haptic Buzz (Vibration)",
                                color = textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Vibrate softly when focus completes",
                                color = textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
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

                // SWITCH 3: NOTIFICATIONS & REMINDERS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedBellIcon(tint = goldColor)
                        Column {
                            Text(
                                text = "Notifications & Reminders",
                                color = textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Timer chimes & streak protection",
                                color = textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!isGranted) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        isNotificationsEnabled = true
                                        prefs.edit().putBoolean("pref_notifications", true).apply()
                                    }
                                } else {
                                    isNotificationsEnabled = true
                                    prefs.edit().putBoolean("pref_notifications", true).apply()
                                }
                            } else {
                                isNotificationsEnabled = false
                                prefs.edit().putBoolean("pref_notifications", false).apply()
                            }
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

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// =============================================================================
// ✨ HAND-DRAWN VECTOR ICONS (SELF-CONTAINED)
// =============================================================================

@Composable
private fun OutlinedArrowBackIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        val arrow = Path().apply {
            moveTo(w * 0.60f, h * 0.20f)
            lineTo(w * 0.28f, h * 0.50f)
            lineTo(w * 0.60f, h * 0.80f)
        }
        drawPath(arrow, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = tint, start = Offset(w * 0.30f, h * 0.50f), end = Offset(w * 0.78f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OutlinedPaletteIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
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
private fun OutlinedSunAwakeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        // बीच का घेरा (Center Circle)
        drawCircle(
            color = tint,
            radius = w * 0.22f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = stroke)
        )

        // चारों तरफ़ सूरज की किरणें (Awake Rays)
        val r1 = w * 0.32f
        val r2 = w * 0.44f
        val cx = w * 0.50f
        val cy = h * 0.50f

        // Top, Bottom, Left, Right
        drawLine(color = tint, start = Offset(cx, cy - r1), end = Offset(cx, cy - r2), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(cx, cy + r1), end = Offset(cx, cy + r2), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(cx - r1, cy), end = Offset(cx - r2, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(cx + r1, cy), end = Offset(cx + r2, cy), strokeWidth = stroke, cap = StrokeCap.Round)

        // 4 Diagonals (तिरछी किरणें)
        val d1 = r1 * 0.707f
        val d2 = r2 * 0.707f
        drawLine(color = tint, start = Offset(cx + d1, cy - d1), end = Offset(cx + d2, cy - d2), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(cx - d1, cy - d1), end = Offset(cx - d2, cy - d2), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(cx + d1, cy + d1), end = Offset(cx + d2, cy + d2), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(cx - d1, cy + d1), end = Offset(cx - d2, cy + d2), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OutlinedPhoneVibeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
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
private fun OutlinedBellIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()

        val bell = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            cubicTo(w * 0.34f, h * 0.14f, w * 0.26f, h * 0.32f, w * 0.26f, h * 0.56f)
            lineTo(w * 0.18f, h * 0.72f)
            lineTo(w * 0.82f, h * 0.72f)
            lineTo(w * 0.74f, h * 0.56f)
            cubicTo(w * 0.74f, h * 0.32f, w * 0.66f, h * 0.14f, w * 0.50f, h * 0.14f)
            close()
        }
        drawPath(bell, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.41f, h * 0.74f),
            size = Size(w * 0.18f, h * 0.14f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
