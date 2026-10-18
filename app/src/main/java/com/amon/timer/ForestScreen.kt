package com.amon.timer

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private data class ForestNode(
    val id: Int,
    val x: Float,
    val y: Float
)

private const val FOREST_PREFS = "amon_forest_layout"
private const val FOREST_KEY = "nodes"

private fun saveForestNodes(context: Context, nodes: List<ForestNode>) {
    val array = JSONArray()

    nodes.forEach { node ->
        array.put(
            JSONObject().apply {
                put("id", node.id)
                put("x", node.x)
                put("y", node.y)
            }
        )
    }

    context.getSharedPreferences(
        FOREST_PREFS,
        Context.MODE_PRIVATE
    ).edit().putString(FOREST_KEY, array.toString()).apply()
}

private fun loadForestNodes(context: Context): List<ForestNode>? {
    return try {
        val raw = context.getSharedPreferences(
            FOREST_PREFS,
            Context.MODE_PRIVATE
        ).getString(FOREST_KEY, null) ?: return null

        val array = JSONArray(raw)

        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            ForestNode(
                id = item.getInt("id"),
                x = item.getDouble("x").toFloat().coerceIn(0.05f, 0.95f),
                y = item.getDouble("y").toFloat().coerceIn(0.02f, 0.98f)
            )
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ForestScreen() {
    val context = LocalContext.current

    var nodes by remember { mutableStateOf<List<ForestNode>>(emptyList()) }
    var selectedId by remember { mutableIntStateOf(1) }
    var editMode by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("Add levels and adjust their positions.") }

    val green = Color(0xFF65C887)
    val panel = Color(0xE6192B21)
    val white = Color(0xFFF1F7F2)

    LaunchedEffect(Unit) {
        nodes = loadForestNodes(context) ?: (1..12).map { id ->
            ForestNode(
                id = id,
                x = if (id % 2 == 0) 0.68f else 0.32f,
                y = 0.96f - (id - 1) * 0.075f
            )
        }
        selectedId = nodes.lastOrNull()?.id ?: 1
    }

    val selected = nodes.firstOrNull { it.id == selectedId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A140E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AMON • FOREST",
                    color = green,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Level Builder",
                    color = white,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Button(
                onClick = { editMode = !editMode },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editMode) green else Color(0xFF34483A)
                )
            ) {
                Text(
                    if (editMode) "Preview" else "Edit",
                    color = Color.Black
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip("LEVELS", nodes.size.toString(), panel, white)
            InfoChip("SELECTED", selectedId.toString(), panel, white)
        }

        Spacer(Modifier.height(10.dp))

        // The map is taller than the phone screen and can scroll.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF355440), RoundedCornerShape(20.dp))
        ) {
            val scroll = rememberScrollState()
            val mapHeight = (nodes.size * 100).coerceAtLeast(1200)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight.dp)
                ) {
                    val mapWidth = maxWidth
                    val totalHeight = mapHeight.dp

                    Image(
                        painter = painterResource(
                            id = R.drawable.forest_map_background
                        ),
                        contentDescription = "Forest level map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )

                    // Connect all nodes in order with a visible winding path.
                    Canvas(Modifier.fillMaxSize()) {
                        if (nodes.size > 1) {
                            val path = Path()

                            nodes.forEachIndexed { index, node ->
                                val point = androidx.compose.ui.geometry.Offset(
                                    node.x * size.width,
                                    node.y * size.height
                                )

                                if (index == 0) {
                                    path.moveTo(point.x, point.y)
                                } else {
                                    val previous = nodes[index - 1]
                                    val previousPoint =
                                        androidx.compose.ui.geometry.Offset(
                                            previous.x * size.width,
                                            previous.y * size.height
                                        )

                                    val middleY =
                                        (previousPoint.y + point.y) / 2f

                                    path.cubicTo(
                                        previousPoint.x, middleY,
                                        point.x, middleY,
                                        point.x, point.y
                                    )
                                }
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFFD8C18A),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 7.dp.toPx()
                                )
                            )
                            drawPath(
                                path = path,
                                color = Color(0xFF493C27),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2.dp.toPx()
                                )
                            )
                        }
                    }

                    nodes.forEach { node ->
                        val isSelected = node.id == selectedId

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = mapWidth * node.x - 25.dp,
                                    y = totalHeight * node.y - 25.dp
                                )
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFFFD56A)
                                    else Color(0xFF1D633A)
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 2.dp,
                                    color = if (isSelected) Color.White
                                    else Color(0xFFB8E8C4),
                                    shape = CircleShape
                                )
                                .pointerInput(node.id, editMode, mapWidth, totalHeight) {
                                    if (editMode) {
                                        detectDragGestures(
                                            onDragStart = {
                                                selectedId = node.id
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()

                                                val widthPx = size.width.toFloat()
                                                val heightPx = size.height.toFloat()

                                                nodes = nodes.map { current ->
                                                    if (current.id == node.id) {
                                                        current.copy(
                                                            x = (current.x + dragAmount.x / widthPx)
                                                                .coerceIn(0.05f, 0.95f),
                                                            y = (current.y + dragAmount.y / heightPx)
                                                                .coerceIn(0.02f, 0.98f)
                                                        )
                                                    } else current
                                                }
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = node.id.toString(),
                                color = if (isSelected) Color(0xFF17251B)
                                else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Editing controls
        if (editMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(panel)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Selected Level: $selectedId",
                    color = white,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val next = (nodes.maxOfOrNull { it.id } ?: 0) + 1
                            val last = nodes.lastOrNull()

                            nodes = nodes + ForestNode(
                                id = next,
                                x = if (last == null) 0.5f
                                else (1f - last.x).coerceIn(0.15f, 0.85f),
                                y = if (last == null) 0.95f
                                else (last.y - 0.065f).coerceAtLeast(0.03f)
                            )
                            selectedId = next
                            message = "Level $next added."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34784A)
                        )
                    ) {
                        Text("+ Next Level")
                    }

                    Button(
                        onClick = {
                            if (selected != null) {
                                nodes = nodes.map { node ->
                                    if (node.id == selectedId) {
                                        node.copy(
                                            y = (node.y - 0.01f).coerceAtLeast(0.02f)
                                        )
                                    } else node
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("↑ Up")
                    }

                    Button(
                        onClick = {
                            if (selected != null) {
                                nodes = nodes.map { node ->
                                    if (node.id == selectedId) {
                                        node.copy(
                                            y = (node.y + 0.01f).coerceAtMost(0.98f)
                                        )
                                    } else node
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("↓ Down")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (selected != null) {
                                nodes = nodes.map { node ->
                                    if (node.id == selectedId) {
                                        node.copy(
                                            x = (node.x - 0.01f).coerceAtLeast(0.05f)
                                        )
                                    } else node
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("← Left") }

                    OutlinedButton(
                        onClick = {
                            if (selected != null) {
                                nodes = nodes.map { node ->
                                    if (node.id == selectedId) {
                                        node.copy(
                                            x = (node.x + 0.01f).coerceAtMost(0.95f)
                                        )
                                    } else node
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Right →") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            saveForestNodes(context, nodes)
                            message = "Layout saved on this phone."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE4BE63)
                        )
                    ) {
                        Text("Save Layout", color = Color(0xFF17251B))
                    }

                    OutlinedButton(
                        onClick = {
                            if (nodes.size > 1) {
                                nodes = nodes.filterNot { it.id == selectedId }
                                selectedId = nodes.lastOrNull()?.id ?: 1
                                message = "Selected level deleted."
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete Level")
                    }
                }

                Text(
                    message,
                    color = Color(0xFFBDD7C4),
                    fontSize = 12.sp
                )
            }
        } else {
            Text(
                "Preview mode • Editing controls hidden",
                color = Color(0xFFBDD7C4),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    background: Color,
    foreground: Color
) {
    Surface(
        modifier = Modifier.weight(1f),
        color = background,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = Color(0xFFA9C9B1), fontSize = 10.sp)
            Text(
                value,
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
