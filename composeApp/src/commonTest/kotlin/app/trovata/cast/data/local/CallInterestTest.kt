package app.trovata.cast.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class CallInterestTest {

    @Test
    fun leftOutKeepsOnlyWhatDidNotReachTheCart() {
        val interests = listOf(
            interest(1, clientOpens = 2, cartUnits = 12),
            interest(2, clientOpens = 1),
            interest(3, sellerShows = 1),
            interest(4, cartUnits = 6),
        )

        assertEquals(listOf(2L, 3L), interests.leftOut().map { it.produtoPreId })
    }

    @Test
    fun whatTheClientTypedOrOpenedComesBeforeWhatTheSellerShowed() {
        val interests = listOf(
            interest(1, sellerShows = 3, firstAtMs = 1),
            interest(2, clientOpens = 1, firstAtMs = 5),
            interest(3, clientOpens = 1, draftUnits = 12, firstAtMs = 9),
            interest(4, clientOpens = 3, firstAtMs = 7),
        )

        assertEquals(listOf(3L, 4L, 2L, 1L), interests.leftOut().map { it.produtoPreId })
    }

    @Test
    fun labelFallsBackToRefAndThenToTheId() {
        assertEquals("Vestido Midi", interest(1, name = "Vestido Midi", ref = "AN-104").label)
        assertEquals("Ref. AN-104", interest(1, ref = "AN-104").label)
        assertEquals("Produto 1", interest(1).label)
    }

    private fun interest(
        produtoPreId: Long,
        clientOpens: Int = 0,
        sellerShows: Int = 0,
        draftUnits: Int = 0,
        cartUnits: Int = 0,
        name: String? = null,
        ref: String? = null,
        firstAtMs: Long = 0,
    ) = CallInterest(
        produtoPreId = produtoPreId,
        clientOpens = clientOpens,
        sellerShows = sellerShows,
        draftUnits = draftUnits,
        cartUnits = cartUnits,
        ref = ref,
        name = name,
        imageUrl = null,
        firstAtMs = firstAtMs,
        lastAtMs = firstAtMs,
    )
}
