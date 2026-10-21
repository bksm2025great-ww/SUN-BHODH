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
import androidx.compose.ui.graphics.StrokeCap
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
    val color: Color,
    val darkBaseColor: Color,
    val glowBorderColor: Color,
    val icon: String
)

private val journeyRanks = listOf(
    JourneyRank("Bronze", 1, Color(0xFFCD8B56), Color(0xFF6E4322), Color(0xFFE8A87C), "♟"),
    JourneyRank("Silver", 11, Color(0xFFC5D0D8), Color(0xFF5A6977), Color(0xFFE2E8F0), "♞"),
    JourneyRank("Gold", 21, Color(0xFFFFD166), Color(0xFF9E771B), Color(0xFFFFE299), "♜"),
    JourneyRank("Platinum", 31, Color(0xFF70D8C2), Color(0xFF236E5E), Color(0xFFA6EFE0), "♝"),
    JourneyRank("Emerald", 41, Color(0xFF55D68B), Color(0xFF1B733E), Color(0xFF95EBB7), "♛"),
    JourneyRank("Diamond", 51, Color(0xFF6CCBFF), Color(0xFF1B5982), Color(0xFFA8E2FF), "◆"),
    JourneyRank("Master", 61, Color(0xFFB6A0FF), Color(0xFF503A99), Color(0xFFD6C9FF), "♚"),
    JourneyRank("Grandmaster", 71, Color(0xFFFF7D9B), Color(0xFF8C2037), Color(0xFFFFB5C6), "★")
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
 * डुओलिंगो स्मूथ 8-स्टेप ज़िग-ज़ैग गणित
 */
private fun getNodeXFraction(level: Int): Float {
    return when ((level - 1) % 8) {
        0 -> 0.50f
        1 -> 0.32f
        2 -> 0.22f
        3 -> 0.32f
        4 -> 0.50f
        5 -> 0.68f
        6 -> 0.78f
        7 -> 0.68f
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

    // एक्टिव लेवल के लिए सांस लेता हुआ ऑरा
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val scrollState = rememberScrollState()
    val stepHeightDp = 88.dp

    LaunchedEffect(currentLevel, levelCount) {
        val targetY = ((levelCount - currentLevel) * 88).dp
        val targetPx = with(density) { targetY.toPx() }.toInt()
        scrollState.animateScrollTo(targetPx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // शीर्ष हेडर
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

        // स्टेट्स बार
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

        // लीग बैनर
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

        // टेस्टिंग कंट्रोल्स
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

        // 3D इंटरैक्टिव मैप बॉक्स
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(1.2.dp, panelBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF0D141C))
        ) {
            val mapHeight = (levelCount * 88 + 90).dp

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

                    // 1. मास्टर कोऑर्डिनेट पॉइंट्स
                    val points = remember(levelCount, w, h) {
                        (1..levelCount).map { level ->
                            val xFraction = getNodeXFraction(level)
                            val y = h - ((level - 1) * stepPx) - bottomPadPx
                            Offset(w * xFraction, y)
                        }
                    }

                    // 2. कैनवास: 3D स्तरित सड़क
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

                            // अंडर-शैडो और मेन सड़क
                            drawPath(fullPath, Color(0xFF141F2B), style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(fullPath, Color(0xFF223141), style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(fullPath, Color(0xFF2D4054), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                            // पूर्ण हो चुका चमकीला ट्रैक
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
                                    drawPath(completedPath, currentRank.color.copy(alpha = 0.35f), style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
                                    drawPath(completedPath, currentRank.color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                                }
                            }
                        }
                    }

                    // 3. 3D स्प्रिंग-फ़िज़िक्स नोड्स
                    (1..levelCount).forEach { level ->
                        val point = points[level - 1]
                        val isComplete = level <= completedLevel
                        val isCurrent = level == currentLevel
                        val rank = rankForLevel(level)

                        val nodeSize = if (isCurrent) 60.dp else 54.dp
                        val nodeLeft = with(density) { point.x.toDp() } - (nodeSize / 2)
                        val nodeTop = with(density) { point.y.toDp() } - (nodeSize / 2)

                        Box(
                            modifier = Modifier
                                .offset(x = nodeLeft, y = nodeTop)
                                .size(nodeSize + 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(nodeSize * pulseScale)
                                        .clip(CircleShape)
                                        .background(rank.color.copy(alpha = pulseAlpha))
                                )
                            }

                            SpringPhysicsLevelNode(
                                level = level,
                                isComplete = isComplete,
                                isCurrent = isCurrent,
                                rank = rank,
                                nodeSize = nodeSize,
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

    // लेवल डिटेल्स डायलॉग
    if (showLevelDetails) {
        val rank = rankForLevel(selectedLevel)
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
                Text("Level $selectedLevel • ${rank.name}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status, color = rank.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("XP Target: ${levelXp(selectedLevel)} XP")
                    Text("League Theme: ${rank.name}")
                    Text(
                        if (selectedLevel <= completedLevel) "This level is already unlocked and mastered!"
                        else if (selectedLevel == currentLevel) "Active focus level! Earn XP to advance."
                        else "Reach this checkpoint by mastering earlier levels."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLevelDetails = false }) { Text("Close", color = accent) }
            }
        )
    }

    // रीसेट प्रोग्रेस डायलॉग
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
 * 🎴 असली स्प्रिंग फ़िज़िक्स (Duolingo-grade Spring Bounce) + 3D रून नोड
 */
@Composable
private fun SpringPhysicsLevelNode(
    level: Int,
    isComplete: Boolean,
    isCurrent: Boolean,
    rank: JourneyRank,
    nodeSize: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 🌊 असली स्प्रिंग फ़िज़िक्स: छूते ही दबेगा और छोड़ते ही जेली जैसा बाउंस होगा
    val springScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "node_bounce"
    )

    val depthOffset = if (isPressed) 1.5.dp else 4.dp

    // कलर्स: लॉक्ड = डोर्मेंट स्लेट स्टोन, कम्प्लीटेड/एक्टिव = लीग मेटैलिक थीम
    val faceColor = when {
        isCurrent -> rank.color
        isComplete -> rank.color.copy(alpha = 0.90f)
        else -> Color(0xFF1B242F) // डोर्मेंट डार्क स्लेट स्टोन
    }

    val baseColor = when {
        isCurrent -> rank.darkBaseColor
        isComplete -> rank.darkBaseColor
        else -> Color(0xFF0F151C) // गहरा स्टोन शैडो
    }

    val numberColor = when {
        isCurrent -> Color(0xFF09141B)
        isComplete -> Color(0xFF09141B)
        else -> Color(0xFF4C6175) // पत्थर में तराशा हुआ डार्क नंबर
    }

    Box(
        modifier = Modifier
            .size(nodeSize)
            .graphicsLayer {
                scaleX = springScale
                scaleY = springScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. 3D एक्सट्रूडेड बेस (गहराई)
        Box(
            modifier = Modifier
                .offset(y = depthOffset)
                .size(nodeSize)
                .clip(CircleShape)
                .background(baseColor)
        )

        // 2. टॉप फेस प्लेट (3D बेवेल और ग्लो के साथ)
        Box(
            modifier = Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(faceColor)
                .border(
                    width = if (isCurrent) 2.5.dp else (if (isComplete) 1.8.dp else 1.dp),
                    color = when {
                        isCurrent -> Color.White
                        isComplete -> rank.glowBorderColor
                        else -> Color(0xFF283646) // हल्का स्टोन एज
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$level",
                color = numberColor,
                fontSize = if (isCurrent) 17.sp else 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
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
        border = BorderStroke(1.dp, Color(0xFF293747))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, color = Color(0xFF9BAABD), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
