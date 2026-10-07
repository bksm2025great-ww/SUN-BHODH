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

data class ShelfBook(
    val id: String,
    val subject: String,
    val durationMinutes: Int,
    val spineColor: Color,
    val accentColor: Color,
    val widthFactor: Float,
    val heightFactor: Float,
    val hasRibbon: Boolean
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

    // सेशन्स को असली किताबों में बदलें (अगर कोई सेशन नहीं है तो डेमो के लिए 12 किताबें)
    val books = remember(sessions) {
        val palette = listOf(
            Pair(Color(0xFF8B2635), Color(0xFFFDE047)), // रूबी रेड
            Pair(Color(0xFF1E3A8A), Color(0xFF93C5FD)), // रॉयल ब्लू
            Pair(Color(0xFF14532D), Color(0xFF86EFAC)), // एमराल्ड ग्रीन
            Pair(Color(0xFF78350F), Color(0xFFFDE68A)), // विंटेज लेदर
            Pair(Color(0xFF581C87), Color(0xFFD8B4FE)), // मिस्टिक पर्पल
            Pair(Color(0xFF0F766E), Color(0xFF99F6E4))  // गहरा फिरोजी
        )

        if (sessions.isEmpty()) {
            List(12) { i ->
                val p = palette[i % palette.size]
                ShelfBook(
                    id = "b_$i",
                    subject = when (i % 4) {
                        0 -> "Mathematics"
                        1 -> "History"
                        2 -> "Deep Science"
                        else -> "Philosophy"
                    },
                    durationMinutes = 25 + (i * 5),
                    spineColor = p.first,
                    accentColor = p.second,
                    widthFactor = 0.85f + (i % 3) * 0.15f,
                    heightFactor = 0.88f + ((i * 7) % 4) * 0.05f,
                    hasRibbon = i % 3 == 0
                )
            }
        } else {
            sessions.mapIndexed { i, s ->
                val p = palette[i % palette.size]
                ShelfBook(
                    id = "b_$i",
                    subject = s.subject,
                    durationMinutes = s.durationMinutes,
                    spineColor = p.first,
                    accentColor = p.second,
                    widthFactor = 0.85f + (i % 3) * 0.15f,
                    heightFactor = 0.88f + ((i * 7) % 4) * 0.05f,
                    hasRibbon = s.durationMinutes >= 45
                )
            }
        }
    }

    val totalMins = remember(sessions, books) {
        if (sessions.isNotEmpty()) sessions.sumOf { it.durationMinutes }
        else books.sumOf { it.durationMinutes }
    }
    val totalHours = String.format(Locale.getDefault(), "%.1f", totalMins / 60f)

    val infiniteTransition = rememberInfiniteTransition(label = "library_life")

    // 1. मोमबत्ती की लौ का हिलना (Flame Flicker)
    val flameFlicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame"
    )

    // 2. हवा में तैरते सुनहरे कण (Floating Dust Motes)
    val dustPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dust"
    )

    // चुनी गई किताब के बाहर खिसकने का एनिमेशन (Slide-out animation)
    val coroutineScope = rememberCoroutineScope()
    var selectedBookIndex by remember { mutableStateOf<Int?>(null) }
    val bookSlideOffsets = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    val libraryBg = if (isDark) Color(0xFF0F0B08) else Color(0xFFF7F3EE)
    val woodShelfColor = if (isDark) Color(0xFF2B1810) else Color(0xFFD4A373)
    val woodShadowColor = if (isDark) Color(0xFF170C08) else Color(0xFFB07D4C)

    // कुल शेल्व्स (हर शेल्फ़ पर 6 किताबें)
    val booksPerShelf = 6
    val shelfCount = maxOf(2, ceil(books.size / booksPerShelf.toDouble()).toInt())
    val shelfHeightDp = 180.dp
   val totalCanvasHeightDp = (160 + shelfCount * 180).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(libraryBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalCanvasHeightDp)
                    .pointerInput(books) {
                        detectTapGestures { tapOffset ->
                            val w = size.width
                            val startY = 160.dp.toPx()
                            val shelfStep = 180.dp.toPx()

                            books.forEachIndexed { index, _ ->
                                val shelfIdx = index / booksPerShelf
                                val posInShelf = index % booksPerShelf
                                val shelfBaseY = startY + (shelfIdx * shelfStep) + 120.dp.toPx()

                                val bookWidthPx = 38.dp.toPx()
                                val startX = w * 0.14f + (posInShelf * (bookWidthPx + 10.dp.toPx()))

                                if (tapOffset.x in startX..(startX + bookWidthPx) &&
                                    tapOffset.y in (shelfBaseY - 110.dp.toPx())..shelfBaseY
                                ) {
                                    selectedBookIndex = index
                                    val anim = bookSlideOffsets.getOrPut(index) { Animatable(0f) }
                                    coroutineScope.launch {
                                        anim.animateTo(-24.dp.toPx(), tween(160, easing = FastOutSlowInEasing))
                                        anim.animateTo(-16.dp.toPx(), spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val startY = 160.dp.toPx()
                val shelfStep = 180.dp.toPx()

                // A. शेल्व्स (लकड़ी की पटरियाँ) खींचना
                for (s in 0 until shelfCount) {
                    val shelfY = startY + (s * shelfStep) + 120.dp.toPx()

                    // शेल्फ़ की परछाई
                    drawRect(
                        color = woodShadowColor,
                        topLeft = Offset(w * 0.06f, shelfY + 8.dp.toPx()),
                        size = Size(w * 0.88f, 10.dp.toPx())
                    )
                    // मुख्य लकड़ी की पटरी
                    drawRoundRect(
                        color = woodShelfColor,
                        topLeft = Offset(w * 0.06f, shelfY),
                        size = Size(w * 0.88f, 12.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }

                // B. पहली शेल्फ़ पर विंटेज मोमबत्ती (Cozy Candle)
                val candleBaseX = w * 0.84f
                val candleBaseY = startY + 120.dp.toPx()
                drawVintageCandle(
                    base = Offset(candleBaseX, candleBaseY),
                    flamePulse = flameFlicker,
                    isDark = isDark
                )

                // C. किताबें सजाना (Drawing Books)
                books.forEachIndexed { index, book ->
                    val shelfIdx = index / booksPerShelf
                    val posInShelf = index % booksPerShelf
                    val shelfBaseY = startY + (shelfIdx * shelfStep) + 120.dp.toPx()

                    val bookWidthPx = 36.dp.toPx() * book.widthFactor
                    val bookHeightPx = 88.dp.toPx() * book.heightFactor
                    val bookX = w * 0.14f + (posInShelf * (38.dp.toPx() + 10.dp.toPx()))
                    val slideY = bookSlideOffsets[index]?.value ?: 0f

                    drawLibraryBook(
                        book = book,
                        topLeft = Offset(bookX, shelfBaseY - bookHeightPx + slideY),
                        width = bookWidthPx,
                        height = bookHeightPx,
                        isSelected = selectedBookIndex == index
                    )
                }

                // D. हवा में तैरते सुनहरे धूल के कण (Floating Golden Dust)
                if (isDark) {
                    val dustColor = Color(0xFFFDE047)
                    for (i in 0..10) {
                        val dx = (w * 0.15f) + sin(dustPhase + i * 1.5f) * (w * 0.35f) + (i * 24.dp.toPx()) % (w * 0.7f)
                        val dy = startY + cos(dustPhase + i * 1.2f) * 60.dp.toPx() + (i * 45.dp.toPx())
                        val dAlpha = (sin(dustPhase + i) * 0.5f + 0.5f).coerceIn(0.15f, 0.75f)

                        drawCircle(
                            color = dustColor.copy(alpha = dAlpha * 0.25f),
                            radius = 3.dp.toPx(),
                            center = Offset(dx, dy)
                        )
                        drawCircle(
                            color = dustColor.copy(alpha = dAlpha),
                            radius = 1.2.dp.toPx(),
                            center = Offset(dx, dy)
                        )
                    }
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
                        Text(text = "📚", fontSize = 14.sp)
                        Text(
                            text = "MYSTIC LIBRARY",
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
                            text = "${books.size} Books",
                            color = goldColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", color = textMuted, fontSize = 10.sp)
                        Text(
                            text = "${totalHours}h Read",
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
// 📖 विंटेज लेदर-बाउंड बुक इंजन (Procedural Book Engine)
// -----------------------------------------------------------------------------
private fun DrawScope.drawLibraryBook(
    book: ShelfBook,
    topLeft: Offset,
    width: Float,
    height: Float,
    isSelected: Boolean
) {
    // 1. किताब की परछाई
    drawRect(
        color = Color(0x33000000),
        topLeft = Offset(topLeft.x + 2.dp.toPx(), topLeft.y + 4.dp.toPx()),
        size = Size(width, height)
    )

    // 2. मुख्य जिल्द (Book Spine)
    drawRoundRect(
        color = book.spineColor,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = CornerRadius(2.dp.toPx())
    )

    // 3. जिल्द की शेडिंग (3D Curved Spine Effect)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.25f)
            ),
            startX = topLeft.x,
            endX = topLeft.x + width
        ),
        topLeft = topLeft,
        size = Size(width, height)
    )

    // 4. सुनहरी धारियाँ (Gold Ribs / Embossed Foil)
    val goldStripeColor = book.accentColor.copy(alpha = 0.85f)
    drawLine(
        color = goldStripeColor,
        start = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + height * 0.16f),
        end = Offset(topLeft.x + width - 3.dp.toPx(), topLeft.y + height * 0.16f),
        strokeWidth = 2.dp.toPx()
    )
    drawLine(
        color = goldStripeColor,
        start = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + height * 0.20f),
        end = Offset(topLeft.x + width - 3.dp.toPx(), topLeft.y + height * 0.20f),
        strokeWidth = 1.2.dp.toPx()
    )

    drawLine(
        color = goldStripeColor,
        start = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + height * 0.80f),
        end = Offset(topLeft.x + width - 3.dp.toPx(), topLeft.y + height * 0.80f),
        strokeWidth = 1.2.dp.toPx()
    )
    drawLine(
        color = goldStripeColor,
        start = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + height * 0.84f),
        end = Offset(topLeft.x + width - 3.dp.toPx(), topLeft.y + height * 0.84f),
        strokeWidth = 2.dp.toPx()
    )

    // 5. सिल्क बुकमार्क रिबन (अगर सेशन लंबा था)
    if (book.hasRibbon) {
        val ribbonX = topLeft.x + width * 0.5f
        drawLine(
            color = Color(0xFFDC2626),
            start = Offset(ribbonX, topLeft.y + height),
            end = Offset(ribbonX + 2.dp.toPx(), topLeft.y + height + 14.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 6. अगर किताब सिलेक्टेड है (गोल्डन चमक)
    if (isSelected) {
        drawRoundRect(
            color = Color(0xFFFDE047),
            topLeft = topLeft,
            size = Size(width, height),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// -----------------------------------------------------------------------------
// 🕯️ विंटेज मोमबत्ती इंजन (Cozy Candle with Flame Flicker)
// -----------------------------------------------------------------------------
private fun DrawScope.drawVintageCandle(
    base: Offset,
    flamePulse: Float,
    isDark: Boolean
) {
    val candleW = 12.dp.toPx()
    val candleH = 26.dp.toPx()
    val cTopLeft = Offset(base.x - candleW / 2, base.y - candleH)

    // मोम की बॉडी
    drawRoundRect(
        color = if (isDark) Color(0xFFFDF6B2) else Color(0xFFFEF9C3),
        topLeft = cTopLeft,
        size = Size(candleW, candleH),
        cornerRadius = CornerRadius(1.dp.toPx())
    )

    // बत्ती (Wick)
    val wickTop = Offset(base.x, cTopLeft.y - 5.dp.toPx())
    drawLine(Color(0xFF27272A), Offset(base.x, cTopLeft.y), wickTop, strokeWidth = 1.5.dp.toPx())

    // आग की चमक (Aura Glow)
    val auraR = 24.dp.toPx() * flamePulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55FBBF24), Color.Transparent),
            center = wickTop,
            radius = auraR
        ),
        radius = auraR,
        center = wickTop
    )

    // लौ (Flame)
    val flH = 14.dp.toPx() * flamePulse
    val flamePath = Path().apply {
        moveTo(wickTop.x, wickTop.y - flH)
        cubicTo(wickTop.x + 5.dp.toPx(), wickTop.y - flH * 0.4f, wickTop.x + 3.dp.toPx(), wickTop.y, wickTop.x, wickTop.y)
        cubicTo(wickTop.x - 3.dp.toPx(), wickTop.y, wickTop.x - 5.dp.toPx(), wickTop.y - flH * 0.4f, wickTop.x, wickTop.y - flH)
        close()
    }
    drawPath(flamePath, Color(0xFFF59E0B))

    // लौ का अंदरूनी सफेद-पीला हिस्सा
    drawCircle(Color(0xFFFEF08A), 2.8.dp.toPx(), Offset(wickTop.x, wickTop.y - 4.dp.toPx()))
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
