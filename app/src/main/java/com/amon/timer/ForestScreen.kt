package com.amon.timer

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.sp
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

    val gridSize = 6
    val baseTileWidth = with(density) { 90.dp.toPx() }
    val baseTileHeight = baseTileWidth / 2f

    // 🎨 एसेट्स लोड करें
    val catBitmap = remember { loadBitmapSafe(context, "cat_rock") }
    val treeBitmap = remember { loadBitmapSafe(context, "tree_oak") }
    val benchBitmap = remember { loadBitmapSafe(context, "bench_park") }

    // 🎛️ लाइव ट्यूनर स्टेट्स (डिफ़ॉल्ट सेटिंग्स)
    var overlapSpacing by remember { mutableFloatStateOf(0.92f) } // टाइल्स का खिंचाव
    var tileScaleRatio by remember { mutableFloatStateOf(0.80f) }  // इमेज स्केल अनुपात
    var anchorYRatio by remember { mutableFloatStateOf(0.70f) }    // Y-अक्ष अलाइनमेंट
    var showDebugPanel by remember { mutableStateOf(true) }

    // टाइल्स का लेआउट (ग्रुप 1 और ग्रुप 2)
    val tiles = remember {
        mutableStateListOf<ForestTile>().apply {
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val type = when {
                        // 🐾 ग्रुप 1: ट्री और कैट
                        r == 0 && c == 0 -> TileItemType.TREE_OAK
                        r == 0 && c == 1 -> TileItemType.TREE_OAK
                        r == 1 && c == 0 -> TileItemType.TREE_OAK
                        r == 1 && c == 1 -> TileItemType.CAT_ROCK
                        r == 1 && c == 2 -> TileItemType.TREE_OAK
                        r == 2 && c == 0 -> TileItemType.TREE_OAK
                        r == 2 && c == 1 -> TileItemType.TREE_OAK

                        // 🪑 ग्रुप 2: बेंच क्लस्टर (2x2)
                        r == 4 && c == 3 -> TileItemType.BENCH_PARK
                        r == 4 && c == 4 -> TileItemType.BENCH_PARK
                        r == 5 && c == 3 -> TileItemType.BENCH_PARK
                        r == 5 && c == 4 -> TileItemType.BENCH_PARK

                        else -> TileItemType.EMPTY
                    }
                    add(ForestTile(row = r, col = c, itemType = type))
                }
            }
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 2.5f)
        panOffset += offsetChange
    }

    val bgGradient = Brush.verticalGradient(
        colors = if (isDark) listOf(Color(0xFF111713), Color(0xFF090D0A))
        else listOf(Color(0xFFE8F0E9), Color(0xFFD6E3D8))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
        ) {
            val centerX = size.width / 2f + panOffset.x
            val centerY = size.height / 3.2f + panOffset.y

            val sortedTiles = tiles.sortedWith(compareBy({ it.row + it.col }, { it.row }))

            sortedTiles.forEach { tile ->
                // 🛠️ लाइव स्लाइडर वैल्यू का इस्तेमाल
                val isoX = (tile.col - tile.row) * (baseTileWidth / 2f) * overlapSpacing * scale
                val isoY = (tile.col + tile.row) * (baseTileHeight / 2f) * overlapSpacing * scale

                val slotX = centerX + isoX
                val slotY = centerY + isoY

                drawTileBase(
                    cx = slotX,
                    cy = slotY,
                    w = baseTileWidth * scale,
                    h = baseTileHeight * scale,
                    isDark = isDark
                )

                val bmp = when (tile.itemType) {
                    TileItemType.CAT_ROCK -> catBitmap
                    TileItemType.TREE_OAK -> treeBitmap
                    TileItemType.BENCH_PARK -> benchBitmap
                    TileItemType.EMPTY -> null
                }

                bmp?.let { imageBitmap ->
                    val renderW = (baseTileWidth / tileScaleRatio) * scale
                    val renderH = renderW
                    val dx = slotX - (renderW / 2f)
                    val dy = slotY - (renderH * anchorYRatio)

                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                        dstSize = IntSize(renderW.roundToInt(), renderH.roundToInt())
                    )
                }
            }
        }

        // 🔄 रीसेट और टॉगल पैनल बटन
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showDebugPanel = !showDebugPanel },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Text(if (showDebugPanel) "Hide Controls" else "Tune Grid", fontSize = 12.sp)
            }
            IconButton(
                onClick = {
                    scale = 1f
                    panOffset = Offset.Zero
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
            }
        }

        // 🎛️ लाइव ट्यूनर पैनल (स्क्रीन के निचले हिस्से में)
        if (showDebugPanel) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 1. Spacing Slider (टाइल्स को चिपकाने के लिए)
                    Text(
                        "1. Spacing (खिंचाव): ${String.format("%.3f", overlapSpacing)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = overlapSpacing,
                        onValueChange = { overlapSpacing = it },
                        valueRange = 0.70f..1.05f
                    )

                    // 2. Scale Ratio Slider (इमेज की चौड़ाई अनुपात)
                    Text(
                        "2. Scale Ratio: ${String.format("%.3f", tileScaleRatio)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = tileScaleRatio,
                        onValueChange = { tileScaleRatio = it },
                        valueRange = 0.65f..0.95f
                    )

                    // 3. Anchor Y Slider (ऊपर-नीचे एडजस्टमेंट)
                    Text(
                        "3. Anchor Y: ${String.format("%.3f", anchorYRatio)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = anchorYRatio,
                        onValueChange = { anchorYRatio = it },
                        valueRange = 0.60f..0.85f
                    )
                }
            }
        }
    }
}

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

fun loadBitmapSafe(context: Context, resName: String): ImageBitmap? {
    return try {
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            bitmap?.asImageBitmap()
        } else null
    } catch (_: Exception) {
        null
    }
}
