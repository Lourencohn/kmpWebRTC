package app.trovata.cast.data.signaling

import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.decodeSignaling
import app.trovata.cast.protocol.encode
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SignalingState {
    data object Disconnected : SignalingState()
    data object Connecting : SignalingState()
    data class Connected(val youAre: PeerRole, val peerId: String) : SignalingState()
    data class Failed(val code: String, val message: String) : SignalingState()
}

interface SignalingTransport {
    suspend fun connect()
    suspend fun send(raw: String)
    suspend fun close(reason: String?)
    suspend fun listen(onMessage: suspend (String) -> Unit)
}

class SignalingClient(
    private val transport: SignalingTransport,
    private val peerId: String,
    private val role: PeerRole,
    private val displayName: String?,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val log = Logger.withTag("SignalingClient")

    private val _state = MutableStateFlow<SignalingState>(SignalingState.Disconnected)
    val state: StateFlow<SignalingState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<SignalingMessage>(replay = 0, extraBufferCapacity = 16)
    val incoming: SharedFlow<SignalingMessage> = _incoming.asSharedFlow()

    private var listenerJob: Job? = null

    suspend fun start() {
        if (_state.value !is SignalingState.Disconnected && _state.value !is SignalingState.Failed) return
        _state.value = SignalingState.Connecting
        log.i { "connecting peerId=$peerId role=$role" }
        runCatching {
            transport.connect()
            transport.send(SignalingMessage.Hello(role = role, peerId = peerId, displayName = displayName).encode())
        }.onFailure { err ->
            log.e(err) { "transport connect failed" }
            _state.value = SignalingState.Failed("transport_failed", err.message ?: "unknown")
            return
        }
        log.i { "connected, hello sent" }
        listenerJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            transport.listen { raw ->
                val parsed = runCatching { decodeSignaling(raw) }.getOrNull() ?: return@listen
                handleIncoming(parsed)
            }
        }
    }

    private suspend fun handleIncoming(message: SignalingMessage) {
        log.i { "recv ${message::class.simpleName}" }
        when (message) {
            is SignalingMessage.RoomState -> {
                _state.value = SignalingState.Connected(
                    youAre = message.youAre.role,
                    peerId = message.youAre.peerId,
                )
            }
            is SignalingMessage.ProtocolError -> {
                _state.value = SignalingState.Failed(message.code, message.message)
            }
            else -> { /* relay-only */ }
        }
        _incoming.emit(message)
    }

    suspend fun send(message: SignalingMessage) {
        transport.send(message.encode())
    }

    suspend fun stop(reason: String? = null) {
        runCatching {
            transport.send(SignalingMessage.Bye(from = peerId, reason = reason).encode())
        }
        runCatching { transport.close(reason) }
        listenerJob?.cancel()
        _state.value = SignalingState.Disconnected
    }

    fun dispose() {
        listenerJob?.cancel()
        scope.cancel()
    }
}
