package app.trovata.cast.ui.components.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.theme.TrovataTokens
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.columnSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.Insets
import com.patrykandpatrick.vico.multiplatform.common.LayeredComponent
import com.patrykandpatrick.vico.multiplatform.common.component.ShapeComponent
import com.patrykandpatrick.vico.multiplatform.common.component.TextComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberLineComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberTextComponent
import com.patrykandpatrick.vico.multiplatform.common.fill
import com.patrykandpatrick.vico.multiplatform.common.shape.CorneredShape
import com.patrykandpatrick.vico.multiplatform.common.shape.MarkerCorneredShape
import kotlin.math.roundToInt

fun integerValueFormatter(): CartesianValueFormatter =
    CartesianValueFormatter { _, value, _ -> value.roundToInt().toString() }

fun compactCurrencyValueFormatter(): CartesianValueFormatter =
    CartesianValueFormatter { _, value, _ ->
        when {
            value >= 1_000_000 -> "${(value / 1_000_000).roundToInt()}mi"
            value >= 1_000 -> "${(value / 1_000).roundToInt()}k"
            else -> value.roundToInt().toString()
        }
    }

@Composable
fun TrovataLineChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = TrovataTokens.colors.brand,
    onDark: Boolean = false,
    yFormatter: CartesianValueFormatter = integerValueFormatter(),
    markerPrefix: String = "",
    markerSuffix: String = "",
) {
    val colors = TrovataTokens.colors
    val gridColor = if (onDark) Color.White.copy(alpha = 0.10f) else colors.line
    val axisTextColor = if (onDark) Color.White.copy(alpha = 0.55f) else colors.ink4
    val areaAlpha = if (onDark) 0.42f else 0.20f

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        if (values.isNotEmpty()) {
            modelProducer.runTransaction { lineSeries { series(values) } }
        }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ -> labels.getOrNull(value.roundToInt()) ?: "" }
    }
    val markerFormatter = remember(markerPrefix, markerSuffix) {
        DefaultCartesianMarker.ValueFormatter.default(prefix = markerPrefix, suffix = markerSuffix)
    }
    val axisLabel = rememberAxisLabelComponent(style = TextStyle(color = axisTextColor, fontSize = 10.sp))

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(
                                Brush.verticalGradient(
                                    listOf(lineColor.copy(alpha = areaAlpha), Color.Transparent),
                                ),
                            ),
                        ),
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.Point(rememberShapeComponent(fill(lineColor), CorneredShape.Pill)),
                        ),
                    ),
                ),
                rangeProvider = remember { CartesianLayerRangeProvider.fixed(minY = 0.0) },
            ),
            startAxis = VerticalAxis.rememberStart(
                line = null,
                tick = null,
                label = axisLabel,
                guideline = rememberAxisGuidelineComponent(fill = fill(gridColor), thickness = 1.dp),
                valueFormatter = yFormatter,
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null,
                tick = null,
                guideline = null,
                label = axisLabel,
                valueFormatter = bottomFormatter,
            ),
            marker = rememberTrovataMarker(onDark = onDark, valueFormatter = markerFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false),
    )
}

@Composable
fun TrovataColumnChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    columnColor: Color = TrovataTokens.colors.brand,
    onDark: Boolean = false,
    yFormatter: CartesianValueFormatter = integerValueFormatter(),
    markerPrefix: String = "",
    markerSuffix: String = "",
) {
    val colors = TrovataTokens.colors
    val gridColor = if (onDark) Color.White.copy(alpha = 0.10f) else colors.line
    val axisTextColor = if (onDark) Color.White.copy(alpha = 0.55f) else colors.ink4

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        if (values.isNotEmpty()) {
            modelProducer.runTransaction { columnSeries { series(values) } }
        }
    }

    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ -> labels.getOrNull(value.roundToInt()) ?: "" }
    }
    val markerFormatter = remember(markerPrefix, markerSuffix) {
        DefaultCartesianMarker.ValueFormatter.default(prefix = markerPrefix, suffix = markerSuffix)
    }
    val axisLabel = rememberAxisLabelComponent(style = TextStyle(color = axisTextColor, fontSize = 10.sp))
    val column = rememberLineComponent(
        fill = fill(columnColor),
        thickness = 16.dp,
        shape = CorneredShape.rounded(topLeftPercent = 36, topRightPercent = 36),
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(column),
                columnCollectionSpacing = 10.dp,
                rangeProvider = remember { CartesianLayerRangeProvider.fixed(minY = 0.0) },
            ),
            startAxis = VerticalAxis.rememberStart(
                line = null,
                tick = null,
                label = axisLabel,
                guideline = rememberAxisGuidelineComponent(fill = fill(gridColor), thickness = 1.dp),
                valueFormatter = yFormatter,
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null,
                tick = null,
                guideline = null,
                label = axisLabel,
                valueFormatter = bottomFormatter,
            ),
            marker = rememberTrovataMarker(onDark = onDark, valueFormatter = markerFormatter),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false),
    )
}

@Composable
private fun rememberTrovataMarker(
    onDark: Boolean,
    valueFormatter: DefaultCartesianMarker.ValueFormatter,
): CartesianMarker {
    val colors = TrovataTokens.colors
    val background = if (onDark) Color(0xFF0A1429) else colors.surface
    val stroke = if (onDark) Color.White.copy(alpha = 0.18f) else colors.line
    val textColor = if (onDark) Color.White else colors.ink
    val frontColor = if (onDark) Color.White else colors.surface

    val labelBackground = rememberShapeComponent(
        fill = fill(background),
        shape = MarkerCorneredShape(CorneredShape.Corner.Rounded),
        strokeFill = fill(stroke),
        strokeThickness = 1.dp,
    )
    val label = rememberTextComponent(
        style = TextStyle(color = textColor, textAlign = TextAlign.Center, fontSize = 12.sp),
        padding = Insets(8.dp, 4.dp),
        background = labelBackground,
        minWidth = TextComponent.MinWidth.fixed(40.dp),
    )
    val indicatorFront = rememberShapeComponent(fill(frontColor), CorneredShape.Pill)
    val guideline = rememberAxisGuidelineComponent()
    return rememberDefaultCartesianMarker(
        label = label,
        valueFormatter = valueFormatter,
        indicator = { color ->
            LayeredComponent(
                back = ShapeComponent(Fill(color.copy(alpha = 0.15f)), CorneredShape.Pill),
                front = LayeredComponent(
                    back = ShapeComponent(fill = Fill(color), shape = CorneredShape.Pill),
                    front = indicatorFront,
                    padding = Insets(5.dp),
                ),
                padding = Insets(10.dp),
            )
        },
        indicatorSize = 36.dp,
        guideline = guideline,
    )
}
