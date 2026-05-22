package app.trovata.cast.feature.call

import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.SignalingMessage
import com.shepeliev.webrtckmp.DataChannel
import com.shepeliev.webrtckmp.IceCandidate
import com.shepeliev.webrtckmp.IceConnectionState
import com.shepeliev.webrtckmp.IceServer
import com.shepeliev.webrtckmp.MediaDevices
import com.shepeliev.webrtckmp.MediaStream
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            channel.onOpen.collect { startPresenceLoop() }
        }
        collectorJobs += scope.launch {
            channel.onClose.collect {
                presenceJob?.cancel()
                presenceJob = null
            }
        }
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
        _state.value = PeerSessionState.Closed
    }

    fun dispose() {
        scope.cancel()
    }
}
