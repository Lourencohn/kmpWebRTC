package app.trovata.cast.ui.components

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource

data class GarmentTint(val background: Color, val foreground: Color)

object FashionPalette {
    val tints: List<GarmentTint> = listOf(
        GarmentTint(Color(0xFFEEEAE0), Color(0xFF7C6E58)),
        GarmentTint(Color(0xFFE6EAE5), Color(0xFF5C6C5F)),
        GarmentTint(Color(0xFFE8E4DC), Color(0xFF534234)),
        GarmentTint(Color(0xFFDDE4E6), Color(0xFF3A5260)),
        GarmentTint(Color(0xFFE8DEDA), Color(0xFF8B4C44)),
        GarmentTint(Color(0xFFE0DCD2), Color(0xFF3D3833)),
        GarmentTint(Color(0xFFF0E7D6), Color(0xFF7A5A2C)),
        GarmentTint(Color(0xFFD8DDD2), Color(0xFF3C4530)),
    )

    operator fun get(index: Int): GarmentTint = tints[index.mod(tints.size)]
}

object ProductSwatchPalette {
    val swatches: List<Color> = listOf(
        Color(0xFF3F4744),
        Color(0xFFA5806A),
        Color(0xFFC7B79B),
        Color(0xFF5E5B57),
    )
}

enum class ProductTag { Novo, TopVenda, PreVenda }

data class Product(
    val ref: String,
    val produtoPreId: Long? = null,
    val name: String,
    val garment: GarmentKind,
    val tintIndex: Int,
    val price: String,
    val moq: Int,
    val sizes: List<String>,
    val colorCount: Int,
    val tag: ProductTag? = null,
    val image: DrawableResource? = null,
    val imageUrl: String? = null,
)
