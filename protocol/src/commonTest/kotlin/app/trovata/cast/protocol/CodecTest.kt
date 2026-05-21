package app.trovata.cast.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CodecTest {
    @Test
    fun pointAtRoundTrip() {
        val event = SessionEvent.PointAt(productId = "AN-217", ts = 1_700_000_000_000, from = "seller-1")
        val raw = event.encode()
        val decoded = decodeSessionEvent(raw)
        assertIs<SessionEvent.PointAt>(decoded)
        assertEquals(event, decoded)
    }

    @Test
    fun navigateProductRoundTrip() {
        val event = SessionEvent.Navigate(
            route = Route.Product(sku = "AN-217"),
            ts = 1_700_000_000_000,
            from = "buyer-1",
        )
        val raw = event.encode()
        val decoded = decodeSessionEvent(raw)
        assertIs<SessionEvent.Navigate>(decoded)
        assertEquals(event, decoded)
    }
}
