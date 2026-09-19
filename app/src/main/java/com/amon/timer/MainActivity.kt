package com.amon.timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.amon.timer.ui.theme.AmonTheme

// 🟢 START: [MAIN_ACTIVITY_ENTRY]
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // सिस्टम और यूज़र थीम (Dark / Light) लागू करना
            AmonTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // इसके बाद हम MainScreen फ़ाइल बनाएंगे जो यहाँ लोड होगी
                    MainScreen()
                }
            }
        }
    }
}
// 🔴 END: [MAIN_ACTIVITY_ENTRY]
