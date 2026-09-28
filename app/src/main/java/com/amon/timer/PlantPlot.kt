package com.amon.studyflow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amon.studyflow.R

/**
 * PlantPlot: यह कंपोनेंट मिट्टी और पौधे को एक सिंगल यूनिट में लॉक करता है।
 * पौधा चाहे कितना भी बड़ा या छोटा हो, उसकी जड़ हमेशा मिट्टी के केंद्र में ही रहेगी।
 */
@Composable
fun PlantPlot(
    @DrawableRes plantResId: Int?, // अगर पौधा उगा है तो R.drawable.plant_lotus, वर्ना null
    modifier: Modifier = Modifier,
    plotWidth: Dp = 110.dp,        // क्यारी की चौड़ाई
    plotHeight: Dp = 120.dp        // पूरे स्लॉट की कुल ऊँचाई
) {
    Box(
        modifier = modifier
            .size(width = plotWidth, height = plotHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. लेयर 1: मिट्टी का बेस (Soil Mound)
        // यह बॉक्स के बिल्कुल बॉटम पर फिक्स रहेगा
        Image(
            painter = painterResource(id = R.drawable.soil_mound),
            contentDescription = "Soil Mound",
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp) // मिट्टी की अपनी ऊँचाई
                .align(Alignment.BottomCenter)
        )

        // 2. लेयर 2: आपका पौधा (Dynamic Plant)
        // पौधे की जड़ (BottomCenter) को मिट्टी के बीच वाले गड्ढे में बिठाने के लिए 
        // 14.dp का वर्टिकल ऑफ़सेट दिया गया है, ताकि यह बॉर्डर पर कभी न फिसले।
        if (plantResId != null) {
            Image(
                painter = painterResource(id = plantResId),
                contentDescription = "Active Plant",
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.BottomCenter)
                    .offset(y = (-14).dp) // यह पौधे की जड़ को मिट्टी के ठीक बीच में धँसा देता है
            )
        }
    }
}
