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

// 11 अनोखी प्रजातियाँ ताकि आप अपनी पसंद का पेड़ चुन सकें
enum class TreeSpecies(val title: String) {
    CLOUD_OAK("Oak"),
    LAYERED_PINE("Pine"),
    SAKURA_PINK("Sakura"),
    AUTUMN_MAPLE("Maple"),
    WEEPING_WILLOW("Willow"),
    TALL_CYPRESS("Cypress"),
    ZEN_BONSAI("Bonsai"),
    PALM_COCONUT("Palm"),
    SILVER_BIRCH("Birch"),
    FANTASY_SHROOM("Shroom"),
    GOLDEN_AMON("Amon Gold")
}

data class PlacedTree(
    val id: String,
    val subject: String,
    val durationMinutes: Int,
    val species: TreeSpecies,
    val gridSlot: Int
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
    val allSpecies = remember { TreeSpecies.values() }

    // 11 के 11 पेड़ स्क्रीन पर सीधे प्रीव्यू के लिए दिखाए जा रहे हैं
    val trees = remember(sessions) {
        if (sessions.isEmpty()) {
            allSpecies.mapIndexed { index, spec ->
                PlacedTree(
                    id = "preview_$index",
                    subject = spec.title,
                    durationMinutes = 25 + index * 5,
                    species = spec,
                    gridSlot = index
                )
            }
        } else {
            sessions.mapIndexed { index, s ->
                val chosenSpecies = allSpecies[index % allSpecies.size]
                PlacedTree(
                    id = "tree_$index",
                    subject = s.subject,
                    durationMinutes = s.durationMinutes,
                    species = chosenSpecies,
                    gridSlot = index
                )
            }
        }
    }

    val totalMins = remember(sessions, trees) {
        if (sessions.isNotEmpty()) sessions.sumOf { it.durationMinutes }
        else trees.sumOf { it.durationMinutes }
    }
    val totalHours = String.format(Locale.getDefault(), "%.1f", totalMins / 60f)

    val infiniteTransition = rememberInfiniteTransition(label = "nature_anim")

    // हवा में झूलना
    val windPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wind"
    )

    // तालाब की लहरें
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    // जुगनू
    val fireflyGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val coroutineScope = rememberCoroutineScope()
    val squishAnimatables = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    val meadowBg = if (isDark) Color(0xFF0D1410) else Color(0xFFEDF6EC)
    val meadowPathColor = if (isDark) Color(0xFF16211B) else Color(0xFFE2EFE0)
    val pondWaterColor = if (isDark) Color(0xFF162F3D) else Color(0xFFC7E8F3)
    val pondDeepColor = if (isDark) Color(0xFF10232E) else Color(0xFFB1DFEE)
    val rippleColor = if (isDark) Color(0x55A5E3F6) else Color(0x66FFFFFF)

    // कुल पेड़ों के हिसाब से डायनामिक स्क्रोल ऊँचाई
    val rowCount = ceil(trees.size / 2.0).toInt()
    val canvasHeightDp = (260 + rowCount * 135).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(meadowBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeightDp)
                    .pointerInput(trees) {
                        detectTapGestures { tapOffset ->
                            val w = size.width
                            val stepY = 130.dp.toPx()
                            val startY = 160.dp.toPx()

                            trees.forEachIndexed { index, _ ->
                                val isLeft = index % 2 == 0
                                val treeX = if (isLeft) w * 0.28f else w * 0.72f
                                val treeY = startY + (index / 2) * stepY

                                val dist = hypot(tapOffset.x - treeX, tapOffset.y - treeY)
                                if (dist < 42.dp.toPx()) {
                                    val anim = squishAnimatables.getOrPut(index) { Animatable(1f) }
                                    coroutineScope.launch {
                                        anim.snapTo(0.82f)
                                        anim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // प्राकृतिक घुमावदार रास्ता (पेड़ों के करीब)
                val trailPath = Path().apply {
                    moveTo(w * 0.50f, 0f)
                    cubicTo(w * 0.46f, h * 0.22f, w * 0.54f, h * 0.45f, w * 0.48f, h * 0.68f)
                    cubicTo(w * 0.44f, h * 0.82f, w * 0.52f, h * 0.94f, w * 0.50f, h)
                }
                drawPath(
                    path = trailPath,
                    color = meadowPathColor,
                    style = Stroke(width = 46.dp.toPx(), cap = StrokeCap.Round)
                )

                // रास्ते के पत्थर
                val pebbleColor = if (isDark) Color(0x2AFFFFFF) else Color(0x3B8DA588)
                for (step in 0..16) {
                    val py = (h / 16f) * step
                    val px = (w * 0.50f) + sin(step.toDouble() * 1.4).toFloat() * 18.dp.toPx()
                    drawCircle(color = pebbleColor, radius = 2.2.dp.toPx(), center = Offset(px, py))
                }

                // शांत तालाब
                val pondCenter = Offset(w * 0.20f, 100.dp.toPx())
                val pondRadiusX = 52.dp.toPx()
                val pondRadiusY = 38.dp.toPx()

                drawOval(
                    color = pondWaterColor,
                    topLeft = Offset(pondCenter.x - pondRadiusX, pondCenter.y - pondRadiusY),
                    size = Size(pondRadiusX * 2, pondRadiusY * 2)
                )
                drawOval(
                    color = pondDeepColor,
                    topLeft = Offset(pondCenter.x - pondRadiusX * 0.65f, pondCenter.y - pondRadiusY * 0.65f),
                    size = Size(pondRadiusX * 1.3f, pondRadiusY * 1.3f)
                )

                val wave1R = pondRadiusX * 0.3f + (pondRadiusX * 0.65f * rippleProgress)
                val waveAlpha = (1f - rippleProgress).coerceIn(0f, 1f)
                drawOval(
                    color = rippleColor.copy(alpha = waveAlpha * 0.6f),
                    topLeft = Offset(pondCenter.x - wave1R, pondCenter.y - wave1R * 0.72f),
                    size = Size(wave1R * 2, wave1R * 1.44f),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                val stepY = 130.dp.toPx()
                val startY = 160.dp.toPx()

                // पेड़ों का ड्रॉइंग लूप
                trees.forEachIndexed { index, tree ->
                    val isLeft = index % 2 == 0
                    // रास्ते के किनारों के बिल्कुल पास
                    val baseX = if (isLeft) w * 0.28f else w * 0.72f
                    val baseY = startY + (index / 2) * stepY

                    val swayAngle = sin(windPhase + index * 1.1f) * 3f
                    val swayOffset = Offset(sin(Math.toRadians(swayAngle.toDouble())).toFloat() * 9.dp.toPx(), 0f)
                    val squish = squishAnimatables[index]?.value ?: 1f

                    drawAdvancedTree(
                        species = tree.species,
                        base = Offset(baseX, baseY),
                        sway = swayOffset,
                        squish = squish,
                        isDark = isDark
                    )
                }

                // रात के जुगनू
                if (isDark) {
                    val fireflyColor = Color(0xFFFDE047)
                    val pts = listOf(
                        Offset(w * 0.35f, 110.dp.toPx()),
                        Offset(w * 0.65f, 180.dp.toPx()),
                        Offset(w * 0.22f, 320.dp.toPx()),
                        Offset(w * 0.78f, 440.dp.toPx()),
                        Offset(w * 0.40f, 560.dp.toPx())
                    )
                    pts.forEachIndexed { i, pt ->
                        val fx = pt.x + sin(windPhase * 0.9f + i) * 10.dp.toPx()
                        val fy = pt.y + cos(windPhase * 0.9f + i) * 8.dp.toPx()
                        val g = (fireflyGlow + sin(i.toFloat()).absoluteValue * 0.2f).coerceIn(0.2f, 1f)
                        drawCircle(color = fireflyColor.copy(alpha = g * 0.25f), radius = 5.dp.toPx(), center = Offset(fx, fy))
                        drawCircle(color = fireflyColor.copy(alpha = g), radius = 1.8.dp.toPx(), center = Offset(fx, fy))
                    }
                }
            }
        }

        // 🌟 बेहद मिनिमल और खूबसूरत फ्लोटिंग हेडर (No Big Box)
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
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🌿", fontSize = 14.sp)
                        Text(
                            text = "AMON PARK",
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
                            text = "${trees.size} Trees",
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
// 🌳 11 प्रोसीजरल पेड़ों का मास्टर इंजन (Zero Lollipops!)
// -----------------------------------------------------------------------------
private fun DrawScope.drawAdvancedTree(
    species: TreeSpecies,
    base: Offset,
    sway: Offset,
    squish: Float,
    isDark: Boolean
) {
    val trunkBrown = if (isDark) Color(0xFF423126) else Color(0xFF5D4037)
    val shadowColor = if (isDark) Color(0x33000000) else Color(0x18000000)

    // 1. ज़मीन पर परछाई
    drawOval(
        color = shadowColor,
        topLeft = Offset(base.x - 18.dp.toPx(), base.y - 4.dp.toPx()),
        size = Size(36.dp.toPx(), 10.dp.toPx())
    )

    when (species) {
        // 1. CLOUD OAK (फूला हुआ बादल ओक)
        TreeSpecies.CLOUD_OAK -> {
            val h = 18.dp.toPx() * squish
            drawRoundRect(color = trunkBrown, topLeft = Offset(base.x - 3.dp.toPx(), base.y - h), size = Size(6.dp.toPx(), h), cornerRadius = CornerRadius(2.dp.toPx()))
            val c = Offset(base.x + sway.x, base.y - h + sway.y - 12.dp.toPx())
            val darkGreen = if (isDark) Color(0xFF1B4D2E) else Color(0xFF22C55E)
            val lightGreen = if (isDark) Color(0xFF2E6F40) else Color(0xFF4ADE80)
            val r = 16.dp.toPx() * squish

            drawCircle(darkGreen, r, Offset(c.x - 7.dp.toPx(), c.y + 2.dp.toPx()))
            drawCircle(darkGreen, r, Offset(c.x + 7.dp.toPx(), c.y + 2.dp.toPx()))
            drawCircle(lightGreen, r * 1.15f, Offset(c.x, c.y - 4.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.2f), r * 0.4f, Offset(c.x - 4.dp.toPx(), c.y - 8.dp.toPx()))
        }

        // 2. LAYERED PINE (तिकोना देवदार)
        TreeSpecies.LAYERED_PINE -> {
            val h = 20.dp.toPx() * squish
            drawRect(color = trunkBrown, topLeft = Offset(base.x - 2.5.dp.toPx(), base.y - h), size = Size(5.dp.toPx(), h))
            val c = Offset(base.x + sway.x, base.y - h + sway.y)
            val pineColor = if (isDark) Color(0xFF133E31) else Color(0xFF059669)

            for (i in 0..2) {
                val y = c.y - (i * 9.dp.toPx())
                val w = (16.dp.toPx() - i * 3.dp.toPx()) * squish
                val tri = Path().apply {
                    moveTo(c.x, y - 12.dp.toPx())
                    lineTo(c.x + w, y + 2.dp.toPx())
                    lineTo(c.x - w, y + 2.dp.toPx())
                    close()
                }
                drawPath(tri, pineColor)
            }
        }

        // 3. SAKURA PINK (गुलाबी जापानी सकुरा)
        TreeSpecies.SAKURA_PINK -> {
            val h = 18.dp.toPx() * squish
            drawRoundRect(color = trunkBrown, topLeft = Offset(base.x - 3.dp.toPx(), base.y - h), size = Size(6.dp.toPx(), h), cornerRadius = CornerRadius(2.dp.toPx()))
            val c = Offset(base.x + sway.x, base.y - h + sway.y - 12.dp.toPx())
            val deepPink = if (isDark) Color(0xFF9F2B55) else Color(0xFFEC4899)
            val softPink = if (isDark) Color(0xFFBC4B75) else Color(0xFFF472B6)
            val r = 15.dp.toPx() * squish

            drawCircle(deepPink, r, Offset(c.x - 6.dp.toPx(), c.y + 2.dp.toPx()))
            drawCircle(deepPink, r, Offset(c.x + 6.dp.toPx(), c.y + 2.dp.toPx()))
            drawCircle(softPink, r * 1.1f, Offset(c.x, c.y - 4.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.4f), 3.dp.toPx(), Offset(c.x + 4.dp.toPx(), c.y - 6.dp.toPx()))
        }

        // 4. AUTUMN MAPLE (नारंगी मेपल)
        TreeSpecies.AUTUMN_MAPLE -> {
            val h = 18.dp.toPx() * squish
            drawRoundRect(color = trunkBrown, topLeft = Offset(base.x - 3.dp.toPx(), base.y - h), size = Size(6.dp.toPx(), h), cornerRadius = CornerRadius(2.dp.toPx()))
            val c = Offset(base.x + sway.x, base.y - h + sway.y - 12.dp.toPx())
            val deepAmber = if (isDark) Color(0xFF9A3412) else Color(0xFFEA580C)
            val brightOrange = if (isDark) Color(0xFFC2410C) else Color(0xFFF97316)
            val r = 16.dp.toPx() * squish

            drawCircle(deepAmber, r, Offset(c.x - 6.dp.toPx(), c.y + 3.dp.toPx()))
            drawCircle(deepAmber, r, Offset(c.x + 6.dp.toPx(), c.y + 3.dp.toPx()))
            drawCircle(brightOrange, r * 1.1f, Offset(c.x, c.y - 4.dp.toPx()))
        }

        // 5. WEEPING WILLOW (झुकती हुई लतादार विलो)
        TreeSpecies.WEEPING_WILLOW -> {
            val h = 22.dp.toPx() * squish
            drawRoundRect(color = trunkBrown, topLeft = Offset(base.x - 3.dp.toPx(), base.y - h), size = Size(6.dp.toPx(), h), cornerRadius = CornerRadius(2.dp.toPx()))
            val c = Offset(base.x + sway.x, base.y - h + sway.y - 10.dp.toPx())
            val willowGreen = if (isDark) Color(0xFF1F513F) else Color(0xFF10B981)

            drawOval(willowGreen, Offset(c.x - 16.dp.toPx(), c.y - 10.dp.toPx()), Size(32.dp.toPx(), 20.dp.toPx()))
            // लटकती पत्तियाँ
            for (dx in listOf(-12.dp, -6.dp, 0.dp, 6.dp, 12.dp)) {
                drawLine(
                    color = willowGreen,
                    start = Offset(c.x + dx.toPx(), c.y),
                    end = Offset(c.x + dx.toPx() + (sway.x * 0.5f), c.y + 14.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 6. TALL CYPRESS (लंबा पतला साइप्रस)
        TreeSpecies.TALL_CYPRESS -> {
            val h = 12.dp.toPx() * squish
            drawRect(color = trunkBrown, topLeft = Offset(base.x - 2.dp.toPx(), base.y - h), size = Size(4.dp.toPx(), h))
            val c = Offset(base.x + sway.x, base.y - h + sway.y - 18.dp.toPx())
            val cypressColor = if (isDark) Color(0xFF0F3824) else Color(0xFF15803D)
            drawOval(cypressColor, Offset(c.x - 8.dp.toPx() * squish, c.y - 14.dp.toPx()), Size(16.dp.toPx() * squish, 36.dp.toPx()))
        }

        // 7. ZEN BONSAI (घुमावदार बोंसाई)
        TreeSpecies.ZEN_BONSAI -> {
            val trunkPath = Path().apply {
                moveTo(base.x, base.y)
                cubicTo(base.x - 6.dp.toPx(), base.y - 8.dp.toPx(), base.x + 8.dp.toPx(), base.y - 14.dp.toPx(), base.x + sway.x, base.y - 20.dp.toPx() * squish)
            }
            drawPath(trunkPath, trunkBrown, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
            val c = Offset(base.x + sway.x, base.y - 20.dp.toPx() * squish + sway.y)
            val bonsaiGreen = if (isDark) Color(0xFF1E4E2B) else Color(0xFF16A34A)

            drawOval(bonsaiGreen, Offset(c.x - 14.dp.toPx(), c.y - 6.dp.toPx()), Size(28.dp.toPx() * squish, 12.dp.toPx()))
            drawOval(bonsaiGreen, Offset(c.x - 20.dp.toPx(), c.y + 4.dp.toPx()), Size(16.dp.toPx() * squish, 8.dp.toPx()))
        }

        // 8. PALM COCONUT (नारियल का पेड़)
        TreeSpecies.PALM_COCONUT -> {
            val trunkPath = Path().apply {
                moveTo(base.x, base.y)
                quadraticBezierTo(base.x + 8.dp.toPx(), base.y - 14.dp.toPx(), base.x + 4.dp.toPx() + sway.x, base.y - 26.dp.toPx() * squish)
            }
            drawPath(trunkPath, trunkBrown, style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round))
            val tip = Offset(base.x + 4.dp.toPx() + sway.x, base.y - 26.dp.toPx() * squish)
            val palmGreen = if (isDark) Color(0xFF1B5E20) else Color(0xFF22C55E)

            // 4 झुके हुए पत्ते
            for (ang in listOf(-50.0, -15.0, 30.0, 65.0)) {
                val rad = Math.toRadians(ang)
                val ex = tip.x + (cos(rad) * 16.dp.toPx()).toFloat()
                val ey = tip.y + (sin(rad) * 12.dp.toPx()).toFloat()
                drawLine(palmGreen, tip, Offset(ex, ey), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }

        // 9. SILVER BIRCH (सफ़ेद बर्च)
        TreeSpecies.SILVER_BIRCH -> {
            val h = 24.dp.toPx() * squish
            val whiteTrunk = Color(0xFFE2E8F0)
            drawRoundRect(whiteTrunk, Offset(base.x - 2.5.dp.toPx(), base.y - h), Size(5.dp.toPx(), h), cornerRadius = CornerRadius(2.dp.toPx()))
            // काले निशान
            drawRect(Color(0xFF334155), Offset(base.x - 2.dp.toPx(), base.y - h * 0.4f), Size(4.dp.toPx(), 2.dp.toPx()))
            drawRect(Color(0xFF334155), Offset(base.x - 2.dp.toPx(), base.y - h * 0.7f), Size(4.dp.toPx(), 2.dp.toPx()))

            val c = Offset(base.x + sway.x, base.y - h + sway.y - 10.dp.toPx())
            val limeGreen = if (isDark) Color(0xFF3F6212) else Color(0xFF84CC16)
            drawCircle(limeGreen, 13.dp.toPx() * squish, c)
        }

        // 10. FANTASY SHROOM (जादुई मशरूम ट्री)
        TreeSpecies.FANTASY_SHROOM -> {
            val h = 16.dp.toPx() * squish
            val stemColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFFF1F5F9)
            drawRoundRect(stemColor, Offset(base.x - 3.5.dp.toPx(), base.y - h), Size(7.dp.toPx(), h), cornerRadius = CornerRadius(3.dp.toPx()))

            val c = Offset(base.x + sway.x, base.y - h + sway.y - 8.dp.toPx())
            val capColor = if (isDark) Color(0xFF6B21A8) else Color(0xFF8B5CF6)
            drawOval(capColor, Offset(c.x - 16.dp.toPx() * squish, c.y - 10.dp.toPx()), Size(32.dp.toPx() * squish, 18.dp.toPx()))
            // सफ़ेद जादुई बिंदु
            drawCircle(Color.White, 2.dp.toPx(), Offset(c.x - 6.dp.toPx(), c.y - 4.dp.toPx()))
            drawCircle(Color.White, 2.dp.toPx(), Offset(c.x + 6.dp.toPx(), c.y - 4.dp.toPx()))
        }

        // 11. GOLDEN AMON (अमोन का शाही सुनहरा पेड़)
        TreeSpecies.GOLDEN_AMON -> {
            val h = 20.dp.toPx() * squish
            drawRoundRect(trunkBrown, Offset(base.x - 3.dp.toPx(), base.y - h), Size(6.dp.toPx(), h), cornerRadius = CornerRadius(2.dp.toPx()))
            val c = Offset(base.x + sway.x, base.y - h + sway.y - 14.dp.toPx())
            val r = 18.dp.toPx() * squish

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF08A), Color(0xFFEAB308), Color(0xFFCA8A04)),
                    center = c,
                    radius = r
                ),
                radius = r,
                center = c
            )
            drawCircle(Color.White.copy(alpha = 0.6f), 3.5.dp.toPx(), Offset(c.x - 5.dp.toPx(), c.y - 5.dp.toPx()))
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
