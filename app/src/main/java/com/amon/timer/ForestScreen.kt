package com.amon.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ForestScreen() {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkTheme.value

    val goldColor = ThemeManager.getAccentColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val cardBg = ThemeManager.getCardColor()
    val glassBorder = if (isDark) Color(0x22FFFFFF) else Color(0x33000000)

    val sessions = remember { FocusSessionManager.getAllSessions(context) }
    val totalMins = remember(sessions) { sessions.sumOf { it.durationMinutes } }
    val totalHours = String.format(Locale.getDefault(), "%.1f", totalMins / 60f)

    // 🌳 WebP इमेज लोड (R.drawable.tree_maple)
    val mapleTreeBitmap = remember {
        try {
            ImageBitmap.imageResource(context.resources, R.drawable.tree_maple)
        } catch (_: Exception) {
            null
        }
    }

    // 🎥 कैमरा कंट्रोल: ज़ूम और पैन
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // 🌌 बैकग्राउंड
    val skyBg = if (isDark) Color(0xFF0D0F14) else Color(0xFFF1F5F0)
    val gridLineColor = if (isDark) Color(0x24FFFFFF) else Color(0x335A7B66)

    // 🧱 ग्रिड नाप (2:1 आइसोमेट्रिक अनुपात)
    val tileWDp = 100.dp
    val tileHDp = 50.dp

    // 🌲 पास-पास के 5 टेस्ट स्लॉट्स (क्लस्टर)
    val testClusterSlots = remember {
        listOf(
            Pair(-1, 0),  // पीछे-बाएँ
            Pair(0, -1),  // पीछे-दाएँ
            Pair(0, 0),   // बीच का मुख्य पेड़
            Pair(0, 1),   // आगे-बाएँ
            Pair(1, 0)    // आगे-दाएँ
        ).sortedBy { (col, row) -> col + row } // 🎯 जादुई क्रम: पीछे वाले पहले, आगे वाले बाद में!
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBg)
    ) {
        // ---------------------------------------------------------------------
        // 🌍 UNIFIED WORLD CONTAINER (कैमरा ज़ूम और ड्रैग)
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.4f, 2.5f)
                        panOffset += pan
                    }
                }
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(3600.dp) // 50x50 का विशाल कैनवास
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val tileWPx = tileWDp.toPx()
                val tileHPx = tileHDp.toPx()
                val halfW = tileWPx / 2f
                val halfH = tileHPx / 2f

                val gridRadius = 14 // 50x50 ग्रिड

                // -------------------------------------------------------------
                // 1. लेयर 1: सबसे नीचे पूरा वायरफ़्रेम ग्रिड (GRID PASS)
                // -------------------------------------------------------------
                for (col in -gridRadius..gridRadius) {
                    for (row in -gridRadius..gridRadius) {
                        val slotCenterX = centerX + (col - row) * halfW
                        val slotCenterY = centerY + (col + row) * halfH

                        val diamondPath = Path().apply {
                            moveTo(slotCenterX, slotCenterY - halfH)
                            lineTo(slotCenterX + halfW, slotCenterY)
                            lineTo(slotCenterX, slotCenterY + halfH)
                            lineTo(slotCenterX - halfW, slotCenterY)
                            close()
                        }

                        drawPath(
                            path = diamondPath,
                            color = gridLineColor,
                            style = Stroke(width = 0.85.dp.toPx())
                        )
                    }
                }

                // -------------------------------------------------------------
                // 2. लेयर 2: सभी 5 पेड़ों की परछाइयाँ (SHADOW PASS)
                // -------------------------------------------------------------
                testClusterSlots.forEach { (c, r) ->
                    val slotCenterX = centerX + (c - r) * halfW
                    val slotCenterY = centerY + (c + r) * halfH

                    drawOval(
                        color = Color(0x33000000),
                        topLeft = Offset(slotCenterX - 40.dp.toPx(), slotCenterY + 4.dp.toPx()),
                        size = Size(80.dp.toPx(), 28.dp.toPx())
                    )
                }

                // -------------------------------------------------------------
                // 3. लेयर 3: 3D पेड़ सही क्रम में (TREE RENDERING PASS)
                // -------------------------------------------------------------
                if (mapleTreeBitmap != null) {
                    val treeRenderW = 140.dp.toPx()
                    val treeRenderH = 140.dp.toPx()

                    testClusterSlots.forEach { (c, r) ->
                        val slotCenterX = centerX + (c - r) * halfW
                        val slotCenterY = centerY + (c + r) * halfH

                        val drawPosX = slotCenterX - (treeRenderW / 2f)
                        // यह ऑफ़सेट पेड़ के चबूतरे को ग्रिड के ठीक ऊपर उभरा हुआ रखता है
                        val drawPosY = (slotCenterY + halfH) - (treeRenderH * 0.88f)

                        drawImage(
                            image = mapleTreeBitmap,
                            dstOffset = IntOffset(drawPosX.roundToInt(), drawPosY.roundToInt()),
                            dstSize = IntSize(treeRenderW.roundToInt(), treeRenderH.roundToInt())
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 👑 TOP STATUS BAR
        // ---------------------------------------------------------------------
        Surface(
            shape = RoundedCornerShape(50),
            color = cardBg.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .border(1.dp, glassBorder, RoundedCornerShape(50))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🍁 Maple Forest (${testClusterSlots.size} Trees)",
                    color = textMain,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "•", color = textMuted, fontSize = 11.sp)
                Text(
                    text = "⏱️ ${totalHours}h Focused",
                    color = goldColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ---------------------------------------------------------------------
        // 🎯 RE-CENTER BUTTON
        // ---------------------------------------------------------------------
        Surface(
            shape = CircleShape,
            color = cardBg.copy(alpha = 0.94f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .size(46.dp)
                .border(1.dp, glassBorder, CircleShape),
            onClick = {
                zoomScale = 1.0f
                panOffset = Offset.Zero
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "🎯", fontSize = 18.sp)
            }
        }
    }
}

// 📅 Universal Smart Date Parser
fun parseSessionDateUniversal(rawDateStr: String): Date? {
    if (rawDateStr.isBlank()) return null
    val clean = rawDateStr.replace("\n", " ").replace("\r", " ").replace("\"", "").replace("'", "").replace(",", " ").replace(Regex("\\s+"), " ").trim()
    clean.toLongOrNull()?.let { millis -> if (millis > 1000000000000L) return Date(millis) }
    val formats = listOf(
        SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )
    for (fmt in formats) {
        try {
            val d = fmt.parse(clean)
            if (d != null) return d
        } catch (_: Exception) {}
    }
    return null
}
