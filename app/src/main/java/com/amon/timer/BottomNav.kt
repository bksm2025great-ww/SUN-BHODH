package com.amon.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// 🟢 1. VECTOR ICONS (HOME, CIRCLE BADGE, STATS GROWTH, PROFILE)
// =============================================================================
@Composable
fun SimpleNavIcon(
    index: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    cutoutColor: Color = Color(0xFF14141E)
) {
    when (index) {
        0 -> {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = tint,
                modifier = modifier
            )
        }
        1 -> {
            // 🟢 Naya Circle Icon: Circular Badge with 3 People Silhouette Cutout
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val r = size.minDimension * 0.48f

                // Outer circle clipping taaki saare shapes badge ke andar rahein
                val circlePath = Path().apply {
                    addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                }

                clipPath(circlePath) {
                    // Base filled circular badge
                    drawCircle(
                        color = tint,
                        radius = r,
                        center = Offset(cx, cy)
                    )

                    val centerHeadR = r * 0.28f
                    val centerHeadCenter = Offset(cx, cy - r * 0.28f)
                    val centerBodyW = r * 0.96f
                    val centerBodyH = r * 0.85f

                    val sideHeadR = r * 0.20f
                    val sideHeadY = cy - r * 0.16f
                    val sideBodyW = r * 0.72f
                    val sideBodyH = r * 0.68f

                    // Left person silhouette
                    val leftCx = cx - r * 0.44f
                    drawCircle(
                        color = cutoutColor,
                        radius = sideHeadR,
                        center = Offset(leftCx, sideHeadY)
                    )
                    drawArc(
                        color = cutoutColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(leftCx - sideBodyW / 2f, sideHeadY + sideHeadR * 0.4f),
                        size = Size(sideBodyW, sideBodyH * 1.4f)
                    )

                    // Right person silhouette
                    val rightCx = cx + r * 0.44f
                    drawCircle(
                        color = cutoutColor,
                        radius = sideHeadR,
                        center = Offset(rightCx, sideHeadY)
                    )
                    drawArc(
                        color = cutoutColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(rightCx - sideBodyW / 2f, sideHeadY + sideHeadR * 0.4f),
                        size = Size(sideBodyW, sideBodyH * 1.4f)
                    )

                    // Center person ke peeche ka boundary separation gap (tint color)
                    val gapStroke = 1.8.dp.toPx()
                    drawCircle(
                        color = tint,
                        radius = centerHeadR + gapStroke,
                        center = centerHeadCenter
                    )
                    drawArc(
                        color = tint,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(cx - (centerBodyW / 2f + gapStroke), (cy - r * 0.02f) - gapStroke),
                        size = Size(centerBodyW + gapStroke * 2f, (centerBodyH * 1.5f) + gapStroke * 2f)
                    )

                    // Center main person (In front)
                    drawCircle(
                        color = cutoutColor,
                        radius = centerHeadR,
                        center = centerHeadCenter
                    )
                    drawArc(
                        color = cutoutColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(cx - centerBodyW / 2f, cy - r * 0.02f),
                        size = Size(centerBodyW, centerBodyH * 1.5f)
                    )
                }
            }
        }
        2 -> {
            // 🟢 Naya Stats Icon: 4 Growth Bars with Rising Trend Arrow
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height

                // Baseline
                val baseStroke = 1.8.dp.toPx()
                drawLine(
                    color = tint,
                    start = Offset(w * 0.08f, h * 0.90f),
                    end = Offset(w * 0.92f, h * 0.90f),
                    strokeWidth = baseStroke,
                    cap = StrokeCap.Round
                )

                // 4 Growth Bars
                val barW = w * 0.13f
                val barCorner = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                val barHeights = listOf(0.24f, 0.40f, 0.56f, 0.72f)
                val barXStarts = listOf(0.13f, 0.33f, 0.53f, 0.73f)

                barHeights.forEachIndexed { i, heightRatio ->
                    val barH = h * heightRatio
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * barXStarts[i], h * 0.90f - barH),
                        size = Size(barW, barH),
                        cornerRadius = barCorner
                    )
                }

                // Rising Trend Arrow Curve
                val arrowStroke = 2.4.dp.toPx()
                val arrowCurvePath = Path().apply {
                    moveTo(w * 0.08f, h * 0.62f)
                    cubicTo(
                        w * 0.35f, h * 0.60f,
                        w * 0.60f, h * 0.45f,
                        w * 0.86f, h * 0.15f
                    )
                }
                drawPath(
                    path = arrowCurvePath,
                    color = tint,
                    style = Stroke(width = arrowStroke, cap = StrokeCap.Round)
                )

                // Arrowhead
                val arrowHeadPath = Path().apply {
                    moveTo(w * 0.62f, h * 0.18f)
                    lineTo(w * 0.88f, h * 0.14f)
                    lineTo(w * 0.84f, h * 0.40f)
                }
                drawPath(
                    path = arrowHeadPath,
                    color = tint,
                    style = Stroke(width = arrowStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        3 -> {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = tint,
                modifier = modifier
            )
        }
    }
}

// =============================================================================
// 🟢 2. CURVED BOTTOM BAR
// =============================================================================
@Composable
fun AmonCurvedBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    goldColor: Color,
    cardBg: Color,
    borderCol: Color,
    hasNewUpdate: Boolean = false
) {
    val tabTitles = listOf("Home", "Circle", "Stats", "Profile")

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "notch_slide"
    )

    val inactiveColor = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barHeight = 64.dp.toPx()
            val startY = size.height - barHeight
            val tabWidth = size.width / 4f

            val notchCenterX = (animatedIndex + 0.5f) * tabWidth
            val notchRadius = 34.dp.toPx()
            val shoulderWidth = 14.dp.toPx()

            val path = Path().apply {
                moveTo(0f, startY)
                val left = notchCenterX - notchRadius
                val right = notchCenterX + notchRadius

                lineTo(left - shoulderWidth, startY)
                cubicTo(left, startY, left, startY + notchRadius * 0.95f, notchCenterX, startY + notchRadius * 0.95f)
                cubicTo(right, startY + notchRadius * 0.95f, right, startY, right + shoulderWidth, startY)

                lineTo(size.width, startY)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            drawPath(path = path, color = cardBg)
            drawPath(path = path, color = borderCol, style = Stroke(width = 1.2.dp.toPx()))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / 4
            val bubbleSize = 54.dp
            val bubbleX = (tabWidth * animatedIndex) + (tabWidth / 2) - (bubbleSize / 2)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = bubbleX, y = 2.dp)
                    .size(bubbleSize)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF08080B) else Color(0xFFF8FAFC))
                    .border(2.6.dp, goldColor, CircleShape)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    SimpleNavIcon(
                        index = selectedIndex,
                        tint = goldColor,
                        cutoutColor = if (isDark) Color(0xFF08080B) else Color(0xFFF8FAFC),
                        modifier = Modifier.size(26.dp)
                    )

                    if (hasNewUpdate && selectedIndex == 3) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.5.dp, if (isDark) Color(0xFF08080B) else Color(0xFFF8FAFC), CircleShape)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = index == selectedIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(index) }
                            .padding(top = 4.dp)
                    ) {
                        if (!isSelected) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                SimpleNavIcon(
                                    index = index,
                                    tint = inactiveColor,
                                    cutoutColor = cardBg,
                                    modifier = Modifier.size(22.dp)
                                )

                                if (hasNewUpdate && index == 3) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 3.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .border(1.dp, cardBg, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = inactiveColor
                            )
                        } else {
                            Spacer(modifier = Modifier.height(26.dp))
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = goldColor
                            )
                        }
                    }
                }
            }
        }
    }
}
