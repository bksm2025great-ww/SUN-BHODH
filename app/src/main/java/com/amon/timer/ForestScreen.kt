package com.amon.timer

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

enum class TreeSpecies {
    CLASSIC_OAK,    // रोज़ाना के सामान्य फोकस के लिए
    GEOMETRIC_PINE, // कठिन और तकनीकी विषयों के लिए
    SAKURA_PINK,    // भाषा और रचनात्मक विषयों के लिए
    GOLDEN_AMON     // 60+ मिनट के मैराथन फोकस के लिए
}

data class PlacedTree(
    val id: String,
    val subject: String,
    val durationMinutes: Int,
    val species: TreeSpecies,
    val gridSlot: Int,
    val scaleFactor: Float = 1f
)

@Composable
fun ForestScreen() {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkTheme.value

    val goldColor = ThemeManager.getAccentColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val cardBg = ThemeManager.getCardColor()
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)

    // 1. पढ़ाई के असली सेशन्स लोड करें
    val sessions = remember { FocusSessionManager.getAllSessions(context) }

    // 2. सेशन्स को पेड़ों में बदलें (अगर कोई सेशन नहीं है तो डेमो के लिए 6 पेड़ दिखाएँ)
    val trees = remember(sessions) {
        if (sessions.isEmpty()) {
            listOf(
                PlacedTree("d1", "All", 25, TreeSpecies.CLASSIC_OAK, 0),
                PlacedTree("d2", "Math", 45, TreeSpecies.GEOMETRIC_PINE, 1),
                PlacedTree("d3", "English", 30, TreeSpecies.SAKURA_PINK, 2),
                PlacedTree("d4", "Mastery", 65, TreeSpecies.GOLDEN_AMON, 3),
                PlacedTree("d5", "Physics", 50, TreeSpecies.CLASSIC_OAK, 4),
                PlacedTree("d6", "History", 35, TreeSpecies.SAKURA_PINK, 5)
            )
        } else {
            sessions.mapIndexed { index, s ->
                val species = when {
                    s.durationMinutes >= 60 -> TreeSpecies.GOLDEN_AMON
                    s.subject.contains("Math", ignoreCase = true) || s.subject.contains("Sci", ignoreCase = true) -> TreeSpecies.GEOMETRIC_PINE
                    s.subject.contains("Eng", ignoreCase = true) || s.subject.contains("Read", ignoreCase = true) -> TreeSpecies.SAKURA_PINK
                    else -> TreeSpecies.CLASSIC_OAK
                }
                PlacedTree(
                 id = "tree_$index",
                    subject = s.subject,
                    durationMinutes = s.durationMinutes,
                    species = species,
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

    val infiniteTransition = rememberInfiniteTransition(label = "zen_nature_clocks")

    // A. हवा में झूलना (Sine wave wind sway)
    val windPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wind_sway"
    )

    // B. तालाब की गोल लहरें (Pond Ripple Pulse)
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pond_ripple"
    )

    // C. रात के जुगनू की चमक (Firefly Glow)
    val fireflyGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firefly_glow"
    )

    // D. टच करने पर पेड़ का उछलना (Tap Squish Physics State)
    val coroutineScope = rememberCoroutineScope()
    val squishAnimatables = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    val meadowBg = if (isDark) Color(0xFF0F1713) else Color(0xFFEFF7EE)
    val meadowPathColor = if (isDark) Color(0xFF19251E) else Color(0xFFE2EFE0)
    val pondWaterColor = if (isDark) Color(0xFF17303E) else Color(0xFFC7E8F3)
    val pondDeepColor = if (isDark) Color(0xFF11242F) else Color(0xFFB1DFEE)
    val rippleColor = if (isDark) Color(0x55A5E3F6) else Color(0x66FFFFFF)

    // कैनवास की डायनामिक ऊँचाई (पेड़ों की संख्या के अनुसार स्वतः बढ़ेगी)
    val totalSlots = maxOf(trees.size, 6)
    val canvasHeightDp = (580 + (totalSlots / 2) * 160).dp

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
                            val stepY = 160.dp.toPx()
                            val startY = 240.dp.toPx()

                            trees.forEachIndexed { index, _ ->
                                val isLeft = index % 2 == 0
                                val treeX = if (isLeft) w * 0.24f else w * 0.76f
                                val treeY = startY + (index / 2) * stepY

                                val dist = hypot(tapOffset.x - treeX, tapOffset.y - treeY)
                                if (dist < 46.dp.toPx()) {
                                    val anim = squishAnimatables.getOrPut(index) { Animatable(1f) }
                                    coroutineScope.launch {
                                        anim.snapTo(0.78f) // दबेगा
                                        anim.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) // उछलकर सेट होगा
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val trailPath = Path().apply {
                    moveTo(w * 0.50f, 0f)
                    cubicTo(w * 0.44f, h * 0.18f, w * 0.58f, h * 0.36f, w * 0.48f, h * 0.52f)
                    cubicTo(w * 0.38f, h * 0.68f, w * 0.56f, h * 0.84f, w * 0.50f, h)
                }
                drawPath(
                    path = trailPath,
                    color = meadowPathColor,
                    style = Stroke(width = 54.dp.toPx(), cap = StrokeCap.Round)
                )

                // रास्ते के किनारे छोटे कंकड़ (Pebbles)
                val pebbleColor = if (isDark) Color(0x33FFFFFF) else Color(0x448DA588)
                for (step in 0..12) {
                    val py = (h / 12f) * step
                    val px = (w * 0.50f) + sin(step.toDouble() * 1.5).toFloat() * 26.dp.toPx()
                    drawCircle(color = pebbleColor, radius = 2.4.dp.toPx(), center = Offset(px, py))
                }

                val pondCenter = Offset(w * 0.22f, 150.dp.toPx())
                val pondRadiusX = 58.dp.toPx()
                val pondRadiusY = 44.dp.toPx()

                // तालाब का बेस
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

                // तालाब की गोल लहरें (Pulsating Waves)
                val wave1R = pondRadiusX * 0.3f + (pondRadiusX * 0.65f * rippleProgress)
                val waveAlpha = (1f - rippleProgress).coerceIn(0f, 1f)
                drawOval(
                    color = rippleColor.copy(alpha = waveAlpha * 0.7f),
                    topLeft = Offset(pondCenter.x - wave1R, pondCenter.y - wave1R * 0.75f),
                    size = Size(wave1R * 2, wave1R * 1.5f),
                    style = Stroke(width = 2.dp.toPx())
                )

                val stepY = 160.dp.toPx()
                val startY = 240.dp.toPx()

                trees.forEachIndexed { index, tree ->
                    val isLeft = index % 2 == 0
                    val baseX = if (isLeft) w * 0.24f else w * 0.76f
                    val baseY = startY + (index / 2) * stepY

                    // हवा की चाल (अलग-अलग पेड़ों की स्वतंत्र चाल)
                    val swayAngle = sin(windPhase + index * 1.3f) * 3.5f
                    val swayOffset = Offset(sin(Math.toRadians(swayAngle.toDouble())).toFloat() * 12.dp.toPx(), 0f)

                    // टैप करने पर लचीला स्केल
                    val squish = squishAnimatables[index]?.value ?: 1f

                    drawZenTree(
                        species = tree.species,
                        base = Offset(baseX, baseY),
                        sway = swayOffset,
                        squish = squish,
                        isDark = isDark
                    )
                }

                if (isDark) {
                    val fireflyColor = Color(0xFFFDE047)
                    val positions = listOf(
                        Offset(w * 0.35f, 130.dp.toPx()),
                        Offset(w * 0.70f, 210.dp.toPx()),
                        Offset(w * 0.18f, 370.dp.toPx()),
                        Offset(w * 0.82f, 490.dp.toPx()),
                        Offset(w * 0.45f, 620.dp.toPx())
                    )

                    positions.forEachIndexed { i, pt ->
                        val floatX = pt.x + sin(windPhase * 0.8f + i) * 14.dp.toPx()
                        val floatY = pt.y + cos(windPhase * 0.8f + i) * 10.dp.toPx()
                        val glow = (fireflyGlow + sin(i.toFloat()).absoluteValue * 0.2f).coerceIn(0.2f, 1f)

                        drawCircle(
                            color = fireflyColor.copy(alpha = glow * 0.3f),
                            radius = 6.dp.toPx(),
                            center = Offset(floatX, floatY)
                        )
                        drawCircle(
                            color = fireflyColor.copy(alpha = glow),
                            radius = 2.dp.toPx(),
                            center = Offset(floatX, floatY)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg.copy(alpha = 0.92f))
                .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🌿", fontSize = 16.sp)
                        Text(
                            text = "ZEN PARKLAND",
                            color = textMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap trees to wiggle • Deep study meadow",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(
                        icon = "🌲",
                        count = "${trees.size}",
                        label = "Trees",
                        goldColor = goldColor,
                        textMain = textMain,
                        isDark = isDark
                    )
                    StatPill(
                        icon = "⏱️",
                        count = "${totalHours}h",
                        label = "Focus",
                        goldColor = goldColor,
                        textMain = textMain,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawZenTree(
    species: TreeSpecies,
    base: Offset,
    sway: Offset,
    squish: Float,
    isDark: Boolean
) {
    val trunkColor = if (isDark) Color(0xFF4A3728) else Color(0xFF6D4C41)
    val shadowColor = if (isDark) Color(0x33000000) else Color(0x18000000)

    // 1. ज़मीन पर परछाई (Ground Shadow)
    drawOval(
        color = shadowColor,
        topLeft = Offset(base.x - 22.dp.toPx(), base.y - 6.dp.toPx()),
        size = Size(44.dp.toPx(), 14.dp.toPx())
    )

    // 2. पेड़ का तना (Wood Trunk)
    val trunkHeight = 22.dp.toPx() * squish
    val trunkWidth = 6.dp.toPx()
    drawRoundRect(
        color = trunkColor,
        topLeft = Offset(base.x - trunkWidth / 2, base.y - trunkHeight),
        size = Size(trunkWidth, trunkHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // 3. ताज / पत्तियाँ (Foliage Canopy)
    val canopyCenter = Offset(base.x + sway.x, base.y - trunkHeight + sway.y - 14.dp.toPx())

    when (species) {
        TreeSpecies.CLASSIC_OAK -> {
            val mainGreen = if (isDark) Color(0xFF2E6F40) else Color(0xFF4ADE80)
            val shadowGreen = if (isDark) Color(0xFF1E4E2C) else Color(0xFF22C55E)
            val r = 24.dp.toPx() * squish

            drawCircle(color = shadowGreen, radius = r * 0.95f, center = Offset(canopyCenter.x, canopyCenter.y + 3.dp.toPx()))
            drawCircle(color = mainGreen, radius = r, center = canopyCenter)
            drawCircle(color = Color.White.copy(alpha = 0.22f), radius = r * 0.45f, center = Offset(canopyCenter.x - 5.dp.toPx(), canopyCenter.y - 6.dp.toPx()))
        }

        TreeSpecies.GEOMETRIC_PINE -> {
            val pineGreen = if (isDark) Color(0xFF1B4D3E) else Color(0xFF10B981)
            val darkPine = if (isDark) Color(0xFF12382D) else Color(0xFF059669)

            for (layer in 0..2) {
                val layerY = canopyCenter.y + (layer * 10.dp.toPx()) - 8.dp.toPx()
                val halfW = (18.dp.toPx() + layer * 7.dp.toPx()) * squish
                val triHeight = 20.dp.toPx() * squish

                val path = Path().apply {
                    moveTo(canopyCenter.x, layerY - triHeight / 2)
                    lineTo(canopyCenter.x + halfW, layerY + triHeight / 2)
                    lineTo(canopyCenter.x - halfW, layerY + triHeight / 2)
                    close()
                }
                drawPath(path = path, color = if (layer == 2) darkPine else pineGreen)
            }
        }

        TreeSpecies.SAKURA_PINK -> {
            val pinkMain = if (isDark) Color(0xFFC0627F) else Color(0xFFF472B6)
            val pinkSoft = if (isDark) Color(0xFF8B3A54) else Color(0xFFFBCFE8)
            val r = 22.dp.toPx() * squish

            drawCircle(color = pinkSoft, radius = r * 1.15f, center = canopyCenter)
            drawCircle(color = pinkMain, radius = r * 0.85f, center = Offset(canopyCenter.x - 4.dp.toPx(), canopyCenter.y - 4.dp.toPx()))
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 3.dp.toPx(), center = Offset(canopyCenter.x + 6.dp.toPx(), canopyCenter.y - 6.dp.toPx()))
        }

        TreeSpecies.GOLDEN_AMON -> {
            val goldBase = Color(0xFFF59E0B)
            val goldBright = Color(0xFFFDE047)
            val r = 26.dp.toPx() * squish

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(goldBright, goldBase),
                    center = canopyCenter,
                    radius = r
                ),
                radius = r,
                center = canopyCenter
            )
            drawCircle(color = Color.White, radius = 3.2.dp.toPx(), center = Offset(canopyCenter.x, canopyCenter.y - r * 0.45f))
        }
    }
}

@Composable
private fun StatPill(
    icon: String,
    count: String,
    label: String,
    goldColor: Color,
    textMain: Color,
    isDark: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF1E2822) else Color(0xFFF1F5F9))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(text = count, color = textMain, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(text = label, color = goldColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -----------------------------------------------------------------------------
// 📅 Universal Smart Date Parser (प्रोजेक्ट सुरक्षा के लिए सुरक्षित रखा गया)
// -----------------------------------------------------------------------------
fun parseSessionDateUniversal(rawDateStr: String): Date? {
    if (rawDateStr.isBlank()) return null

    val clean = rawDateStr
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("\"", "")
        .replace("'", "")
        .replace(",", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    clean.toLongOrNull()?.let { millis ->
        if (millis > 1000000000000L) return Date(millis)
    }

    val formats = listOf(
        SimpleDateFormat("dd MMM yyyy hh:mm:ss a", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("MMMM dd yyyy", Locale.ENGLISH)
    )

    for (fmt in formats) {
        try {
            val d = fmt.parse(clean)
            if (d != null) return d
        } catch (_: Exception) {}
    }

    val ymdMatch = Regex("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})").find(clean)
    if (ymdMatch != null) {
        val (y, m, d) = ymdMatch.destructured
        return Calendar.getInstance().apply {
            set(y.toInt(), m.toInt() - 1, d.toInt(), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    val dmyMatch = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})").find(clean)
    if (dmyMatch != null) {
        val (d, m, y) = dmyMatch.destructured
        return Calendar.getInstance().apply {
            set(d.toInt(), m.toInt() - 1, y.toInt(), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    return null
}
