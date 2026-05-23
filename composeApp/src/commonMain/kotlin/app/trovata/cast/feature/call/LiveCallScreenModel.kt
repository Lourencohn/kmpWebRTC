package app.trovata.cast.feature.call

import app.trovata.cast.data.local.CartRepository
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.data.signaling.KtorSignalingTransport
import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.data.signaling.SignalingState
import app.trovata.cast.data.signaling.signalingUrlFor
import app.trovata.cast.platform.ServerConfig
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.PeerRole
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CartLineUi(
    val productId: String,
    val size: String,
    val units: Int,
    val updatedAtMs: Long,
) {
    val name: String get() = SampleCatalog.products.firstOrNull { it.ref == productId }?.name ?: productId
    val unitPriceLabel: String get() = SampleCatalog.products.firstOrNull { it.ref == productId }?.price ?: ""
}

data class CartToast(
    val text: String,
    val createdAtMs: Long,
)

data class LiveCallUiState(
    val signaling: SignalingState = SignalingState.Disconnected,
    val peer: PeerSessionState = PeerSessionState.Idle,
    val token: String,
    val role: PeerRole,
    val localMuted: Boolean = false,
    val remoteMuted: Boolean = false,
    val cart: List<CartLineUi> = emptyList(),
    val focusedProductId: String? = null,
    val showProductSheet: Boolean = false,
    val showCartDrawer: Boolean = false,
    val toast: CartToast? = null,
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
    private val token: String,
    private val sessionId: String,
    private val clientName: String?,
    private val httpClient: HttpClient,
    private val cartRepository: CartRepository,
    private val sellerPeerId: String = "seller-${randomSuffix()}",
    private val sellerName: String = "Vendedor",
) : ScreenModel {

    private val transport = KtorSignalingTransport(
        httpClient = httpClient,
        wsUrl = signalingUrlFor(ServerConfig.baseUrl, token),
    )
    private val signaling = SignalingClient(
        transport = transport,
        peerId = sellerPeerId,
        role = PeerRole.Seller,
        displayName = sellerName,
        scope = screenModelScope,
    )
    private val peer = PeerSession(
        signaling = signaling,
        selfPeerId = sellerPeerId,
        selfRole = PeerRole.Seller,
        scope = screenModelScope,
    )

    private val _state = MutableStateFlow(
        LiveCallUiState(token = token, role = PeerRole.Seller),
    )
    val state: StateFlow<LiveCallUiState> = _state.asStateFlow()

    init {
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
                _state.update { it.copy(focusedProductId = msg.productId, showProductSheet = true) }
            }
        }
        screenModelScope.launch {
            peer.remoteCartUpdate.collect { msg -> handleRemoteCartUpdate(msg) }
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
            signaling.start()
            peer.start()
        }
    }

    fun toggleMute() {
        peer.setLocalMuted(!_state.value.localMuted)
    }

    fun publishScroll(productId: String, offset: Float) {
        peer.publishScroll(productId, offset)
    }

    fun publishPointAt(productId: String) {
        peer.publishPointAt(productId)
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

    fun hangup() {
        screenModelScope.launch {
            peer.close("hangup")
            signaling.stop("hangup")
        }
    }

    private suspend fun handleRemoteCartUpdate(msg: DataChannelMessage.CartUpdate) {
        val previous = cartRepository.snapshot(sessionId).firstOrNull {
            it.sku == msg.productId && it.size == msg.size
        }
        cartRepository.apply(sessionId, msg.productId, msg.size, msg.units, msg.ts)
        val name = SampleCatalog.products.firstOrNull { it.ref == msg.productId }?.name ?: msg.productId
        val who = clientName?.split(' ')?.firstOrNull() ?: "Cliente"
        val previousUnits = previous?.units ?: 0
        val text = when {
            msg.units == 0 -> "$who removeu $name (${msg.size})"
            previousUnits == 0 -> "$who adicionou ${msg.units}un de $name (${msg.size})"
            msg.units > previousUnits ->
                "$who subiu $name (${msg.size}) para ${msg.units}un"
            else -> "$who reduziu $name (${msg.size}) para ${msg.units}un"
        }
        _state.update {
            it.copy(toast = CartToast(text = text, createdAtMs = Clock.System.now().toEpochMilliseconds()))
        }
    }

    override fun onDispose() {
        peer.dispose()
        signaling.dispose()
    }
}

private fun randomSuffix(): String =
    (1..6).map { ('a'..'z').random() }.joinToString("")
