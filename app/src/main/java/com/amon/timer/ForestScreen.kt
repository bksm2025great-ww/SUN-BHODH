package com.amon.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForestScreen() {
    val isDark = ThemeManager.isDarkTheme.value
    val goldColor = Color(0xFFF5A524)

    // 🌿 गहरा शांत बैकग्राउंड
    val bgGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xFF0F1410), Color(0xFF080B09))
        } else {
            listOf(Color(0xFFE8EFE9), Color(0xFFDCE5DD))
        }
    )

    // 🎴 कार्ड का बैकग्राउंड और बॉर्डर
    val cardBg = if (isDark) Color(0xEE161A1D) else Color(0xF2FFFFFF)
    val cardBorder = if (isDark) Color(0x22F5A524) else Color(0x1F000000)
    val textMain = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        // 🌟 सेंट्रल कोज़ी कमिंग सून कार्ड
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.25f)
                )
                .border(1.2.dp, cardBorder, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🌲 पेड़ का सिंबल
                Text(
                    text = "🌲",
                    fontSize = 54.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // सब-टाइटल
                Text(
                    text = "FOREST EXPEDITION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMuted,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // मुख्य टाइटल
                Text(
                    text = "Coming Soon ✨",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = goldColor,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // विवरण पैरा 1
                Text(
                    text = "We are hand-crafting deep focus landscapes & lush garden ecosystems.",
                    fontSize = 14.sp,
                    color = textMain,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(12.dp))

                // विवरण पैरा 2
                Text(
                    text = "Stay focused and build your study discipline while the forest grows!",
                    fontSize = 13.5.sp,
                    color = textMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 🚀 एक्टिव डेवलपमेंट कैप्सूल बैज
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(goldColor.copy(alpha = 0.12f))
                        .border(1.2.dp, goldColor.copy(alpha = 0.65f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "🚀  In Active Development",
                        color = goldColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
