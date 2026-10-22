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
import androidx.compose.ui.graphics.Brush
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
    val materialName: String,
    val firstLevel: Int,
    val topFaceColor: Color,     // CSS: background-color
    val bottomBaseColor: Color,  // CSS: box-shadow color
    val glowBorderColor: Color,
    val rankSymbol: String
)

private val journeyRanks = listOf(
    JourneyRank(
        name = "Bronze",
        materialName = "Rustic Copper",
        firstLevel = 1,
        topFaceColor = Color(0xFFD97736),
        bottomBaseColor = Color(0xFF7A3811),
        glowBorderColor = Color(0xFFFFB07E),
        rankSymbol = "♟"
    ),
    JourneyRank(
        name = "Silver",
        materialName = "Forged Steel",
        firstLevel = 11,
        topFaceColor = Color(0xFFCBD5E1),
        bottomBaseColor = Color(0xFF475569),
        glowBorderColor = Color(0xFFF8FAFC),
        rankSymbol = "♞"
    ),
    JourneyRank(
        name = "Gold",
        materialName = "Royal Gold",
        firstLevel = 21,
        topFaceColor = Color(0xFFFFB800),
        bottomBaseColor = Color(0xFFA06D00),
        glowBorderColor = Color(0xFFFFE382),
        rankSymbol = "♜"
    ),
    JourneyRank(
        name = "Platinum",
        materialName = "Mint Titanium",
        firstLevel = 31,
        topFaceColor = Color(0xFF70D8C2),
        bottomBaseColor = Color(0xFF1B594D),
        glowBorderColor = Color(0xFFAEF7E8),
        rankSymbol = "♝"
    ),
    JourneyRank(
        name = "Emerald",
        materialName = "Forest Jade",
        firstLevel = 41,
        topFaceColor = Color(0xFF55D68B),
        bottomBaseColor = Color(0xFF186637),
        glowBorderColor = Color(0xFF99F3BD),
        rankSymbol = "♛"
    ),
    JourneyRank(
        name = "Diamond",
        materialName = "Celestial Diamond",
        firstLevel = 51,
        topFaceColor = Color(0xFF6CCBFF),
        bottomBaseColor = Color(0xFF1B557A),
        glowBorderColor = Color(0xFFBCE6FF),
        rankSymbol = "◆"
    ),
    JourneyRank(
        name = "Master",
        materialName = "Mystic Obsidian",
        firstLevel = 61,
        topFaceColor = Color(0xFFB6A0FF),
        bottomBaseColor = Color(0xFF4E3794),
        glowBorderColor = Color(0xFFDFD4FF),
        rankSymbol = "♚"
    ),
    JourneyRank(
        name = "Grandmaster",
        materialName = "Crimson Ruby",
        firstLevel = 71,
        topFaceColor = Color(0xFFFF7D9B),
        bottomBaseColor = Color(0xFF7A1D31),
        glowBorderColor = Color(0xFFFFBAC7),
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

private fun getNodeXFraction(level: Int): Float {
    return when ((level - 1) % 8) {
        0 -> 0.50f
        1 -> 0.33f
        2 -> 0.23f
        3 -> 0.33f
        4 -> 0.50f
        5 -> 0.67f
        6 -> 0.77f
        7 -> 0.67f
        else -> 0.50f
    }
}

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

    val bg = Color(0xFF091017)
    val panel = Color(0xFF131D27)
    val panelBorder = Color(0xFF243444)
    val textMain = Color(0xFFF2F6FA)
    val textMuted = Color(0xFF8FA1B5)
    val accent = Color(0xFFFFB800)
    val currentLevel = (completedLevel + 1).coerceAtMost(levelCount)
    val currentRank = rankForLevel(currentLevel)
    val xpTarget = levelXp(currentLevel)
    val xpProgress = (testXp.toFloat() / xpTarget).coerceIn(0f, 1f)

    fun persist() = saveLevelTestState(context, levelCount, completedLevel, testXp)

    val infiniteTransition = rememberInfiniteTransition(label = "bloom_anim")
    val bloomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bloom_scale"
    )
    val bloomAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bloom_alpha"
    )

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
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "AMON  /  FOCUS JOURNEY",
                color = currentRank.topFaceColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Your Level Path",
                color = textMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Material: ${currentRank.materialName}",
                color = textMuted,
                fontSize = 11.5.sp
            )
        }

        // Stats Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JourneyStat("LEVELS", levelCount.toString(), panel, textMain, Modifier.weight(1f))
            JourneyStat("COMPLETED", completedLevel.toString(), panel, accent, Modifier.weight(1f))
            JourneyStat("TEST XP", testXp.toString(), panel, Color(0xFFFFD166), Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))

        // League Milestone Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = panel,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, panelBorder)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentRank.topFaceColor.copy(alpha = 0.16f))
                            .border(1.dp, currentRank.topFaceColor.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentRank.rankSymbol, color = currentRank.topFaceColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${currentRank.name} League", color = currentRank.topFaceColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Level $currentLevel • Next milestone", color = textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$testXp XP", color = Color(0xFFFFD166), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = xpProgress,
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = currentRank.topFaceColor,
                    trackColor = Color(0xFF243444)
                )
                Spacer(Modifier.height(6.dp))
                Text("$testXp / $xpTarget XP for current level", color = textMuted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Action Testing Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    levelCount = (levelCount + 10).coerceAtMost(100)
                    persist()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = currentRank.topFaceColor, contentColor = Color(0xFF09141B)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+10 Levels", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

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
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("+100 XP", fontSize = 12.sp) }

            OutlinedButton(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Reset", fontSize = 12.sp) }
        }

        Spacer(Modifier.height(8.dp))

        // 3D Map Viewport (No Grid Dots - Clean Matte)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(1.2.dp, panelBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF0B121A))
        ) {
            val mapHeight = (levelCount * 92 + 90).dp

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
                    val bottomPadPx = 54f * densityVal

                    val points = remember(levelCount, w, h) {
                        (1..levelCount).map { level ->
                            val xFraction = getNodeXFraction(level)
                            val y = h - ((level - 1) * stepPx) - bottomPadPx
                            Offset(w * xFraction, y)
                        }
                    }

                    // Road Canvas
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF0B121A))

                        if (points.size > 1) {
                            val baseFullPath = buildSmoothRoadPath(points)

                            // 3D Road Base Layers
                            drawPath(baseFullPath, Color(0xFF070B10), style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(baseFullPath, Color(0xFF16212D), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(baseFullPath, Color(0xFF223142), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            drawPath(baseFullPath, Color(0xFF2D3E52), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                            // Completed Energy Path
                            if (currentLevel > 1) {
                                val completedPoints = points.take(currentLevel)
                                val completedPath = buildSmoothRoadPath(completedPoints)

                                drawPath(completedPath, currentRank.topFaceColor.copy(alpha = 0.28f), style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                drawPath(completedPath, currentRank.topFaceColor.copy(alpha = 0.70f), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                drawPath(completedPath, Color.White.copy(alpha = 0.95f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }

                            // Live XP Progressive Beam
                            if (currentLevel < levelCount && xpProgress > 0.01f) {
                                val currentSegmentPoints = listOf(points[currentLevel - 1], points[currentLevel])
                                val rawSegment = buildSmoothRoadPath(currentSegmentPoints)

                                val pathMeasure = PathMeasure()
                                pathMeasure.setPath(rawSegment, false)
                                val progressiveSegment = Path()
                                val targetLength = pathMeasure.length * xpProgress
                                pathMeasure.getSegment(0f, targetLength, progressiveSegment, true)

                                drawPath(progressiveSegment, currentRank.topFaceColor.copy(alpha = 0.35f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                drawPath(progressiveSegment, currentRank.topFaceColor, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                drawPath(progressiveSegment, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                                val t = xpProgress
                                val u = 1f - t
                                val p0 = currentSegmentPoints[0]
                                val p3 = currentSegmentPoints[1]
                                val deltaY = p3.y - p0.y
                                val p1 = Offset(p0.x, p0.y + deltaY * 0.45f)
                                val p2 = Offset(p3.x, p3.y - deltaY * 0.45f)

                                val tipX = u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x
                                val tipY = u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y
                                val tipOffset = Offset(tipX, tipY)

                                drawCircle(currentRank.topFaceColor, radius = 6.dp.toPx(), center = tipOffset)
                                drawCircle(Color.White, radius = 3.dp.toPx(), center = tipOffset)
                            }
                        }
                    }

                    // CSS-Styled 3D Push Button Nodes
                    (1..levelCount).forEach { level ->
                        val point = points[level - 1]
                        val isComplete = level <= completedLevel
                        val isCurrent = level == currentLevel
                        val rank = rankForLevel(level)

                        val nodeSize = if (isCurrent) 60.dp else 54.dp
                        val containerSize = nodeSize + 36.dp

                        val containerLeft = with(density) { point.x.toDp() } - (containerSize / 2)
                        val containerTop = with(density) { point.y.toDp() } - (containerSize / 2)

                        Box(
                            modifier = Modifier
                                .offset(x = containerLeft, y = containerTop)
                                .size(containerSize),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(nodeSize * 2.2f * bloomScale)
                                        .graphicsLayer { alpha = bloomAlpha }
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    rank.topFaceColor.copy(alpha = 0.70f),
                                                    rank.topFaceColor.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(nodeSize * 1.34f)
                                        .border(2.dp, rank.glowBorderColor.copy(alpha = 0.85f), CircleShape)
                                )
                            }

                            // 🚀 Pure CSS Logic 3D Push Button
                            Css3DMechanicalButton(
                                isComplete = isComplete,
                                isCurrent = isCurrent,
                                rank = rank,
                                nodeSize = nodeSize,
                                onClick = {
                                    selectedLevel = level
                                    showLevelDetails = true
                                }
                            )

                            Text(
                                text = "LV $level",
                                color = if (isCurrent) Color.White else (if (isComplete) rank.topFaceColor else Color(0xFF55687C)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.offset(y = (nodeSize / 2) + 8.dp)
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
            selectedLevel == currentLevel -> "CURRENT ACTIVE LEVEL"
            else -> "LOCKED STAGE"
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status, color = rank.topFaceColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("League Material: ${rank.materialName}")
                    Text("XP Target: ${levelXp(selectedLevel)} XP")
                    Text(
                        if (selectedLevel <= completedLevel) "This level has been conquered! Trophy claimed."
                        else if (selectedLevel == currentLevel) "Active Focus Level: Complete sessions to ignite the path to the next level."
                        else "Reach this stage by completing preceding levels."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelDetails = false }) { Text("Close", color = accent) }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = panel,
            title = { Text("Reset test progress?", color = textMain) },
            text = { Text("This resets level count to 20 and clears test XP and completed checkpoints.", color = textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    levelCount = 20
                    completedLevel = 0
                    testXp = 0
                    selectedLevel = 1
                    persist()
                    showResetDialog = false
                }) { Text("Reset", color = Color(0xFFFF8B8B)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = accent) }
            }
        )
    }
}

/**
 * 🎯 Pure CSS Replica:
 * - .container: box-shadow: 0 .5em #baseColor; transition: all .1s ease;
 * - .container:has(.button:active): box-shadow: none; margin-top: .5em;
 */
@Composable
private fun Css3DMechanicalButton(
    isComplete: Boolean,
    isCurrent: Boolean,
    rank: JourneyRank,
    nodeSize: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowDepth = 6.dp

    // CSS: transition: all .1s ease
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) shadowDepth else 0.dp,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "css_press_offset"
    )

    val currentShadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "css_shadow_fade"
    )

    val faceColor = when {
        isCurrent || isComplete -> rank.topFaceColor
        else -> Color(0xFF243242)
    }

    val baseColor = when {
        isCurrent || isComplete -> rank.bottomBaseColor
        else -> Color(0xFF131B24)
    }

    Box(
        modifier = Modifier
            .size(nodeSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. CSS: box-shadow: 0 .5em #baseColor
        if (currentShadowAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .offset(y = shadowDepth)
                    .size(nodeSize)
                    .graphicsLayer { alpha = currentShadowAlpha }
                    .clip(CircleShape)
                    .background(baseColor)
                    .border(1.dp, baseColor, CircleShape)
            )
        }

        // 2. CSS: .container (top face with 1px border) + active margin-top
        Box(
            modifier = Modifier
                .offset(y = pressOffsetY)
                .size(nodeSize)
                .clip(CircleShape)
                .background(faceColor)
                .border(
                    width = if (isCurrent) 2.dp else 1.2.dp,
                    color = when {
                        isCurrent -> Color.White
                        isComplete -> rank.glowBorderColor
                        else -> baseColor
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isComplete || isCurrent) "🏆" else "🔒",
                fontSize = if (isCurrent) 22.sp else 19.sp
            )
        }
    }
}

@Composable
private fun JourneyStat(
    label: String,
    value: String,
    background: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = background,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF243444))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, color = Color(0xFF8FA1B5), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
