package com.amon.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// --- आग की सेटिंग्स का साफ़ डेटा मॉडल ---
private data class FlameConfig(
    val outerColor: Color,
    val midColor: Color,
    val innerColor: Color,
    val shapeType: Int
)

// --- चिंगारी (Spark Particle) का डेटा मॉडल ---
private data class SparkParticle(
    val xOffset: Float,
    val startY: Float,
    val targetY: Float,
    val size: Float,
    val speed: Float,
    val delay: Int
)

@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier.size(54.dp)
) {
    // 🎨 1. स्ट्रीक के हिसाब से रंग और आकार (Shape) का निर्धारण
    val config = remember(streakDays) {
        when {
            streakDays <= 0 -> FlameConfig(
                outerColor = Color(0xFF334155),
                midColor = Color(0xFF64748B),
                innerColor = Color(0xFF94A3B8),
                shapeType = 1
            )
            streakDays in 1..6 -> FlameConfig( // Tier 1: Ember
                outerColor = Color(0xFFDC2626),
                midColor = Color(0xFFF97316),
                innerColor = Color(0xFFFDE047),
                shapeType = 1
            )
            streakDays in 7..20 -> FlameConfig( // Tier 2: Volcanic Lava
                outerColor = Color(0xFF581C1C),
                midColor = Color(0xFFEA580C),
                innerColor = Color(0xFFFFB800),
                shapeType = 2
            )
            streakDays in 21..49 -> FlameConfig( // Tier 3: Solar Gold
                outerColor = Color(0xFFB45309),
                midColor = Color(0xFFFACC15),
                innerColor = Color(0xFFFEF08A),
                shapeType = 3
            )
            streakDays in 50..99 -> FlameConfig( // Tier 4: Electric Mystic
                outerColor = Color(0xFF4C1D95),
                midColor = Color(0xFF8B5CF6),
                innerColor = Color(0xFFEC4899),
                shapeType = 4
            )
            streakDays in 100..199 -> FlameConfig( // Tier 5: Cyan Plasma
                outerColor = Color(0xFF1E3A8A),
                midColor = Color(0xFF06B6D4),
                innerColor = Color(0xFFE0F7FA),
                shapeType = 4
            )
            else -> FlameConfig( // Tier 6: Cosmic Nebula (200+)
                outerColor = Color(0xFF1E1B4B),
                midColor = Color(0xFF93C5FD),
                innerColor = Color(0xFFFFFFFF),
                shapeType = 4
            )
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

    val sparks = remember(config.shapeType) {
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

        // चिंगारियाँ (Floating Sparks)
        if (streakDays > 0) {
            sparks.forEach { spark ->
                val progress = ((frameTimer + (spark.delay / 2200f)) % 1f) * spark.speed
                if (progress in 0f..1f) {
                    val sparkX = centerX + spark.xOffset * (1f - progress * 0.3f)
                    val sparkY = h * (spark.startY - (spark.startY - spark.targetY) * progress) / 100f
                    val alpha = (1f - progress).coerceIn(0f, 1f)

                    drawCircle(
                        color = config.midColor.copy(alpha = alpha * 0.85f),
                        radius = spark.size.dp.toPx() * (1f - progress * 0.5f),
                        center = Offset(sparkX, sparkY)
                    )
                }
            }
        }

        // पाथ (Paths)
        val outerPath = Path()
        val midPath = Path()
        val innerPath = Path()

        when (config.shapeType) {
            1 -> {
                createShape1(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape1(midPath, w, h, centerX, centerY, scale, 0.72f)
                createShape1(innerPath, w, h, centerX, centerY, scale, 0.44f)
            }
            2 -> {
                createShape2(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape2(midPath, w, h, centerX, centerY, scale, 0.72f)
                createShape2(innerPath, w, h, centerX, centerY, scale, 0.44f)
            }
            3 -> {
                createShape3(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape3(midPath, w, h, centerX, centerY, scale, 0.75f)
                createShape3(innerPath, w, h, centerX, centerY, scale, 0.48f)
            }
            else -> {
                createShape4(outerPath, w, h, centerX, centerY, scale, 1.0f)
                createShape4(midPath, w, h, centerX, centerY, scale, 0.75f)
                createShape4(innerPath, w, h, centerX, centerY, scale, 0.48f)
            }
        }

        // ३ लेयर्स ड्रॉइंग
        drawPath(
            path = outerPath,
            color = config.outerColor.copy(alpha = 0.95f)
        )
        drawPath(
            path = midPath,
            color = config.midColor
        )
        drawPath(
            path = innerPath,
            color = config.innerColor
        )
    }
}

// =============================================================================
// 🎨 ४ आकृतियों (Shapes) का पाथ फ़ंक्शन
// =============================================================================

private fun createShape1(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.9f)
    path.cubicTo(cx - r * 1.3f, cy + r * 0.9f, cx - r * 1.2f, cy - r * 0.2f, cx - r * 0.5f, cy - r * 0.9f)
    path.cubicTo(cx - r * 0.1f, cy - r * 1.5f, cx, cy - r * 1.7f, cx, cy - r * 1.8f)
    path.cubicTo(cx + r * 0.15f, cy - r * 1.1f, cx + r * 0.6f, cy - r * 0.8f, cx + r * 1.0f, cy - r * 0.1f)
    path.cubicTo(cx + r * 1.3f, cy + r * 0.5f, cx + r * 0.7f, cy + r * 0.9f, cx, cy + r * 0.9f)
    path.close()
}

private fun createShape2(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.95f)
    path.cubicTo(cx - r * 1.4f, cy + r * 0.8f, cx - r * 1.1f, cy - r * 0.4f, cx - r * 0.4f, cy - r * 1.0f)
    path.cubicTo(cx - r * 0.1f, cy - r * 1.4f, cx - r * 0.1f, cy - r * 1.8f, cx, cy - r * 1.9f)
    path.cubicTo(cx + r * 0.25f, cy - r * 1.3f, cx + r * 0.5f, cy - r * 0.9f, cx + r * 0.7f, cy - r * 0.5f)
    path.cubicTo(cx + r * 0.95f, cy - r * 0.1f, cx + r * 0.9f, cy + r * 0.5f, cx + r * 0.3f, cy + r * 0.9f)
    path.cubicTo(cx + r * 0.1f, cy + r * 0.95f, cx - r * 0.1f, cy + r * 0.95f, cx, cy + r * 0.95f)
    path.close()
}

private fun createShape3(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.92f)
    path.cubicTo(cx - r * 1.4f, cy + r * 0.8f, cx - r * 1.3f, cy - r * 0.1f, cx - r * 0.6f, cy - r * 0.6f)
    path.cubicTo(cx - r * 0.8f, cy - r * 1.0f, cx - r * 0.3f, cy - r * 1.4f, cx, cy - r * 1.85f)
    path.cubicTo(cx + r * 0.2f, cy - r * 1.2f, cx + r * 0.6f, cy - r * 0.8f, cx + r * 0.8f, cy - r * 0.4f)
    path.cubicTo(cx + r * 0.4f, cy - r * 0.1f, cx + r * 0.6f, cy + r * 0.2f, cx + r * 0.85f, cy + r * 0.5f)
    path.cubicTo(cx + r * 1.1f, cy + r * 0.9f, cx + r * 0.5f, cy + r * 0.95f, cx, cy + r * 0.92f)
    path.close()
}

private fun createShape4(path: Path, w: Float, h: Float, cx: Float, cy: Float, scale: Float, f: Float) {
    val r = (w * 0.38f) * scale * f
    path.reset()
    path.moveTo(cx, cy + r * 0.9f)
    path.cubicTo(cx - r * 1.3f, cy + r * 0.6f, cx - r * 1.1f, cy - r * 0.1f, cx - r * 0.7f, cy - r * 0.4f)
    path.lineTo(cx - r * 0.85f, cy - r * 0.55f)
    path.cubicTo(cx - r * 1.0f, cy - r * 1.0f, cx - r * 0.4f, cy - r * 1.4f, cx, cy - r * 1.9f)
    path.cubicTo(cx + r * 0.25f, cy - r * 1.4f, cx + r * 0.65f, cy - r * 0.8f, cx + r * 0.5f, cy - r * 0.5f)
    path.lineTo(cx + r * 0.75f, cy - r * 0.35f)
    path.cubicTo(cx + r * 1.1f, cy + r * 0.2f, cx + r * 0.9f, cy + r * 0.7f, cx, cy + r * 0.9f)
    path.close()
}
