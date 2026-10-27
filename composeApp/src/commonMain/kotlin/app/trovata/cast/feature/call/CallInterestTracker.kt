package app.trovata.cast.feature.call

import app.trovata.cast.data.local.CallInterestEvent
import app.trovata.cast.data.local.CartProduct
import app.trovata.cast.data.local.InterestSource
import app.trovata.cast.protocol.DataChannelMessage
import kotlinx.datetime.Clock

class CallInterestTracker(
    private val record: (CallInterestEvent) -> Unit,
    private val recordCart: (List<CartProduct>, Long) -> Unit,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val focusBySource = mutableMapOf<InterestSource, Long?>()
    private val draftsByProduct = mutableMapOf<Long, MutableMap<String, Int>>()

    fun onClientMessage(message: DataChannelMessage) {
        when (message) {
            is DataChannelMessage.Navigate -> onFocus(message.view.focus?.produtoPreId, InterestSource.Client)
            is DataChannelMessage.QuantityDraft -> onDraft(message)
            else -> Unit
        }
    }

    fun onSellerMessage(message: DataChannelMessage) {
        if (message is DataChannelMessage.Navigate) onFocus(message.view.focus?.produtoPreId, InterestSource.Seller)
    }

    fun onCallEnded(cart: List<CartLineUi>) {
        val products = cart.mapNotNull { line ->
            val produtoPreId = line.produtoPreId ?: return@mapNotNull null
            CartProduct(
                produtoPreId = produtoPreId,
                ref = line.ref.takeIf { it.isNotBlank() },
                name = line.name.takeIf { it.isNotBlank() },
                imageUrl = line.imageUrl,
                units = line.units,
            )
        }
        recordCart(products, nowMs())
    }

    private fun onFocus(produtoPreId: Long?, source: InterestSource) {
        val previous = focusBySource[source]
        focusBySource[source] = produtoPreId
        if (produtoPreId == null || produtoPreId == previous) return
        record(CallInterestEvent.Focused(produtoPreId, source, nowMs()))
    }

    private fun onDraft(message: DataChannelMessage.QuantityDraft) {
        val sizes = draftsByProduct.getOrPut(message.produtoPreId) { mutableMapOf() }
        sizes[message.gradeKey] = message.units.coerceAtLeast(0)
        val total = sizes.values.sum()
        if (total <= 0) return
        record(CallInterestEvent.Drafted(message.produtoPreId, total, nowMs()))
    }
}
