package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.CallInterest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FollowUpNotesTest {

    @Test
    fun listsWhatWasLeftOutWithTheReason() {
        val notes = leftOutNotes(
            listOf(
                interest(1, name = "Vestido Midi", ref = "AN-104", clientOpens = 1, draftUnits = 12),
                interest(2, name = "Blusa Tricô", ref = "BT-9", clientOpens = 3),
                interest(3, clientOpens = 1),
                interest(4, name = "Saia Linho", sellerShows = 1),
                interest(5, name = "Calça Reta", clientOpens = 1, cartUnits = 6),
            ),
        )

        assertEquals(
            "Ficou de fora do pedido na última chamada:\n" +
                "- Vestido Midi · ref. AN-104 · chegou a digitar 12 un\n" +
                "- Blusa Tricô · ref. BT-9 · abriu 3 vezes\n" +
                "- Produto 3 · o cliente abriu\n" +
                "- Saia Linho · você mostrou",
            notes,
        )
    }

    @Test
    fun longListsAreCut() {
        val notes = leftOutNotes((1L..9L).map { interest(it, name = "Produto $it", clientOpens = 1) })

        assertEquals(7, notes?.lines()?.count { it.startsWith("- ") })
        assertEquals("- e mais 3", notes?.lines()?.last())
    }

    @Test
    fun nothingLeftOutMeansNoNotes() {
        assertNull(leftOutNotes(emptyList()))
        assertNull(leftOutNotes(listOf(interest(1, clientOpens = 1, cartUnits = 2))))
    }

    private fun interest(
        produtoPreId: Long,
        name: String? = null,
        ref: String? = null,
        clientOpens: Int = 0,
        sellerShows: Int = 0,
        draftUnits: Int = 0,
        cartUnits: Int = 0,
    ) = CallInterest(
        produtoPreId = produtoPreId,
        clientOpens = clientOpens,
        sellerShows = sellerShows,
        draftUnits = draftUnits,
        cartUnits = cartUnits,
        ref = ref,
        name = name,
        imageUrl = null,
        firstAtMs = produtoPreId,
        lastAtMs = produtoPreId,
    )
}
