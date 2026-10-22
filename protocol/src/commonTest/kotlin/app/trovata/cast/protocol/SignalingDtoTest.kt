package app.trovata.cast.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SignalingDtoTest {

    @Test
    fun helloRoundTrip() {
        val msg = SignalingMessage.Hello(
            role = PeerRole.Seller,
            peerId = "ses_seller_42",
            displayName = "Atelier Norte",
        )
        val raw = msg.encode()
        assertTrue(raw.contains("\"type\":\"hello\""))
        val decoded = decodeSignaling(raw)
        assertIs<SignalingMessage.Hello>(decoded)
        assertEquals(msg, decoded)
    }

    @Test
    fun offerAnswerIceRoundTrip() {
        val offer = SignalingMessage.Offer(sdp = "v=0...", from = "p1", to = "p2")
        val answer = SignalingMessage.Answer(sdp = "v=0...", from = "p2", to = "p1")
        val ice = SignalingMessage.IceCandidate(
            sdpMid = "0",
            sdpMLineIndex = 0,
            candidate = "candidate:abc",
            from = "p1",
            to = "p2",
        )
        listOf(offer, answer, ice).forEach { msg ->
            val raw = msg.encode()
            val decoded = decodeSignaling(raw)
            assertEquals(msg, decoded)
        }
    }

    @Test
    fun roomStateRoundTrip() {
        val state = SignalingMessage.RoomState(
            peers = listOf(
                PeerSummary("a", PeerRole.Seller, "Vendedor"),
                PeerSummary("b", PeerRole.Buyer, null),
            ),
            youAre = PeerSummary("b", PeerRole.Buyer, null),
        )
        val raw = state.encode()
        val decoded = decodeSignaling(raw) as SignalingMessage.RoomState
        assertEquals(state.peers, decoded.peers)
        assertEquals(state.youAre, decoded.youAre)
    }

    @Test
    fun peerEventsRoundTrip() {
        val joined = SignalingMessage.PeerJoined(PeerSummary("p1", PeerRole.Buyer))
        val left = SignalingMessage.PeerLeft("p1", "lost connection")
        assertEquals(joined, decodeSignaling(joined.encode()))
        assertEquals(left, decodeSignaling(left.encode()))
    }

    @Test
    fun presencePingRoundTrip() {
        val ping = SignalingMessage.PresencePing(from = "p1", sentAtMs = 1_700_000_000_000L)
        assertEquals(ping, decodeSignaling(ping.encode()))
    }

    @Test
    fun protocolErrorRoundTrip() {
        val err = SignalingMessage.ProtocolError(code = "room_full", message = "Sala cheia")
        assertEquals(err, decodeSignaling(err.encode()))
    }
}
