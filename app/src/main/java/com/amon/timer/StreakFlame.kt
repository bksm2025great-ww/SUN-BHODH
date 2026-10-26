package com.amon.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// --- चिंगारी (Spark Particle) का डेटा मॉडल ---
private data class SparkParticle(
    val xOffset: Float, // X-अक्ष पर स्थिति (-25dp से +25dp)
    val startY: Float,   // कहाँ से शुरू होगी
    val targetY: Float,  // कहाँ तक जाएगी (ऊपर)
    val size: Float,     // चिंगारी का साइज़
    val speed: Float,    // ऊपर उठने की रफ़्तार
    val delay: Int       // शुरू होने का समय
)

@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier.size(54.dp)
) {
    // 🎨 1. स्ट्रीक के हिसाब से टियर, रंग और आकार (Shape) का निर्धारण
    val (outerColor, midColor, innerColor, shapeType) = remember(streakDays) {
        when {
            streakDays in 1..6 -> { // Tier 1: Ember
                Triple(Color(0xFFDC2626), Color(0xFFF97316), Color(0xFFFDE047)) to 1
            }
            streakDays in 7..20 -> { // Tier 2: Volcanic Lava
                Triple(Color(0xFF581C1C), Color(0xFFEA580C), Color(0xFFFFB800)) to 2
            }
            streakDays in 21..49 -> { // Tier 3: Solar Gold
                Triple(Color(0xFFB45309), Color(0xFFFACC15), Color(0xFFFEF08A)) to 3
            }
            streakDays in 50..99 -> { // Tier 4: Electric Mystic
                Triple(Color(0xFF4C1D95), Color(0xFF8B5CF6), Color(0xFFEC4899)) to 4
            }
            streakDays in 100..199 -> { // Tier 5: Cyan Plasma
                Triple(Color(0xFF1E3A8A), Color(0xFF06B6D4), Color(0xFFE0F7FA)) to 4
            }
            else -> { // Tier 6: Cosmic Nebula (200+)
                Triple(Color(0xFF1E1B4B), Color(0xFF93C5FD), Color(0xFFFFFFFF)) to 4
            }
        }
    }

    // 🔄 2. धड़कन (Breathing/Pulse) एनिमेशन
    val infiniteTransition = rememberInfiniteTransition(label = "flame_breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // हल्का सा कंपन (Flicker Effect)
    val flickerY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerY"
    )

    // ✨ 3. चिंगारी (Sparks) एनिमेशन
    val frameTimer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparks_time"
    )

    // चिंगारियों की स्थिति को याद रखना
    val sparks = remember(shapeType) {
        List(6) {
            SparkParticle(
                xOffset = Random.nextDouble(-18.0, 18.0).toFloat(),
                startY = Random.nextDouble(55.0, 85.0).toFloat(),
                targetY = Random.nextDouble(10.0, 40.0).toFloat(),
                size = Random.nextDouble(2.0, 4.5).toFloat(),
                speed = Random.nextDouble(0.5, 1.2).toFloat(),
                delay = Random.nextInt(0, 1000)
            )
        }
    }

    // 🎨 4. आग का लाइव ड्रॉइंग (Canvas)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f + flickerY

        // --- चिंगारियाँ (Floating Sparks) बनाना ---
        sparks.forEach { spark ->
            // समय के अनुसार चिंगारी की ऊँचाई और पारदर्शिता (fade-out) तय करना
            val progress = ((frameTimer + (spark.delay / 2200f)) % 1f) * spark.speed
            if (progress in 0f..1f) {
                val sparkX = centerX + spark.xOffset * (1f - progress * 0.3f)
                val sparkY = h * (spark.startY - (spark.startY - spark.targetY) * progress) / 100f
                val alpha = (1f - progress).coerceIn(0f, 1f)

                drawCircle(
                    color = midColor.copy(alpha = alpha * 0.85f),
                    radius = spark.size.dp.toPx() * (1f - progress * 0.5f),
                    center = Offset(sparkX, sparkY)
                )
            }
        }

        // --- ४ चुनिंदा आकृतियों के लिए पाथ (Path) जेनरेट करना ---
        val outerPath = Path()
        val midPath = Path()
        val innerPath = Path()

        when (shapeType) {
            1 -> { // 📦 Shape 1: Ember - गोल और नैचुरल क्लासिक
                createShape1(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape1(midPath, w, h, centerX, centerY, scale, 0.72f)
                createShape1(innerPath, w, h, centerX, centerY, scale, 0.44f)
            }
            2 -> { // 🌊 Shape 2: Volcanic Lava - घुमावदार लहरदार
                createShape2(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape2(midPath, w, h, centerX, centerY, scale, 0.72f)
                createShape2(innerPath, w, h, centerX, centerY, scale, 0.44f)
            }
            3 -> { // 🔥 Shape 3: Solar Gold - घनी और बहु-परत (Natural Swirl)
                createShape3(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape3(midPath, w, h, centerX, centerY, scale, 0.75f)
                createShape3(innerPath, w, h, centerX, centerY, scale, 0.48f)
            }
            else -> { // 💎 Shape 4: High Tiers - बिखरा और टूट-टूट कर भड़कता हुआ
                createShape4(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape4(midPath, w, h, centerX, centerY, scale, 0.75f)
                createShape4(innerPath, w, h, centerX, centerY, scale, 0.48f)
            }
        }

        // --- ३ लेयर्स में आग की ड्रॉइंग ---
        // 1. बाहरी घेरा (Outer Glow)
        drawPath(
            path = outerPath,
            color = outerColor.copy(alpha = 0.95f)
        )
        // 2. बीच की लौ (Mid Body)
        drawPath(
            path = midPath,
            color = midColor
        )
        // 3. अंदरूनी दिल (Inner Core)
        drawPath(
            path = innerPath,
            color = innerColor
        )
    }
}

// =============================================================================
// 🎨 ४ भव्य आकृतियों (Shapes) का गणितीय ढाँचा (Math Custom Paths)
// =============================================================================

private fun createShape1(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.9f)
    path.cubicTo(cx - r * 1.3f, cy + r * 0.9f, cx - r * 1.2f, cy - r * 0.2f, cx - r * 0.5f, cy - r * 0.9f)
    path.cubicTo(cx - r * 0.1f, cy - r * 1.5f, cx, cy - r * 1.7f, cx, cy - r * 1.8f) // नोक
    path.cubicTo(cx + r * 0.15f, cy - r * 1.1f, cx + r * 0.6f, cy - r * 0.8f, cx + r * 1.0f, cy - r * 0.1f)
    path.cubicTo(cx + r * 1.3f, cy + r * 0.5f, cx + r * 0.7f, cy + r * 0.9f, cx, cy + r * 0.9f)
    path.close()
}

private fun createShape2(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.95f)
    path.cubicTo(cx - r * 1.4f, cy + r * 0.8f, cx - r * 1.1f, cy - r * 0.4f, cx - r * 0.4f, cy - r * 1.0f)
    path.cubicTo(cx - r * 0.1f, cy - r * 1.4f, cx - r * 0.1f, cy - r * 1.8f, cx, cy - r * 1.9f) // घुमावदार नोक
    path.cubicTo(cx + r * 0.25f, cy - r * 1.3f, cx + r * 0.5f, cy - r * 0.9f, cx + r * 0.7f, cy - r * 0.5f)
    path.cubicTo(cx + r * 0.95f, cy - r * 0.1f, cx + r * 0.9f, cy + r * 0.5f, cx + r * 0.3f, cy + r * 0.9f)
    path.cubicTo(cx + r * 0.1f, cy + r * 0.95f, cx - r * 0.1f, cy + r * 0.95f, cx, cy + r * 0.95f)
    path.close()
}

private fun createShape3(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.92f)
    // बाईं तरफ की लपटें
    path.cubicTo(cx - r * 1.4f, cy + r * 0.8f, cx - r * 1.3f, cy - r * 0.1f, cx - r * 0.6f, cy - r * 0.6f)
    path.cubicTo(cx - r * 0.8f, cy - r * 1.0f, cx - r * 0.3f, cy - r * 1.4f, cx, cy - r * 1.85f) // मुख्य नोक
    // दाईं तरफ की लहरदार परतें (Multilayered spiral)
    path.cubicTo(cx + r * 0.2f, cy - r * 1.2f, cx + r * 0.6f, cy - r * 0.8f, cx + r * 0.8f, cy - r * 0.4f)
    path.cubicTo(cx + r * 0.4f, cy - r * 0.1f, cx + r * 0.6f, cy + r * 0.2f, cx + r * 0.85f, cy + r * 0.5f)
    path.cubicTo(cx + r * 1.1f, cy + r * 0.9f, cx + r * 0.5f, cy + r * 0.95f, cx, cy + r * 0.92f)
    path.close()
}

private fun createShape4(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.9f)
    // खंडित या शार्ड्स वाली आकृतियाँ (Splits / geometric segmented look)
    path.cubicTo(cx - r * 1.3f, cy + r * 0.6f, cx - r * 1.1f, cy - r * 0.1f, cx - r * 0.7f, cy - r * 0.4f)
    path.lineTo(cx - r * 0.85f, cy - r * 0.55f) // तीखा कट-आउट
    path.cubicTo(cx - r * 1.0f, cy - r * 1.0f, cx - r * 0.4f, cy - r * 1.4f, cx, cy - r * 1.9f) // तीखी मुख्य नोक
    // दाईं तरफ की खंडित नोकें
    path.cubicTo(cx + r * 0.25f, cy - r * 1.4f, cx + r * 0.65f, cy - r * 0.8f, cx + r * 0.5f, cy - r * 0.5f)
    path.lineTo(cx + r * 0.75f, cy - r * 0.35f) // तीखा कट-आउट
    path.cubicTo(cx + r * 1.1f, cy + r * 0.2f, cx + r * 0.9f, cy + r * 0.7f, cx, cy + r * 0.9f)
    path.close()
}
