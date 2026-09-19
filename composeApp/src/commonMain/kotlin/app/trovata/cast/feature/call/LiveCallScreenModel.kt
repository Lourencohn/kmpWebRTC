package app.trovata.cast.feature.call

import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.remote.sfa.CarrinhoApi
import app.trovata.cast.data.remote.sfa.CarrinhoItemLinha
import app.trovata.cast.data.remote.sfa.SfaApiResult
import app.trovata.cast.data.remote.sfa.SfaConfig
import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.data.signaling.SignalingState
import app.trovata.cast.protocol.CartChangeReason
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.OrderLine
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.decodeDataChannel
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.scope.Scope

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

class LiveCallScreenModel(
    private val spec: CallSpec,
    private val signaling: SignalingClient,
    private val peer: PeerSession,
    private val orderRepository: OrderRepository,
    private val carrinhoApi: CarrinhoApi,
    private val callScope: Scope,
) : ScreenModel {

    private val token = spec.token
    private val sessionId = spec.sessionId
    private val clientName = spec.clientName
    private val clientEmail = spec.clientEmail
    private val catalogoLinkId = spec.catalogoLinkId
    private val sellerName = spec.sellerName
    private val clientShop = spec.clientShop
    private val empresaSlug = spec.empresaSlug
    private val catalogoUuid = spec.catalogoUuid

    val webBridge = LiveWebBridge(onPageMessage = ::handlePageMessage)

    private val _state = MutableStateFlow(
        LiveCallUiState(
            token = token,
            role = PeerRole.Seller,
            pageUrl = sellerPageUrl(spec.inviteUrl, SfaConfig.catalogWebBaseUrlOverride),
            carrinhoId = spec.carrinhoId,
        ),
    )
    val state: StateFlow<LiveCallUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch { openCart() }
        screenModelScope.launch {
            signaling.state.collect { s -> _state.update { it.copy(signaling = s) } }
        }
        screenModelScope.launch {
            peer.state.collect { p ->
                _state.update { it.copy(peer = p) }
                webBridge.updateStatus(p.toBridgeStatus())
            }
        }
        screenModelScope.launch {
            peer.localMuted.collect { m -> _state.update { it.copy(localMuted = m) } }
        }
        screenModelScope.launch {
            peer.remoteMuted.collect { m -> _state.update { it.copy(remoteMuted = m) } }
        }
        screenModelScope.launch {
            peer.incomingRaw.collect { payload -> webBridge.deliverRaw(payload) }
        }
        screenModelScope.launch {
            peer.incoming.collect { message -> handleRemoteMessage(message) }
        }
    }

    private fun handleRemoteMessage(message: DataChannelMessage) {
        when (message) {
            is DataChannelMessage.CartInvalidated -> handleRemoteCartInvalidated(message)
            is DataChannelMessage.OrderPlaced -> _state.update { it.copy(showCartDrawer = false) }
            else -> Unit
        }
    }

    private fun handlePageMessage(payload: String) {
        if (!peer.publishRaw(payload)) return
        val message = decodeDataChannel(payload) ?: return
        if (message is DataChannelMessage.CartInvalidated) handleOwnCartChange(message)
    }

    private suspend fun openCart() {
        val email = clientEmail?.trim().orEmpty()
        if (email.isBlank()) {
            _state.update {
                it.copy(
                    cartStage = CartStage.Failed,
                    cartError = "Esse catálogo link não tem e-mail de cliente. Cadastre no Catálogo Link para vender na chamada.",
                )
            }
            return
        }
        _state.update { it.copy(cartStage = CartStage.Opening, cartError = null) }

        when (val sessao = carrinhoApi.abrirCarrinho(empresaSlug, catalogoUuid, email)) {
            is SfaApiResult.Fail -> {
                _state.update { it.copy(cartStage = CartStage.Failed, cartError = sessao.message) }
                return
            }
            is SfaApiResult.Ok -> _state.update {
                it.copy(
                    carrinhoId = sessao.value.id,
                    cartClientName = sessao.value.clienteNome,
                    cartStage = CartStage.Ready,
                    cartError = null,
                )
            }
        }
        refreshCart()
    }

    fun retryCart() {
        if (_state.value.cartStage == CartStage.Opening) return
        screenModelScope.launch { openCart() }
    }

    private suspend fun refreshCart() {
        val carrinhoId = _state.value.carrinhoId ?: return
        when (val result = carrinhoApi.itens(empresaSlug, catalogoUuid, carrinhoId)) {
            is SfaApiResult.Fail -> _state.update { it.copy(cartError = result.message) }
            is SfaApiResult.Ok -> _state.update {
                it.copy(cart = result.value.map { linha -> linha.toCartLineUi() }, cartError = null)
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

    fun toggleDrawing() {
        val enabled = !_state.value.drawing
        _state.update { it.copy(drawing = enabled) }
        webBridge.setDrawing(enabled)
    }

    fun clearDrawing() {
        webBridge.clearDrawing()
    }

    fun openCartDrawer() {
        _state.update { it.copy(showCartDrawer = true) }
        screenModelScope.launch { refreshCart() }
    }

    fun dismissCartDrawer() {
        _state.update { it.copy(showCartDrawer = false) }
    }

    fun dismissToast() {
        _state.update { it.copy(toast = null) }
    }

    fun clearCartError() {
        _state.update { it.copy(cartError = null) }
    }

    fun confirmOrder() {
        val current = _state.value
        if (current.summary != null || current.isFinishingCart) return
        if (current.cart.isEmpty()) return
        val carrinhoId = current.carrinhoId ?: return
        val linkId = catalogoLinkId
        if (linkId == null) {
            _state.update {
                it.copy(cartError = "Sessão sem o identificador do catálogo link. Gere o convite novamente.")
            }
            return
        }
        _state.update { it.copy(isFinishingCart = true, cartError = null) }

        screenModelScope.launch {
            when (val result = carrinhoApi.marcarProntoParaEnvio(empresaSlug, linkId, carrinhoId)) {
                is SfaApiResult.Fail -> _state.update {
                    it.copy(isFinishingCart = false, cartError = result.message)
                }
                is SfaApiResult.Ok -> {
                    val ts = Clock.System.now().toEpochMilliseconds()
                    val lines = current.cart.flatMap { it.toOrderLines() }
                    val totalCents = current.cartTotalCents
                    val orderId = "CAR-$carrinhoId"
                    val placed = DataChannelMessage.OrderPlaced(
                        carrinhoId = carrinhoId,
                        ts = ts,
                        from = spec.sellerPeerId,
                        pedidoId = orderId,
                    )
                    peer.publish(placed)
                    webBridge.deliver(placed)
                    persistOrder(
                        orderId = orderId,
                        ts = ts,
                        lines = lines,
                        totalCents = totalCents,
                        confirmedByMe = true,
                    )
                    _state.update {
                        it.copy(
                            isFinishingCart = false,
                            summary = OrderSummaryUi(
                                orderId = orderId,
                                ts = ts,
                                lines = lines,
                                totalCents = totalCents,
                                confirmedByMe = true,
                            ),
                            showCartDrawer = false,
                        )
                    }
                }
            }
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
            clientName = _state.value.cartClientName ?: clientName,
            clientShop = clientShop,
            sellerName = sellerName,
            totalCents = totalCents,
            confirmedByMe = confirmedByMe,
            createdAtMs = ts,
            lines = lines,
        )
    }

    fun hangup() {
        webBridge.updateStatus(LiveWebBridge.STATUS_CLOSED)
        screenModelScope.launch {
            peer.close("hangup")
            signaling.stop("hangup")
        }
    }

    private fun handleOwnCartChange(msg: DataChannelMessage.CartInvalidated) {
        val units = msg.hint?.unitsDelta ?: 0
        val text = when (msg.reason) {
            CartChangeReason.ItemAdded ->
                if (units > 0) "${units}un lançadas no pedido" else "Pedido atualizado"
            CartChangeReason.ItemRemoved -> "Item removido do pedido"
            CartChangeReason.QuantityChanged -> "Quantidade ajustada no pedido"
            CartChangeReason.PrazoChanged -> "Prazo do pedido alterado"
            CartChangeReason.Cleared -> "Pedido esvaziado"
            CartChangeReason.Finalized -> "Pedido finalizado"
        }
        showToast(text)
        screenModelScope.launch { refreshCart() }
    }

    private fun handleRemoteCartInvalidated(msg: DataChannelMessage.CartInvalidated) {
        val who = clientName?.split(' ')?.firstOrNull() ?: "Cliente"
        val name = msg.hint?.label
        val units = msg.hint?.unitsDelta ?: 0
        val text = when (msg.reason) {
            CartChangeReason.ItemAdded -> when {
                units > 0 && name != null -> "$who adicionou ${units}un de $name"
                units > 0 -> "$who adicionou ${units}un ao carrinho"
                else -> "$who atualizou o carrinho"
            }
            CartChangeReason.ItemRemoved -> "$who removeu ${name ?: "um item"}"
            CartChangeReason.QuantityChanged -> "$who ajustou ${name ?: "o carrinho"}"
            CartChangeReason.PrazoChanged -> "$who mudou o prazo"
            CartChangeReason.Cleared -> "$who esvaziou o carrinho"
            CartChangeReason.Finalized -> "$who finalizou o carrinho"
        }
        showToast(text)
        screenModelScope.launch { refreshCart() }
    }

    private fun showToast(text: String) {
        _state.update {
            it.copy(toast = CartToast(text = text, createdAtMs = Clock.System.now().toEpochMilliseconds()))
        }
    }

    override fun onDispose() {
        callScope.close()
    }
}

private fun PeerSessionState.toBridgeStatus(): String = when (this) {
    PeerSessionState.Idle -> LiveWebBridge.STATUS_IDLE
    PeerSessionState.Negotiating -> LiveWebBridge.STATUS_NEGOTIATING
    PeerSessionState.Connected -> LiveWebBridge.STATUS_CONNECTED
    is PeerSessionState.Failed -> LiveWebBridge.STATUS_FAILED
    PeerSessionState.Closed -> LiveWebBridge.STATUS_CLOSED
}

fun CarrinhoItemLinha.toCartLineUi(): CartLineUi = CartLineUi(
    itemId = itemId,
    produtoPreId = produtoPreId,
    ref = ref,
    name = nome,
    color = cor,
    imageUrl = imageUrl,
    units = quantidade,
    totalCents = totalCents ?: 0L,
    sizes = tamanhos.map { CartSizeUi(it.complemento2Id, it.label, it.quantidade) },
)

private fun CartLineUi.toOrderLines(): List<OrderLine> {
    val unitPriceCents = if (units > 0) totalCents / units else 0L
    if (sizes.isEmpty()) {
        return listOf(
            OrderLine(
                productId = ref,
                size = "Único",
                units = units,
                unitPriceCents = unitPriceCents,
            ),
        )
    }
    return sizes.map { size ->
        OrderLine(
            productId = ref,
            size = size.label,
            units = size.units,
            unitPriceCents = unitPriceCents,
        )
    }
}
