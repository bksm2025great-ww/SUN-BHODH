package com.amon.studyflow.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.amon.studyflow.R
import com.amon.studyflow.ui.components.PlantPlot

@Composable
fun GardenPathwayScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. पगडंडी और गेट वाला बैकग्राउंड
        Image(
            painter = painterResource(id = R.drawable.bg_pathway_meadow),
            contentDescription = "Garden Pathway",
            modifier = Modifier.fillMaxWidth()
        )

        // 2. पगडंडी के मोड़ों पर पौधे (स्लॉट्स)
        // स्लॉट 1 (कमल का फूल)
        PlantPlot(
            plantResId = R.drawable.plant_lotus,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 50.dp, y = 180.dp)
        )

        // स्लॉट 2 (चेरी ब्लॉसम)
        PlantPlot(
            plantResId = R.drawable.plant_cherry_blossom,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 220.dp, y = 320.dp)
        )

        // स्लॉट 3 (बर्ड ऑफ़ पैराडाइज़)
        PlantPlot(
            plantResId = R.drawable.plant_bird_of_paradise,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 60.dp, y = 480.dp)
        )

        // स्लॉट 4 (रॉयल ब्लू आइरिस)
        PlantPlot(
            plantResId = R.drawable.plant_iris,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 210.dp, y = 640.dp)
        )
    }
}
