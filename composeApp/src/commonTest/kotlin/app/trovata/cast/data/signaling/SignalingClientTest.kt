package app.trovata.cast.data.signaling

import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.PeerSummary
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.decodeSignaling
import app.trovata.cast.protocol.encode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SignalingClientTest {

    private class FakeTransport : SignalingTransport {
        val sent = mutableListOf<String>()
        val incoming = Channel<String>(capacity = Channel.UNLIMITED)
        var connected = false
        var closeReason: String? = null
        var failOnConnect: Boolean = false

        override suspend fun connect() {
            if (failOnConnect) error("boom")
            connected = true
        }

        override suspend fun send(raw: String) {
            sent += raw
        }

        override suspend fun close(reason: String?) {
            closeReason = reason
            incoming.close()
        }

        override suspend fun listen(onMessage: suspend (String) -> Unit) {
            for (raw in incoming) onMessage(raw)
        }
    }

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
