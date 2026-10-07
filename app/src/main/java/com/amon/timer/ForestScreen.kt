package com.amon.timer

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

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

    val infiniteTransition = rememberInfiniteTransition(label = "living_sanctuary")

    // 1. साँस लेने की गति (Breathing Physics)
    val breathePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathing"
    )

    // 2. आग की लौ का स्पंदन (Soul Fire Pulse)
    val firePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_pulse"
    )

    // 3. जादुई कणों का ऊपर तैरना (Floating Embers)
    val emberProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "embers"
    )

    // 4. टच करने पर उछलने का एनिमेशन (Tap Squish & Joy Bounce)
    val coroutineScope = rememberCoroutineScope()
    val guardianSquish = remember { Animatable(1f) }
    val heartPopProgress = remember { Animatable(0f) }

    val sanctuaryBg = if (isDark) Color(0xFF0B1310) else Color(0xFFEDF5EC)
    val grassHillColor = if (isDark) Color(0xFF13221C) else Color(0xFFDFEFE0)
    val stoneAltarColor = if (isDark) Color(0xFF22362C) else Color(0xFFCBDCD0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sanctuaryBg)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val w = size.width
                        val h = size.height

                        // गार्जियन की स्थिति (आग के दाईं तरफ़)
                        val guardianCenter = Offset(w * 0.68f, h * 0.54f)
                        val dist = hypot(tapOffset.x - guardianCenter.x, tapOffset.y - guardianCenter.y)

                        // अगर यूजर ने नन्हें जीव को छुआ
                        if (dist < 60.dp.toPx()) {
                            coroutineScope.launch {
                                // जेली की तरह सिकुड़ना और फिर उछलना
                                guardianSquish.snapTo(0.72f)
                                launch {
                                    guardianSquish.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                                // प्यार भरा दिल ऊपर उड़ना
                                heartPopProgress.snapTo(0.01f)
                                heartPopProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(900, easing = FastOutSlowInEasing)
                                )
                                heartPopProgress.snapTo(0f)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // A. शांत पहाड़ी ज़मीन (Curved Sanctuary Ground)
            val groundPath = Path().apply {
                moveTo(0f, h * 0.44f)
                cubicTo(w * 0.35f, h * 0.40f, w * 0.65f, h * 0.47f, w, h * 0.43f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path = groundPath, color = grassHillColor)

            // B. केंद्र में पत्थर की वेदी (The Stone Altar)
            val altarCenter = Offset(w * 0.36f, h * 0.53f)
            drawOval(
                color = if (isDark) Color(0x33000000) else Color(0x1A000000),
                topLeft = Offset(altarCenter.x - 52.dp.toPx(), altarCenter.y + 14.dp.toPx()),
                size = Size(104.dp.toPx(), 26.dp.toPx())
            )
            drawOval(
                color = stoneAltarColor,
                topLeft = Offset(altarCenter.x - 44.dp.toPx(), altarCenter.y - 12.dp.toPx()),
                size = Size(88.dp.toPx(), 28.dp.toPx())
            )

            // C. द सोल फ़ायर (The Soul Fire & Aura)
            val auraRadius = (60.dp.toPx()) * firePulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x66FBBF24),
                        Color(0x33F59E0B),
                        Color.Transparent
                    ),
                    center = altarCenter,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = altarCenter
            )

            // आग की जादुई लौ
            val flameHeight = (36.dp.toPx()) * firePulse
            val flamePath = Path().apply {
                moveTo(altarCenter.x, altarCenter.y - flameHeight)
                cubicTo(
                    altarCenter.x + 18.dp.toPx(), altarCenter.y - flameHeight * 0.4f,
                    altarCenter.x + 12.dp.toPx(), altarCenter.y,
                    altarCenter.x, altarCenter.y
                )
                cubicTo(
                    altarCenter.x - 12.dp.toPx(), altarCenter.y,
                    altarCenter.x - 18.dp.toPx(), altarCenter.y - flameHeight * 0.4f,
                    altarCenter.x, altarCenter.y - flameHeight
                )
                close()
            }
            drawPath(flamePath, color = Color(0xFFF59E0B))

            // आग का आंतरिक चमकदार हिस्सा
            val innerFlamePath = Path().apply {
                val ih = flameHeight * 0.65f
                moveTo(altarCenter.x, altarCenter.y - ih)
                cubicTo(
                    altarCenter.x + 9.dp.toPx(), altarCenter.y - ih * 0.3f,
                    altarCenter.x + 6.dp.toPx(), altarCenter.y,
                    altarCenter.x, altarCenter.y
                )
                cubicTo(
                    altarCenter.x - 6.dp.toPx(), altarCenter.y,
                    altarCenter.x - 9.dp.toPx(), altarCenter.y - ih * 0.3f,
                    altarCenter.x, altarCenter.y - ih
                )
                close()
            }
            drawPath(innerFlamePath, color = Color(0xFFFEF08A))

            // आग से उठती चिंगारियाँ (Floating Embers)
            for (i in 0..4) {
                val progress = (emberProgress + i * 0.2f) % 1f
                val ey = altarCenter.y - (progress * 70.dp.toPx())
                val ex = altarCenter.x + sin(progress * PI.toFloat() * 2f + i) * 16.dp.toPx()
                val eAlpha = (1f - progress).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = eAlpha),
                    radius = (2.2.dp.toPx()) * (1f - progress * 0.5f),
                    center = Offset(ex, ey)
                )
            }

            // D. पहला गार्जियन: द एस्ट्रल फ़ॉक्स-विस्प (The Astral Guardian)
            val breathOffset = sin(breathePhase) * 2.2.dp.toPx()
            val gBase = Offset(w * 0.68f, h * 0.54f)
            val currentSquish = guardianSquish.value

            drawGuardianSpirit(
                base = gBase,
                squish = currentSquish,
                breathY = breathOffset,
                isDark = isDark
            )

            // E. प्यार भरा दिल / स्पार्कल्स (टैप करने पर ऊपर उड़ेगा)
            if (heartPopProgress.value > 0f) {
                val hp = heartPopProgress.value
                val hy = gBase.y - 45.dp.toPx() - (hp * 35.dp.toPx())
                val hx = gBase.x + sin(hp * 4f) * 6.dp.toPx()
                val hAlpha = (1f - hp).coerceIn(0f, 1f)

                // नन्हा चमकता दिल
                drawCircle(
                    color = Color(0xFFF472B6).copy(alpha = hAlpha),
                    radius = 5.dp.toPx() * (1f - hp * 0.3f),
                    center = Offset(hx - 3.dp.toPx(), hy)
                )
                drawCircle(
                    color = Color(0xFFF472B6).copy(alpha = hAlpha),
                    radius = 5.dp.toPx() * (1f - hp * 0.3f),
                    center = Offset(hx + 3.dp.toPx(), hy)
                )
                val tri = Path().apply {
                    moveTo(hx - 7.dp.toPx(), hy + 1.dp.toPx())
                    lineTo(hx + 7.dp.toPx(), hy + 1.dp.toPx())
                    lineTo(hx, hy + 8.dp.toPx())
                    close()
                }
                drawPath(tri, color = Color(0xFFF472B6).copy(alpha = hAlpha))
            }
        }

        // 🌟 बेहद मिनिमल और खूबसूरत फ्लोटिंग हेडर
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg.copy(alpha = 0.88f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "✨", fontSize = 14.sp)
                        Text(
                            text = "SPIRIT SANCTUM",
                            color = textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Soul Fire Lv.1",
                            color = goldColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", color = textMuted, fontSize = 10.sp)
                        Text(
                            text = "${totalHours}h Focus",
                            color = textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 🦊 जादुई नन्हा गार्जियन (Procedural Spirit Drawing Engine)
// -----------------------------------------------------------------------------
private fun DrawScope.drawGuardianSpirit(
    base: Offset,
    squish: Float,
    breathY: Float,
    isDark: Boolean
) {
    // 1. ज़मीन पर परछाई
    val shadowW = 34.dp.toPx() * squish
    drawOval(
        color = if (isDark) Color(0x3A000000) else Color(0x1C000000),
        topLeft = Offset(base.x - shadowW / 2, base.y - 3.dp.toPx()),
        size = Size(shadowW, 10.dp.toPx())
    )

    val bodyCenter = Offset(base.x, base.y - (18.dp.toPx() * squish) + breathY)
    val bodyRadius = 18.dp.toPx() * squish

    // 2. गोल जादुई पूँछ (Fluffy Tail)
    val tailColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFF97316)
    drawOval(
        color = tailColor,
        topLeft = Offset(bodyCenter.x + 6.dp.toPx(), bodyCenter.y - 2.dp.toPx()),
        size = Size(16.dp.toPx() * squish, 12.dp.toPx() * squish)
    )
    drawCircle(
        color = Color(0xFFFEF08A),
        radius = 4.dp.toPx() * squish,
        center = Offset(bodyCenter.x + 20.dp.toPx(), bodyCenter.y + 4.dp.toPx())
    )

    // 3. गोल शरीर (Soft Cream / Warm Amber Body)
    val bodyColor = if (isDark) Color(0xFFFDE68A) else Color(0xFFFEF3C7)
    drawCircle(
        color = bodyColor,
        radius = bodyRadius,
        center = bodyCenter
    )

    // 4. नन्हे प्यारे कान (Cute Pointed Ears)
    val earPath = Path().apply {
        // बायाँ कान
        moveTo(bodyCenter.x - 12.dp.toPx(), bodyCenter.y - 10.dp.toPx())
        lineTo(bodyCenter.x - 8.dp.toPx(), bodyCenter.y - 24.dp.toPx() * squish)
        lineTo(bodyCenter.x - 2.dp.toPx(), bodyCenter.y - 12.dp.toPx())
        close()
        // दायाँ कान
        moveTo(bodyCenter.x + 2.dp.toPx(), bodyCenter.y - 12.dp.toPx())
        lineTo(bodyCenter.x + 8.dp.toPx(), bodyCenter.y - 24.dp.toPx() * squish)
        lineTo(bodyCenter.x + 12.dp.toPx(), bodyCenter.y - 10.dp.toPx())
        close()
    }
    drawPath(earPath, color = tailColor)

    // 5. प्यारी सोती/मुस्कुराती आँखें (^ ^ Eyes)
    val eyeColor = if (isDark) Color(0xFF78350F) else Color(0xFF92400E)
    val leftEye = Offset(bodyCenter.x - 6.dp.toPx(), bodyCenter.y - 2.dp.toPx())
    val rightEye = Offset(bodyCenter.x + 6.dp.toPx(), bodyCenter.y - 2.dp.toPx())

    drawArc(
        color = eyeColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(leftEye.x - 3.dp.toPx(), leftEye.y - 2.dp.toPx()),
        size = Size(6.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        color = eyeColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(rightEye.x - 3.dp.toPx(), rightEye.y - 2.dp.toPx()),
        size = Size(6.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // 6. गुलाबी गाल (Blushing Cheeks - Oxytocin warmth)
    val blushColor = Color(0xFFF472B6).copy(alpha = 0.55f)
    drawCircle(color = blushColor, radius = 2.4.dp.toPx(), center = Offset(bodyCenter.x - 9.dp.toPx(), bodyCenter.y + 3.dp.toPx()))
    drawCircle(color = blushColor, radius = 2.4.dp.toPx(), center = Offset(bodyCenter.x + 9.dp.toPx(), bodyCenter.y + 3.dp.toPx()))
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
