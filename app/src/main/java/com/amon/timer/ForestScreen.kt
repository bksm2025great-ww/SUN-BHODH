package com.amon.timer

import android.content.Context
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
    val icon: String
)

private val journeyRanks = listOf(
    JourneyRank("Bronze", 1, Color(0xFFCD8B56), "♟"),
    JourneyRank("Silver", 11, Color(0xFFC5D0D8), "♞"),
    JourneyRank("Gold", 21, Color(0xFFFFD166), "♜"),
    JourneyRank("Platinum", 31, Color(0xFF70D8C2), "♝"),
    JourneyRank("Emerald", 41, Color(0xFF55D68B), "♛"),
    JourneyRank("Diamond", 51, Color(0xFF6CCBFF), "◆"),
    JourneyRank("Master", 61, Color(0xFFB6A0FF), "♚"),
    JourneyRank("Grandmaster", 71, Color(0xFFFF7D9B), "★")
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

    val scrollState = rememberScrollState()

    // 🎯 स्क्रीन खुलते ही एक्टिव लेवल पर ऑटो-स्क्रॉल
    LaunchedEffect(currentLevel, levelCount) {
        val targetY = ((levelCount - currentLevel) * 82).dp
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
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "AMON  /  FOCUS JOURNEY",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your Level Path",
                color = textMain,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Testing mode • Progress is saved on this device",
                color = textMuted,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(6.dp))
        Text(
            text = "LEVEL MAP • Tap a checkpoint to inspect",
            color = textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .border(1.dp, panelBorder, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color(0xFF101923))
        ) {
            val mapHeight = (levelCount * 82 + 80).dp
            Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().height(mapHeight)
                ) {
                    val w = constraints.maxWidth.toFloat()
                    val densityVal = density.density
                    val h = mapHeight.value * densityVal
                    val points = remember(levelCount, w, h) {
                        (1..levelCount).map { level ->
                            val xFraction = when (level % 4) {
                                1 -> 0.25f
                                2 -> 0.55f
                                3 -> 0.76f
                                else -> 0.47f
                            }
                            Offset(w * xFraction, h - ((level - 1) * 82f * densityVal) - 40f * densityVal)
                        }
                    }

                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF101923))
                        for (dotY in 30..size.height.toInt() step 52) {
                            for (dotX in 20..size.width.toInt() step 32) {
                                drawCircle(Color.White.copy(alpha = 0.035f), radius = 1.3.dp.toPx(), center = Offset(dotX.toFloat(), dotY.toFloat()))
                            }
                        }
                        if (points.size > 1) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                points.zipWithNext().forEach { (a, b) ->
                                    val midY = (a.y + b.y) / 2f
                                    cubicTo(a.x, midY, b.x, midY, b.x, b.y)
                                }
                            }
                            drawPath(path, Color(0xFF263747), style = Stroke(width = 16.dp.toPx()))
                            drawPath(path, Color(0xFF3B5265), style = Stroke(width = 2.dp.toPx()))
                        }
                    }

                    (1..levelCount).forEach { level ->
                        val xFraction = when (level % 4) {
                            1 -> 0.25f
                            2 -> 0.55f
                            3 -> 0.76f
                            else -> 0.47f
                        }
                        val y = mapHeight - ((level - 1) * 82).dp - 40.dp
                        val isComplete = level <= completedLevel
                        val isCurrent = level == currentLevel
                        val rank = rankForLevel(level)
                        val nodeColor = when {
                            isComplete -> Color(0xFF79D7A4)
                            isCurrent -> rank.color
                            else -> Color(0xFF273543)
                        }
                        Column(
                            modifier = Modifier
                                .offset(x = maxWidth * xFraction - 27.dp, y = y - 27.dp)
                                .width(54.dp)
                                .clickable {
                                    selectedLevel = level
                                    showLevelDetails = true
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 54.dp else 48.dp)
                                    .clip(CircleShape)
                                    .background(nodeColor)
                                    .border(
                                        width = if (isCurrent) 3.dp else 1.dp,
                                        color = if (isCurrent) Color.White else rank.color.copy(alpha = if (isComplete) 0.9f else 0.45f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isComplete -> "✓"
                                        isCurrent -> level.toString()
                                        else -> "🔒"
                                    },
                                    color = if (isComplete || isCurrent) Color(0xFF0A1711) else Color(0xFFB6C4D1),
                                    fontSize = if (isCurrent) 15.sp else 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text("LV $level", color = if (isCurrent) textMain else textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
            selectedLevel == currentLevel -> "CURRENT LEVEL"
            else -> "LOCKED"
        }
        AlertDialog(
            onDismissRequest = { showLevelDetails = false },
            containerColor = panel,
            titleContentColor = textMain,
            textContentColor = textMuted,
            title = { Text("Level $selectedLevel • ${rank.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status, color = rank.color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("XP target: ${levelXp(selectedLevel)} XP")
                    Text("Reward: ${rank.name} Journey points")
                    Text(
                        if (selectedLevel <= completedLevel) "This checkpoint is already complete."
                        else if (selectedLevel == currentLevel) "Use +100 XP to simulate progress."
                        else "Complete earlier levels to unlock this checkpoint."
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
            text = { Text("This resets the level count to 20 and clears test XP and completed levels.", color = textMuted) },
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
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = Color(0xFF9BAABD), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
