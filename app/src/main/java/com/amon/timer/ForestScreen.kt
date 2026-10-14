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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // 🎥 कैमरा कंट्रोल: ज़ूम और पैन
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // 🌌 डार्क बैकग्राउंड (स्पेस / मिनिमल थीम)
    val skyBg = if (isDark) Color(0xFF0D0F14) else Color(0xFFF1F5F0)
    val gridLineColor = if (isDark) Color(0x26FFFFFF) else Color(0x335A7B66)

    // 🧱 ग्रिड और 3D टाइल का नाप
    val tileWDp = 100.dp
    val tileHDp = 50.dp
    val soilDepthDp = 24.dp // सेंटर वाली टाइल की 3D मिट्टी की मोटाई

    // 🎨 3D घास और मिट्टी के रंग
    val grassTopColor = if (isDark) Color(0xFF388E3C) else Color(0xFF5DBB46)
    val grassBorderColor = if (isDark) Color(0xFF4CAF50) else Color(0xFF76C444)
    val grassSpeckColor = if (isDark) Color(0x5581C784) else Color(0x55A5E876)

    val soilLeftColor = if (isDark) Color(0xFF4E342E) else Color(0xFF6D4C41)   // बायीं मिट्टी (रोशनी)
    val soilRightColor = if (isDark) Color(0xFF3E2723) else Color(0xFF4E342E)  // दायीं मिट्टी (छाया)
    val soilLineColor = if (isDark) Color(0x44000000) else Color(0x33000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBg)
    ) {
        // ---------------------------------------------------------------------
        // 🌍 UNIFIED WORLD CONTAINER (कैमरा: ग्रिड को ज़ूम और ड्रैग करना)
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
                modifier = Modifier.size(3600.dp) // विशाल कैनवास
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val tileWPx = tileWDp.toPx()
                val tileHPx = tileHDp.toPx()
                val halfW = tileWPx / 2f
                val halfH = tileHPx / 2f
                val soilDepthPx = soilDepthDp.toPx()

                // ग्रिड का फैलाव (रेडियस 14 = लगभग 800+ टाइल्स का विशाल मैदान)
                val gridRadius = 14

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

                        // ⭐ केवल सेंटर स्लॉट (0, 0) पर 3D घास + मिट्टी की टाइल बनेगी
                        if (col == 0 && row == 0) {
                            // A. 3D मिट्टी की बायीं दीवार (Soil Left Cliff)
                            val soilLeft = Path().apply {
                                moveTo(slotCenterX - halfW, slotCenterY)
                                lineTo(slotCenterX, slotCenterY + halfH)
                                lineTo(slotCenterX, slotCenterY + halfH + soilDepthPx)
                                lineTo(slotCenterX - halfW, slotCenterY + soilDepthPx)
                                close()
                            }
                            drawPath(soilLeft, color = soilLeftColor)

                            // B. 3D मिट्टी की दायीं दीवार (Soil Right Cliff)
                            val soilRight = Path().apply {
                                moveTo(slotCenterX, slotCenterY + halfH)
                                lineTo(slotCenterX + halfW, slotCenterY)
                                lineTo(slotCenterX + halfW, slotCenterY + soilDepthPx)
                                lineTo(slotCenterX, slotCenterY + halfH + soilDepthPx)
                                close()
                            }
                            drawPath(soilRight, color = soilRightColor)

                            // मिट्टी के बीच की बॉर्डर लाइन
                            drawLine(
                                color = soilLineColor,
                                start = Offset(slotCenterX, slotCenterY + halfH),
                                end = Offset(slotCenterX, slotCenterY + halfH + soilDepthPx),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // C. ऊपर की मखमली हरी घास (Lush Grass Surface)
                            drawPath(diamondPath, color = grassTopColor)
                            drawPath(
                                diamondPath,
                                color = grassBorderColor,
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // घास के छोटे-छोटे तिनके (Texture Details)
                            drawCircle(
                                color = grassSpeckColor,
                                radius = 1.2.dp.toPx(),
                                center = Offset(slotCenterX - 10.dp.toPx(), slotCenterY - 4.dp.toPx())
                            )
                            drawCircle(
                                color = grassSpeckColor,
                                radius = 1.0.dp.toPx(),
                                center = Offset(slotCenterX + 12.dp.toPx(), slotCenterY + 2.dp.toPx())
                            )
                        } else {
                            // 🌐 बाकी पूरा मैदान: आपका मनपसंद वायरफ़्रेम ग्रिड
                            drawPath(
                                path = diamondPath,
                                color = gridLineColor,
                                style = Stroke(width = 0.85.dp.toPx())
                            )
                        }
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
                    text = "🌱 Forest Grid",
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
