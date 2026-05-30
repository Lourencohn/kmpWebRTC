package app.trovata.cast.ui.screens.insights

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.local.centsToBrl
import app.trovata.cast.data.sample.InsightsHeroPalette
import app.trovata.cast.feature.insights.InsightKpi
import app.trovata.cast.feature.insights.InsightsUiState
import app.trovata.cast.feature.insights.TopProduct
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.HBar
import app.trovata.cast.ui.components.PlaceholderBar
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.Sparkline
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    modifier: Modifier = Modifier,
    onOpenAccount: () -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = "Vendas",
                    title = "Insights",
                    subtitle = state.subtitle,
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.filter,
                )
            }

            item { HeroRevenueCard(state) }

            if (state.hasData) {
                item { KpiGrid(state.kpis) }

                if (state.topProducts.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionLabel(text = "Mais pedidos no mês")
                            TopProductsList(state.topProducts)
                        }
                    }
                }
            } else {
                item { KpiGridScaffold() }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionLabel(text = "Mais pedidos no mês")
                        TopProductsScaffold()
                    }
                }
                item { EmptyHint() }
            }
        }
    }
}

@Composable
private fun HeroRevenueCard(state: InsightsUiState) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, shape, clip = false)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(InsightsHeroPalette.gradientTop, InsightsHeroPalette.gradientBottom),
                ),
                shape = shape,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.brand.copy(alpha = 0.45f), Color.Transparent),
                        radius = 320f,
                    ),
                    shape = shape,
                ),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(InsightsHeroPalette.accent, RoundedCornerShape(50)),
                )
                Text(
                    text = "VENDAS · ESTE MÊS",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.12.em,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.hasData) state.revenueLabel else "R$ —",
                        color = if (state.hasData) Color.White else Color.White.copy(alpha = 0.55f),
                        style = TrovataTokens.type.mono.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.04).em,
                        ),
                    )
                    if (state.hasData) {
                        state.deltaLabel?.let { delta ->
                            Text(
                                text = delta,
                                color = InsightsHeroPalette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TrovataTokens.type.mono,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                if (state.hasData && state.spark.size > 1) {
                    Sparkline(
                        data = state.spark,
                        width = 110.dp,
                        height = 44.dp,
                        color = InsightsHeroPalette.accent,
                        fill = true,
                        dot = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiGrid(kpis: List<InsightKpi>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        kpis.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { kpi ->
                    KpiCardView(kpi = kpi, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KpiCardView(kpi: InsightKpi, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = modifier, padding = 12.dp) {
        Column {
            Text(
                text = kpi.label.uppercase(),
                color = colors.ink4,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = kpi.value,
                color = colors.ink,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TopProductsList(entries: List<TopProduct>) {
    val colors = TrovataTokens.colors
    val maxUnits = (entries.maxOfOrNull { it.units } ?: 1).coerceAtLeast(1)
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
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = centsToBrl(entry.revenueCents),
                            color = colors.ink3,
                            style = TrovataTokens.type.mono.copy(fontSize = 11.5.sp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HBar(
                            progress = entry.units.toFloat() / maxUnits,
                            color = colors.brand,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${entry.units} un",
                            color = colors.brand,
                            style = TrovataTokens.type.mono.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
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
private fun KpiGridScaffold() {
    val labels = listOf("Pedidos", "Ticket médio", "Itens", "Sessões")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { label -> KpiCardScaffold(label = label, modifier = Modifier.weight(1f)) }
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
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = "—",
                color = colors.ink4,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TopProductsScaffold() {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Column {
            repeat(4) { index ->
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
                if (index < 3) {
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
        text = "Os números entram aqui assim que você fechar pedidos nas sessões.",
        color = colors.ink4,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp),
    )
}
