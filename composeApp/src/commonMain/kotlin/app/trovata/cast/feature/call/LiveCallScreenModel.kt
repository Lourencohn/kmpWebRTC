package app.trovata.cast.feature.call

import app.trovata.cast.data.local.CartRepository
import app.trovata.cast.data.local.CatalogRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.local.toUiProduct
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.data.signaling.SignalingState
import app.trovata.cast.protocol.CartChangeReason
import app.trovata.cast.protocol.CatalogRoute
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.LiveAnchor
import app.trovata.cast.protocol.OrderLine
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.ProductFocus
import app.trovata.cast.protocol.ScrollAnchor
import app.trovata.cast.protocol.ViewState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.scope.Scope

data class CartLineUi(
    val productId: String,
    val size: String,
    val units: Int,
    val updatedAtMs: Long,
)

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
    val localMuted: Boolean = false,
    val remoteMuted: Boolean = false,
    val cart: List<CartLineUi> = emptyList(),
    val products: List<Product> = emptyList(),
    val priceCentsByRef: Map<String, Long> = emptyMap(),
    val collectionLabel: String = "",
    val focusedProductId: String? = null,
    val showProductSheet: Boolean = false,
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
}

class LiveCallScreenModel(
    private val spec: CallSpec,
    private val signaling: SignalingClient,
    private val peer: PeerSession,
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val catalogRepository: CatalogRepository,
    private val sessionsRepository: SessionsRepository,
    private val callScope: Scope,
) : ScreenModel {

    private val token = spec.token
    private val sessionId = spec.sessionId
    private val clientName = spec.clientName
    private val collectionLabel = spec.collectionLabel
    private val sellerName = spec.sellerName
    private val clientShop = spec.clientShop
    private val priceTableId = spec.priceTableId
    private val carrinhoId = spec.carrinhoId

    private val _state = MutableStateFlow(
        LiveCallUiState(token = token, role = PeerRole.Seller, collectionLabel = collectionLabel),
    )
    val state: StateFlow<LiveCallUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            val selected = sessionsRepository.selectedSkus(sessionId)
            val catalog = if (selected.isNotEmpty()) {
                catalogRepository.snapshotForRefs(selected, priceTableId)
            } else {
                catalogRepository.snapshot(priceTableId)
            }
            val ui = catalog.map { it.toUiProduct() }
            val prices = buildMap {
                catalog.forEach { product -> product.priceCents?.let { put(product.ref, it) } }
            }
            _state.update { it.copy(products = ui, priceCentsByRef = prices) }
        }
        screenModelScope.launch {
            signaling.state.collect { s -> _state.update { it.copy(signaling = s) } }
        }
        screenModelScope.launch {
            peer.state.collect { p -> _state.update { it.copy(peer = p) } }
        }
        screenModelScope.launch {
            peer.localMuted.collect { m -> _state.update { it.copy(localMuted = m) } }
        }
        screenModelScope.launch {
            peer.remoteMuted.collect { m -> _state.update { it.copy(remoteMuted = m) } }
        }
        screenModelScope.launch {
            peer.remoteNavigate.collect { msg ->
                val ref = refOf(msg.view.focus?.produtoPreId) ?: return@collect
                _state.update { it.copy(focusedProductId = ref, showProductSheet = true) }
            }
        }
        screenModelScope.launch {
            peer.remoteCartInvalidated.collect { msg -> handleRemoteCartInvalidated(msg) }
        }
        screenModelScope.launch {
            peer.remoteOrderPlaced.collect {
                _state.update { it.copy(showProductSheet = false, showCartDrawer = false) }
            }
        }
        screenModelScope.launch {
            cartRepository.observe(sessionId).collect { lines ->
                _state.update {
                    it.copy(
                        cart = lines.map { line ->
                            CartLineUi(
                                productId = line.sku,
                                size = line.size,
                                units = line.units,
                                updatedAtMs = line.updatedAtMs,
                            )
                        },
                    )
                }
            }
        }
    }

    fun start() {
        screenModelScope.launch {
            peer.start()
            signaling.start()
        }
    }

    fun toggleMute() {
        peer.setLocalMuted(!_state.value.localMuted)
    }

    fun publishScroll(productId: String, offset: Float) {
        peer.publishScroll(
            ScrollAnchor(
                produtoPreId = produtoPreIdOf(productId),
                itemOffsetRatio = offset,
            ),
        )
    }

    fun publishPointAt(productId: String) {
        val id = produtoPreIdOf(productId) ?: return
        peer.publishPointAt(LiveAnchor.product(id))
    }

    fun openProductDetail(productId: String) {
        _state.update { it.copy(focusedProductId = productId, showProductSheet = true) }
        val id = produtoPreIdOf(productId) ?: return
        peer.publishNavigate(
            ViewState(
                route = CatalogRoute.Todos,
                focus = ProductFocus(produtoPreId = id),
            ),
        )
    }

    private fun produtoPreIdOf(ref: String): Long? =
        _state.value.products.firstOrNull { it.ref == ref }?.produtoPreId

    private fun refOf(produtoPreId: Long?): String? {
        if (produtoPreId == null) return null
        return _state.value.products.firstOrNull { it.produtoPreId == produtoPreId }?.ref
    }

    fun openCartDrawer() {
        _state.update { it.copy(showCartDrawer = true) }
    }

    fun dismissCartDrawer() {
        _state.update { it.copy(showCartDrawer = false) }
    }

    fun dismissProductSheet() {
        _state.update { it.copy(showProductSheet = false) }
    }

    fun dismissToast() {
        _state.update { it.copy(toast = null) }
    }

    fun confirmOrder() {
        val current = _state.value
        if (current.summary != null) return
        if (current.cart.isEmpty()) return
        val ts = Clock.System.now().toEpochMilliseconds()
        val lines = current.cart.map { line ->
            OrderLine(
                productId = line.productId,
                size = line.size,
                units = line.units,
                unitPriceCents = current.priceCentsByRef[line.productId] ?: 0L,
            )
        }
        val totalCents = lines.sumOf { it.subtotalCents }
        val orderId = newOrderId(ts)
        carrinhoId?.let { peer.publishOrderPlaced(carrinhoId = it, pedidoId = orderId) }
        screenModelScope.launch {
            persistOrder(
                orderId = orderId,
                ts = ts,
                lines = lines,
                totalCents = totalCents,
                confirmedByMe = true,
            )
        }
        _state.update {
            it.copy(
                summary = OrderSummaryUi(
                    orderId = orderId,
                    ts = ts,
                    lines = lines,
                    totalCents = totalCents,
                    confirmedByMe = true,
                ),
                showCartDrawer = false,
                showProductSheet = false,
            )
        }
    }

    private suspend fun persistOrder(
        orderId: String,
        ts: Long,
        lines: List<OrderLine>,
        totalCents: Long,
        confirmedByMe: Boolean,
    ) {
        if (orderRepository.get(orderId) != null) return
        orderRepository.persist(
            orderId = orderId,
            sessionId = sessionId,
            sessionToken = token,
            clientName = clientName,
            clientShop = clientShop,
            sellerName = sellerName,
            totalCents = totalCents,
            confirmedByMe = confirmedByMe,
            createdAtMs = ts,
            lines = lines,
        )
    }

    fun hangup() {
        screenModelScope.launch {
            peer.close("hangup")
            signaling.stop("hangup")
        }
    }

    private fun handleRemoteCartInvalidated(msg: DataChannelMessage.CartInvalidated) {
        val who = clientName?.split(' ')?.firstOrNull() ?: "Cliente"
        val name = msg.hint?.label
            ?: msg.hint?.produtoPreId?.let { refOf(it) }
            ?: "o carrinho"
        val units = msg.hint?.unitsDelta ?: 0
        val text = when (msg.reason) {
            CartChangeReason.ItemAdded -> "$who adicionou ${units}un de $name"
            CartChangeReason.ItemRemoved -> "$who removeu $name"
            CartChangeReason.QuantityChanged -> "$who ajustou $name"
            CartChangeReason.PrazoChanged -> "$who mudou o prazo"
            CartChangeReason.Cleared -> "$who esvaziou o carrinho"
            CartChangeReason.Finalized -> "$who finalizou o carrinho"
        }
        _state.update {
            it.copy(toast = CartToast(text = text, createdAtMs = Clock.System.now().toEpochMilliseconds()))
        }
    }

    override fun onDispose() {
        callScope.close()
    }
}

private fun newOrderId(ts: Long): String {
    val tsPart = ts.toString(36).takeLast(6)
    val suffix = (1..4).map { ('A'..'Z').random() }.joinToString("")
    return "ORD-$tsPart-$suffix"
}
