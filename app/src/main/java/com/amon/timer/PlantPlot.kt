package com.amon.timer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlantPlot(
    @DrawableRes plantResId: Int?, 
    modifier: Modifier = Modifier,
    plotWidth: Dp = 110.dp,        
    plotHeight: Dp = 120.dp        
) {
    Box(
        modifier = modifier
            .size(width = plotWidth, height = plotHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. मिट्टी का बेस (Soil Mound)
        Image(
            painter = painterResource(id = R.drawable.soil_mound),
            contentDescription = "Soil Mound",
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .align(Alignment.BottomCenter)
        )

        // 2. पौधा (मिट्टी के केंद्र में)
        if (plantResId != null) {
            Image(
                painter = painterResource(id = plantResId),
                contentDescription = "Active Plant",
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomCenter)
                    .offset(y = (-14).dp)
            )
        }
    }
}
