package app.trovata.cast.feature.call

import app.trovata.cast.data.signaling.KtorSignalingTransport
import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.data.signaling.SignalingState
import app.trovata.cast.data.signaling.signalingUrlFor
import app.trovata.cast.platform.ServerConfig
import app.trovata.cast.protocol.PeerRole
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveCallUiState(
    val signaling: SignalingState = SignalingState.Disconnected,
    val peer: PeerSessionState = PeerSessionState.Idle,
    val token: String,
    val role: PeerRole,
    val localMuted: Boolean = false,
    val remoteMuted: Boolean = false,
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
}

class LiveCallScreenModel(
    private val token: String,
    private val httpClient: HttpClient,
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
            signaling.state.collect { s -> _state.value = _state.value.copy(signaling = s) }
        }
        screenModelScope.launch {
            peer.state.collect { p -> _state.value = _state.value.copy(peer = p) }
        }
        screenModelScope.launch {
            peer.localMuted.collect { m -> _state.value = _state.value.copy(localMuted = m) }
        }
        screenModelScope.launch {
            peer.remoteMuted.collect { m -> _state.value = _state.value.copy(remoteMuted = m) }
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

    fun hangup() {
        screenModelScope.launch {
            peer.close("hangup")
            signaling.stop("hangup")
        }
    }

    override fun onDispose() {
        peer.dispose()
        signaling.dispose()
    }
}

private fun randomSuffix(): String =
    (1..6).map { ('a'..'z').random() }.joinToString("")
