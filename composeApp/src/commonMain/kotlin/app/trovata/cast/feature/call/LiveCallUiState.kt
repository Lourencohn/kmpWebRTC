package app.trovata.cast.feature.call

import app.trovata.cast.data.local.OrderLine
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.signaling.SignalingState
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.webrtc.PeerSessionState

data class CartSizeUi(
    val complemento2Id: Long?,
    val label: String,
    val units: Int,
)

data class CartLineUi(
    val itemId: Long,
    val produtoPreId: Long?,
    val ref: String,
    val name: String,
    val color: String?,
    val imageUrl: String?,
    val units: Int,
    val totalCents: Long,
    val sizes: List<CartSizeUi>,
) {
    val sizesLabel: String get() = sizes.joinToString(" · ") { "${it.label} ${it.units}un" }
}

enum class CartStage {
    Idle,
    Opening,
    Ready,
    Failed,
}

data class CartToast(
    val text: String,
    val createdAtMs: Long,
)

data class OrderSummaryUi(
    val orderId: String,
    val ts: Long,
    val lines: List<OrderLine>,
    val totalCents: Long,
    val confirmedByMe: Boolean,
)

data class LiveCallUiState(
    val signaling: SignalingState = SignalingState.Disconnected,
    val peer: PeerSessionState = PeerSessionState.Idle,
    val token: String,
    val role: PeerRole,
    val pageUrl: String,
    val localMuted: Boolean = false,
    val remoteMuted: Boolean = false,
    val drawing: Boolean = false,
    val cart: List<CartLineUi> = emptyList(),
    val cartStage: CartStage = CartStage.Idle,
    val cartError: String? = null,
    val carrinhoId: Long? = null,
    val cartClientName: String? = null,
    val isFinishingCart: Boolean = false,
    val showCartDrawer: Boolean = false,
    val toast: CartToast? = null,
    val summary: OrderSummaryUi? = null,
) {
    val isLive: Boolean get() = peer is PeerSessionState.Connected
    val isNegotiating: Boolean get() =
        signaling is SignalingState.Connecting ||
        peer is PeerSessionState.Negotiating
    val errorMessage: String? get() = when {
        signaling is SignalingState.Failed -> signaling.message
        peer is PeerSessionState.Failed -> peer.reason
        else -> null
    }
    val cartCount: Int get() = cart.sumOf { it.units }
    val cartTotalCents: Long get() = cart.sumOf { it.totalCents }
}

fun CartItemLine.toCartLineUi(): CartLineUi = CartLineUi(
    itemId = itemId,
    produtoPreId = produtoPreId,
    ref = ref,
    name = name,
    color = color,
    imageUrl = imageUrl,
    units = quantity,
    totalCents = totalCents ?: 0L,
    sizes = sizes.map { CartSizeUi(it.complemento2Id, it.label, it.quantity) },
)
