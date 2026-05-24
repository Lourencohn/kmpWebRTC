package app.trovata.cast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.sample.FashionPalette
import app.trovata.cast.data.sample.Product
import app.trovata.cast.theme.TrovataTokens
import org.jetbrains.compose.resources.painterResource

enum class ProductRowSize { Sm, Md, Lg }

@Composable
fun ProductRow(
    product: Product,
    modifier: Modifier = Modifier,
    size: ProductRowSize = ProductRowSize.Md,
    quantity: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = TrovataTokens.colors
    val type = TrovataTokens.type
    val thumb: Dp = when (size) {
        ProductRowSize.Sm -> 36.dp
        ProductRowSize.Md -> 44.dp
        ProductRowSize.Lg -> 52.dp
    }
    val tint = FashionPalette[product.tintIndex]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(thumb)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.background),
            contentAlignment = Alignment.Center,
        ) {
            if (product.image != null) {
                Image(
                    painter = painterResource(product.image),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Garment(kind = product.garment, tint = tint.foreground, size = thumb * 0.74f)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.ref,
                style = type.mono.copy(color = colors.ink4),
                fontSize = 9.5.sp,
                letterSpacing = 0.04.em,
            )
            Text(
                text = product.name,
                color = colors.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.01).em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (quantity != null) {
                Text(
                    text = "$quantity un · ${product.price}",
                    color = colors.ink3,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) trailing()
    }
}
