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

enum class SpiritType {
    ASTRAL_FOX,    // आग के पास बैठी लोमड़ी
    SCHOLAR_OWL,   // किताब पढ़ता प्यारा उल्लू
    SLEEPY_BADGER, // पत्तों पर सोता हुआ नन्हा भालू
    JELLY_PUFF     // तालाब के पास फुदकने वाली जेली
}

data class ActiveSpirit(
    val id: String,
    val type: SpiritType,
    val name: String,
    val relativeX: Float,
    val relativeY: Float
)

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
    val totalHoursNum = totalMins / 60f
    val totalHours = String.format(Locale.getDefault(), "%.1f", totalHoursNum)

    // कुल फोकस के हिसाब से आग का लेवल तय होता है
    val fireLevel = when {
        totalHoursNum >= 10f -> 3
        totalHoursNum >= 3f -> 2
        else -> 1
    }

    // अभयारण्य के निवासी (Companions)
    val activeSpirits = remember {
        listOf(
            ActiveSpirit("s1", SpiritType.ASTRAL_FOX, "Kitsu", 0.65f, 0.54f),
            ActiveSpirit("s2", SpiritType.SCHOLAR_OWL, "Hoot", 0.22f, 0.38f),
            ActiveSpirit("s3", SpiritType.SLEEPY_BADGER, "Pebble", 0.76f, 0.72f),
            ActiveSpirit("s4", SpiritType.JELLY_PUFF, "Boba", 0.28f, 0.68f)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sanctuary_physics")

    // 1. साँस लेने की गति
    val breathePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath"
    )

    // 2. आग की लौ की गति
    val firePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_anim"
    )

    // 3. हवा में तैरती चिंगारियाँ
    val emberProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "embers_flow"
    )

    // 4. तालाब की लहरें
    val pondRipple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pond_wave"
    )

    // टच रिस्पॉन्स के लिए एनिमेटेबल्स
    val coroutineScope = rememberCoroutineScope()
    val spiritSquish = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val spiritPopHeart = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

    val sanctuaryBg = if (isDark) Color(0xFF09120E) else Color(0xFFEBF5EB)
    val hillColor = if (isDark) Color(0xFF11211A) else Color(0xFFDCEFDC)
    val pathPebbleColor = if (isDark) Color(0xFF1A3026) else Color(0xFFCDE4CD)
    val pondWater = if (isDark) Color(0xFF142B38) else Color(0xFFBCE3F0)
    val pondWaterDeep = if (isDark) Color(0xFF0D1E27) else Color(0xFFA5D6E7)

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

                        activeSpirits.forEach { sp ->
                            val sx = w * sp.relativeX
                            val sy = h * sp.relativeY
                            val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)

                            if (dist < 46.dp.toPx()) {
                                val sqAnim = spiritSquish.getOrPut(sp.id) { Animatable(1f) }
                                val heartAnim = spiritPopHeart.getOrPut(sp.id) { Animatable(0f) }

                                coroutineScope.launch {
                                    sqAnim.snapTo(0.74f)
                                    launch {
                                        sqAnim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    heartAnim.snapTo(0.01f)
                                    heartAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(850, easing = FastOutSlowInEasing)
                                    )
                                    heartAnim.snapTo(0f)
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // A. शांत घुमावदार पहाड़ी घाटी (Sanctuary Hills)
            val hillPath = Path().apply {
                moveTo(0f, h * 0.32f)
                cubicTo(w * 0.30f, h * 0.28f, w * 0.70f, h * 0.36f, w, h * 0.30f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath, hillColor)

            // B. सुखद छोटा रास्ता
            val meadowTrail = Path().apply {
                moveTo(w * 0.15f, h * 0.34f)
                cubicTo(w * 0.35f, h * 0.44f, w * 0.45f, h * 0.58f, w * 0.85f, h * 0.78f)
            }
            drawPath(
                path = meadowTrail,
                color = pathPebbleColor,
                style = Stroke(width = 38.dp.toPx(), cap = StrokeCap.Round)
            )

            // C. नन्हा ज़ेन तालाब (The Zen Pond)
            val pCenter = Offset(w * 0.24f, h * 0.68f)
            val prX = 54.dp.toPx()
            val prY = 36.dp.toPx()

            drawOval(pondWater, Offset(pCenter.x - prX, pCenter.y - prY), Size(prX * 2, prY * 2))
            drawOval(pondWaterDeep, Offset(pCenter.x - prX * 0.6f, pCenter.y - prY * 0.6f), Size(prX * 1.2f, prY * 1.2f))

            // तालाब की गोल लहर
            val waveR = prX * 0.3f + (prX * 0.65f * pondRipple)
            val waveAlpha = (1f - pondRipple).coerceIn(0f, 1f)
            drawOval(
                color = Color.White.copy(alpha = waveAlpha * 0.45f),
                topLeft = Offset(pCenter.x - waveR, pCenter.y - waveR * 0.65f),
                size = Size(waveR * 2, waveR * 1.3f),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // D. केंद्र में अपग्रेडेड सोल हर्थ (The Great Campfire)
            val altarCenter = Offset(w * 0.48f, h * 0.50f)
            drawCampfire(
                center = altarCenter,
                level = fireLevel,
                pulse = firePulse,
                emberProg = emberProgress,
                isDark = isDark
            )

            // E. सभी 4 जादुई जीवों का सजीव चित्रण (Living Companions)
            activeSpirits.forEach { sp ->
                val sx = w * sp.relativeX
                val sy = h * sp.relativeY
                val squishVal = spiritSquish[sp.id]?.value ?: 1f
                val breathY = sin(breathePhase + sp.relativeX * 5f) * 2.2.dp.toPx()

                when (sp.type) {
                    SpiritType.ASTRAL_FOX -> {
                        drawAstralFox(Offset(sx, sy), squishVal, breathY, isDark)
                    }
                    SpiritType.SCHOLAR_OWL -> {
                        drawScholarOwl(Offset(sx, sy), squishVal, breathY, isDark)
                    }
                    SpiritType.SLEEPY_BADGER -> {
                        drawSleepyBadger(Offset(sx, sy), squishVal, breathY, isDark)
                    }
                    SpiritType.JELLY_PUFF -> {
                        drawJellyPuff(Offset(sx, sy), squishVal, breathY, isDark)
                    }
                }

                // छूने पर प्यार भरा दिल या सितारे का उड़ना
                val popProg = spiritPopHeart[sp.id]?.value ?: 0f
                if (popProg > 0f) {
                    drawFloatingLove(Offset(sx, sy - 34.dp.toPx()), popProg)
                }
            }
        }

        // 🌟 बेहद मिनिमल फ्लोटिंग हेडर
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg.copy(alpha = 0.90f),
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
                        Text(text = "🏕️", fontSize = 14.sp)
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
                            text = "Hearth Lv.$fireLevel",
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
// 🔥 अपग्रेडेड कैम्पफ़ायर इंजन (Campfire / Hearth)
// -----------------------------------------------------------------------------
private fun DrawScope.drawCampfire(
    center: Offset,
    level: Int,
    pulse: Float,
    emberProg: Float,
    isDark: Boolean
) {
    // 1. ज़मीन पर गर्म रोशनी (Glow Aura)
    val auraR = (50.dp.toPx() + level * 12.dp.toPx()) * pulse
    val auraColor = if (level >= 3) Color(0xFFFBBF24) else Color(0xFFF59E0B)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(auraColor.copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = auraR
        ),
        radius = auraR,
        center = center
    )

    // 2. पत्थर का घेरा (Stone Circle)
    val stoneCount = 6 + level * 2
    for (i in 0 until stoneCount) {
        val ang = (i.toFloat() / stoneCount) * 2f * PI.toFloat()
        val dist = 24.dp.toPx() + (level * 4.dp.toPx())
        val stoneX = center.x + cos(ang) * dist
        val stoneY = center.y + sin(ang) * (dist * 0.55f)
        drawOval(
            color = if (isDark) Color(0xFF334155) else Color(0xFF94A3B8),
            topLeft = Offset(stoneX - 5.dp.toPx(), stoneY - 3.dp.toPx()),
            size = Size(10.dp.toPx(), 6.dp.toPx())
        )
    }

    // 3. लकड़ी के लट्ठे (Crossed Fire Logs)
    val woodColor = Color(0xFF5D4037)
    drawLine(woodColor, Offset(center.x - 14.dp.toPx(), center.y + 6.dp.toPx()), Offset(center.x + 14.dp.toPx(), center.y - 2.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(woodColor, Offset(center.x - 12.dp.toPx(), center.y - 3.dp.toPx()), Offset(center.x + 12.dp.toPx(), center.y + 7.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)

    // 4. आग की लौ (Animated Flame)
    val flameH = (28.dp.toPx() + level * 8.dp.toPx()) * pulse
    val flamePath = Path().apply {
        moveTo(center.x, center.y - flameH)
        cubicTo(center.x + 14.dp.toPx(), center.y - flameH * 0.4f, center.x + 10.dp.toPx(), center.y, center.x, center.y)
        cubicTo(center.x - 10.dp.toPx(), center.y, center.x - 14.dp.toPx(), center.y - flameH * 0.4f, center.x, center.y - flameH)
        close()
    }
    drawPath(flamePath, color = Color(0xFFEA580C))

    val innerFlame = Path().apply {
        val ih = flameH * 0.65f
        moveTo(center.x, center.y - ih)
        cubicTo(center.x + 8.dp.toPx(), center.y - ih * 0.35f, center.x + 5.dp.toPx(), center.y, center.x, center.y)
        cubicTo(center.x - 5.dp.toPx(), center.y, center.x - 8.dp.toPx(), center.y - ih * 0.35f, center.x, center.y - ih)
        close()
    }
    drawPath(innerFlame, color = Color(0xFFFDE047))

    // 5. ऊपर उड़ती चिंगारियाँ (Embers)
    for (i in 0..4) {
        val p = (emberProg + i * 0.22f) % 1f
        val ey = center.y - (p * (50.dp.toPx() + level * 14.dp.toPx()))
        val ex = center.x + sin(p * 2f * PI.toFloat() + i) * 12.dp.toPx()
        val alpha = (1f - p).coerceIn(0f, 1f)
        drawCircle(Color(0xFFFEF08A).copy(alpha = alpha), radius = 2.dp.toPx(), center = Offset(ex, ey))
    }
}

// -----------------------------------------------------------------------------
// 🦊 1. द एस्ट्रल फ़ॉक्स (Astral Fox)
// -----------------------------------------------------------------------------
private fun DrawScope.drawAstralFox(base: Offset, squish: Float, breathY: Float, isDark: Boolean) {
    val bCenter = Offset(base.x, base.y - 15.dp.toPx() * squish + breathY)
    val r = 16.dp.toPx() * squish

    // परछाई
    drawOval(Color(0x22000000), Offset(base.x - 18.dp.toPx(), base.y - 3.dp.toPx()), Size(36.dp.toPx(), 9.dp.toPx()))

    // पूँछ
    drawOval(Color(0xFFF97316), Offset(bCenter.x + 5.dp.toPx(), bCenter.y - 2.dp.toPx()), Size(15.dp.toPx() * squish, 11.dp.toPx() * squish))
    drawCircle(Color(0xFFFEF3C7), 4.dp.toPx() * squish, Offset(bCenter.x + 18.dp.toPx(), bCenter.y + 4.dp.toPx()))

    // शरीर
    drawCircle(Color(0xFFFED7AA), r, bCenter)

    // कान
    val ear = Path().apply {
        moveTo(bCenter.x - 10.dp.toPx(), bCenter.y - 8.dp.toPx())
        lineTo(bCenter.x - 7.dp.toPx(), bCenter.y - 20.dp.toPx() * squish)
        lineTo(bCenter.x - 2.dp.toPx(), bCenter.y - 10.dp.toPx())
        close()
        moveTo(bCenter.x + 2.dp.toPx(), bCenter.y - 10.dp.toPx())
        lineTo(bCenter.x + 7.dp.toPx(), bCenter.y - 20.dp.toPx() * squish)
        lineTo(bCenter.x + 10.dp.toPx(), bCenter.y - 8.dp.toPx())
        close()
    }
    drawPath(ear, Color(0xFFEA580C))

    // आँखें (^ ^)
    val eyeCol = Color(0xFF7C2D12)
    drawArc(eyeCol, 180f, 180f, false, Offset(bCenter.x - 7.dp.toPx(), bCenter.y - 2.dp.toPx()), Size(5.dp.toPx(), 4.dp.toPx()), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    drawArc(eyeCol, 180f, 180f, false, Offset(bCenter.x + 2.dp.toPx(), bCenter.y - 2.dp.toPx()), Size(5.dp.toPx(), 4.dp.toPx()), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    // गाल
    drawCircle(Color(0xFFF472B6).copy(alpha = 0.6f), 2.dp.toPx(), Offset(bCenter.x - 8.dp.toPx(), bCenter.y + 2.dp.toPx()))
    drawCircle(Color(0xFFF472B6).copy(alpha = 0.6f), 2.dp.toPx(), Offset(bCenter.x + 8.dp.toPx(), bCenter.y + 2.dp.toPx()))
}

// -----------------------------------------------------------------------------
// 🦉 2. द स्कॉलर आउल (Scholar Owl with glowing book)
// -----------------------------------------------------------------------------
private fun DrawScope.drawScholarOwl(base: Offset, squish: Float, breathY: Float, isDark: Boolean) {
    val bCenter = Offset(base.x, base.y - 16.dp.toPx() * squish + breathY)
    val r = 15.dp.toPx() * squish

    // लकड़ी का लट्ठा जिसपर उल्लू बैठा है
    drawRoundRect(Color(0xFF5D4037), Offset(base.x - 18.dp.toPx(), base.y - 5.dp.toPx()), Size(36.dp.toPx(), 10.dp.toPx()), CornerRadius(4.dp.toPx()))

    // शरीर
    drawOval(Color(0xFF64748B), Offset(bCenter.x - r, bCenter.y - r * 1.1f), Size(r * 2, r * 2.2f))
    drawOval(Color(0xFFE2E8F0), Offset(bCenter.x - r * 0.65f, bCenter.y - r * 0.5f), Size(r * 1.3f, r * 1.4f))

    // बड़ी गोल चश्मे जैसी आँखें
    val eyeR = 4.2.dp.toPx() * squish
    drawCircle(Color.White, eyeR, Offset(bCenter.x - 5.dp.toPx(), bCenter.y - 6.dp.toPx()))
    drawCircle(Color.White, eyeR, Offset(bCenter.x + 5.dp.toPx(), bCenter.y - 6.dp.toPx()))
    drawCircle(Color(0xFF0F172A), eyeR * 0.45f, Offset(bCenter.x - 5.dp.toPx(), bCenter.y - 6.dp.toPx()))
    drawCircle(Color(0xFF0F172A), eyeR * 0.45f, Offset(bCenter.x + 5.dp.toPx(), bCenter.y - 6.dp.toPx()))

    // चोंच
    val beak = Path().apply {
        moveTo(bCenter.x - 2.dp.toPx(), bCenter.y - 3.dp.toPx())
        lineTo(bCenter.x + 2.dp.toPx(), bCenter.y - 3.dp.toPx())
        lineTo(bCenter.x, bCenter.y + 1.dp.toPx())
        close()
    }
    drawPath(beak, Color(0xFFF59E0B))

    // नन्ही जादुई किताब (Tiny Book in paws)
    val bookCenter = Offset(bCenter.x, bCenter.y + 8.dp.toPx())
    drawRoundRect(Color(0xFF3B82F6), Offset(bookCenter.x - 8.dp.toPx(), bookCenter.y - 4.dp.toPx()), Size(16.dp.toPx(), 8.dp.toPx()), CornerRadius(1.5.dp.toPx()))
    drawRect(Color.White, Offset(bookCenter.x - 7.dp.toPx(), bookCenter.y - 3.dp.toPx()), Size(14.dp.toPx(), 6.dp.toPx()))
}

// -----------------------------------------------------------------------------
// 🦡 3. द स्लीपिंग बेजर (Sleeping Badger with Zzz)
// -----------------------------------------------------------------------------
private fun DrawScope.drawSleepyBadger(base: Offset, squish: Float, breathY: Float, isDark: Boolean) {
    val bCenter = Offset(base.x, base.y - 10.dp.toPx() * squish + breathY * 0.7f)

    // पत्तों का बिस्तर
    drawOval(Color(0xFFD97706), Offset(base.x - 20.dp.toPx(), base.y - 5.dp.toPx()), Size(40.dp.toPx(), 12.dp.toPx()))

    // मुड़ा हुआ गोल शरीर (Curled up body)
    drawOval(Color(0xFF78716C), Offset(bCenter.x - 16.dp.toPx(), bCenter.y - 10.dp.toPx()), Size(32.dp.toPx() * squish, 20.dp.toPx() * squish))
    drawOval(Color(0xFFE7E5E4), Offset(bCenter.x - 14.dp.toPx(), bCenter.y - 7.dp.toPx()), Size(18.dp.toPx() * squish, 14.dp.toPx() * squish))

    // सोती हुई आँख (- -)
    drawLine(Color(0xFF1C1917), Offset(bCenter.x - 10.dp.toPx(), bCenter.y), Offset(bCenter.x - 5.dp.toPx(), bCenter.y), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)

    // 'Zzz' तैरते हुए अक्षर
    val zAlpha = ((breathY / 2.2.dp.toPx() + 1f) * 0.5f).coerceIn(0.2f, 0.9f)
    drawCircle(Color.White.copy(alpha = zAlpha), 2.2.dp.toPx(), Offset(bCenter.x + 12.dp.toPx(), bCenter.y - 12.dp.toPx()))
    drawCircle(Color.White.copy(alpha = zAlpha * 0.7f), 1.6.dp.toPx(), Offset(bCenter.x + 16.dp.toPx(), bCenter.y - 18.dp.toPx()))
}

// -----------------------------------------------------------------------------
// 🫧 4. द जेली पफ़ (Bouncing Jelly Puff)
// -----------------------------------------------------------------------------
private fun DrawScope.drawJellyPuff(base: Offset, squish: Float, breathY: Float, isDark: Boolean) {
    val bCenter = Offset(base.x, base.y - 12.dp.toPx() * squish + breathY)
    val r = 13.dp.toPx() * squish

    // परछाई
    drawOval(Color(0x22000000), Offset(base.x - 14.dp.toPx(), base.y - 2.dp.toPx()), Size(28.dp.toPx(), 7.dp.toPx()))

    // जेली शरीर (Water droplet shape)
    drawCircle(Color(0xFF38BDF8), r, bCenter)
    drawCircle(Color(0xFFBAE6FD), r * 0.45f, Offset(bCenter.x - 3.dp.toPx(), bCenter.y - 4.dp.toPx()))

    // क्यूट आँखें (• •)
    drawCircle(Color(0xFF0369A1), 1.6.dp.toPx(), Offset(bCenter.x - 4.dp.toPx(), bCenter.y))
    drawCircle(Color(0xFF0369A1), 1.6.dp.toPx(), Offset(bCenter.x + 4.dp.toPx(), bCenter.y))
}

// -----------------------------------------------------------------------------
// 💖 टच करने पर प्यार भरा दिल / स्पार्कल्स (Floating Love Reaction)
// -----------------------------------------------------------------------------
private fun DrawScope.drawFloatingLove(center: Offset, progress: Float) {
    val hy = center.y - (progress * 30.dp.toPx())
    val hx = center.x + sin(progress * 5f) * 6.dp.toPx()
    val alpha = (1f - progress).coerceIn(0f, 1f)

    drawCircle(Color(0xFFF472B6).copy(alpha = alpha), 4.dp.toPx() * (1f - progress * 0.3f), Offset(hx - 2.5.dp.toPx(), hy))
    drawCircle(Color(0xFFF472B6).copy(alpha = alpha), 4.dp.toPx() * (1f - progress * 0.3f), Offset(hx + 2.5.dp.toPx(), hy))

    val tri = Path().apply {
        moveTo(hx - 6.dp.toPx(), hy + 1.dp.toPx())
        lineTo(hx + 6.dp.toPx(), hy + 1.dp.toPx())
        lineTo(hx, hy + 7.dp.toPx())
        close()
    }
    drawPath(tri, Color(0xFFF472B6).copy(alpha = alpha))
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
