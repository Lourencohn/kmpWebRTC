package app.trovata.cast.data.signaling

import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.PeerSummary
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.decodeSignaling
import app.trovata.cast.protocol.encode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SignalingClientTest {

    private class FakeTransport : SignalingTransport {
        val sent = mutableListOf<String>()
        val incoming = Channel<String>(capacity = Channel.UNLIMITED)
        var connected = false
        var clientCloseReason: String? = null
        var serverCloseReason: String? = null
        var sendFailure: Throwable? = null
        var failOnConnect: Boolean = false

        override suspend fun connect() {
            if (failOnConnect) error("boom")
            connected = true
        }

        override suspend fun send(raw: String) {
            sendFailure?.let { throw it }
            sent += raw
        }

        override suspend fun close(reason: String?) {
            clientCloseReason = reason
            incoming.close()
        }

        override suspend fun listen(onMessage: suspend (String) -> Unit) {
            for (raw in incoming) onMessage(raw)
        }

        override suspend fun closeReason(): String? = serverCloseReason

        fun closeFromServer(reason: String?) {
            serverCloseReason = reason
            incoming.close()
        }

        fun rejectBeforeHello(reason: String) {
            serverCloseReason = reason
            sendFailure = CancellationException("WebSocket session closed with code VIOLATED_POLICY.")
        }
    }

    private fun roomState() = SignalingMessage.RoomState(
        peers = listOf(PeerSummary("p1", PeerRole.Seller, "Atelier")),
        youAre = PeerSummary("p1", PeerRole.Seller, "Atelier"),
    ).encode()

    private fun makeClient(
        transport: FakeTransport,
        scope: CoroutineScope,
        role: PeerRole = PeerRole.Seller,
    ) = SignalingClient(
        transport = transport,
        peerId = "p1",
        role = role,
        displayName = "Atelier",
        scope = scope,
    )


    @Test
    fun startSendsHelloAndConnects() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        assertTrue(transport.connected)
        val helloRaw = transport.sent.single()
        val parsed = decodeSignaling(helloRaw)
        assertIs<SignalingMessage.Hello>(parsed)
        assertEquals(PeerRole.Seller, parsed.role)
        assertEquals("p1", parsed.peerId)
    }

    @Test
    fun roomStateMovesToConnected() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.incoming.send(
            SignalingMessage.RoomState(
                peers = listOf(PeerSummary("p1", PeerRole.Seller, "Atelier")),
                youAre = PeerSummary("p1", PeerRole.Seller, "Atelier"),
            ).encode(),
        )
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Connected>(state)
        assertEquals(PeerRole.Seller, state.youAre)
    }

    @Test
    fun incomingMessagesAreEmitted() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        val captured = mutableListOf<SignalingMessage>()
        val collector = backgroundScope.launch {
            client.incoming.collect { captured += it }
        }
        transport.incoming.send(
            SignalingMessage.Offer(sdp = "v=0...", from = "p2", to = "p1").encode(),
        )
        runCurrent()
        collector.cancel()
        val offer = captured.firstOrNull { it is SignalingMessage.Offer }
        assertIs<SignalingMessage.Offer>(offer)
        assertEquals("v=0...", offer.sdp)
    }

    @Test
    fun protocolErrorMovesToFailed() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.incoming.send(
            SignalingMessage.ProtocolError("room_full", "cheia").encode(),
        )
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("room_full", state.code)
    }

    @Test
    fun transportFailureFails() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport().apply { failOnConnect = true }
        val client = makeClient(transport, backgroundScope)
        client.start()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("transport_failed", state.code)
    }

    @Test
    fun helloRejectedForUnknownTokenTellsSellerToCreateNewInvite() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport().apply { rejectBeforeHello("unknown_token") }
        val client = makeClient(transport, backgroundScope)
        client.start()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("unknown_token", state.code)
        assertEquals("Esse convite não vale mais. Crie um novo convite para o cliente.", state.message)
    }

    @Test
    fun serverClosingWithUnknownTokenAfterHelloFails() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.closeFromServer("unknown_token")
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("unknown_token", state.code)
        assertEquals("Esse convite não vale mais. Crie um novo convite para o cliente.", state.message)
    }

    @Test
    fun serverClosingWithRoomFullFails() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.incoming.send(SignalingMessage.ProtocolError("room_full", "Já há dois participantes.").encode())
        transport.closeFromServer("room_full")
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("room_full", state.code)
        assertEquals(
            "Essa sessão já tem duas pessoas conectadas. Encerre a outra chamada ou crie um novo convite.",
            state.message,
        )
    }

    @Test
    fun serverClosingWithRoleTakenFails() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.closeFromServer("role_taken")
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("role_taken", state.code)
        assertEquals(
            "Essa sessão já tem um vendedor conectado. Encerre a outra chamada ou espere um pouco e tente de novo.",
            state.message,
        )
    }

    @Test
    fun socketClosedWithoutReasonBeforeRoomStateFails() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.closeFromServer(null)
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("connection_closed", state.code)
        assertEquals("A conexão com a sessão caiu antes de abrir. Desligue e tente de novo.", state.message)
    }

    @Test
    fun listenErrorBeforeRoomStateFails() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.incoming.close(IllegalStateException("socket reset"))
        runCurrent()
        val state = client.state.value
        assertIs<SignalingState.Failed>(state)
        assertEquals("connection_closed", state.code)
    }

    @Test
    fun socketClosedAfterRoomStateKeepsConnected() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.incoming.send(roomState())
        transport.closeFromServer(null)
        runCurrent()
        assertIs<SignalingState.Connected>(client.state.value)
    }

    @Test
    fun stopWhileConnectingNeverReportsFailure() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        val seen = mutableListOf<SignalingState>()
        backgroundScope.launch { client.state.collect { seen += it } }
        client.start()
        client.stop("hangup")
        runCurrent()
        assertEquals(SignalingState.Disconnected, client.state.value)
        assertTrue(seen.none { it is SignalingState.Failed })
    }

    @Test
    fun stopSendsByeAndClosesTransport() = runTest(UnconfinedTestDispatcher()) {
        val transport = FakeTransport()
        val client = makeClient(transport, backgroundScope)
        client.start()
        transport.sent.clear()
        client.stop("manual")
        runCurrent()
        val byeRaw = transport.sent.single()
        val parsed = decodeSignaling(byeRaw)
        assertIs<SignalingMessage.Bye>(parsed)
        assertEquals("manual", parsed.reason)
        assertEquals(SignalingState.Disconnected, client.state.value)
    }

    @Test
    fun signalingUrlForRewritesScheme() {
        assertEquals("ws://10.0.2.2:8080/ws/session/abc", signalingUrlFor("http://10.0.2.2:8080", "abc"))
        assertEquals("wss://srv.io/ws/session/xyz", signalingUrlFor("https://srv.io/", "xyz"))
    }

}
