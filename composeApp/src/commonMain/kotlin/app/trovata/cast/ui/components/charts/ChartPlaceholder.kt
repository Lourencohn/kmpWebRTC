package app.trovata.cast.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.trovata.cast.data.sample.InsightsHeroPalette
import app.trovata.cast.theme.TrovataTokens

private val linePlaceholderShape = listOf(0.42f, 0.58f, 0.40f, 0.68f, 0.52f, 0.76f, 0.60f)
private val columnPlaceholderShape = listOf(0.50f, 0.34f, 0.66f, 0.46f, 0.80f, 0.58f, 0.70f)

@Composable
fun LineChartPlaceholder(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val colors = TrovataTokens.colors
    val gridColor = if (onDark) Color.White.copy(alpha = 0.10f) else colors.line
    val lineColor = if (onDark) InsightsHeroPalette.accent.copy(alpha = 0.5f) else colors.brand.copy(alpha = 0.35f)
    val areaColor = lineColor.copy(alpha = 0.18f)

    Canvas(modifier) {
        val rows = 4
        for (i in 0..rows) {
            val y = size.height * i / rows
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        val points = linePlaceholderShape.mapIndexed { index, fraction ->
            val x = if (linePlaceholderShape.size == 1) 0f else size.width * index / (linePlaceholderShape.size - 1)
            Offset(x, size.height * (1f - fraction))
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(areaColor, Color.Transparent)))
        drawPath(line, color = lineColor, style = Stroke(width = 2.5f))
        points.forEach { drawCircle(lineColor, radius = 3f, center = it) }
    }
}

@Composable
fun ColumnChartPlaceholder(
    modifier: Modifier = Modifier,
    columnColor: Color = TrovataTokens.colors.brand.copy(alpha = 0.18f),
) {
    val gridColor = TrovataTokens.colors.line

    Canvas(modifier) {
        val rows = 4
        for (i in 0..rows) {
            val y = size.height * i / rows
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        val slot = size.width / columnPlaceholderShape.size
        val columnWidth = minOf(16.dp.toPx(), slot * 0.55f)
        columnPlaceholderShape.forEachIndexed { index, fraction ->
            val centerX = slot * index + slot / 2
            val columnHeight = size.height * fraction
            drawRoundRect(
                color = columnColor,
                topLeft = Offset(centerX - columnWidth / 2, size.height - columnHeight),
                size = Size(columnWidth, columnHeight),
                cornerRadius = CornerRadius(columnWidth * 0.36f, columnWidth * 0.36f),
            )
        }
    }
}
