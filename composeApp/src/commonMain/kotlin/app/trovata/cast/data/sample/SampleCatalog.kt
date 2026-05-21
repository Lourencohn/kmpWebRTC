package app.trovata.cast.data.sample

import androidx.compose.ui.graphics.Color
import app.trovata.cast.ui.components.GarmentKind

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
)

object SampleCatalog {
    val collection: String = "Coleção Outono · Atelier Norte"

    val products: List<Product> = listOf(
        Product("AN-104", "Blusa Tricot Canelado", GarmentKind.Sweater, 0, "R$ 89,90", 6, listOf("PP", "P", "M", "G", "GG"), 3, ProductTag.Novo),
        Product("AN-217", "Camisa Linho Manga Longa", GarmentKind.Shirt, 1, "R$ 119,00", 6, listOf("P", "M", "G", "GG"), 4, null),
        Product("AN-088", "Vestido Midi Algodão", GarmentKind.Dress, 4, "R$ 159,00", 4, listOf("P", "M", "G"), 2, ProductTag.TopVenda),
        Product("AN-301", "Polo Bordado Frente", GarmentKind.Polo, 3, "R$ 99,00", 6, listOf("P", "M", "G", "GG"), 5, null),
        Product("AN-156", "Calça Wide Alfaiataria", GarmentKind.Pants, 5, "R$ 169,00", 4, listOf("36", "38", "40", "42", "44"), 3, null),
        Product("AN-422", "Jaqueta Sarja Oversized", GarmentKind.Jacket, 2, "R$ 229,00", 3, listOf("P", "M", "G"), 2, ProductTag.PreVenda),
        Product("AN-512", "Camiseta Algodão Pima", GarmentKind.Tee, 6, "R$ 59,90", 8, listOf("PP", "P", "M", "G", "GG"), 6, null),
        Product("AN-077", "Tênis Couro Curado", GarmentKind.Shoe, 0, "R$ 279,00", 3, listOf("36", "37", "38", "39", "40", "41", "42", "43"), 2, null),
        Product("AN-621", "Saia Plissada Midi", GarmentKind.Skirt, 7, "R$ 139,00", 4, listOf("P", "M", "G"), 3, null),
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
