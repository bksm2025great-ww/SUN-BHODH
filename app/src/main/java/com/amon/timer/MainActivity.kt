package com.amon.timer

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.amon.timer.ui.theme.AmonTheme
import kotlinx.coroutines.delay

// 📱 App की 2 मुख्य स्टेज (Splash ➔ Direct Main)
private enum class AppScreenState {
    SPLASH,  // 1.5 second ka Angel of Time intro
    MAIN     // Main Timer, Forest, Stats & Profile
}

// 🟢 START: [MAIN_ACTIVITY_ENTRY]
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚡ 120Hz Force Unlock: फ़ोन की स्क्रीन को उसके सबसे उच्चतम रिफ्रेश रेट पर लॉक करना
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

        // 🛡️ सुरक्षा कवच: अलार्म एरर की वजह से ऐप कभी क्रैश नहीं होगा
        try {
            AmonReminderManager.scheduleAllReminders(this)
        } catch (e: Exception) {
            Log.e("AmonLaunch", "Safe Reminder catch: ${e.localizedMessage}")
        }

        setContent {
            AmonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F12)
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreenState.SPLASH) }

                    // ⚡ 1.5 second ka splash delay aur seedhe MainScreen par entry
                    LaunchedEffect(Unit) {
                        delay(1500)
                        currentScreen = AppScreenState.MAIN
                    }

                    // Smooth transition animation
                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 400),
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreenState.SPLASH -> {
                                SplashScreenContent()
                            }
                            AppScreenState.MAIN -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // 1. मुख्य टाइमर स्क्रीन (MainScreen)
                                    MainScreen()

                                    // 🔄 WhatsApp Line-Art Styled Android Auto-Rotate Button
                                    FocusRotateButtonOverlay(activity = this@MainActivity)

                                    // 2. Android 13+ Notification Permission Dialog
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val context = LocalContext.current
                                        var showPermissionDialog by remember {
                                            val isGranted = ContextCompat.checkSelfPermission(
                                                context,
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

// =============================================================================
// 🔘 MINIMAL OUTLINE ROTATE BUTTON (PROFILE-SCREEN AESTHETIC)
// =============================================================================
@Composable
private fun FocusRotateButtonOverlay(activity: ComponentActivity) {
    val isRunning = TimerService.isTimerRunning.value
    val isPaused = TimerService.isTimerPaused.value
    val isFocusActive = isRunning || isPaused

    var isLandscape by remember { mutableStateOf(false) }

    // 🛡️ फ़ोकस ख़त्म होते ही स्क्रीन तुरंत वापस सीधी (Portrait) लॉक हो जाएगी
    LaunchedEffect(isFocusActive) {
        if (!isFocusActive) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isLandscape = false
        }
    }

    AnimatedVisibility(
        visible = isFocusActive,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, end = 16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            // 🪨 ProfileScreen जैसा स्लीक चारकोल ग्लास बटन
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.4f),
                        ambientColor = Color.Black.copy(alpha = 0.2f)
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E26))
                    .border(
                        width = 1.1.dp,
                        color = if (isLandscape) Color(0xFFF5A524) else Color(0x33FFFFFF),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isLandscape) {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            isLandscape = false
                        } else {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            isLandscape = true
                        }
                    }
            ) {
                AndroidAutoRotateLineIcon(
                    tint = if (isLandscape) Color(0xFFF5A524) else Color(0xFFE2E8F0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// =============================================================================
// 🔄 WHATSAPP-STYLE OUTLINED ANDROID AUTO-ROTATE ICON (CANVAS VECTOR)
// =============================================================================
@Composable
private fun AndroidAutoRotateLineIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val strokeWidth = 1.4.dp.toPx()

        // 1. बीच का क्लीन आउटलाइन फ़ोन (WhatsApp Style Line Art)
        val phoneW = w * 0.32f
        val phoneH = h * 0.50f
        drawRoundRect(
            color = tint,
            topLeft = Offset(center.x - phoneW / 2f, center.y - phoneH / 2f),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // 2. ऊपरी घुमावदार तीर (Top Arc Arrow)
        val arcRadius = w * 0.38f
        drawArc(
            color = tint,
            startAngle = 205f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // तीर का मुँह (Top Arrowhead)
        val arrowPath1 = Path().apply {
            moveTo(center.x + arcRadius * 0.10f, center.y - arcRadius * 1.02f)
            lineTo(center.x + arcRadius * 0.40f, center.y - arcRadius * 0.90f)
            lineTo(center.x + arcRadius * 0.25f, center.y - arcRadius * 0.62f)
            close()
        }
        drawPath(path = arrowPath1, color = tint)

        // 3. निचला घुमावदार तीर (Bottom Arc Arrow)
        drawArc(
            color = tint,
            startAngle = 25f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // तीर का मुँह (Bottom Arrowhead)
        val arrowPath2 = Path().apply {
            moveTo(center.x - arcRadius * 0.10f, center.y + arcRadius * 1.02f)
            lineTo(center.x - arcRadius * 0.40f, center.y + arcRadius * 0.90f)
            lineTo(center.x - arcRadius * 0.25f, center.y + arcRadius * 0.62f)
            close()
        }
        drawPath(path = arrowPath2, color = tint)
    }
}

// 🔔 Notification Rationale Card (Material 3 Design)
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

// ✨ 1.5 second wali Amon Splash Screen (Angel of Time)
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
// 🔴 END: [MAIN_ACTIVITY_ENTRY]
