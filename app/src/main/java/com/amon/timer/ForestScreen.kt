package com.amon.timer

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val color: Color,
    val darkBaseColor: Color,
    val icon: String
)

private val journeyRanks = listOf(
    JourneyRank("Bronze", 1, Color(0xFFCD8B56), Color(0xFF7A4A25), "♟"),
    JourneyRank("Silver", 11, Color(0xFFC5D0D8), Color(0xFF6B7985), "♞"),
    JourneyRank("Gold", 21, Color(0xFFFFD166), Color(0xFF9E771B), "♜"),
    JourneyRank("Platinum", 31, Color(0xFF70D8C2), Color(0xFF287C6A), "♝"),
    JourneyRank("Emerald", 41, Color(0xFF55D68B), Color(0xFF1B7A42), "♛"),
    JourneyRank("Diamond", 51, Color(0xFF6CCBFF), Color(0xFF206796), "◆"),
    JourneyRank("Master", 61, Color(0xFFB6A0FF), Color(0xFF5C47A8), "♚"),
    JourneyRank("Grandmaster", 71, Color(0xFFFF7D9B), Color(0xFF9E2A44), "★")
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

/**
 * डुओलिंगो ज़िग-ज़ैग गणित: सेंटर -> बायाँ -> सेंटर -> दायाँ -> सेंटर
 */
private fun getNodeXFraction(level: Int): Float {
    return when ((level - 1) % 8) {
        0 -> 0.50f
        1 -> 0.30f
        2 -> 0.20f
        3 -> 0.30f
        4 -> 0.50f
        5 -> 0.70f
        6 -> 0.80f
        7 -> 0.70f
        else -> 0.50f
    }
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

    val bg = Color(0xFF0B1118)
    val panel = Color(0xFF141E29)
    val panelBorder = Color(0xFF293747)
    val textMain = Color(0xFFF2F6FA)
    val textMuted = Color(0xFF9BAABD)
    val accent = Color(0xFF79D7A4)
    val currentLevel = (completedLevel + 1).coerceAtMost(levelCount)
    val currentRank = rankForLevel(currentLevel)
    val xpTarget = levelXp(currentLevel)
    val xpProgress = (testXp.toFloat() / xpTarget).coerceIn(0f, 1f)

    fun persist() = saveLevelTestState(context, levelCount, completedLevel, testXp)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "AMON  /  FOCUS JOURNEY",
                color = accent,
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
                text = "Duolingo-grade path progression system",
                color = textMuted,
                fontSize = 11.5.sp
            )
        }

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
                            .background(currentRank.color.copy(alpha = 0.16f))
                            .border(1.dp, currentRank.color.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentRank.icon, color = currentRank.color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${currentRank.name} League", color = currentRank.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Level $currentLevel • Next milestone", color = textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$testXp XP", color = Color(0xFFFFD166), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = xpProgress,
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = currentRank.color,
                    trackColor = Color(0xFF293747)
                )
                Spacer(Modifier.height(6.dp))
                Text("$testXp / $xpTarget XP for current level", color = textMuted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

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
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF092014)),
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(1.2.dp, panelBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF0D141C))
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

                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF0D141C))

                        for (dotY in 24..size.height.toInt() step 48) {
                            for (dotX in 16..size.width.toInt() step 32) {
                                drawCircle(Color.White.copy(alpha = 0.025f), radius = 1.2.dp.toPx(), center = Offset(dotX.toFloat(), dotY.toFloat()))
                            }
                        }

                        if (points.size > 1) {
                            val fullPath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.zipWithNext().forEach { (a, b) ->
                                    val midY = (a.y + b.y) / 2f
                                    cubicTo(a.x, midY, b.x, midY, b.x, b.y)
                                }
                            }

                            drawPath(fullPath, Color(0xFF141F2B), style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(fullPath, Color(0xFF223141), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(fullPath, Color(0xFF2D4054), style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))

                            if (completedLevel > 0) {
                                val completedPoints = points.take((completedLevel + 1).coerceAtMost(levelCount))
                                if (completedPoints.size > 1) {
                                    val completedPath = Path().apply {
                                        moveTo(completedPoints.first().x, completedPoints.first().y)
                                        completedPoints.zipWithNext().forEach { (a, b) ->
                                            val midY = (a.y + b.y) / 2f
                                            cubicTo(a.x, midY, b.x, midY, b.x, b.y)
                                        }
                                    }
                                    drawPath(completedPath, currentRank.color.copy(alpha = 0.4f), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))
                                    drawPath(completedPath, currentRank.color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                                }
                            }
                        }
                    }

                    (1..levelCount).forEach { level ->
                        val point = points[level - 1]
                        val isComplete = level <= completedLevel
                        val isCurrent = level == currentLevel
                        val isMilestone = (level % 4 == 0)
                        val rank = rankForLevel(level)

                        val nodeWidth = if (isMilestone) 68.dp else (if (isCurrent) 58.dp else 52.dp)
                        val nodeHeight = if (isMilestone) 56.dp else (if (isCurrent) 58.dp else 52.dp)

                        val nodeLeft = with(density) { point.x.toDp() } - (nodeWidth / 2)
                        val nodeTop = with(density) { point.y.toDp() } - (nodeHeight / 2)

                        Box(
                            modifier = Modifier
                                .offset(x = nodeLeft, y = nodeTop)
                                .size(width = nodeWidth, height = nodeHeight + 8.dp)
                                .clickable {
                                    selectedLevel = level
                                    showLevelDetails = true
                                },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(nodeWidth * pulseScale)
                                        .clip(CircleShape)
                                        .background(rank.color.copy(alpha = pulseAlpha))
                                )
                            }

                            Duolingo3DNode(
                                level = level,
                                isComplete = isComplete,
                                isCurrent = isCurrent,
                                isMilestone = isMilestone,
                                rank = rank,
                                width = nodeWidth,
                                height = nodeHeight
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLevelDetails) {
        val rank = rankForLevel(selectedLevel)
        val isMilestone = (selectedLevel % 4 == 0)
        val status = when {
            selectedLevel <= completedLevel -> "COMPLETED"
            selectedLevel == currentLevel -> "CURRENT LEVEL"
            else -> "LOCKED"
        }
        AlertDialog(
            onDismissRequest = { showLevelDetails = false },
            containerColor = panel,
            titleContentColor = textMain,
            textContentColor = textMuted,
            title = {
                Text(if (isMilestone) "🎁 Milestone Chest • Level $selectedLevel" else "Level $selectedLevel • ${rank.name}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status, color = rank.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("XP Target: ${levelXp(selectedLevel)} XP")
                    Text(
                        if (isMilestone) "Reward: Streak Shield + Bonus ${rank.name} Journey points"
                        else "Reward: ${rank.name} League Points"
                    )
                    Text(
                        if (selectedLevel <= completedLevel) "This checkpoint is completed!"
                        else if (selectedLevel == currentLevel) "Focus and earn XP to complete this milestone."
                        else "Complete earlier levels to unlock."
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

@Composable
private fun Duolingo3DNode(
    level: Int,
    isComplete: Boolean,
    isCurrent: Boolean,
    isMilestone: Boolean,
    rank: JourneyRank,
    width: Dp,
    height: Dp
) {
    val depthOffset = 4.dp
    val shape = if (isMilestone) RoundedCornerShape(16.dp) else CircleShape

    val faceColor = when {
        isComplete -> Color(0xFF55D68B)
        isCurrent -> rank.color
        else -> Color(0xFF233240)
    }

    val baseColor = when {
        isComplete -> Color(0xFF2A8751)
        isCurrent -> rank.darkBaseColor
        else -> Color(0xFF131D26)
    }

    Box(
        modifier = Modifier.size(width = width, height = height + depthOffset),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset(y = depthOffset)
                .size(width = width, height = height)
                .clip(shape)
                .background(baseColor)
        )

        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .clip(shape)
                .background(faceColor)
                .border(
                    width = if (isCurrent) 2.6.dp else 1.2.dp,
                    color = if (isCurrent) Color.White else Color.White.copy(alpha = if (isComplete) 0.35f else 0.12f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isMilestone) {
                Text(
                    text = if (isComplete) "🎁✓" else "🎁",
                    fontSize = 21.sp
                )
            } else {
                Text(
                    text = when {
                        isComplete -> "✓"
                        isCurrent -> "$level"
                        else -> "🔒"
                    },
                    color = if (isComplete || isCurrent) Color(0xFF091710) else Color(0xFF8697A8),
                    fontSize = if (isCurrent) 16.sp else 13.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
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
        border = BorderStroke(1.dp, Color(0xFF293747))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, color = Color(0xFF9BAABD), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
