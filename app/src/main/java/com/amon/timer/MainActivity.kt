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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.amon.timer.ui.theme.AmonTheme
import kotlinx.coroutines.delay

private enum class AppScreenState {
    SPLASH,
    AUTH,
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

        val userManager = UserManager(this)
        if (userManager.getUserName().isNotBlank() && !userManager.getUserName().equals("Guest", ignoreCase = true)) {
            CloudSyncManager.registerUserOnCloud(this)
        }

        setContent {
            AmonTheme {
                val context = LocalContext.current
                val localUserManager = remember { UserManager(context) }

                val isDark = ThemeManager.isDarkTheme.value
                val appBgColor = ThemeManager.getBackgroundColor()

                var currentScreen by remember { mutableStateOf(AppScreenState.SPLASH) }

                val currentStatusBarColor = when (currentScreen) {
                    AppScreenState.SPLASH -> Color(0xFF0F0F12)
                    AppScreenState.AUTH -> Color(0xFF0A0A0E)
                    AppScreenState.MAIN -> appBgColor
                }
                val isLightStatusBarIcons = when (currentScreen) {
                    AppScreenState.SPLASH -> false
                    AppScreenState.AUTH -> false
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
                        delay(1200)
                        if (!localUserManager.isFreshAuthDone()) {
                            currentScreen = AppScreenState.AUTH
                        } else {
                            CloudSyncManager.registerUserOnCloud(context)
                            currentScreen = AppScreenState.MAIN
                        }
                    }

                    Crossfade(
                        targetState = currentScreen,
                        animationSpec = tween(durationMillis = 400),
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreenState.SPLASH -> SplashScreenContent()
                            AppScreenState.AUTH -> {
                                AuthScreen(
                                    onAuthComplete = {
                                        localUserManager.setFreshAuthDone(true)
                                        currentScreen = AppScreenState.MAIN
                                    }
                                )
                            }
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
