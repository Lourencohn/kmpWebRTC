package app.trovata.cast.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.FocusAccent
import app.trovata.cast.data.sample.FocusEntry
import app.trovata.cast.data.sample.FunnelStep
import app.trovata.cast.data.sample.InsightsHeroPalette
import app.trovata.cast.data.sample.KpiCard
import app.trovata.cast.data.sample.SampleInsights
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.HBar
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.Sparkline
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.components.TrovataCard
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun InsightsScreen(
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
                    eyebrow = "Atelier Norte · Outono 26",
                    title = "Insights",
                    subtitle = "23 dias · maio em curso",
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.filter,
                )
            }

            item { PeriodSelector() }

            item { HeroRevenueCard() }

            item { KpiGrid() }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Tempo em foco vs. conversão",
                        action = {
                            Text(
                                text = "Top 5 · este mês",
                                color = colors.ink4,
                                fontSize = 11.sp,
                            )
                        },
                    )
                    FocusList(SampleInsights.focus)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Funil da sessão")
                    FunnelCard(SampleInsights.funnel)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(text = "Insight da semana")
                    InsightCallout()
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SampleInsights.periods.forEach { p ->
            val borderColor = if (p.active) colors.line else Color.Transparent
            val bg = if (p.active) colors.surface else Color.Transparent
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(if (p.active) 1.dp else 0.dp, RoundedCornerShape(8.dp), clip = false)
                    .background(bg, RoundedCornerShape(8.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = p.label,
                    color = if (p.active) colors.ink else colors.ink4,
                    fontSize = 12.sp,
                    fontWeight = if (p.active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun HeroRevenueCard() {
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
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.04).em,
                                ),
                            ) {
                                append(SampleInsights.headerNumberMain)
                            }
                            withStyle(
                                SpanStyle(
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            ) {
                                append(SampleInsights.headerNumberSuffix)
                            }
                        },
                        style = TrovataTokens.type.mono,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = SampleInsights.headerDelta,
                            color = InsightsHeroPalette.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TrovataTokens.type.mono,
                        )
                        Text(
                            text = SampleInsights.headerCompare,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                        )
                    }
                }
                Sparkline(
                    data = SampleInsights.revenueSpark,
                    width = 110.dp,
                    height = 44.dp,
                    color = InsightsHeroPalette.accent,
                    fill = true,
                    dot = true,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SampleInsights.heroStats.forEach { stat ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stat.label.uppercase(),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.08.em,
                        )
                        Text(
                            text = stat.value,
                            color = Color.White,
                            style = TrovataTokens.type.mono.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        stat.sub?.let { sub ->
                            Text(
                                text = sub,
                                color = Color.White.copy(alpha = 0.55f),
                                style = TrovataTokens.type.mono.copy(fontSize = 10.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiGrid() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SampleInsights.kpis.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { kpi ->
                    KpiCardView(kpi = kpi, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KpiCardView(kpi: KpiCard, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val accentColor = when {
        kpi.label == "Conversão" -> colors.jade
        kpi.label == "Tempo em call" -> colors.ink4
        else -> colors.brand
    }
    TrovataCard(modifier = modifier, padding = 12.dp) {
        Column {
            Text(
                text = kpi.label.uppercase(),
                color = colors.ink4,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = kpi.value,
                    color = colors.ink,
                    style = TrovataTokens.type.mono.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).em,
                    ),
                )
                Sparkline(
                    data = kpi.spark,
                    width = 50.dp,
                    height = 20.dp,
                    color = accentColor,
                    fill = false,
                    dot = false,
                )
            }
            Text(
                text = kpi.delta,
                color = colors.ink3,
                fontSize = 10.5.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FocusList(entries: List<FocusEntry>) {
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
                            text = entry.productName,
                            color = colors.ink,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = entry.timeInFocus,
                            color = colors.ink3,
                            style = TrovataTokens.type.mono.copy(fontSize = 11.5.sp),
                        )
                    }
                    val accent = when (entry.accent) {
                        FocusAccent.Jade -> colors.jade
                        FocusAccent.Brand -> colors.brand
                        FocusAccent.Warn -> colors.warn
                        FocusAccent.Muted -> colors.ink4
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HBar(
                            progress = entry.conversion / 100f,
                            color = accent,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${entry.conversion}%",
                            color = accent,
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
private fun FunnelCard(steps: List<FunnelStep>) {
    val colors = TrovataTokens.colors
    TrovataCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Column {
            steps.forEachIndexed { index, step ->
                val color = when (index) {
                    0 -> colors.ink3
                    steps.lastIndex -> colors.jade
                    else -> colors.brand
                }
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = step.label,
                            color = colors.ink,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = step.count.toString(),
                                color = colors.ink,
                                style = TrovataTokens.type.mono.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.02).em,
                                ),
                            )
                            Text(
                                text = "${step.percent}%",
                                color = colors.ink4,
                                style = TrovataTokens.type.mono.copy(fontSize = 11.sp),
                            )
                        }
                    }
                    HBar(
                        progress = step.percent / 100f,
                        color = color,
                        height = 5.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                if (index < steps.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun InsightCallout() {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape, clip = false)
            .background(colors.jadeTint, shape)
            .border(1.dp, colors.jadeTint, shape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.jade, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TrovataIcons.zap,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = SampleInsights.insightText,
            color = colors.jade2,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}
