package com.amon.timer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun StreakFlame(
    streakDays: Int = 1,
    modifier: Modifier = Modifier.size(54.dp),
    isLarge: Boolean = false
) {
    // 🎯 बड़ी स्क्रीन/जर्नल के लिए flame_stats और छोटी जगह/हेडर के लिए flame_main
    val rawResId = if (isLarge) R.raw.flame_stats else R.raw.flame_main

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawResId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = streakDays > 0
    )

    Box(
        modifier = modifier.alpha(if (streakDays > 0) 1f else 0.35f),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}
