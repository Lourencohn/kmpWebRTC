package app.trovata.cast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    width: Dp = 76.dp,
    height: Dp = 22.dp,
    color: Color = Color.Black,
    fill: Boolean = true,
    dot: Boolean = true,
    strokeWidth: Dp = 1.5.dp,
) {
    val sample = if (data.size >= 2) data else listOf(0f, 0f)
    val max = sample.maxOrNull() ?: 0f
    val min = sample.minOrNull() ?: 0f
    val range = (max - min).takeIf { it != 0f } ?: 1f

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val w = size.width
        val h = size.height
        val pad = 3f
        val points = sample.mapIndexed { i, v ->
            val x = (i.toFloat() / (sample.size - 1)) * (w - 4f) + 2f
            val y = h - ((v - min) / range) * (h - pad * 2f) - pad
            Offset(x, y)
        }

        if (fill) {
            val area = Path().apply {
                moveTo(points.first().x, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, h)
                close()
            }
            drawPath(path = area, color = color.copy(alpha = 0.10f))
        }

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = line,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        if (dot) {
            drawCircle(color = color, radius = 2.4f.dp.toPx(), center = points.last())
        }
    }
}
