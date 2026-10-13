package com.amon.timer

import android.content.Context
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
import androidx.compose.ui.draw.clip
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

    // डेटाबेस से आज के और कुल सेशन्स निकालना
    val sessions = remember { FocusSessionManager.getAllSessions(context) }
    val totalMins = remember(sessions) { sessions.sumOf { it.durationMinutes } }
    val totalHours = String.format(Locale.getDefault(), "%.1f", totalMins / 60f)

    // 🎥 कैमरा कंट्रोल: ज़ूम (Scale) और खिसकाव (Pan/Offset)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // बैकग्राउंड और ग्रिड लाइनों के रंग
    val skyBg = if (isDark) Color(0xFF0D0F14) else Color(0xFFF1F5F0)
    val gridLineColor = if (isDark) Color(0x22FFFFFF) else Color(0x335A7B66)
    val centerTileFill = if (isDark) goldColor.copy(alpha = 0.18f) else Color(0x3322C55E)
    val centerTileStroke = if (isDark) goldColor else Color(0xFF22C55E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBg)
    ) {
        // ---------------------------------------------------------------------
        // 🌍 1. UNIFIED WORLD CONTAINER (कैमरा: ग्रिड और पौधे 100% एक साथ रहेंगे)
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // उँगली से ड्रैग और दो उँगलियों से ज़ूम को पकड़ने वाला इंजन
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.55f, 2.4f)
                        panOffset += pan
                    }
                }
                .graphicsLayer {
                    // पूरे विश्व (World) को एक साथ स्केल और ट्रांसलेट करता है
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                },
            contentAlignment = Alignment.Center
        ) {
            // 📐 2.5D डायमंड ग्रिड कैनवास (2:1 रेशियो)
            Canvas(
                modifier = Modifier
                    .size(2400.dp) // विशाल कैनवास क्षेत्र
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                // 2:1 आइसोमेट्रिक अनुपात: 120 dp चौड़ाई, 60 dp ऊँचाई
                val tileWPx = 120.dp.toPx()
                val tileHPx = 60.dp.toPx()

                val halfW = tileWPx / 2f
                val halfH = tileHPx / 2f

                val gridRadius = 8 // -8 से +8 यानी लगभग 289 डायमंड स्लॉट्स

                // पूरे ग्रिड के जाल को ड्रा करना
                for (col in -gridRadius..gridRadius) {
                    for (row in -gridRadius..gridRadius) {
                        // आइसोमेट्रिक गणितीय प्रोजेक्शन
                        val slotCenterX = centerX + (col - row) * halfW
                        val slotCenterY = centerY + (col + row) * halfH

                        val diamondPath = Path().apply {
                            moveTo(slotCenterX, slotCenterY - halfH) // ऊपर का कोना
                            lineTo(slotCenterX + halfW, slotCenterY) // दायाँ कोना
                            lineTo(slotCenterX, slotCenterY + halfH) // नीचे का कोना
                            lineTo(slotCenterX - halfW, slotCenterY) // बायाँ कोना
                            close()
                        }

                        // सेंटर स्लॉट (0, 0) जहाँ पहला पौधा उगेगा, उसे थोड़ा हाइलाइट करेंगे
                        if (col == 0 && row == 0) {
                            drawPath(
                                path = diamondPath,
                                color = centerTileFill
                            )
                            drawPath(
                                path = diamondPath,
                                color = centerTileStroke,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        } else {
                            // बाकी सारे सामान्य ग्रिड डिब्बे
                            drawPath(
                                path = diamondPath,
                                color = gridLineColor,
                                style = Stroke(width = 0.85.dp.toPx())
                            )
                        }
                    }
                }

                // 🌟 सेंटर मार्कर बिंदु (सटीक सेंटर जांचने के लिए)
                drawCircle(
                    color = centerTileStroke,
                    radius = 3.5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        }

        // ---------------------------------------------------------------------
        // 👑 2. TOP FLOATING STATUS BAR (साफ़ और पारदर्शी हेडर)
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
                    text = "🌲 ${sessions.size} Trees",
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
        // 🎯 3. RE-CENTER BUTTON (अगर नक्शा बहुत दूर चला जाए तो 1 क्लिक में रीसेट)
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
                // कैमरे को तुरंत वापस सेंटर पर ले आना
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

// -----------------------------------------------------------------------------
// 📅 Universal Smart Date Parser (प्रोजेक्ट सुरक्षा के लिए सुरक्षित रखा गया)
// -----------------------------------------------------------------------------
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
