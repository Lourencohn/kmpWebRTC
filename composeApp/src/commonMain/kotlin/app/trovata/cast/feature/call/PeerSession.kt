package app.trovata.cast.feature.call

import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.protocol.CartChangeHint
import app.trovata.cast.protocol.CartChangeReason
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.ScrollAnchor
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.ViewState
import app.trovata.cast.protocol.decodeDataChannel
import app.trovata.cast.protocol.encode
import co.touchlab.kermit.Logger
import com.shepeliev.webrtckmp.DataChannel
import com.shepeliev.webrtckmp.DataChannelState
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceConnectionState
import com.shepeliev.webrtckmp.IceServer
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
import com.shepeliev.webrtckmp.MediaStreamTrackKind
import com.shepeliev.webrtckmp.OfferAnswerOptions
import com.shepeliev.webrtckmp.PeerConnection
import com.shepeliev.webrtckmp.PeerConnectionState
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.onConnectionStateChange
import com.shepeliev.webrtckmp.onDataChannel
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onIceConnectionStateChange
import com.shepeliev.webrtckmp.onTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed class PeerSessionState {
    data object Idle : PeerSessionState()
    data object Negotiating : PeerSessionState()
    data object Connected : PeerSessionState()
    data class Failed(val reason: String) : PeerSessionState()
    data object Closed : PeerSessionState()
}

private val DefaultIceServers = listOf(
    IceServer(urls = listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302")),
)

class PeerSession(
    private val signaling: SignalingClient,
    private val selfPeerId: String,
    private val selfRole: PeerRole,
    private val iceServers: List<IceServer> = DefaultIceServers,
    private val iceServersProvider: (suspend () -> List<IceServer>)? = null,
    private val presenceIntervalMs: Long = 5_000,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val log = Logger.withTag("PeerSession")

    private val _state = MutableStateFlow<PeerSessionState>(PeerSessionState.Idle)
    val state: StateFlow<PeerSessionState> = _state.asStateFlow()

    private val _remoteAudio = MutableStateFlow<MediaStream?>(null)
    val remoteAudio: StateFlow<MediaStream?> = _remoteAudio.asStateFlow()

    private val _localMuted = MutableStateFlow(false)
    val localMuted: StateFlow<Boolean> = _localMuted.asStateFlow()

    private val _remoteMuted = MutableStateFlow(false)
    val remoteMuted: StateFlow<Boolean> = _remoteMuted.asStateFlow()

    private val _remoteScroll = MutableSharedFlow<DataChannelMessage.Scroll>(extraBufferCapacity = 4)
    val remoteScroll: SharedFlow<DataChannelMessage.Scroll> = _remoteScroll.asSharedFlow()

    private val _remotePointAt = MutableSharedFlow<DataChannelMessage.PointAt>(extraBufferCapacity = 4)
    val remotePointAt: SharedFlow<DataChannelMessage.PointAt> = _remotePointAt.asSharedFlow()

    private val _remoteNavigate = MutableSharedFlow<DataChannelMessage.Navigate>(extraBufferCapacity = 4)
    val remoteNavigate: SharedFlow<DataChannelMessage.Navigate> = _remoteNavigate.asSharedFlow()

    private val _remoteCartInvalidated = MutableSharedFlow<DataChannelMessage.CartInvalidated>(extraBufferCapacity = 16)
    val remoteCartInvalidated: SharedFlow<DataChannelMessage.CartInvalidated> = _remoteCartInvalidated.asSharedFlow()

    private val _remoteOrderPlaced = MutableSharedFlow<DataChannelMessage.OrderPlaced>(extraBufferCapacity = 4)
    val remoteOrderPlaced: SharedFlow<DataChannelMessage.OrderPlaced> = _remoteOrderPlaced.asSharedFlow()

    private val _outgoingScroll = MutableStateFlow<DataChannelMessage.Scroll?>(null)

    private var pc: PeerConnection? = null
    private var dc: DataChannel? = null
    private var remotePeerId: String? = null
    private var localStream: MediaStream? = null
    private var presenceJob: Job? = null
    private val collectorJobs = mutableListOf<Job>()

    suspend fun start() {
        if (pc != null) return
        log.i { "start role=$selfRole peerId=$selfPeerId" }
        _state.value = PeerSessionState.Negotiating
        val resolvedIceServers = iceServersProvider
            ?.let { provider -> runCatching { provider() }.getOrNull()?.takeIf { it.isNotEmpty() } }
            ?: iceServers
        log.i { "iceServers=${resolvedIceServers.flatMap { it.urls }}" }
        val connection = PeerConnection(RtcConfiguration(iceServers = resolvedIceServers))
        pc = connection

        localStream = runCatching { MediaDevices.getUserMedia(audio = true) }
            .onFailure { log.w(it) { "getUserMedia failed" } }
            .getOrNull()
        log.i { "localStream tracks=${localStream?.tracks?.size ?: 0}" }
        localStream?.tracks?.forEach { track -> connection.addTrack(track, localStream!!) }

        collectorJobs += scope.launch {
            connection.onIceCandidate.collect { candidate ->
                val target = remotePeerId ?: return@collect
                signaling.send(
                    SignalingMessage.IceCandidate(
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex,
                        candidate = candidate.candidate,
                        from = selfPeerId,
                        to = target,
                    ),
                )
            }
        }
        collectorJobs += scope.launch {
            connection.onIceConnectionStateChange.collect { iceState ->
                log.i { "iceConnectionState=$iceState" }
                when (iceState) {
                    IceConnectionState.Connected, IceConnectionState.Completed -> markConnected()
                    IceConnectionState.Failed -> _state.value = PeerSessionState.Failed("ice_failed")
                    else -> Unit
                }
            }
        }
        collectorJobs += scope.launch {
            connection.onConnectionStateChange.collect { connState ->
                log.i { "connectionState=$connState" }
                when (connState) {
                    PeerConnectionState.Connected -> markConnected()
                    PeerConnectionState.Failed -> _state.value = PeerSessionState.Failed("connection_failed")
                    else -> Unit
                }
            }
        }
        collectorJobs += scope.launch {
            connection.onTrack.collect { event ->
                event.streams.firstOrNull()?.let { _remoteAudio.value = it }
            }
        }
        collectorJobs += scope.launch {
            connection.onDataChannel.collect { incoming -> bindDataChannel(incoming) }
        }
        collectorJobs += scope.launch(start = CoroutineStart.UNDISPATCHED) {
            signaling.incoming.collect { handleSignal(it) }
        }
        collectorJobs += scope.launch {
            _outgoingScroll.filterNotNull().sample(33).collect { message ->
                val channel = dc ?: return@collect
                if (channel.readyState != DataChannelState.Open) return@collect
                channel.send(message.encode().encodeToByteArray())
            }
        }

        if (selfRole == PeerRole.Buyer) {
            createOffer()
        }
    }

    private fun markConnected() {
        if (_state.value is PeerSessionState.Connected) return
        log.i { "session live" }
        _state.value = PeerSessionState.Connected
    }

    private suspend fun createOffer() {
        val connection = pc ?: return
        val channel = connection.createDataChannel("presence", id = 1) ?: return
        bindDataChannel(channel)
        val offer = connection.createOffer(OfferAnswerOptions())
        connection.setLocalDescription(offer)
        signaling.send(
            SignalingMessage.Offer(
                sdp = offer.sdp,
                from = selfPeerId,
                to = remotePeerId,
            ),
        )
    }

    private suspend fun handleSignal(message: SignalingMessage) {
        val connection = pc ?: return
        when (message) {
            is SignalingMessage.RoomState -> {
                val other = message.peers.firstOrNull { it.peerId != selfPeerId }
                remotePeerId = other?.peerId
                log.i { "roomState remotePeer=$remotePeerId" }
            }
            is SignalingMessage.PeerJoined -> {
                remotePeerId = message.peer.peerId
                log.i { "peerJoined remotePeer=$remotePeerId" }
            }
            is SignalingMessage.PeerLeft -> {
                if (message.peerId == remotePeerId) {
                    _state.value = PeerSessionState.Failed("peer_left")
                }
            }
            is SignalingMessage.Offer -> {
                log.i { "offer from=${message.from}" }
                remotePeerId = message.from
                connection.setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, message.sdp))
                val answer = connection.createAnswer(OfferAnswerOptions())
                connection.setLocalDescription(answer)
                log.i { "answer sent to=${message.from}" }
                signaling.send(
                    SignalingMessage.Answer(
                        sdp = answer.sdp,
                        from = selfPeerId,
                        to = message.from,
                    ),
                )
            }
            is SignalingMessage.Answer -> {
                log.i { "answer from=${message.from}" }
                connection.setRemoteDescription(SessionDescription(SessionDescriptionType.Answer, message.sdp))
            }
            is SignalingMessage.IceCandidate -> {
                runCatching {
                    connection.addIceCandidate(
                        IceCandidate(
                            sdpMid = message.sdpMid,
                            sdpMLineIndex = message.sdpMLineIndex,
                            candidate = message.candidate,
                        ),
                    )
                }.onFailure { log.w { "addIceCandidate failed: ${it.message}" } }
            }
            else -> Unit
        }
    }

    private fun bindDataChannel(channel: DataChannel) {
        dc = channel
        collectorJobs += scope.launch {
            channel.onOpen.collect {
                log.i { "dataChannel open=${channel.label}" }
                markConnected()
                startPresenceLoop()
                if (_localMuted.value) sendMuteState(_localMuted.value)
            }
        }
        collectorJobs += scope.launch {
            channel.onClose.collect {
                presenceJob?.cancel()
                presenceJob = null
            }
        }
        collectorJobs += scope.launch {
            channel.onMessage.collect { bytes ->
                val payload = bytes.decodeToString()
                when (val parsed = decodeDataChannel(payload)) {
                    is DataChannelMessage.Mute -> _remoteMuted.value = parsed.muted
                    is DataChannelMessage.Scroll -> _remoteScroll.tryEmit(parsed)
                    is DataChannelMessage.PointAt -> _remotePointAt.tryEmit(parsed)
                    is DataChannelMessage.Navigate -> _remoteNavigate.tryEmit(parsed)
                    is DataChannelMessage.CartInvalidated -> _remoteCartInvalidated.tryEmit(parsed)
                    is DataChannelMessage.OrderPlaced -> _remoteOrderPlaced.tryEmit(parsed)
                    null -> Unit
                }
            }
        }
    }

    fun setLocalMuted(muted: Boolean) {
        if (_localMuted.value == muted) return
        localStream?.tracks
            ?.filter { it.kind == MediaStreamTrackKind.Audio }
            ?.forEach { it.enabled = !muted }
        _localMuted.value = muted
        sendMuteState(muted)
    }

    private fun sendMuteState(muted: Boolean) {
        val channel = dc ?: return
        if (channel.readyState != DataChannelState.Open) return
        val payload = DataChannelMessage.Mute(
            muted = muted,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
        ).encode()
        channel.send(payload.encodeToByteArray())
    }

    fun publishScroll(anchor: ScrollAnchor) {
        _outgoingScroll.value = DataChannelMessage.Scroll(
            anchor = anchor,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
        )
    }

    fun publishPointAt(
        target: String,
        xRatio: Float = 0.5f,
        yRatio: Float = 0.5f,
        durationMs: Long = 3_000,
    ) = send(
        DataChannelMessage.PointAt(
            target = target,
            xRatio = xRatio,
            yRatio = yRatio,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
            durationMs = durationMs,
        ),
    )

    fun publishNavigate(view: ViewState) = send(
        DataChannelMessage.Navigate(
            view = view,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
        ),
    )

    fun publishCartInvalidated(
        carrinhoId: Long,
        reason: CartChangeReason,
        hint: CartChangeHint? = null,
    ) = send(
        DataChannelMessage.CartInvalidated(
            carrinhoId = carrinhoId,
            reason = reason,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
            hint = hint,
        ),
    )

    fun publishOrderPlaced(carrinhoId: Long, pedidoId: String? = null) = send(
        DataChannelMessage.OrderPlaced(
            carrinhoId = carrinhoId,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
            pedidoId = pedidoId,
        ),
    )

    private fun send(message: DataChannelMessage): Boolean {
        val channel = dc ?: return false
        if (channel.readyState != DataChannelState.Open) return false
        channel.send(message.encode().encodeToByteArray())
        return true
    }

    private fun startPresenceLoop() {
        presenceJob?.cancel()
        presenceJob = scope.launch {
            while (isActive) {
                val ping = SignalingMessage.PresencePing(
                    from = selfPeerId,
                    sentAtMs = Clock.System.now().toEpochMilliseconds(),
                )
                signaling.send(ping)
                delay(presenceIntervalMs)
            }
        }
    }

    suspend fun close(@Suppress("UNUSED_PARAMETER") reason: String? = null) {
        presenceJob?.cancel()
        collectorJobs.forEach { it.cancel() }
        collectorJobs.clear()
        dc?.close()
        dc = null
        localStream?.tracks?.forEach { it.stop() }
        localStream = null
        pc?.close()
        pc = null
        _localMuted.value = false
        _remoteMuted.value = false
        _outgoingScroll.value = null
        _state.value = PeerSessionState.Closed
    }

    fun dispose() {
        scope.cancel()
    }
}
