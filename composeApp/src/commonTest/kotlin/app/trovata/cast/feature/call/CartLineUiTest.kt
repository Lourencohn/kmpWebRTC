package app.trovata.cast.feature.call

import app.trovata.cast.data.remote.sfa.CarrinhoItemLinha
import app.trovata.cast.data.remote.sfa.CarrinhoItemTamanho
import app.trovata.cast.data.remote.sfa.dto.PrecoDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CartLineUiTest {

    private val linha = CarrinhoItemLinha(
        itemId = 51,
        produtoPreId = 4821,
        ref = "22587",
        nome = "Blusa Tricot",
        cor = "AZUL",
        imageUrl = null,
        quantidade = 18,
        unitarioCents = 14490,
        totalCents = 260820,
        tamanhos = listOf(
            CarrinhoItemTamanho(complemento2Id = 11, label = "P", quantidade = 12),
            CarrinhoItemTamanho(complemento2Id = 12, label = "M", quantidade = 6),
        ),
    )

    @Test
    fun descreveOsTamanhosLancadosNaLinha() {
        val ui = linha.toCartLineUi()

        assertEquals(51L, ui.itemId)
        assertEquals("22587", ui.ref)
        assertEquals(18, ui.units)
        assertEquals(260820L, ui.totalCents)
        assertEquals("P 12un · M 6un", ui.sizesLabel)
    }

    @Test
    fun itemSemValorNaoQuebraOTotal() {
        val ui = linha.copy(totalCents = null).toCartLineUi()

        assertEquals(0L, ui.totalCents)
    }

    @Test
    fun precoFracionarioViraCentavosExatos() {
        assertEquals(14490L, PrecoDto(numerador = "1449", denominador = "10").cents)
        assertEquals(260820L, PrecoDto(numerador = "26082", denominador = "10").cents)
        assertEquals(3333L, PrecoDto(numerador = "100", denominador = "3").cents)
    }

    @Test
    fun precoCaiParaOValorDecimalQuandoNaoVemFracao() {
        assertEquals(14490L, PrecoDto(valor = "144.900000").cents)
        assertNull(PrecoDto().cents)
    }
}
