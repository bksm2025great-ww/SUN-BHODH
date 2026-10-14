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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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

    // 🎥 कैमरा कंट्रोल: ज़ूम और खिसकाव
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val skyBg = if (isDark) Color(0xFF0D0F14) else Color(0xFFF1F5F0)
    val gridLineColor = if (isDark) Color(0x22FFFFFF) else Color(0x335A7B66)
    val centerTileFill = if (isDark) goldColor.copy(alpha = 0.18f) else Color(0x3322C55E)
    val centerTileStroke = if (isDark) goldColor else Color(0xFF22C55E)

    // ग्रिड माप (2:1 आइसोमेट्रिक अनुपात)
    val tileWDp = 120.dp
    val tileHDp = 60.dp

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
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.55f, 2.4f)
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
                modifier = Modifier.size(2400.dp) // विशाल कैनवास
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val tileWPx = tileWDp.toPx()
                val tileHPx = tileHDp.toPx()
                val halfW = tileWPx / 2f
                val halfH = tileHPx / 2f

                val gridRadius = 8

                // A. 2.5D ग्रिड बनाना
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

                        if (col == 0 && row == 0) {
                            // सेंटर बेस को रेंडर करना
                            drawPath(path = diamondPath, color = centerTileFill)
                            drawPath(
                                path = diamondPath,
                                color = centerTileStroke,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            // B. 🌲 3D आइसोमेट्रिक पेड़ का रेंडर (सेंटर स्लॉट पर सीधा माउंट)
                            drawIsometricTree(
                                x = slotCenterX,
                                y = slotCenterY,
                                scale = 1.0f,
                                isDark = isDark,
                                goldColor = goldColor
                            )
                        } else {
                            // सामान्य ग्रिड डिब्बे
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
        // 👑 2. TOP STATUS BAR
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
        // 🎯 3. RE-CENTER BUTTON
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

// -----------------------------------------------------------------------------
// 🌲 2.5D STYLIZED PROCEDURAL TREE DRAW ENGINE (3D आइसोमेट्रिक पेड़)
// -----------------------------------------------------------------------------
private fun DrawScope.drawIsometricTree(
    x: Float,
    y: Float,
    scale: Float = 1.0f,
    isDark: Boolean,
    goldColor: Color
) {
    val treeScale = scale * 1.15f
    
    // 🍂 1. ज़मीन पर परछाई (Oval Ground Shadow)
    drawOval(
        color = Color(0x3D000000), // पारदर्शी काली छाया
        topLeft = Offset(x - 22.dp.toPx() * treeScale, y - 6.dp.toPx() * treeScale),
        size = Size(44.dp.toPx() * treeScale, 13.dp.toPx() * treeScale)
    )

    // 🪵 2. 3D तना (Isometric Vertical Trunk)
    val trunkW = 10.dp.toPx() * treeScale
    val trunkH = 24.dp.toPx() * treeScale
    val trunkBaseX = x
    val trunkBaseY = y + 2.dp.toPx() * treeScale

    // तने का बायाँ (छाया वाला) हिस्सा
    val leftTrunkPath = Path().apply {
        moveTo(trunkBaseX, trunkBaseY)
        lineTo(trunkBaseX - trunkW / 2, trunkBaseY - 3.dp.toPx() * treeScale)
        lineTo(trunkBaseX - trunkW / 2, trunkBaseY - trunkH)
        lineTo(trunkBaseX, trunkBaseY - trunkH + 2.dp.toPx() * treeScale)
        close()
    }
    drawPath(
        path = leftTrunkPath,
        color = if (isDark) Color(0xFF2E170F) else Color(0xFF452718)
    )

    // तने का दायाँ (रोशनी वाला) हिस्सा
    val rightTrunkPath = Path().apply {
        moveTo(trunkBaseX, trunkBaseY)
        lineTo(trunkBaseX + trunkW / 2, trunkBaseY - 3.dp.toPx() * treeScale)
        lineTo(trunkBaseX + trunkW / 2, trunkBaseY - trunkH)
        lineTo(trunkBaseX, trunkBaseY - trunkH + 2.dp.toPx() * treeScale)
        close()
    }
    drawPath(
        path = rightTrunkPath,
        color = if (isDark) Color(0xFF3F2116) else Color(0xFF5C3825)
    )

    // 🌿 3. झुंडदार पत्तियाँ (Overlapping tiered Foliage Engine)
    val tiersCount = 3
    val tierWBase = 46.dp.toPx() * treeScale
    val tierHBase = 26.dp.toPx() * treeScale
    
    // पत्ती स्तरों के रंग (नीचे से ऊपर: गहरा हरा -> चमकदार वाइब्रेंट हरा)
    val foliageColors = if (isDark) {
        listOf(
            Pair(Color(0xFF0F4D19), Color(0xFF145E20)), // निचला स्तर
            Pair(Color(0xFF125C1F), Color(0xFF1B7029)), // मध्य स्तर
            Pair(Color(0xFF156E25), Color(0xFF228A32))  // शिखर स्तर
        )
    } else {
        listOf(
            Pair(Color(0xFF1A5A2B), Color(0xFF227A3C)), // निचला स्तर
            Pair(Color(0xFF1E6F33), Color(0xFF2B9449)), // मध्य स्तर
            Pair(Color(0xFF268D41), Color(0xFF3AA754))  // शिखर स्तर
        )
    }

    for (i in 0 until tiersCount) {
        val tierOverlapY = (14.dp.toPx() * i) * treeScale
        val tierCenterY = (y - trunkH) - 4.dp.toPx() * treeScale + tierOverlapY - (8.dp.toPx() * i) * treeScale

        val tWidth = tierWBase * (1.0f - i * 0.20f)
        val tHeight = tierHBase * (1.0f - i * 0.12f)
        
        val fColorLeft = foliageColors[i].first
        val fColorRight = foliageColors[i].second

        // बायाँ पर्णसमूह (Left Shaded Face)
        val leftLeaf = Path().apply {
            moveTo(x, tierCenterY - tHeight / 2)
            lineTo(x - tWidth / 2, tierCenterY + 4.dp.toPx() * treeScale)
            lineTo(x, tierCenterY + tHeight / 2)
            close()
        }
        drawPath(path = leftLeaf, color = fColorLeft)

        // दायाँ पर्णसमूह (Right Lit Face)
        val rightLeaf = Path().apply {
            moveTo(x, tierCenterY - tHeight / 2)
            lineTo(x + tWidth / 2, tierCenterY + 4.dp.toPx() * treeScale)
            lineTo(x, tierCenterY + tHeight / 2)
            close()
        }
        drawPath(path = rightLeaf, color = fColorRight)
        
        // 💡 शीर्ष भाग पर उभार (Top highlighting overlays)
        val topCap = Path().apply {
            moveTo(x, tierCenterY - tHeight / 2)
            lineTo(x + tWidth / 4, tierCenterY - tHeight / 4)
            lineTo(x, tierCenterY)
            lineTo(x - tWidth / 4, tierCenterY - tHeight / 4)
            close()
        }
        drawPath(path = topCap, color = fColorRight.copy(alpha = 0.22f))

        // ✨ सुनहरे फलों/गेंदा के दानों की चमक (Gold Fruits/Sparks)
        // कुछ रैंडम बिंदुओं पर छोटी चमकें
        val fruitBaseY = tierCenterY + 2.dp.toPx() * treeScale
        val dotsOffsets = listOf(
            Offset(-tWidth * 0.25f, fruitBaseY + 1.dp.toPx() * treeScale),
            Offset(-tWidth * 0.12f, fruitBaseY - 5.dp.toPx() * treeScale),
            Offset(tWidth * 0.18f, fruitBaseY + 3.dp.toPx() * treeScale),
            Offset(tWidth * 0.28f, fruitBaseY - 2.dp.toPx() * treeScale),
            Offset(0f, fruitBaseY - 9.dp.toPx() * treeScale)
        )

        dotsOffsets.forEach { offset ->
            // फल के पीछे छोटी परछाई / उभार
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = 2.2.dp.toPx() * treeScale,
                center = Offset(x + offset.x, offset.y + 0.8f.dp.toPx() * treeScale)
            )
            // सुनहरा फल
            drawCircle(
                color = goldColor,
                radius = 1.7.dp.toPx() * treeScale,
                center = Offset(x + offset.x, offset.y)
            )
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
