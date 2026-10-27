package app.trovata.cast.feature.call

import app.trovata.cast.data.local.CallInterestEvent
import app.trovata.cast.data.local.CartProduct
import app.trovata.cast.data.local.InterestSource
import app.trovata.cast.protocol.CatalogRoute
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.ProductFocus
import app.trovata.cast.protocol.ViewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallInterestTrackerTest {

    private val events = mutableListOf<CallInterestEvent>()
    private val carts = mutableListOf<Pair<List<CartProduct>, Long>>()
    private val tracker = CallInterestTracker(
        record = { events += it },
        recordCart = { products, atMs -> carts += products to atMs },
        nowMs = { 1_000 },
    )

    @Test
    fun clientOpeningAProductIsRecordedOncePerOpening() {
        tracker.onClientMessage(navigate(77))
        tracker.onClientMessage(navigate(77))
        tracker.onClientMessage(navigate(null))
        tracker.onClientMessage(navigate(77))

        assertEquals(
            listOf<CallInterestEvent>(
                CallInterestEvent.Focused(77, InterestSource.Client, 1_000),
                CallInterestEvent.Focused(77, InterestSource.Client, 1_000),
            ),
            events,
        )
    }

    @Test
    fun sellerAndClientFocusAreTrackedApart() {
        tracker.onSellerMessage(navigate(77))
        tracker.onClientMessage(navigate(77))
        tracker.onSellerMessage(navigate(88))

        assertEquals(
            listOf(InterestSource.Seller, InterestSource.Client, InterestSource.Seller),
            events.map { (it as CallInterestEvent.Focused).source },
        )
    }

    @Test
    fun quantityDraftsAddUpAcrossSizes() {
        tracker.onClientMessage(draft(77, "P", 6))
        tracker.onClientMessage(draft(77, "M", 4))
        tracker.onClientMessage(draft(77, "P", 2))
        tracker.onClientMessage(draft(77, "P", 0))
        tracker.onClientMessage(draft(77, "M", 0))

        assertEquals(
            listOf(6, 10, 6, 4),
            events.map { (it as CallInterestEvent.Drafted).units },
        )
    }

    @Test
    fun sellerDraftsAndOtherMessagesAreIgnored() {
        tracker.onSellerMessage(draft(77, "P", 6))
        tracker.onClientMessage(DataChannelMessage.PointAt(target = "produto:77", ts = 1, from = "buyer"))
        tracker.onClientMessage(DataChannelMessage.Mute(muted = true, ts = 1, from = "buyer"))

        assertTrue(events.isEmpty())
    }

    @Test
    fun endingTheCallSavesWhatIsInTheCart() {
        tracker.onCallEnded(
            listOf(
                cartLine(produtoPreId = 77, ref = "AN-104", name = "Vestido Midi", units = 8),
                cartLine(produtoPreId = null, ref = "AVULSO", name = "Sem cadastro", units = 1),
            ),
        )

        val (products, atMs) = carts.single()
        assertEquals(listOf(CartProduct(77, "AN-104", "Vestido Midi", null, 8)), products)
        assertEquals(1_000, atMs)
    }

    private fun navigate(produtoPreId: Long?) = DataChannelMessage.Navigate(
        view = ViewState(route = CatalogRoute.Todos, focus = produtoPreId?.let { ProductFocus(it) }),
        ts = 1,
        from = "peer",
    )

    private fun draft(produtoPreId: Long, gradeKey: String, units: Int) = DataChannelMessage.QuantityDraft(
        produtoPreId = produtoPreId,
        gradeKey = gradeKey,
        units = units,
        ts = 1,
        from = "peer",
    )

    private fun cartLine(produtoPreId: Long?, ref: String, name: String, units: Int) = CartLineUi(
        itemId = 1,
        produtoPreId = produtoPreId,
        ref = ref,
        name = name,
        color = null,
        imageUrl = null,
        units = units,
        totalCents = 0,
        sizes = emptyList(),
    )
}
