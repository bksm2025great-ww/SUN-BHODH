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
    val primaryColor: Color,
    val darkBaseColor: Color,
    val glowBorderColor: Color,
    val flameOuterColor: Color,
    val flameInnerColor: Color,
    val icon: String
)

private val journeyRanks = listOf(
    JourneyRank(
        name = "Bronze",
        materialName = "Woodland Timber",
        firstLevel = 1,
        primaryColor = Color(0xFFCD8B56),
        darkBaseColor = Color(0xFF5C381E),
        glowBorderColor = Color(0xFFE5A675),
        flameOuterColor = Color(0xFFFF9E58),
        flameInnerColor = Color(0xFFFFE5B4),
        icon = "♟"
    ),
    JourneyRank(
        name = "Silver",
        materialName = "Forged Iron",
        firstLevel = 11,
        primaryColor = Color(0xFFCBD5E1),
        darkBaseColor = Color(0xFF475569),
        glowBorderColor = Color(0xFFF1F5F9),
        flameOuterColor = Color(0xFF93C5FD),
        flameInnerColor = Color(0xFFFFFFFF),
        icon = "♞"
    ),
    JourneyRank(
        name = "Gold",
        materialName = "Solar Amber Gold",
        firstLevel = 21,
        primaryColor = Color(0xFFFFD166),
        darkBaseColor = Color(0xFF997316),
        glowBorderColor = Color(0xFFFFE8A3),
        flameOuterColor = Color(0xFFFFA000),
        flameInnerColor = Color(0xFFFFFDE7),
        icon = "♜"
    ),
    JourneyRank(
        name = "Platinum",
        materialName = "Frosted Mint Titanium",
        firstLevel = 31,
        primaryColor = Color(0xFF70D8C2),
        darkBaseColor = Color(0xFF1B594D),
        glowBorderColor = Color(0xFFAEF7E8),
        flameOuterColor = Color(0xFF00FFC2),
        flameInnerColor = Color(0xFFE6FFFA),
        icon = "♝"
    ),
    JourneyRank(
        name = "Emerald",
        materialName = "Deep Forest Jade",
        firstLevel = 41,
        primaryColor = Color(0xFF55D68B),
        darkBaseColor = Color(0xFF186637),
        glowBorderColor = Color(0xFF99F3BD),
        flameOuterColor = Color(0xFF22C55E),
        flameInnerColor = Color(0xFFDCFCE7),
        icon = "♛"
    ),
    JourneyRank(
        name = "Diamond",
        materialName = "Celestial Diamond",
        firstLevel = 51,
        primaryColor = Color(0xFF6CCBFF),
        darkBaseColor = Color(0xFF1B557A),
        glowBorderColor = Color(0xFFBCE6FF),
        flameOuterColor = Color(0xFF38BDF8),
        flameInnerColor = Color(0xFFF0F9FF),
        icon = "◆"
    ),
    JourneyRank(
        name = "Master",
        materialName = "Mystic Obsidian",
        firstLevel = 61,
        primaryColor = Color(0xFFB6A0FF),
        darkBaseColor = Color(0xFF4E3794),
        glowBorderColor = Color(0xFFDFD4FF),
        flameOuterColor = Color(0xFFA855F7),
        flameInnerColor = Color(0xFFFAF5FF),
        icon = "♚"
    ),
    JourneyRank(
        name = "Grandmaster",
        materialName = "Crimson Ruby Star",
        firstLevel = 71,
        primaryColor = Color(0xFFFF7D9B),
        darkBaseColor = Color(0xFF7A1D31),
        glowBorderColor = Color(0xFFFFBAC7),
        flameOuterColor = Color(0xFFF43F5E),
        flameInnerColor = Color(0xFFFFF1F2),
        icon = "★"
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

    val bg = Color(0xFF091017)
    val panel = Color(0xFF131D27)
    val panelBorder = Color(0xFF243444)
    val textMain = Color(0xFFF2F6FA)
    val textMuted = Color(0xFF8FA1B5)
    val accent = Color(0xFF70D8C2)
    val currentLevel = (completedLevel + 1).coerceAtMost(levelCount)
    val currentRank = rankForLevel(currentLevel)
    val xpTarget = levelXp(currentLevel)
    val xpProgress = (testXp.toFloat() / xpTarget).coerceIn(0f, 1f)

    fun persist() = saveLevelTestState(context, levelCount, completedLevel, testXp)

    // ✨ आउटर ब्लूम और लाइट एमिशन के लिए ब्रीदिंग एनीमेशन
    val infiniteTransition = rememberInfiniteTransition(label = "outer_bloom_anim")
    val bloomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bloom_scale"
    )
    val bloomAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bloom_alpha"
    )

    val scrollState = rememberScrollState()
    val stepHeightDp = 90.dp

    LaunchedEffect(currentLevel, levelCount) {
        val targetY = ((levelCount - currentLevel) * 90).dp
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
                color = currentRank.primaryColor,
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
                            .background(currentRank.primaryColor.copy(alpha = 0.16f))
                            .border(1.dp, currentRank.primaryColor.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(currentRank.icon, color = currentRank.primaryColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${currentRank.name} League", color = currentRank.primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Level $currentLevel • Next milestone", color = textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$testXp XP", color = Color(0xFFFFD166), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = xpProgress,
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = currentRank.primaryColor,
                    trackColor = Color(0xFF243444)
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
                colors = ButtonDefaults.buttonColors(containerColor = currentRank.primaryColor, contentColor = Color(0xFF09141B)),
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
                .background(Color(0xFF0C131B))
        ) {
            val mapHeight = (levelCount * 90 + 90).dp

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

                    // 2. कैनवास: 3D स्तरित सड़क + XP प्रोग्रेसिव पाथ फ़िल
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF0C131B))

                        // बैकग्राउंड डॉट ग्रिड
                        for (dotY in 24..size.height.toInt() step 48) {
                            for (dotX in 16..size.width.toInt() step 32) {
                                drawCircle(Color.White.copy(alpha = 0.025f), radius = 1.2.dp.toPx(), center = Offset(dotX.toFloat(), dotY.toFloat()))
                            }
                        }

                        if (points.size > 1) {
                            // बेस डार्क पाथ (पूरा रास्ता)
                            val baseFullPath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.zipWithNext().forEach { (a, b) ->
                                    val midY = (a.y + b.y) / 2f
                                    cubicTo(a.x, midY, b.x, midY, b.x, b.y)
                                }
                            }

                            // अंडर-शैडो और मेन सड़क
                            drawPath(baseFullPath, Color(0xFF131B24), style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(baseFullPath, Color(0xFF1D2835), style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(baseFullPath, Color(0xFF283747), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                            // A. पूर्ण हो चुके सेगमेंट्स (1 से currentLevel तक 100% फुल)
                            if (currentLevel > 1) {
                                val completedPath = Path().apply {
                                    moveTo(points[0].x, points[0].y)
                                    for (i in 0 until (currentLevel - 1)) {
                                        val a = points[i]
                                        val b = points[i + 1]
                                        val midY = (a.y + b.y) / 2f
                                        cubicTo(a.x, midY, b.x, midY, b.x, b.y)
                                    }
                                }
                                drawPath(completedPath, currentRank.primaryColor.copy(alpha = 0.25f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(completedPath, currentRank.primaryColor.copy(alpha = 0.60f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(completedPath, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            }

                            // B. 🌟 XP प्रोग्रेसिव पाथ फ़िल (currentLevel से nextLevel के बीच लाइव % से आगे बढ़ता हुआ)
                            if (currentLevel < levelCount && xpProgress > 0.01f) {
                                val segStart = points[currentLevel - 1]
                                val segEnd = points[currentLevel]
                                val rawSegment = Path().apply {
                                    moveTo(segStart.x, segStart.y)
                                    val midY = (segStart.y + segEnd.y) / 2f
                                    cubicTo(segStart.x, midY, segEnd.x, midY, segEnd.x, segEnd.y)
                                }

                                val pathMeasure = PathMeasure()
                                pathMeasure.setPath(rawSegment, false)
                                val progressiveSegment = Path()
                                val targetLength = pathMeasure.length * xpProgress
                                pathMeasure.getSegment(0f, targetLength, progressiveSegment, true)

                                // चमकता हुआ प्रोग्रेसिव बीम
                                drawPath(progressiveSegment, currentRank.primaryColor.copy(alpha = 0.35f), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(progressiveSegment, currentRank.flameOuterColor, style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
                                drawPath(progressiveSegment, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

                                // पाथ के टिप पर तैरता हुआ चमकीला नियॉन स्पार्क
                                val tipOffset = pathMeasure.getPosition(targetLength)
                                drawCircle(currentRank.primaryColor, radius = 6.dp.toPx(), center = tipOffset)
                                drawCircle(Color.White, radius = 3.dp.toPx(), center = tipOffset)
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
                                .size(nodeSize + 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 🌟 आउटर ब्लूम / लाइट एमिशन इफ़ेक्ट (सिर्फ करेंट लेवल पर)
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(nodeSize * 2.2f * bloomScale)
                                        .graphicsLayer { alpha = bloomAlpha }
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    rank.primaryColor.copy(alpha = 0.65f),
                                                    rank.primaryColor.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .size(nodeSize * 1.35f)
                                        .border(2.dp, rank.glowBorderColor.copy(alpha = 0.8f), CircleShape)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
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
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "LV $level",
                                    color = if (isCurrent) Color.White else (if (isComplete) rank.primaryColor else Color(0xFF55687C)),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
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
            selectedLevel == currentLevel -> "CURRENT ACTIVE LEVEL"
            else -> "LOCKED DORMANT RUNE"
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
                    Text(status, color = rank.primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("League Material: ${rank.materialName}")
                    Text("XP Target: ${levelXp(selectedLevel)} XP")
                    Text(
                        if (selectedLevel <= completedLevel) "This level has been fully ignited and conquered!"
                        else if (selectedLevel == currentLevel) "Active Focus Node: Continue studying to progressively ignite the path to the next level."
                        else "This dormant stone node will awaken once preceding levels are completed."
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
 * 🎴 असली स्प्रिंग फ़िज़िक्स + 3D रून + कस्टम वेक्टर फ्लेम (Zero Emojis)
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
        isCurrent -> rank.primaryColor
        isComplete -> rank.primaryColor.copy(alpha = 0.92f)
        else -> Color(0xFF19222C) // डोर्मेंट डार्क स्लेट स्टोन
    }

    val baseColor = when {
        isCurrent -> rank.darkBaseColor
        isComplete -> rank.darkBaseColor
        else -> Color(0xFF0F161E) // गहरा स्टोन शैडो
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
                        else -> Color(0xFF273646) // हल्का स्टोन एज
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // 🔥 नोड्स के अंदर प्योर कस्टम वेक्टर फ्लेम (Zero Emojis)
            PureVectorFlameIcon(
                outerColor = if (isComplete || isCurrent) rank.flameOuterColor else Color(0xFF0F161E),
                innerColor = if (isComplete || isCurrent) rank.flameInnerColor else Color(0xFF243444),
                isDormant = !isComplete && !isCurrent,
                modifier = Modifier.size(nodeSize * 0.52f)
            )
        }
    }
}

/**
 * 🔥 100% प्योर कैनवास वेक्टर फ्लेम (डिवाइस इमोजी से पूरी तरह मुक्त)
 */
@Composable
private fun PureVectorFlameIcon(
    outerColor: Color,
    innerColor: Color,
    isDormant: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (isDormant) {
            // सुप्त पत्थर में तराशी हुई रून फ्लेम
            val stoneEngravedPath = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                cubicTo(w * 0.70f, h * 0.32f, w * 0.90f, h * 0.58f, w * 0.78f, h * 0.86f)
                cubicTo(w * 0.66f, h * 1.00f, w * 0.34f, h * 1.00f, w * 0.22f, h * 0.86f)
                cubicTo(w * 0.10f, h * 0.58f, w * 0.30f, h * 0.32f, w * 0.50f, h * 0.10f)
                close()
            }
            drawPath(stoneEngravedPath, color = outerColor)
            drawPath(stoneEngravedPath, color = innerColor, style = Stroke(width = 1.2.dp.toPx()))
        } else {
            // जीवंत जादुई लपट (Outer Flame)
            val outerFlamePath = Path().apply {
                moveTo(w * 0.50f, h * 0.06f)
                cubicTo(w * 0.72f, h * 0.30f, w * 0.94f, h * 0.56f, w * 0.80f, h * 0.86f)
                cubicTo(w * 0.68f, h * 1.00f, w * 0.32f, h * 1.00f, w * 0.20f, h * 0.86f)
                cubicTo(w * 0.06f, h * 0.56f, w * 0.28f, h * 0.30f, w * 0.50f, h * 0.06f)
                close()
            }
            drawPath(outerFlamePath, color = outerColor)

            // आंतरिक चमकीला कोर (Inner Luminous Spark)
            val innerCorePath = Path().apply {
                moveTo(w * 0.50f, h * 0.40f)
                cubicTo(w * 0.66f, h * 0.54f, w * 0.76f, h * 0.72f, w * 0.64f, h * 0.86f)
                cubicTo(w * 0.56f, h * 0.94f, w * 0.44f, h * 0.94f, w * 0.36f, h * 0.86f)
                cubicTo(w * 0.24f, h * 0.72f, w * 0.34f, h * 0.54f, w * 0.50f, h * 0.40f)
                close()
            }
            drawPath(innerCorePath, color = innerColor)
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
