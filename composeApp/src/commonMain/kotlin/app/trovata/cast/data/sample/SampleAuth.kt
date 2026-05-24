package app.trovata.cast.data.sample

import androidx.compose.ui.graphics.Color

data class AuthBrand(
    val name: String,
    val tag: String,
    val skuCount: Int,
    val colors: List<Color>,
    val selected: Boolean = false,
)

object SampleAuth {
    const val phonePlaceholder: String = "(31) 99876-"
    const val phoneVisible: String = "+55 (31) 99876-2104"
    const val resendCountdown: String = "00:42"
    val codeDigits: List<String> = listOf("8", "4", "3", "2", "", "")
    const val activeDigitIndex: Int = 4

    val brands: List<AuthBrand> = listOf(
        AuthBrand(
            name = "Atelier Norte",
            tag = "Moda atacado",
            skuCount = 47,
            colors = listOf(Color(0xFF3F4744), Color(0xFFA5806A), Color(0xFFC7B79B)),
            selected = true,
        ),
        AuthBrand(
            name = "Casa Maré",
            tag = "Decoração",
            skuCount = 122,
            colors = listOf(Color(0xFFDDE4E6), Color(0xFF3A5260), Color(0xFFE8E4DC)),
        ),
        AuthBrand(
            name = "Cobogó",
            tag = "Calçados artesanais",
            skuCount = 28,
            colors = listOf(Color(0xFF7C6E58), Color(0xFF534234), Color(0xFFEEEAE0)),
        ),
    )

    enum class PriceTier(val label: String, val detail: String) {
        AtacadoA("Atacado A", "Min 6un"),
        AtacadoB("Atacado B", "Min 4un"),
        Mostruario("Mostruário", "Avulso"),
    }

    val selectedTier: PriceTier = PriceTier.AtacadoB
}
