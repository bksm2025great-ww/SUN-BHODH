package com.amon.timer

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val LEVEL_TEST_PREFS = "amon_level_journey_test"
private const val KEY_LEVEL_COUNT = "level_count"
private const val KEY_COMPLETED_LEVEL = "completed_level"
private const val KEY_TEST_XP = "test_xp"

private data class JourneyRank(
    val name: String,
    val firstLevel: Int,
    val topFaceColor: Color,     // CSS: background-color
    val bottomBaseColor: Color,  // CSS: box-shadow
    val rankSymbol: String
)

private val journeyRanks = listOf(
    // Level 1: Aapka Original CSS Violet Theme
    JourneyRank(
        name = "Genesis",
        firstLevel = 1,
        topFaceColor = Color(0xFF644DFF),    // Aapke CSS ka exact #644dff
        bottomBaseColor = Color(0xFF4836BB), // Aapke CSS ka exact #4836bb
        rankSymbol = "✦"
    ),
    JourneyRank(
        name = "Steel",
        firstLevel = 11,
        topFaceColor = Color(0xFF94A3B8),
        bottomBaseColor = Color(0xFF475569),
        rankSymbol = "♞"
    ),
    JourneyRank(
        name = "Gold",
        firstLevel = 21,
        topFaceColor = Color(0xFFFFB800),
        bottomBaseColor = Color(0xFFA06D00),
        rankSymbol = "♜"
    ),
    JourneyRank(
        name = "Mint",
        firstLevel = 31,
        topFaceColor = Color(0xFF34D399),
        bottomBaseColor = Color(0xFF065F46),
        rankSymbol = "♝"
    ),
    JourneyRank(
        name = "Diamond",
        firstLevel = 41,
        topFaceColor = Color(0xFF38BDF8),
        bottomBaseColor = Color(0xFF0369A1),
        rankSymbol = "◆"
    ),
    JourneyRank(
        name = "Master",
        firstLevel = 51,
        topFaceColor = Color(0xFFA855F7),
        bottomBaseColor = Color(0xFF581C87),
        rankSymbol = "♚"
    ),
    JourneyRank(
        name = "Ruby",
        firstLevel = 61,
        topFaceColor = Color(0xFFF43F5E),
        bottomBaseColor = Color(0xFF881337),
        rankSymbol = "★"
    )
)

private fun rankForLevel(level: Int): JourneyRank =
    journeyRanks.lastOrNull { level >= it.firstLevel } ?: journeyRanks.first()

private fun levelXp(level: Int): Int = 100 + (level - 1) * 25

private fun saveLevelTestState(context: Context, count: Int, completed: Int, xp: Int) {
    context.getSharedPreferences(LEVEL_TEST_PREFS, Context.MODE_PRIVATE).edit()
        .putInt(KEY_LEVEL_COUNT, count)
        .putInt(KEY_COMPLETED_LEVEL, completed)
        .putInt(KEY_TEST_XP, xp)
        .apply()
}

// 8-step zigzag calculation
private fun getNodeXFraction(level: Int): Float {
    return when ((level - 1) % 8) {
        0 -> 0.50f
        1 -> 0.32f
        2 -> 0.20f
        3 -> 0.32f
        4 -> 0.50f
        5 -> 0.68f
        6 -> 0.80f
        7 -> 0.68f
        else -> 0.50f
    }
}

// Smooth bezier road path
private fun buildSmoothRoadPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val deltaY = (p1.y - p0.y)

        val cp1X = p0.x
        val cp1Y = p0.y + deltaY * 0.45f

        val cp2X = p1.x
        val cp2Y = p1.y - deltaY * 0.45f

        path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1.x, p1.y)
    }
    return path
}

@Composable
fun ForestScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember(context) {
        context.getSharedPreferences(LEVEL_TEST_PREFS, Context.MODE_PRIVATE)
    }

    var levelCount by remember { mutableIntStateOf(prefs.getInt(KEY_LEVEL_COUNT, 20).coerceIn(10, 100)) }
    var completedLevel by remember { mutableIntStateOf(prefs.getInt(KEY_COMPLETED_LEVEL, 0).coerceIn(0, 100)) }
    var testXp by remember { mutableIntStateOf(prefs.getInt(KEY_TEST_XP, 0).coerceAtLeast(0)) }
    var selectedLevel by remember { mutableIntStateOf(1) }
    var showLevelDetails by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val bg = Color(0xFF080C11)
    val panel = Color(0xFF111721)
    val panelBorder = Color(0xFF1E293B)
    val textMain = Color(0xFFF1F5F9)
    val textMuted = Color(0xFF64748B)

    val currentLevel = (completedLevel + 1).coerceAtMost(levelCount)
    val currentRank = rankForLevel(currentLevel)
    val xpTarget = levelXp(currentLevel)
    val xpProgress = (testXp.toFloat() / xpTarget).coerceIn(0f, 1f)

    fun persist() = saveLevelTestState(context, levelCount, completedLevel, testXp)

    val scrollState = rememberScrollState()
    val stepHeightDp = 92.dp

    LaunchedEffect(currentLevel, levelCount) {
        val targetY = ((levelCount - currentLevel) * 92).dp
        val targetPx = with(density) { targetY.toPx() }.toInt()
        scrollState.animateScrollTo(targetPx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // 1. Minimal Compact Top Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            color = panel,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, panelBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentRank.name} • Level $currentLevel",
                    color = currentRank.topFaceColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$testXp / $xpTarget XP",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Minimal Compact Quick Testing Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    levelCount = (levelCount + 10).coerceAtMost(100)
                    persist()
                },
                modifier = Modifier.weight(1f).height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = currentRank.topFaceColor),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+10 Levels", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = {
                    testXp += 100
                    val currentTarget = levelXp(currentLevel)
                    if (testXp >= currentTarget && completedLevel < levelCount) {
                        testXp -= currentTarget
                        completedLevel++
                    }
                    persist()
                },
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+100 XP", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text("Reset", fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(6.dp))

        // 2. Full Viewport Clean 3D Map (No outer frame limits, maximum scroll space)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bg)
        ) {
            val mapHeight = (levelCount * 92 + 80).dp

            Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                ) {
                    val w = constraints.maxWidth.toFloat()
                    val densityVal = density.density
                    val h = mapHeight.value * densityVal
                    val stepPx = stepHeightDp.value * densityVal
                    val bottomPadPx = 48f * densityVal

                    val points = remember(levelCount, w, h) {
                        (1..levelCount).map { level ->
                            val xFraction = getNodeXFraction(level)
                            val y = h - ((level - 1) * stepPx) - bottomPadPx
                            Offset(w * xFraction, y)
                        }
                    }

                    // Clean Dark Highway (No wide fuzzy glows)
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(bg)

                        if (points.size > 1) {
                            val baseFullPath = buildSmoothRoadPath(points)

                            // Clean solid roadbed
                            drawPath(
                                baseFullPath,
                                Color(0xFF0E1520),
                                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                baseFullPath,
                                Color(0xFF1E293B),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                baseFullPath,
                                Color(0xFF334155),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Unlocked clean path
                            if (currentLevel > 1) {
                                val completedPoints = points.take(currentLevel)
                                val completedPath = buildSmoothRoadPath(completedPoints)

                                drawPath(
                                    completedPath,
                                    currentRank.topFaceColor,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                drawPath(
                                    completedPath,
                                    Color.White.copy(alpha = 0.9f),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            // Active progress pulse line
                            if (currentLevel < levelCount && xpProgress > 0.01f) {
                                val currentSegmentPoints = listOf(points[currentLevel - 1], points[currentLevel])
                                val rawSegment = buildSmoothRoadPath(currentSegmentPoints)

                                val pathMeasure = PathMeasure()
                                pathMeasure.setPath(rawSegment, false)
                                val progressiveSegment = Path()
                                val targetLength = pathMeasure.length * xpProgress
                                pathMeasure.getSegment(0f, targetLength, progressiveSegment, true)

                                drawPath(
                                    progressiveSegment,
                                    currentRank.topFaceColor,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                drawPath(
                                    progressiveSegment,
                                    Color.White,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )

                                // Tip node spark
                                val t = xpProgress
                                val u = 1f - t
                                val p0 = currentSegmentPoints[0]
                                val p3 = currentSegmentPoints[1]
                                val deltaY = p3.y - p0.y
                                val p1 = Offset(p0.x, p0.y + deltaY * 0.45f)
                                val p2 = Offset(p3.x, p3.y - deltaY * 0.45f)

                                val tipX = u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x
                                val tipY = u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y
                                drawCircle(currentRank.topFaceColor, radius = 5.dp.toPx(), center = Offset(tipX, tipY))
                                drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(tipX, tipY))
                            }
                        }
                    }

                    // 3. Perfect 3D Circular Buttons (No bottom labels, no floating rings)
                    val buttonSize = 56.dp
                    (1..levelCount).forEach { level ->
                        val point = points[level - 1]
                        val isComplete = level <= completedLevel
                        val isCurrent = level == currentLevel
                        val rank = rankForLevel(level)

                        val buttonLeft = with(density) { point.x.toDp() } - (buttonSize / 2)
                        val buttonTop = with(density) { point.y.toDp() } - (buttonSize / 2)

                        Box(
                            modifier = Modifier
                                .offset(x = buttonLeft, y = buttonTop)
                                .size(buttonSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Uiverse3DMechanicalButton(
                                isComplete = isComplete,
                                isCurrent = isCurrent,
                                rank = rank,
                                buttonSize = buttonSize,
                                onClick = {
                                    selectedLevel = level
                                    showLevelDetails = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLevelDetails) {
        val rank = rankForLevel(selectedLevel)
        val status = when {
            selectedLevel <= completedLevel -> "COMPLETED"
            selectedLevel == currentLevel -> "ACTIVE STAGE"
            else -> "LOCKED"
        }
        AlertDialog(
            onDismissRequest = { showLevelDetails = false },
            containerColor = panel,
            titleContentColor = textMain,
            textContentColor = textMuted,
            title = {
                Text("Level $selectedLevel • ${rank.name}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(status, color = rank.topFaceColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Target: ${levelXp(selectedLevel)} XP")
                    Text(
                        if (selectedLevel <= completedLevel) "Trophy unlocked for this level!"
                        else if (selectedLevel == currentLevel) "Current focus target: Complete sessions to unlock next path."
                        else "Reach this stage by finishing preceding levels."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelDetails = false }) { Text("Close", color = currentRank.topFaceColor) }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = panel,
            title = { Text("Reset Progress?", color = textMain) },
            text = { Text("This will reset back to Level 1 and 0 XP.", color = textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    levelCount = 20
                    completedLevel = 0
                    testXp = 0
                    selectedLevel = 1
                    persist()
                    showResetDialog = false
                }) { Text("Reset", color = Color(0xFFFF6B6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = Color.White) }
            }
        )
    }
}

/**
 * 🎯 Exact Uiverse 3D Mechanical Button Replica
 * - Perfect 56dp circle puck (No oval distortion)
 * - Solid bottom 3D base (box-shadow equivalent)
 * - 0.1s instant mechanical sink on press
 * - Clean 1px matching border (No white border, no floating rings)
 */
@Composable
private fun Uiverse3DMechanicalButton(
    isComplete: Boolean,
    isCurrent: Boolean,
    rank: JourneyRank,
    buttonSize: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowDepth = 7.dp

    // CSS: transition: all .1s ease
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) shadowDepth else 0.dp,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "uiverse_press_anim"
    )

    // Shadow collapse on active press
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "uiverse_shadow_anim"
    )

    val faceColor = when {
        isCurrent || isComplete -> rank.topFaceColor
        else -> Color(0xFF1E293B)
    }

    val baseColor = when {
        isCurrent || isComplete -> rank.bottomBaseColor
        else -> Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier
            .size(width = buttonSize, height = buttonSize + shadowDepth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Solid 3D Base (box-shadow)
        if (shadowAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .offset(y = shadowDepth)
                    .size(buttonSize)
                    .graphicsLayer { alpha = shadowAlpha }
                    .clip(CircleShape)
                    .background(baseColor)
                    .border(1.dp, baseColor, CircleShape)
            )
        }

        // 2. Moving Circular Face
        Box(
            modifier = Modifier
                .offset(y = pressOffsetY)
                .size(buttonSize)
                .clip(CircleShape)
                .background(faceColor)
                .border(1.dp, baseColor, CircleShape), // Clean matching 1px border
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isComplete || isCurrent) "🏆" else "🔒",
                fontSize = 20.sp
            )
        }
    }
}
