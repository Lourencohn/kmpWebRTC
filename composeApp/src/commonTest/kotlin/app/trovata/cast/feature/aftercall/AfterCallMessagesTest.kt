package app.trovata.cast.feature.aftercall

import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.OrderLine
import app.trovata.cast.data.local.StoredOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class AfterCallMessagesTest {

    @Test
    fun summaryUrlFollowsThePublicCartPage() {
        assertEquals(
            "https://trovata.app.br/catalogo-link-resume/buba/5f6c1d2e/carrinho/9021",
            cartSummaryUrl("https://trovata.app.br/", "buba", "5f6c1d2e", 9021),
        )
    }

    @Test
    fun orderMessageCarriesUnitsTotalAndLink() {
        assertEquals(
            "Oi, Marina! Seu pedido ficou pronto: 24 unidades, total de R$ 4.200,00. " +
                "O resumo completo está aqui: https://trovata.app.br/r",
            orderSummaryMessage("Marina Couto", order(), "https://trovata.app.br/r"),
        )
        assertEquals(
            "Olá! Seu pedido ficou pronto: 24 unidades, total de R$ 4.200,00.",
            orderSummaryMessage(null, order(), null),
        )
    }

    @Test
    fun leftOutMessageNamesWhatTheClientLookedAt() {
        assertEquals(
            "Oi, Marina! Obrigado pela conversa. Anotei os produtos que você olhou: Vestido Midi e Blusa Tricô. " +
                "Se quiser incluir algum no pedido, é só me falar.",
            leftOutMessage("Marina", listOf(interest(1, "Vestido Midi"), interest(2, "Blusa Tricô"))),
        )
        assertEquals(
            "Oi, Marina! Obrigado pela conversa. Anotei os produtos que você olhou: A, B, C e mais 2. " +
                "Se quiser incluir algum no pedido, é só me falar.",
            leftOutMessage("Marina", listOf("A", "B", "C", "D", "E").mapIndexed { i, name -> interest(i.toLong(), name) }),
        )
        assertEquals(
            "Oi, Marina! Obrigado pela conversa. Qualquer coisa, é só me chamar.",
            leftOutMessage("Marina", emptyList()),
        )
    }

    private fun order() = StoredOrder(
        orderId = "CAR-9021",
        sessionId = "ses_1",
        sessionToken = null,
        clientName = "Marina",
        clientShop = null,
        sellerName = "Lourenço",
        totalCents = 420_000,
        confirmedByMe = true,
        createdAtMs = 0,
        lines = listOf(
            OrderLine(productId = "AN-104", size = "P", units = 12, unitPriceCents = 17_500),
            OrderLine(productId = "AN-104", size = "M", units = 12, unitPriceCents = 17_500),
        ),
    )

    private fun interest(produtoPreId: Long, name: String) = CallInterest(
        produtoPreId = produtoPreId,
        clientOpens = 1,
        sellerShows = 0,
        draftUnits = 0,
        cartUnits = 0,
        ref = null,
        name = name,
        imageUrl = null,
        firstAtMs = 0,
        lastAtMs = 0,
    )
}
