package com.amon.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PomodoroSessionTracker(
    totalSessions: Int,
    currentSessionIndex: Int,
    isBreakActive: Boolean,
    isRunning: Boolean,
    goldColor: Color,
    glowYellow: Color,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_session")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isDark) Color(0xFF161620) else Color(0xFFF1F5F9))
            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1), RoundedCornerShape(50))
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSessions) {
                val isCompleted = i < currentSessionIndex
                val isCurrent = i == currentSessionIndex
                val isUpcoming = i > currentSessionIndex

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp)
                ) {
                    when {
                        isCompleted -> {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(goldColor)
                            )
                        }
                        isCurrent -> {
                            if (isBreakActive) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(CircleShape)
                                        .background(goldColor.copy(alpha = if (isRunning) pulseAlpha * 0.4f else 0.3f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                            }
                        }
                        isUpcoming -> {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .border(1.6.dp, if (isDark) Color(0xFF38384A) else Color(0xFF94A3B8), CircleShape)
                            )
                        }
                    }
                }

                if (i < totalSessions) {
                    val isBreakCurrent = (i == currentSessionIndex && isBreakActive)
                    val isBreakDone = (i < currentSessionIndex)

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.8.dp)
                                .background(if (isBreakDone) goldColor.copy(alpha = 0.5f) else (if (isDark) Color(0xFF262636) else Color(0xFFCBD5E1)))
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isBreakCurrent) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isBreakCurrent -> Color(0xFF10B981)
                                        isBreakDone -> goldColor.copy(alpha = 0.6f)
                                        else -> if (isDark) Color(0xFF343446) else Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.8.dp)
                                .background(if (isBreakDone) goldColor.copy(alpha = 0.5f) else (if (isDark) Color(0xFF262636) else Color(0xFFCBD5E1)))
                        )
                    }
                }
            }
        }
    }
}
