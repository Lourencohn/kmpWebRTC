package app.trovata.cast.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun submissionRequestRoundTrip() {
        val payload = OrderSubmissionRequest(
            orderId = "ORD-x",
            sessionToken = "tok123",
            tsMs = 1_700_000_000_000,
            totalCents = 17_980,
            lines = listOf(
                OrderLine(productId = "AN-104", size = "M", units = 2, unitPriceCents = 8_990),
                OrderLine(productId = "AN-217", size = "G", units = 1, unitPriceCents = 11_900),
            ),
            source = OrderSource.Buyer,
            clientName = "Diego",
        )
        val raw = json.encodeToString(OrderSubmissionRequest.serializer(), payload)
        val decoded = json.decodeFromString(OrderSubmissionRequest.serializer(), raw)
        assertEquals(payload, decoded)
        assertTrue(raw.contains("\"source\":\"Buyer\""))
    }

    @Test
    fun submissionResponseRoundTrip() {
        val payload = OrderSubmissionResponse(orderId = "ORD-x", receivedAtMs = 42)
        val raw = json.encodeToString(OrderSubmissionResponse.serializer(), payload)
        assertEquals(payload, json.decodeFromString(OrderSubmissionResponse.serializer(), raw))
    }

    @Test
    fun recordSerializes() {
        val record = OrderRecord(
            orderId = "ORD-x",
            sessionToken = "tok123",
            tsMs = 1_700_000_000_000,
            receivedAtMs = 1_700_000_000_500,
            totalCents = 17_980,
            lines = listOf(
                OrderLine(productId = "AN-104", size = "M", units = 2, unitPriceCents = 8_990),
            ),
            source = OrderSource.Seller,
            clientName = null,
        )
        val raw = json.encodeToString(OrderRecord.serializer(), record)
        val decoded = json.decodeFromString(OrderRecord.serializer(), raw)
        assertEquals(record, decoded)
        assertTrue(raw.contains("\"source\":\"Seller\""))
    }
}
