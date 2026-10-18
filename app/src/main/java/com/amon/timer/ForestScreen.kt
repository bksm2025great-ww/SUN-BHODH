import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun Simple2DCanvasDemo() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. एक गोल घेरा (Circle) बनाना
        drawCircle(
            color = Color(0xFF65C887),
            radius = 60f,
            center = Offset(width / 2f, height / 3f)
        )

        // 2. एक घुमावदार लाइन या पाथ (Path) बनाना
        val customPath = Path().apply {
            moveTo(width * 0.2f, height * 0.4f)
            cubicTo(
                width * 0.8f, height * 0.5f,
                width * 0.1f, height * 0.7f,
                width * 0.8f, height * 0.9f
            )
        }

        // पाथ को स्क्रीन पर ड्रा करना
        drawPath(
            path = customPath,
            color = Color(0xFFD8C18A),
            style = Stroke(width = 20.dp.toPx())
        )
    }
}
