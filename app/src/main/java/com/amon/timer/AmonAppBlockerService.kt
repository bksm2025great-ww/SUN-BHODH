package com.amon.timer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class AmonAppBlockerService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentlyBlockedPackage: String? = null
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString().orEmpty()
        if (pkgName.isBlank()) return

        if (pkgName == packageName || isSafeSystemOrLauncherPackage(pkgName)) {
            if (pkgName == packageName) {
                removeOverlayShield()
            }
            return
        }

        val isFocusRunning = TimerService.isTimerRunning.value || TimerService.isTimerPaused.value
        if (!isFocusRunning) {
            removeOverlayShield()
            return
        }

        if (!BlockAppManager.isBlockingEnabled(this)) {
            removeOverlayShield()
            return
        }

        val settingsConfig = BlockSettingsManager.loadConfig(this)
        if (TimerService.isPomoBreakActive.value && !settingsConfig.blockAppsDuringBreak) {
            removeOverlayShield()
            return
        }

        if (BlockAppManager.isPackageBlocked(this, pkgName)) {
            if (currentlyBlockedPackage != pkgName || overlayView == null) {
                showOverlayShield(pkgName)
            }
        } else {
            removeOverlayShield()
        }
    }

    override fun onInterrupt() {
        removeOverlayShield()
    }

    override fun onDestroy() {
        removeOverlayShield()
        super.onDestroy()
    }

    private fun isSafeSystemOrLauncherPackage(pkgName: String): Boolean {
        val safePrefixes = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.miui.home",
            "com.oppo.launcher",
            "com.sec.android.app.launcher"
        )
        return safePrefixes.any { pkgName.startsWith(it) }
    }

    private fun showOverlayShield(blockedPkg: String) {
        val wm = windowManager ?: (getSystemService(WINDOW_SERVICE) as? WindowManager)?.also {
            windowManager = it
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        removeOverlayShield()

        val appBitmap = try {
            BlockAppManager.getAppIcon(this, blockedPkg)?.toBitmap(width = 180, height = 180)
        } catch (_: Exception) {
            null
        }

        val lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
            onResume()
        }
        overlayLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                AmonOverlayShieldScreen(
                    appIconBitmap = appBitmap,
                    onReturnToAmon = {
                        removeOverlayShield()
                        openAmonApp()
                    }
                )
            }
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            wm.addView(composeView, params)
            overlayView = composeView
            currentlyBlockedPackage = blockedPkg
        } catch (_: Exception) {
            lifecycleOwner.destroy()
            overlayLifecycleOwner = null
        }
    }

    private fun removeOverlayShield() {
        val viewToRemove = overlayView
        val wm = windowManager
        if (viewToRemove != null && wm != null) {
            try {
                wm.removeViewImmediate(viewToRemove)
            } catch (_: Exception) {
                try {
                    wm.removeView(viewToRemove)
                } catch (_: Exception) {}
            }
        }
        overlayView = null
        currentlyBlockedPackage = null

        overlayLifecycleOwner?.destroy()
        overlayLifecycleOwner = null
    }

    private fun openAmonApp() {
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(launchIntent)
        } catch (_: Exception) {}
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = registry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun onCreate() {
            savedStateController.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onResume() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            if (registry.currentState != Lifecycle.State.DESTROYED) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        }
    }
}

@Composable
private fun AmonOverlayShieldScreen(
    appIconBitmap: android.graphics.Bitmap?,
    onReturnToAmon: () -> Unit
) {
    val bgDark = Color(0xFF121216)
    val goldAccent = Color(0xFFE5B44C)
    val softRingColor = Color(0xFFE5B44C)
    val subTextColor = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(210.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = softRingColor.copy(alpha = 0.22f),
                        radius = size.minDimension * 0.46f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = softRingColor.copy(alpha = 0.35f),
                        radius = size.minDimension * 0.36f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Box(contentAlignment = Alignment.BottomEnd) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap.asImageBitmap(),
                            contentDescription = "Blocked App Icon",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(2.dp, goldAccent.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF1E1E28))
                                .border(2.dp, goldAccent.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        ) {
                            Text(
                                text = "APP",
                                color = goldAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset(x = 6.dp, y = 6.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(goldAccent)
                            .border(2.5.dp, bgDark, CircleShape)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val w = size.width
                            val h = size.height
                            drawRoundRect(
                                color = Color(0xFF121216),
                                topLeft = Offset(w * 0.22f, h * 0.46f),
                                size = Size(w * 0.56f, h * 0.42f),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
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
                                color = Color(0xFF121216),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Focus Session Active",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This app is blocked during focus",
                    color = subTextColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onReturnToAmon,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = goldAccent,
                    contentColor = Color(0xFF121216)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Return to AMON",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
