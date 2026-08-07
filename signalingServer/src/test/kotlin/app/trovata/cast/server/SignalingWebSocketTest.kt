package app.trovata.cast.server

import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import app.trovata.cast.protocol.SignalingMessage
import app.trovata.cast.protocol.decodeSignaling
import app.trovata.cast.protocol.encode
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SignalingWebSocketTest {

    private suspend fun io.ktor.client.HttpClient.createSession(): SessionCreateResponse =
        post("/session") {
            contentType(ContentType.Application.Json)
            setBody(
                SessionCreateRequest(
                    empresaSlug = "atelier-norte",
                    catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
                    sellerId = "vend-31",
                    sellerName = "Marina Prado",
                    catalogoNome = "Outono 26",
                    clientName = "Diego",
                ),
            )
        }.body()

    @Test
    fun rejectsUnknownToken() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val session = client.webSocketSession { url("ws://localhost/ws/session/zzz000") }
        val reason = withTimeout(2_000) { session.closeReason.await() }
        assertNotNull(reason)
        assertEquals("unknown_token", reason.message)
    }

    @Test
    fun helloHandshakeBroadcastsRoomState() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val created = client.createSession()
        val session = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }

        session.send(
            SignalingMessage.Hello(
                role = PeerRole.Seller,
                peerId = "seller-1",
                displayName = "Atelier",
            ).encode(),
        )
        val frame = withTimeout(2_000) { session.incoming.receive() }
        val text = (frame as Frame.Text).readText()
        val parsed = decodeSignaling(text)
        assertIs<SignalingMessage.RoomState>(parsed)
        assertEquals(PeerRole.Seller, parsed.youAre.role)
        assertEquals(1, parsed.peers.size)
        session.close()
    }

    @Test
    fun secondPeerSeesPeerJoinedAndCanRelay() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val created = client.createSession()

        val sellerSession = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        sellerSession.send(
            SignalingMessage.Hello(PeerRole.Seller, "seller-1", "Atelier").encode(),
        )
        receiveSignaling(sellerSession) // RoomState for seller

        coroutineScope {
            val sellerNextDeferred = async {
                withTimeout(2_000) { receiveSignaling(sellerSession) }
            }

            val buyerSession = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
            buyerSession.send(
                SignalingMessage.Hello(PeerRole.Buyer, "buyer-1", "Diego").encode(),
            )

            val sellerNotice = sellerNextDeferred.await()
            assertIs<SignalingMessage.PeerJoined>(sellerNotice)
            assertEquals("buyer-1", sellerNotice.peer.peerId)

            val buyerRoomState = withTimeout(2_000) { receiveSignaling(buyerSession) }
            assertIs<SignalingMessage.RoomState>(buyerRoomState)
            assertEquals(2, buyerRoomState.peers.size)

            val sellerOfferAck = async { withTimeout(2_000) { receiveSignaling(sellerSession) } }
            buyerSession.send(
                SignalingMessage.Offer(sdp = "v=0...", from = "buyer-1", to = "seller-1").encode(),
            )
            val relayed = sellerOfferAck.await()
            assertIs<SignalingMessage.Offer>(relayed)
            assertEquals("v=0...", relayed.sdp)

            val sellerByeAck = async { withTimeout(2_000) { receiveSignaling(sellerSession) } }
            buyerSession.send(SignalingMessage.Bye(from = "buyer-1", reason = "tchau").encode())
            val byeRelay = sellerByeAck.await()
            assertIs<SignalingMessage.Bye>(byeRelay)

            sellerSession.close()
        }
    }

    @Test
    fun rejectsRoomFull() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val created = client.createSession()

        val s1 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s1.send(SignalingMessage.Hello(PeerRole.Seller, "p1", "S").encode())
        receiveSignaling(s1)

        val s2 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s2.send(SignalingMessage.Hello(PeerRole.Buyer, "p2", "B").encode())
        receiveSignaling(s2)
        receiveSignaling(s1) // PeerJoined

        val s3 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s3.send(SignalingMessage.Hello(PeerRole.Buyer, "p3", "B3").encode())
        val msg = receiveSignaling(s3)
        assertIs<SignalingMessage.ProtocolError>(msg)
        assertEquals("room_full", msg.code)

        s1.close()
        s2.close()
    }

    @Test
    fun rejectsDuplicateRole() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val created = client.createSession()

        val s1 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s1.send(SignalingMessage.Hello(PeerRole.Seller, "p1", "S").encode())
        receiveSignaling(s1)

        val s2 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s2.send(SignalingMessage.Hello(PeerRole.Seller, "p2", "S2").encode())
        val msg = receiveSignaling(s2)
        assertIs<SignalingMessage.ProtocolError>(msg)
        assertEquals("role_taken", msg.code)

        s1.close()
    }

    @Test
    fun broadcastsPeerLeftWhenDisconnected() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val created = client.createSession()

        val s1 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s1.send(SignalingMessage.Hello(PeerRole.Seller, "p1", "S").encode())
        receiveSignaling(s1)

        val s2 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s2.send(SignalingMessage.Hello(PeerRole.Buyer, "p2", "B").encode())
        receiveSignaling(s2)
        receiveSignaling(s1) // PeerJoined

        s2.close()
        val left = withTimeout(2_000) { receiveSignaling(s1) }
        assertIs<SignalingMessage.PeerLeft>(left)
        assertEquals("p2", left.peerId)

        s1.close()
    }

    private suspend fun receiveSignaling(session: io.ktor.websocket.WebSocketSession): SignalingMessage {
        for (frame in session.incoming) {
            if (frame is Frame.Text) return decodeSignaling(frame.readText())
        }
        error("session closed before receiving signaling frame")
    }

    @Test
    fun helloReceivedAndBroadcastIncludesPeerSummary() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(WebSockets)
        }
        val created = client.createSession()
        val s1 = client.webSocketSession { url("ws://localhost/ws/session/${created.token}") }
        s1.send(SignalingMessage.Hello(PeerRole.Seller, "p1", "S").encode())
        val state = receiveSignaling(s1) as SignalingMessage.RoomState
        val you = state.youAre
        assertEquals("p1", you.peerId)
        assertEquals(PeerRole.Seller, you.role)
        assertNotNull(you.displayName)
        assertTrue(state.peers.any { it.peerId == "p1" })
        s1.close()
    }
}
