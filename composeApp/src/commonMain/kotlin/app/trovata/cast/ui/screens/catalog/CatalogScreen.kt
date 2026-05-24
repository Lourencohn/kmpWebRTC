package app.trovata.cast.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.FashionPalette
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Garment
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ProductCard
import app.trovata.cast.ui.components.ProductCardSize
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.components.TabHeader
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    onOpenAccount: () -> Unit = {},
    onOpenProduct: (app.trovata.cast.data.sample.Product) -> Unit = {},
) {
    val colors = TrovataTokens.colors
    val products = SampleCatalog.products
    val topOfWeek = listOf(products[1], products[3], products[7])
    val grid = listOf(products[0], products[2], products[4], products[5], products[6])

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TabHeader(
                    eyebrow = "Atelier Norte · Verão 26",
                    title = "Catálogo",
                    subtitle = "8 SKUs · 3 estreias · 1 pré-venda",
                    onOpenAccount = onOpenAccount,
                    secondaryIcon = TrovataIcons.plus,
                )
            }

            item { SearchBar() }

            item { FilterChips() }

            item { CollectionHero() }

            item { StatsStrip() }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Top da semana",
                        action = {
                            Text(
                                text = "Ver todos",
                                color = colors.brand,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    topOfWeek.forEachIndexed { index, product ->
                        TopOfWeekCard(
                            product = product,
                            growth = listOf(42, 28, 12)[index],
                            ordersLabel = listOf("18 ped.", "14 ped.", "9 ped.")[index],
                            onClick = { onOpenProduct(product) },
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(
                        text = "Todos os produtos · 47",
                        action = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = TrovataIcons.grid,
                                    contentDescription = null,
                                    tint = colors.ink,
                                    modifier = Modifier.size(14.dp),
                                )
                                Icon(
                                    imageVector = TrovataIcons.list,
                                    contentDescription = null,
                                    tint = colors.ink4,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        grid.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                pair.forEach { product ->
                                    ProductCard(
                                        product = product,
                                        size = ProductCardSize.Sm,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenProduct(product) },
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        ) {
            Btn(
                text = "Adicionar",
                onClick = {},
                kind = BtnKind.Dark,
                size = BtnSize.Md,
                icon = TrovataIcons.plus,
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(999.dp), clip = false),
            )
        }
    }
}

@Composable
private fun SearchBar() {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp), clip = false)
            .background(colors.surface, RoundedCornerShape(14.dp))
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = TrovataIcons.search,
            contentDescription = null,
            tint = colors.ink4,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Buscar por SKU, nome, cor...",
            color = colors.ink4,
            fontSize = 13.5.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .border(1.dp, colors.line, RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = "⌘K",
                color = colors.ink5,
                style = TrovataTokens.type.mono.copy(fontSize = 10.5.sp),
            )
        }
    }
}

@Composable
private fun FilterChips() {
    val items = listOf(
        "Todos" to PillTone.Dark,
        "Verão 26" to PillTone.Neutral,
        "Chenson" to PillTone.Neutral,
        "LEE" to PillTone.Neutral,
        "Dumond" to PillTone.Neutral,
        "Top venda" to PillTone.Neutral,
        "Pré-venda" to PillTone.Neutral,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (label, tone) ->
            Pill(text = label, tone = tone)
        }
    }
}

@Composable
private fun CollectionHero() {
    val colors = TrovataTokens.colors
    val products = SampleCatalog.products
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, shape, clip = false)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2A3431), Color(0xFF1B2422)),
                ),
                shape = shape,
            )
            .border(1.dp, colors.line, shape)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "COLEÇÃO EM DESTAQUE",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.14.em,
                )
                Column {
                    Text(
                        text = "Verão",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.035).em,
                        lineHeight = 33.sp,
                    )
                    Text(
                        text = "vinte e seis",
                        color = Color(0xFFE2D7B8),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.035).em,
                        lineHeight = 32.sp,
                    )
                }
                Text(
                    text = "8 SKUs · 3 estreias · Chenson, LEE, Dumond.\nBolsas femininas atacado.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Btn(
                        text = "Ver coleção",
                        onClick = {},
                        kind = BtnKind.Surface,
                        size = BtnSize.Sm,
                        icon = TrovataIcons.arrowRight,
                    )
                    Btn(
                        text = "Editar",
                        onClick = {},
                        kind = BtnKind.Dark,
                        size = BtnSize.Sm,
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(products[0], products[1], products[2]).forEachIndexed { i, p ->
                    val tint = FashionPalette[p.tintIndex]
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .rotate(if (i == 0) -4f else if (i == 1) 0f else 4f)
                            .background(tint.background, RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Garment(kind = p.garment, tint = tint.foreground, size = 38.dp)
                    }
                }
            }
        }
    }
}

private data class CatalogStat(val label: String, val value: String, val accent: StatAccent)
private enum class StatAccent { Ink, Brand, Warn, Live }

@Composable
private fun StatsStrip() {
    val colors = TrovataTokens.colors
    val stats = listOf(
        CatalogStat("SKUs", "8", StatAccent.Ink),
        CatalogStat("Novos", "3", StatAccent.Brand),
        CatalogStat("Pré-venda", "1", StatAccent.Warn),
        CatalogStat("Baixo", "0", StatAccent.Live),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stats.forEach { stat ->
            val color = when (stat.accent) {
                StatAccent.Ink -> colors.ink
                StatAccent.Brand -> colors.brand
                StatAccent.Warn -> colors.warn
                StatAccent.Live -> colors.live
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stat.label.uppercase(),
                    color = colors.ink4,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.em,
                )
                Text(
                    text = stat.value,
                    color = color,
                    style = TrovataTokens.type.monoBig.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).em,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TopOfWeekCard(
    product: app.trovata.cast.data.sample.Product,
    growth: Int,
    ordersLabel: String,
    onClick: () -> Unit = {},
) {
    val colors = TrovataTokens.colors
    Column(
        modifier = Modifier.width(158.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ProductCard(
            product = product,
            size = ProductCardSize.Sm,
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = TrovataIcons.trend,
                contentDescription = null,
                tint = colors.jade2,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = "+$growth%",
                color = colors.jade2,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "· $ordersLabel",
                color = colors.ink3,
                fontSize = 10.5.sp,
            )
        }
    }
}
