package app.trovata.cast.feature.call

import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.decodeDataChannel
import app.trovata.cast.protocol.encode
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
import com.shepeliev.webrtckmp.RtcConfiguration
import com.shepeliev.webrtckmp.SessionDescription
import com.shepeliev.webrtckmp.SessionDescriptionType
import com.shepeliev.webrtckmp.onDataChannel
import com.shepeliev.webrtckmp.onIceCandidate
import com.shepeliev.webrtckmp.onIceConnectionStateChange
import com.shepeliev.webrtckmp.onTrack
import kotlinx.coroutines.CoroutineScope
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
    private val presenceIntervalMs: Long = 5_000,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
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

    private val _remoteCartUpdate = MutableSharedFlow<DataChannelMessage.CartUpdate>(extraBufferCapacity = 16)
    val remoteCartUpdate: SharedFlow<DataChannelMessage.CartUpdate> = _remoteCartUpdate.asSharedFlow()

    private val _outgoingScroll = MutableStateFlow<DataChannelMessage.Scroll?>(null)

    private var pc: PeerConnection? = null
    private var dc: DataChannel? = null
    private var remotePeerId: String? = null
    private var localStream: MediaStream? = null
    private var presenceJob: Job? = null
    private val collectorJobs = mutableListOf<Job>()

    suspend fun start() {
        if (pc != null) return
        _state.value = PeerSessionState.Negotiating
        val connection = PeerConnection(RtcConfiguration(iceServers = iceServers))
        pc = connection

        localStream = runCatching { MediaDevices.getUserMedia(audio = true) }.getOrNull()
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
                when (iceState) {
                    IceConnectionState.Connected, IceConnectionState.Completed ->
                        _state.value = PeerSessionState.Connected
                    IceConnectionState.Failed -> _state.value = PeerSessionState.Failed("ice_failed")
                    IceConnectionState.Disconnected -> _state.value = PeerSessionState.Negotiating
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
        collectorJobs += scope.launch {
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
            }
            is SignalingMessage.PeerJoined -> {
                remotePeerId = message.peer.peerId
            }
            is SignalingMessage.PeerLeft -> {
                if (message.peerId == remotePeerId) {
                    _state.value = PeerSessionState.Failed("peer_left")
                }
            }
            is SignalingMessage.Offer -> {
                remotePeerId = message.from
                connection.setRemoteDescription(SessionDescription(SessionDescriptionType.Offer, message.sdp))
                val answer = connection.createAnswer(OfferAnswerOptions())
                connection.setLocalDescription(answer)
                signaling.send(
                    SignalingMessage.Answer(
                        sdp = answer.sdp,
                        from = selfPeerId,
                        to = message.from,
                    ),
                )
            }
            is SignalingMessage.Answer -> {
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
                }
            }
            else -> Unit
        }
    }

    private fun bindDataChannel(channel: DataChannel) {
        dc = channel
        collectorJobs += scope.launch {
            channel.onOpen.collect {
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
                    is DataChannelMessage.CartUpdate -> _remoteCartUpdate.tryEmit(parsed)
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
        val payload = DataChannelMessage.Mute(muted = muted, from = selfPeerId).encode()
        channel.send(payload.encodeToByteArray())
    }

    fun publishScroll(productId: String, offset: Float) {
        _outgoingScroll.value = DataChannelMessage.Scroll(
            productId = productId,
            offset = offset,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
        )
    }

    fun publishPointAt(productId: String, durationMs: Long = 3_000) {
        val channel = dc ?: return
        if (channel.readyState != DataChannelState.Open) return
        val payload = DataChannelMessage.PointAt(
            productId = productId,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
            durationMs = durationMs,
        ).encode()
        channel.send(payload.encodeToByteArray())
    }

    fun publishNavigate(productId: String) {
        val channel = dc ?: return
        if (channel.readyState != DataChannelState.Open) return
        val payload = DataChannelMessage.Navigate(
            productId = productId,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
        ).encode()
        channel.send(payload.encodeToByteArray())
    }

    fun publishCartUpdate(productId: String, size: String, units: Int) {
        val channel = dc ?: return
        if (channel.readyState != DataChannelState.Open) return
        val payload = DataChannelMessage.CartUpdate(
            productId = productId,
            size = size,
            units = units,
            ts = Clock.System.now().toEpochMilliseconds(),
            from = selfPeerId,
        ).encode()
        channel.send(payload.encodeToByteArray())
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
