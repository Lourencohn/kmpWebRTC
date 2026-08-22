package app.trovata.cast.ui.screens.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.FashionPalette
import app.trovata.cast.data.remote.sfa.ProdutoGrade
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.sample.ProductSwatchPalette
import app.trovata.cast.data.sample.ProductTag
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.components.Btn
import app.trovata.cast.ui.components.BtnKind
import app.trovata.cast.ui.components.BtnSize
import app.trovata.cast.ui.components.Garment
import app.trovata.cast.ui.components.IconBtn
import app.trovata.cast.ui.components.IconBtnKind
import app.trovata.cast.ui.components.Pill
import app.trovata.cast.ui.components.PillTone
import app.trovata.cast.ui.components.ProductCard
import app.trovata.cast.ui.components.ProductCardSize
import app.trovata.cast.ui.components.SectionLabel
import app.trovata.cast.ui.icons.TrovataIcons
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource

data class ProductDetailSizeStock(
    val label: String,
    val stock: Int,
)

data class ProductDetailColor(
    val name: String,
    val color: Color,
)

@Composable
fun ProductDetailScreen(
    product: Product,
    modifier: Modifier = Modifier,
    imageUrls: List<String> = emptyList(),
    grade: ProdutoGrade? = null,
    related: List<Product> = emptyList(),
    inCallContext: Boolean = false,
    customerName: String? = null,
    onBack: () -> Unit,
    onPointAt: ((String) -> Unit)? = null,
    onShareLink: ((Product) -> Unit)? = null,
    onStartSession: ((Product) -> Unit)? = null,
) {
    val colors = TrovataTokens.colors
    val images = remember(product.ref, imageUrls) { imageUrls.ifEmpty { listOfNotNull(product.imageUrl) } }
    val sizes = remember(product.ref, grade) { grade?.toSizeStocks() ?: sampleSizesFor(product) }
    val swatches = remember(product.ref, grade) { grade?.toColors() ?: sampleSwatchesFor(product) }
    var selectedColor by remember(product.ref) { mutableStateOf(swatches.firstOrNull()?.name) }
    var selectedSize by remember(product.ref) { mutableStateOf(sizes.firstOrNull { it.stock > 0 }?.label ?: sizes.first().label) }
    var galleryIndex by remember(product.ref) { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 56.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                DetailTopBar(product = product, onBack = onBack)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ProductHero(
                    product = product,
                    images = images,
                    activeIndex = galleryIndex,
                    onIndexChange = { galleryIndex = it },
                )
            }
            item {
                Spacer(modifier = Modifier.height(14.dp))
                ProductSummary(product = product, inCallContext = inCallContext, customerName = customerName)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ColorSwatches(
                    options = swatches,
                    selectedName = selectedColor,
                    onSelect = { selectedColor = it },
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                StockBySize(
                    sizes = sizes,
                    selectedLabel = selectedSize,
                    onSelect = { selectedSize = it },
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CompositionCard()
            }
            if (related.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    RelatedProducts(products = related)
                }
            }
        }

        DetailActionBar(
            inCallContext = inCallContext,
            onPointAt = { onPointAt?.invoke(product.ref) },
            onShare = { onShareLink?.invoke(product) },
            onStartSession = { onStartSession?.invoke(product) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DetailTopBar(product: Product, onBack: () -> Unit) {
    val colors = TrovataTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(
            icon = TrovataIcons.back,
            onClick = onBack,
            kind = IconBtnKind.Line,
            size = 36.dp,
            contentDescription = "Voltar",
        )
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "DETALHES DO PRODUTO",
                color = colors.ink4,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            )
            Text(
                text = product.ref,
                color = colors.ink,
                style = TrovataTokens.type.mono.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.04.em,
                ),
            )
        }
        IconBtn(
            icon = TrovataIcons.share,
            onClick = {},
            kind = IconBtnKind.Line,
            size = 36.dp,
            contentDescription = "Compartilhar",
        )
    }
}

@Composable
private fun ProductHero(
    product: Product,
    images: List<String>,
    activeIndex: Int,
    onIndexChange: (Int) -> Unit,
) {
    val colors = TrovataTokens.colors
    val tint = FashionPalette[product.tintIndex]
    val shape = RoundedCornerShape(18.dp)
    val count = if (images.isNotEmpty()) images.size else 4
    val safeIndex = activeIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(shape)
                .background(tint.background),
            contentAlignment = Alignment.Center,
        ) {
            when {
                images.isNotEmpty() -> AsyncImage(
                    model = images[safeIndex],
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                product.image != null -> Image(
                    painter = painterResource(product.image),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Garment(kind = product.garment, tint = tint.foreground, size = 180.dp)
            }
            product.tag?.let { tag ->
                HeroTag(
                    tag = tag,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color(0xCC0E1116), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = TrovataIcons.layers,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = "${safeIndex + 1} / $count",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.04.em,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(count) { i ->
                    val active = i == safeIndex
                    Box(
                        modifier = Modifier
                            .width(if (active) 16.dp else 6.dp)
                            .height(6.dp)
                            .background(
                                if (active) Color(0xA60E1116) else Color(0x330E1116),
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(count) { i ->
                val active = i == safeIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.background)
                        .border(
                            if (active) 1.5.dp else 1.dp,
                            if (active) colors.ink else colors.line,
                            RoundedCornerShape(8.dp),
                        )
                        .clickableNoIndication { onIndexChange(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        images.isNotEmpty() -> AsyncImage(
                            model = images[i],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        product.image != null -> Image(
                            painter = painterResource(product.image),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Garment(kind = product.garment, tint = tint.foreground, size = 36.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTag(tag: ProductTag, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val (bg, label) = when (tag) {
        ProductTag.Novo -> colors.ink to "Novo"
        ProductTag.TopVenda -> colors.jade to "Top venda"
        ProductTag.PreVenda -> colors.warn to "Pré-venda"
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProductSummary(product: Product, inCallContext: Boolean, customerName: String?) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (inCallContext) {
            val text = customerName?.let { "$it está vendo" } ?: "Cliente está vendo"
            Pill(text = text, tone = PillTone.Brand, icon = TrovataIcons.eye)
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    color = colors.ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.02).em,
                    lineHeight = 22.sp,
                )
                Text(
                    text = "Linho 100% · feito em São Paulo",
                    color = colors.ink3,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ATACADO",
                    color = colors.ink4,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.em,
                )
                Text(
                    text = product.price,
                    color = colors.ink,
                    style = TrovataTokens.type.monoBig.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).em,
                    ),
                )
                Text(
                    text = "min ${product.moq}un",
                    color = colors.ink4,
                    fontSize = 10.5.sp,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatches(
    options: List<ProductDetailColor>,
    selectedName: String?,
    onSelect: (String) -> Unit,
) {
    val colors = TrovataTokens.colors
    val selectedLabel = options.firstOrNull { it.name == selectedName }?.name ?: options.firstOrNull()?.name.orEmpty()
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Cor · $selectedLabel",
            action = {
                Text(
                    text = "${options.size} disponíveis",
                    color = colors.ink4,
                    fontSize = 10.5.sp,
                )
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { swatch ->
                val active = swatch.name == selectedName
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(swatch.color, CircleShape)
                        .border(
                            if (active) 2.5.dp else 1.dp,
                            if (active) colors.ink else colors.line,
                            CircleShape,
                        )
                        .clickableNoIndication { onSelect(swatch.name) },
                )
            }
        }
    }
}

@Composable
private fun StockBySize(
    sizes: List<ProductDetailSizeStock>,
    selectedLabel: String,
    onSelect: (String) -> Unit,
) {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel(
            text = "Grade · estoque por tamanho",
            action = { Pill(text = "Em estoque", tone = PillTone.Jade, icon = TrovataIcons.check) },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp), clip = false)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colors.line, RoundedCornerShape(16.dp)),
        ) {
            sizes.forEachIndexed { index, size ->
                val active = size.label == selectedLabel
                val rowBg = if (active) colors.brandTint else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickableNoIndication { onSelect(size.label) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (active) colors.brand else colors.surface2, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = size.label,
                            color = if (active) Color.White else colors.ink2,
                            style = TrovataTokens.type.mono.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tamanho ${size.label}",
                            color = colors.ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = when {
                                size.stock >= 12 -> "Disponibilidade alta"
                                size.stock >= 6 -> "Estoque limitado"
                                size.stock > 0 -> "Pouquíssimas peças"
                                else -> "Esgotado"
                            },
                            color = colors.ink3,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = size.stock.toString(),
                            color = colors.ink,
                            style = TrovataTokens.type.mono.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.01).em,
                            ),
                        )
                        Text(
                            text = "UN",
                            color = colors.ink4,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.08.em,
                        )
                    }
                }
                if (index < sizes.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                }
            }
        }
    }
}

@Composable
private fun CompositionCard() {
    val colors = TrovataTokens.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionLabel(text = "Composição & cuidados")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp), clip = false)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            val rows = listOf(
                "Tecido" to "Linho 100%",
                "Gramatura" to "160 g/m²",
                "Origem" to "São Paulo · BR",
                "Lavagem" to "30°C · sem alvejante",
            ).chunked(2)
            rows.forEachIndexed { i, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { (label, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label.uppercase(),
                                color = colors.ink4,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.06.em,
                            )
                            Text(
                                text = value,
                                color = colors.ink,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                if (i < rows.lastIndex) Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun RelatedProducts(products: List<Product>) {
    Column {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionLabel(text = "Combina bem com")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            products.forEach { p ->
                Box(modifier = Modifier.width(140.dp)) {
                    ProductCard(product = p, size = ProductCardSize.Sm)
                }
            }
        }
    }
}

@Composable
private fun DetailActionBar(
    inCallContext: Boolean,
    onPointAt: () -> Unit,
    onShare: () -> Unit,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.line, RoundedCornerShape(0.dp))
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(
            icon = TrovataIcons.heart,
            onClick = {},
            kind = IconBtnKind.Line,
            size = 44.dp,
            contentDescription = "Favoritar",
        )
        if (inCallContext) {
            Btn(
                text = "Apontar agora",
                onClick = onPointAt,
                kind = BtnKind.Primary,
                size = BtnSize.Md,
                icon = TrovataIcons.pointer,
                modifier = Modifier.weight(1f),
            )
        } else {
            Btn(
                text = "Compartilhar",
                onClick = onShare,
                kind = BtnKind.Surface,
                size = BtnSize.Md,
                icon = TrovataIcons.share,
                modifier = Modifier.weight(1f),
            )
            Btn(
                text = "Iniciar sessão",
                onClick = onStartSession,
                kind = BtnKind.Primary,
                size = BtnSize.Md,
                icon = TrovataIcons.video,
                modifier = Modifier.weight(1.3f),
            )
        }
    }
}

@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

private fun sampleSizesFor(product: Product): List<ProductDetailSizeStock> {
    if (product.sizes.size == 1 && product.sizes.first().equals("Único", ignoreCase = true)) {
        return listOf(ProductDetailSizeStock("Único", 36))
    }
    val seeds = listOf(24, 36, 18, 6, 12)
    return product.sizes.mapIndexed { i, label ->
        ProductDetailSizeStock(label = label, stock = seeds[i % seeds.size])
    }
}

private fun sampleSwatchesFor(product: Product): List<ProductDetailColor> {
    val palette = ProductSwatchPalette.swatches
    val names = listOf("Verde-musgo", "Areia", "Caramelo", "Grafite")
    val count = product.colorCount.coerceAtLeast(1).coerceAtMost(palette.size)
    return List(count) { i -> ProductDetailColor(name = names[i % names.size], color = palette[i % palette.size]) }
}


private fun ProdutoGrade.toSizeStocks(): List<ProductDetailSizeStock> {
    val tamanhos = cores.firstOrNull()?.tamanhos.orEmpty()
    if (tamanhos.isEmpty()) return listOf(ProductDetailSizeStock("Único", saldoTotal))
    return tamanhos.map { ProductDetailSizeStock(label = it.label, stock = it.disponivel) }
}

private fun ProdutoGrade.toColors(): List<ProductDetailColor> {
    val palette = ProductSwatchPalette.swatches
    return cores.mapIndexed { index, cor ->
        ProductDetailColor(name = cor.descricao, color = palette[index % palette.size])
    }
}
