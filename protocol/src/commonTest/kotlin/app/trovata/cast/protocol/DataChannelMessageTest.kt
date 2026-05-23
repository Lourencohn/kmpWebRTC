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
    fun navigate_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.Navigate(
            productId = "AN-088",
            ts = 1_700_000_000_500,
            from = "buyer-xyz",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun cartUpdate_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.CartUpdate(
            productId = "AN-104",
            size = "M",
            units = 12,
            ts = 1_700_000_000_750,
            from = "buyer-xyz",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun cartUpdate_zero_units_removes_line() {
        val raw = DataChannelMessage.CartUpdate(
            productId = "AN-104",
            size = "G",
            units = 0,
            ts = 1L,
            from = "buyer-xyz",
        ).encode()
        val decoded = decodeDataChannel(raw) as DataChannelMessage.CartUpdate
        assertEquals(0, decoded.units)
    }

    @Test
    fun orderConfirm_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.OrderConfirm(
            orderId = "ORD-abc-XYZ",
            ts = 1_700_000_000_900,
            from = "seller-1",
            lines = listOf(
                OrderLine("AN-104", "M", 12, 8_990),
                OrderLine("AN-088", "G", 4, 15_900),
            ),
            totalCents = 12L * 8_990 + 4L * 15_900,
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun orderConfirm_with_empty_lines() {
        val raw = DataChannelMessage.OrderConfirm(
            orderId = "ORD-empty",
            ts = 1L,
            from = "seller-1",
            lines = emptyList(),
            totalCents = 0,
        ).encode()
        val decoded = decodeDataChannel(raw) as DataChannelMessage.OrderConfirm
        assertEquals(0, decoded.lines.size)
        assertEquals(0, decoded.totalCents)
    }

    @Test
    fun unknown_payload_returns_null() {
        assertNull(decodeDataChannel("{\"type\":\"unknown-thing\",\"foo\":1}"))
        assertNull(decodeDataChannel("not json"))
    }
}
