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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// 🟢 1. VECTOR ICONS (HOME, STATS GROWTH, BLOCK, PROFILE)
// =============================================================================
@Composable
fun SimpleNavIcon(
    index: Int,
    tint: Color,
    modifier: Modifier = Modifier
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
            // 🟢 Stats Growth Bars Icon
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height

                val baseStroke = 1.8.dp.toPx()
                drawLine(
                    color = tint,
                    start = Offset(w * 0.08f, h * 0.90f),
                    end = Offset(w * 0.92f, h * 0.90f),
                    strokeWidth = baseStroke,
                    cap = StrokeCap.Round
                )

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
        2 -> {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Block",
                tint = tint,
                modifier = modifier
            )
        }
        else -> {
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
// 🟢 2. SEAMLESS CURVED BOTTOM BAR (4 TABS: HOME, STATS, BLOCK, PROFILE)
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
    val tabTitles = listOf("Home", "Stats", "Block", "Profile")
    val totalTabs = tabTitles.size.toFloat()

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, tabTitles.lastIndex).toFloat(),
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
            val tabWidth = size.width / totalTabs

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
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / totalTabs
            val bubbleSize = 54.dp
            val bubbleX = (tabWidth * animatedIndex) + (tabWidth / 2) - (bubbleSize / 2)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = bubbleX, y = 2.dp)
                    .size(bubbleSize)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(2.6.dp, goldColor, CircleShape)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    SimpleNavIcon(
                        index = selectedIndex,
                        tint = goldColor,
                        modifier = Modifier.size(26.dp)
                    )

                    if (hasNewUpdate && selectedIndex == 3) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.5.dp, cardBg, CircleShape)
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
