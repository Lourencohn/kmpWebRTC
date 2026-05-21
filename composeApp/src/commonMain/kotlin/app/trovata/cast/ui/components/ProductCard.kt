package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.FashionPalette
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.sample.ProductSwatchPalette
import app.trovata.cast.data.sample.ProductTag
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.color.oklch

enum class ProductCardSize { Sm, Md, Lg }

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    size: ProductCardSize = ProductCardSize.Md,
    highlight: Boolean = false,
    pointed: Boolean = false,
    inCart: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    val colors = TrovataTokens.colors
    val type = TrovataTokens.type
    val dims = when (size) {
        ProductCardSize.Sm -> ProductCardDims(96.dp, 8.dp, 11.5.sp, 9.5.sp)
        ProductCardSize.Md -> ProductCardDims(128.dp, 10.dp, 12.5.sp, 10.sp)
        ProductCardSize.Lg -> ProductCardDims(168.dp, 12.dp, 13.5.sp, 10.5.sp)
    }
    val tint = FashionPalette[product.tintIndex]
    val borderColor = if (highlight) colors.brand else colors.line
    val shape = RoundedCornerShape(14.dp)

    val baseModifier = modifier
        .shadow(elevation = if (highlight) 4.dp else 1.dp, shape = shape, clip = false)
        .background(colors.surface, shape)
        .border(if (highlight) 2.dp else 1.dp, borderColor, shape)
    val clickableModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Column(modifier = clickableModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dims.imageHeight)
                .background(tint.background),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Garment(
                    kind = product.garment,
                    tint = tint.foreground,
                    size = dims.imageHeight * 0.74f,
                )
            }
            if (product.tag != null) {
                TagChip(
                    tag = product.tag,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }
            ColorDots(
                count = product.colorCount,
                accent = tint.foreground,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
            if (inCart > 0) {
                CartBadge(
                    count = inCart,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
            if (pointed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, oklch(60.0, 0.18, 30.0), shape),
                )
            }
        }

        Column(modifier = Modifier.padding(dims.contentPadding)) {
            Text(
                text = product.ref,
                style = type.mono.copy(color = colors.ink4),
                fontSize = dims.refSize,
                letterSpacing = 0.04.em,
            )
            Text(
                text = product.name,
                color = colors.ink,
                fontSize = dims.fontSize,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.01).em,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = product.price,
                    color = colors.ink,
                    fontSize = dims.fontSize,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "min ${product.moq}un",
                    color = colors.ink4,
                    fontSize = dims.refSize,
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: ProductTag, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    val (bg, label) = when (tag) {
        ProductTag.Novo -> colors.ink to "Novo"
        ProductTag.TopVenda -> colors.jade to "Top venda"
        ProductTag.PreVenda -> colors.warn to "Pré-venda"
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.005).sp,
        )
    }
}

@Composable
private fun ColorDots(count: Int, accent: Color, modifier: Modifier = Modifier) {
    val swatches = ProductSwatchPalette.swatches
    val visible = count.coerceAtMost(swatches.size)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(visible) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(swatches[i % swatches.size], CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
            )
        }
        if (count > swatches.size) {
            Text(
                text = "+${count - swatches.size}",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun CartBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = TrovataTokens.colors
    Row(
        modifier = modifier
            .background(colors.jade, RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = app.trovata.cast.ui.icons.TrovataIcons.check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class ProductCardDims(
    val imageHeight: Dp,
    val contentPadding: Dp,
    val fontSize: TextUnit,
    val refSize: TextUnit,
)
