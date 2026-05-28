package app.trovata.cast.data.sample

import androidx.compose.ui.graphics.Color
import app.trovata.cast.resources.Res
import app.trovata.cast.resources.product_ch_3484980
import app.trovata.cast.resources.product_ch_3485025
import app.trovata.cast.resources.product_ch_3485059
import app.trovata.cast.resources.product_ch_3485087
import app.trovata.cast.resources.product_ch_3485278
import app.trovata.cast.resources.product_ch_3485310
import app.trovata.cast.resources.product_dm_2025
import app.trovata.cast.resources.product_lee_2842
import app.trovata.cast.ui.components.GarmentKind
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

enum class ProductTag { Novo, TopVenda, PreVenda }

data class Product(
    val ref: String,
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

object SampleCatalog {
    val collection: String = "Verão 26 · Atelier Norte"

    val products: List<Product> = listOf(
        Product(
            ref = "CH-3485059",
            name = "Bolsa Contemporâneo Couro",
            garment = GarmentKind.Shirt,
            tintIndex = 2,
            price = "R$ 189,90",
            moq = 4,
            sizes = listOf("Único"),
            colorCount = 2,
            tag = ProductTag.Novo,
            image = Res.drawable.product_ch_3485059,
        ),
        Product(
            ref = "CH-3485087",
            name = "Bolsa Elegance Monograma",
            garment = GarmentKind.Shirt,
            tintIndex = 2,
            price = "R$ 249,00",
            moq = 4,
            sizes = listOf("Único"),
            colorCount = 1,
            tag = ProductTag.TopVenda,
            image = Res.drawable.product_ch_3485087,
        ),
        Product(
            ref = "CH-3484980",
            name = "Bolsa Matelassê Cute",
            garment = GarmentKind.Shirt,
            tintIndex = 5,
            price = "R$ 199,00",
            moq = 6,
            sizes = listOf("Único"),
            colorCount = 1,
            tag = null,
            image = Res.drawable.product_ch_3484980,
        ),
        Product(
            ref = "CH-3485025",
            name = "Bolsa New Cristal Nude",
            garment = GarmentKind.Shirt,
            tintIndex = 0,
            price = "R$ 219,00",
            moq = 4,
            sizes = listOf("Único"),
            colorCount = 2,
            tag = ProductTag.Novo,
            image = Res.drawable.product_ch_3485025,
        ),
        Product(
            ref = "CH-3485278",
            name = "Bolsa Couro Texturizado",
            garment = GarmentKind.Shirt,
            tintIndex = 2,
            price = "R$ 169,00",
            moq = 6,
            sizes = listOf("Único"),
            colorCount = 1,
            tag = null,
            image = Res.drawable.product_ch_3485278,
        ),
        Product(
            ref = "CH-3485310",
            name = "Bolsa Carbono Trama",
            garment = GarmentKind.Shirt,
            tintIndex = 5,
            price = "R$ 229,00",
            moq = 4,
            sizes = listOf("Único"),
            colorCount = 1,
            tag = ProductTag.PreVenda,
            image = Res.drawable.product_ch_3485310,
        ),
        Product(
            ref = "LEE-2842",
            name = "Bolsa Cute Bear Bege",
            garment = GarmentKind.Shirt,
            tintIndex = 6,
            price = "R$ 179,00",
            moq = 4,
            sizes = listOf("Único"),
            colorCount = 1,
            tag = null,
            image = Res.drawable.product_lee_2842,
        ),
        Product(
            ref = "DM-2025",
            name = "Bolsa Dumond Shopping",
            garment = GarmentKind.Shirt,
            tintIndex = 3,
            price = "R$ 269,00",
            moq = 3,
            sizes = listOf("Único"),
            colorCount = 1,
            tag = ProductTag.TopVenda,
            image = Res.drawable.product_dm_2025,
        ),
    )
}

object ProductSwatchPalette {
    val swatches: List<Color> = listOf(
        Color(0xFF3F4744),
        Color(0xFFA5806A),
        Color(0xFFC7B79B),
        Color(0xFF5E5B57),
    )
}
