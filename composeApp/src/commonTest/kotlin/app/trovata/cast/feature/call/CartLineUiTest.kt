package app.trovata.cast.feature.call

import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.CartItemSize
import app.trovata.cast.data.remote.sfa.dto.PrecoDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CartLineUiTest {

    private val line = CartItemLine(
        itemId = 51,
        produtoPreId = 4821,
        ref = "22587",
        name = "Blusa Tricot",
        color = "AZUL",
        imageUrl = null,
        quantity = 18,
        totalCents = 260820,
        sizes = listOf(
            CartItemSize(complemento2Id = 11, label = "P", quantity = 12),
            CartItemSize(complemento2Id = 12, label = "M", quantity = 6),
        ),
    )

    @Test
    fun describesTheSizesSavedOnTheLine() {
        val ui = line.toCartLineUi()

        assertEquals(51L, ui.itemId)
        assertEquals("22587", ui.ref)
        assertEquals(18, ui.units)
        assertEquals(260820L, ui.totalCents)
        assertEquals("P 12un · M 6un", ui.sizesLabel)
    }

    @Test
    fun itemWithoutValueDoesNotBreakTheTotal() {
        val ui = line.copy(totalCents = null).toCartLineUi()

        assertEquals(0L, ui.totalCents)
    }

    @Test
    fun fractionalPriceBecomesExactCents() {
        assertEquals(14490L, PrecoDto(numerador = "1449", denominador = "10").cents)
        assertEquals(260820L, PrecoDto(numerador = "26082", denominador = "10").cents)
        assertEquals(3333L, PrecoDto(numerador = "100", denominador = "3").cents)
    }

    @Test
    fun priceFallsBackToDecimalValueWhenFractionIsMissing() {
        assertEquals(14490L, PrecoDto(valor = "144.900000").cents)
        assertNull(PrecoDto().cents)
    }
}
