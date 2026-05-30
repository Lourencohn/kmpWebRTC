package app.trovata.cast.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.InsightsHeroPalette
import app.trovata.cast.feature.dashboard.DashKpi
import app.trovata.cast.feature.dashboard.DashMetric
import app.trovata.cast.feature.dashboard.DashPeriod
import app.trovata.cast.feature.dashboard.DashTopProduct
import app.trovata.cast.feature.dashboard.DashTrend
import app.trovata.cast.feature.dashboard.DashboardUiState
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.HBar
import app.trovata.cast.ui.components.PlaceholderBar
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.components.charts.ColumnChartPlaceholder
import app.trovata.cast.ui.components.charts.LineChartPlaceholder
import app.trovata.cast.ui.components.charts.TrovataColumnChart
import app.trovata.cast.ui.components.charts.TrovataLineChart
import app.trovata.cast.ui.components.charts.compactCurrencyValueFormatter
import app.trovata.cast.ui.components.charts.integerValueFormatter

private val HeroAccent = InsightsHeroPalette.accent

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onOpenAccount: () -> Unit = {},
    onPeriodChange: (DashPeriod) -> Unit = {},
    onMetricChange: (DashMetric) -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = "Análise · ${state.period.label}",
                    title = "Painel",
                    subtitle = if (state.subtitle.isBlank()) "Visão analítica" else state.subtitle,
                    onOpenAccount = onOpenAccount,
                )
            }

            when {
                state.isLoading -> item { LoadingCard() }
                !state.hasData -> {
                    item {
                        SegmentedSwitch(
                            options = DashPeriod.entries.map { it.label },
                            selectedIndex = DashPeriod.entries.indexOf(state.period),
                            onSelect = { onPeriodChange(DashPeriod.entries[it]) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    item { HeroCardScaffold(state = state, onMetricChange = onMetricChange) }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionLabel(text = "Indicadores do período")
                            KpiGridScaffold()
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionLabel(text = "Pedidos por dia da semana")
                            WeekdayChartScaffold()
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionLabel(text = "Mais vendidos")
                            TopProductsScaffold()
                        }
                    }

                    item { EmptyHint() }
                }
                else -> {
                    item {
                        SegmentedSwitch(
                            options = DashPeriod.entries.map { it.label },
                            selectedIndex = DashPeriod.entries.indexOf(state.period),
                            onSelect = { onPeriodChange(DashPeriod.entries[it]) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    item { HeroCard(state = state, onMetricChange = onMetricChange) }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionLabel(text = "Indicadores do período")
                            KpiGrid(state.kpis)
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionLabel(text = "Pedidos por dia da semana")
                            WeekdayChartCard(state)
                        }
                    }

                    if (state.topProducts.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionLabel(text = "Mais vendidos")
                                TopProductsCard(state.topProducts)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    state: DashboardUiState,
    onMetricChange: (DashMetric) -> Unit,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(20.dp)

    var mounted by remember { mutableStateOf(false) }
    val reveal by animateFloatAsState(
        targetValue = if (mounted) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "hero-reveal",
    )
    LaunchedEffect(Unit) { mounted = true }

    val deltaColor = when (state.headlineTrend) {
        DashTrend.Up -> HeroAccent
        DashTrend.Down -> Color(0xFFFF6B6B)
        DashTrend.Flat -> Color.White.copy(alpha = 0.55f)
    }

    val yFormatter = remember(state.metric) {
        if (state.metric == DashMetric.Receita) compactCurrencyValueFormatter() else integerValueFormatter()
    }
    val markerPrefix = if (state.metric == DashMetric.Receita) "R$ " else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 24f
            }
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(InsightsHeroPalette.gradientTop, InsightsHeroPalette.gradientBottom),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.brand.copy(alpha = 0.40f), Color.Transparent),
                        radius = 520f,
                    ),
                ),
        )
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(HeroAccent, RoundedCornerShape(50)),
                    )
                    Text(
                        text = state.headlineLabel,
                        color = Color.White.copy(alpha = 0.66f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.12.em,
                    )
                }
            }

            Text(
                text = state.headlineValue,
                color = Color.White,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.04).em,
                ),
                modifier = Modifier.padding(top = 10.dp),
            )
            state.headlineDelta?.let { delta ->
                Text(
                    text = delta,
                    color = deltaColor,
                    style = TrovataTokens.type.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SegmentedSwitch(
                options = DashMetric.entries.map { it.label },
                selectedIndex = DashMetric.entries.indexOf(state.metric),
                onSelect = { onMetricChange(DashMetric.entries[it]) },
                onDark = true,
                modifier = Modifier.padding(top = 16.dp),
            )

            TrovataLineChart(
                values = state.trend.values,
                labels = state.trend.labels,
                lineColor = HeroAccent,
                onDark = true,
                yFormatter = yFormatter,
                markerPrefix = markerPrefix,
                modifier = Modifier.fillMaxWidth().height(156.dp).padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun WeekdayChartCard(state: DashboardUiState) {
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        TrovataColumnChart(
            values = state.weekday.values,
            labels = state.weekday.labels,
            columnColor = TrovataTokens.colors.brand,
            yFormatter = integerValueFormatter(),
            modifier = Modifier.fillMaxWidth().height(168.dp),
        )
    }
}

@Composable
private fun KpiGrid(kpis: List<DashKpi>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        kpis.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { kpi -> KpiCard(kpi = kpi, modifier = Modifier.weight(1f)) }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiCard(kpi: DashKpi, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val deltaColor = when (kpi.trend) {
        DashTrend.Up -> colors.jade
        DashTrend.Down -> colors.live
        DashTrend.Flat -> colors.ink4
    }
    TrovataCard(modifier = modifier, padding = 12.dp) {
        Column {
            Text(
                text = kpi.label.uppercase(),
                color = colors.ink4,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = kpi.value,
                color = colors.ink,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
            kpi.delta?.let { delta ->
                Text(
                    text = delta,
                    color = deltaColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun TopProductsCard(entries: List<DashTopProduct>) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Column {
            entries.forEachIndexed { index, entry ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.name,
                            color = colors.ink,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        Text(
                            text = entry.revenueLabel,
                            color = colors.ink3,
                            style = TrovataTokens.type.mono.copy(fontSize = 11.5.sp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HBar(progress = entry.fraction, color = colors.brand, modifier = Modifier.weight(1f))
                        Text(
                            text = "${entry.units} un",
                            color = colors.brand,
                            style = TrovataTokens.type.mono.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
                if (index < entries.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun SegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val colors = TrovataTokens.colors
    val track = if (onDark) Color.White.copy(alpha = 0.10f) else colors.surface2
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(track)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            val pillColor by animateColorAsState(
                targetValue = when {
                    active && onDark -> Color.White
                    active -> colors.brand
                    else -> Color.Transparent
                },
                animationSpec = tween(220),
                label = "seg-bg",
            )
            val textColor = when {
                active && onDark -> InsightsHeroPalette.gradientTop
                active -> Color.White
                onDark -> Color.White.copy(alpha = 0.6f)
                else -> colors.ink3
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(pillColor)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        TrovataCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Carregando painel…",
                    color = colors.ink3,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun HeroCardScaffold(
    state: DashboardUiState,
    onMetricChange: (DashMetric) -> Unit,
) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(20.dp)
    val placeholderValue = if (state.metric == DashMetric.Receita) "R$ —" else "—"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(InsightsHeroPalette.gradientTop, InsightsHeroPalette.gradientBottom),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.brand.copy(alpha = 0.40f), Color.Transparent),
                        radius = 520f,
                    ),
                ),
        )
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(HeroAccent, RoundedCornerShape(50)),
                )
                Text(
                    text = state.headlineLabel,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.12.em,
                )
            }

            Text(
                text = placeholderValue,
                color = Color.White.copy(alpha = 0.55f),
                style = TrovataTokens.type.mono.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.04).em,
                ),
                modifier = Modifier.padding(top = 10.dp),
            )

            SegmentedSwitch(
                options = DashMetric.entries.map { it.label },
                selectedIndex = DashMetric.entries.indexOf(state.metric),
                onSelect = { onMetricChange(DashMetric.entries[it]) },
                onDark = true,
                modifier = Modifier.padding(top = 16.dp),
            )

            LineChartPlaceholder(
                onDark = true,
                modifier = Modifier.fillMaxWidth().height(156.dp).padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun KpiGridScaffold() {
    val labels = listOf("Pedidos", "Ticket médio", "Conversão", "Sessões", "Itens vendidos", "Clientes ativos")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { label -> KpiCardScaffold(label = label, modifier = Modifier.weight(1f)) }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun KpiCardScaffold(label: String, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = modifier, padding = 12.dp) {
        Column {
            Text(
                text = label.uppercase(),
                color = colors.ink4,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = "—",
                color = colors.ink4,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun WeekdayChartScaffold() {
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        ColumnChartPlaceholder(modifier = Modifier.fillMaxWidth().height(168.dp))
    }
}

@Composable
private fun TopProductsScaffold() {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Column {
            repeat(3) { index ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaceholderBar(modifier = Modifier.fillMaxWidth(0.42f))
                        PlaceholderBar(modifier = Modifier.width(48.dp), height = 10.dp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HBar(progress = 0f, color = colors.brand, modifier = Modifier.weight(1f))
                        PlaceholderBar(modifier = Modifier.width(28.dp), height = 10.dp)
                    }
                }
                if (index < 2) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    val colors = TrovataTokens.colors
    Text(
        text = "Os números entram aqui assim que você abrir sessões e fechar pedidos nas chamadas.",
        color = colors.ink4,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp),
    )
}
