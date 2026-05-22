package app.trovata.cast.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataChannelMessageTest {

    @Test
    fun mute_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.Mute(muted = true, from = "seller-1")
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun scroll_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.Scroll(
            productId = "atelier-norte-coat-01",
            offset = 0.42f,
            ts = 1_700_000_000_000,
            from = "seller-1",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun pointAt_roundtrip_with_default_duration() {
        val original: DataChannelMessage = DataChannelMessage.PointAt(
            productId = "atelier-norte-pants-03",
            ts = 1_700_000_000_000,
            from = "seller-1",
        )
        val raw = original.encode()
        val decoded = decodeDataChannel(raw) as DataChannelMessage.PointAt
        assertEquals(original, decoded)
        assertEquals(3_000L, decoded.durationMs)
    }

    @Test
    fun pointAt_roundtrip_with_custom_duration() {
        val original: DataChannelMessage = DataChannelMessage.PointAt(
            productId = "atelier-norte-shoe-07",
            ts = 1_700_000_000_000,
            from = "seller-1",
            durationMs = 5_000,
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun discriminator_uses_type_field() {
        val raw = DataChannelMessage.Mute(muted = false, from = "buyer-2").encode()
        check(raw.contains("\"type\":\"mute\""))
    }

    @Test
    fun unknown_payload_returns_null() {
        assertNull(decodeDataChannel("{\"type\":\"unknown-thing\",\"foo\":1}"))
        assertNull(decodeDataChannel("not json"))
    }
}
