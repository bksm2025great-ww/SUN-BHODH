package com.amon.timer

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

enum class TileItemType {
    EMPTY,
    CAT_ROCK,    // ग्रुप 1: बिल्ली
    TREE_OAK,    // ग्रुप 1: नया पेड़
    BENCH_PARK   // ग्रुप 2: बेंच
}

data class ForestTile(
    val row: Int,
    val col: Int,
    val itemType: TileItemType = TileItemType.EMPTY
)

@Composable
fun ForestScreen() {
    val isDark = ThemeManager.isDarkTheme.value
    val context = LocalContext.current
    val density = LocalDensity.current

    // 🌿 6x6 का ग्रिड ताकि दोनों ग्रुप्स में गैप दिख सके
    val gridSize = 6
    val tileWidth = with(density) { 90.dp.toPx() }
    val tileHeight = tileWidth / 2f // 2:1 आइसोमेट्रिक अनुपात

    // 🎨 तीनों एसेट्स लोड करें
    val catBitmap = remember { loadBitmapSafe(context, "cat_rock") }
    val treeBitmap = remember { loadBitmapSafe(context, "tree_oak") }
    val benchBitmap = remember { loadBitmapSafe(context, "bench_park") }

    // 🗺️ टाइल्स का लेआउट (दोनों ग्रुप्स का विभाजन)
    val tiles = remember {
        mutableStateListOf<ForestTile>().apply {
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val type = when {
                        // 🐾 ग्रुप 1: ट्री और कैट का क्लस्टर (Row 0 से 2, Col 0 से 2)
                        r == 0 && c == 0 -> TileItemType.TREE_OAK
                        r == 0 && c == 1 -> TileItemType.TREE_OAK
                        r == 1 && c == 0 -> TileItemType.TREE_OAK
                        r == 1 && c == 1 -> TileItemType.CAT_ROCK // सेंटर में बिल्ली
                        r == 1 && c == 2 -> TileItemType.TREE_OAK
                        r == 2 && c == 0 -> TileItemType.TREE_OAK
                        r == 2 && c == 1 -> TileItemType.TREE_OAK

                        // 🪑 ग्रुप 2: थोड़ा गैप देकर 4 बेंच का क्लस्टर (Row 4 से 5, Col 3 से 4)
                        r == 4 && c == 3 -> TileItemType.BENCH_PARK
                        r == 4 && c == 4 -> TileItemType.BENCH_PARK
                        r == 5 && c == 3 -> TileItemType.BENCH_PARK
                        r == 5 && c == 4 -> TileItemType.BENCH_PARK

                        // बीच का खाली गैप
                        else -> TileItemType.EMPTY
                    }
                    add(ForestTile(row = r, col = c, itemType = type))
                }
            }
        }
    }

    // 🔍 पैन और ज़ूम
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 2.5f)
        panOffset += offsetChange
    }

    val bgGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF111713), Color(0xFF090D0A))
        } else {
            listOf(Color(0xFFE8F0E9), Color(0xFFD6E3D8))
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
        ) {
            val centerX = size.width / 2f + panOffset.x
            val centerY = size.height / 3f + panOffset.y

            // Z-इंडेक्स सॉर्टिंग: पीछे से आगे की ओर ड्रा करना
            val sortedTiles = tiles.sortedWith(compareBy({ it.row + it.col }, { it.row }))

            sortedTiles.forEach { tile ->
                // 2:1 आइसोमेट्रिक गणित
                val isoX = (tile.col - tile.row) * (tileWidth / 2f) * scale
                val isoY = (tile.col + tile.row) * (tileHeight / 2f) * scale

                val slotX = centerX + isoX
                val slotY = centerY + isoY

                // 1. बेस ग्रिड डायमंड (हल्की गाइडलाइन)
                drawTileBase(
                    cx = slotX,
                    cy = slotY,
                    w = tileWidth * scale,
                    h = tileHeight * scale,
                    isDark = isDark
                )

                // 2. एसेट रेंडरिंग
                when (tile.itemType) {
                    TileItemType.CAT_ROCK -> {
                        catBitmap?.let { bmp ->
                            // लियोनार्डो बेस रेश्यो: ~76% चौड़ाई, 0.68f Y-एंकर
                            val renderW = (tileWidth / 0.76f) * scale
                            val renderH = renderW
                            val dx = slotX - (renderW / 2f)
                            val dy = slotY - (renderH * 0.68f)

                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                                dstSize = IntSize(renderW.roundToInt(), renderH.roundToInt())
                            )
                        }
                    }

                    TileItemType.TREE_OAK -> {
                        treeBitmap?.let { bmp ->
                            // नए ओक ट्री का बेस: ~78% चौड़ाई, 0.70f Y-एंकर
                            val renderW = (tileWidth / 0.78f) * scale
                            val renderH = renderW
                            val dx = slotX - (renderW / 2f)
                            val dy = slotY - (renderH * 0.70f)

                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                                dstSize = IntSize(renderW.roundToInt(), renderH.roundToInt())
                            )
                        }
                    }

                    TileItemType.BENCH_PARK -> {
                        benchBitmap?.let { bmp ->
                            // बेंच टाइल का बेस: ~80% चौड़ाई, 0.68f Y-एंकर
                            val renderW = (tileWidth / 0.80f) * scale
                            val renderH = renderW
                            val dx = slotX - (renderW / 2f)
                            val dy = slotY - (renderH * 0.68f)

                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                                dstSize = IntSize(renderW.roundToInt(), renderH.roundToInt())
                            )
                        }
                    }

                    TileItemType.EMPTY -> {
                        // खाली जगह को खाली छोड़ें
                    }
                }
            }
        }

        // 🔄 रीसेट बटन (ज़ूम और पोजीशन रीसेट)
        IconButton(
            onClick = {
                scale = 1f
                panOffset = Offset.Zero
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
        }
    }
}

/**
 * आइसोमेट्रिक ग्रिड की हल्की बाउंड्री
 */
fun DrawScope.drawTileBase(cx: Float, cy: Float, w: Float, h: Float, isDark: Boolean) {
    val path = Path().apply {
        moveTo(cx, cy - h / 2f)
        lineTo(cx + w / 2f, cy)
        lineTo(cx, cy + h / 2f)
        lineTo(cx - w / 2f, cy)
        close()
    }
    val lineColor = if (isDark) Color(0x224ADE80) else Color(0x222E7D32)
    drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
}

/**
 * सेफ बिटमैप लोडर
 */
fun loadBitmapSafe(context: Context, resName: String): ImageBitmap? {
    return try {
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) ImageBitmap.imageResource(context.resources, resId) else null
    } catch (_: Exception) {
        null
    }
}
