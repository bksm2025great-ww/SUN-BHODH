package com.amon.timer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.amon.timer.ThemeManager

// 🟢 START: [AMON_THEME]

// 🟡 1. Classic Yellow की डार्क और लाइट थीम
private val YellowDarkColorScheme = darkColorScheme(
    primary = AccentYellow,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val YellowLightColorScheme = lightColorScheme(
    primary = AccentYellow,
    background = Color(0xFFF6F6F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E7EB),
    onPrimary = Color.Black,
    onBackground = Color(0xFF121214),
    onSurface = Color(0xFF121214)
)

// 👑 2. Luxe Gold की डार्क और लाइट थीम
private val GoldDarkColorScheme = darkColorScheme(
    primary = LuxeGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val GoldLightColorScheme = lightColorScheme(
    primary = LuxeGold,
    background = Color(0xFFF6F6F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E7EB),
    onPrimary = Color.Black,
    onBackground = Color(0xFF121214),
    onSurface = Color(0xFF121214)
)

@Composable
fun AmonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // मैनेजर से पूछो कि कौन सा रंग सिलेक्टेड है
    val currentTheme = ThemeManager.currentTheme.value

    // सही थीम डिसाइड करो (डार्क/लाइट और येलो/गोल्ड के हिसाब से)
    val colorScheme = when {
        darkTheme && currentTheme == "Gold" -> GoldDarkColorScheme
        !darkTheme && currentTheme == "Gold" -> GoldLightColorScheme
        darkTheme && currentTheme == "Yellow" -> YellowDarkColorScheme
        else -> YellowLightColorScheme // (!darkTheme && currentTheme == "Yellow")
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
// 🔴 END: [AMON_THEME]
