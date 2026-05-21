package app.trovata.cast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.color.oklch

@Composable
fun RemotePointer(
    name: String,
    modifier: Modifier = Modifier,
    hue: Double = 30.0,
    x: Dp = 0.dp,
    y: Dp = 0.dp,
) {
    val fill = oklch(60.0, 0.18, hue)
    val labelBg = oklch(56.0, 0.18, hue)

    Box(
        modifier = modifier.offset(x = x, y = y),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val s = size.width / 24f
                val path = Path().apply {
                    moveTo(4f * s, 3f * s)
                    lineTo(9f * s, 21f * s)
                    lineTo(12f * s, 13f * s)
                    lineTo(20f * s, 10f * s)
                    close()
                }
                drawPath(path = path, color = fill)
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 1.2f * s, join = StrokeJoin.Round),
                )
            }
            Box(
                modifier = Modifier
                    .offset(x = 14.dp, y = (-2).dp)
                    .shadow(2.dp, RoundedCornerShape(6.dp), clip = false)
                    .background(labelBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.005).sp,
                )
            }
        }
    }
}
