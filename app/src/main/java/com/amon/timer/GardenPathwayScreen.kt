package com.amon.timer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun GardenPathwayScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_pathway_meadow),
            contentDescription = "Garden Pathway",
            modifier = Modifier.fillMaxWidth()
        )

        PlantPlot(
            plantResId = R.drawable.plant_lotus,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 50.dp, y = 180.dp)
        )

        PlantPlot(
            plantResId = R.drawable.plant_cherry_blossom,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 220.dp, y = 320.dp)
        )

        PlantPlot(
            plantResId = R.drawable.plant_bird_of_paradise,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 60.dp, y = 480.dp)
        )

        PlantPlot(
            plantResId = R.drawable.plant_iris,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 210.dp, y = 640.dp)
        )
    }
}
