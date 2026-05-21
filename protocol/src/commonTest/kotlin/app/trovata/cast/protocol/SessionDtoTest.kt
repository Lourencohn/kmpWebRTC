package app.trovata.cast.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun sessionCreateRequestRoundTrip() {
        val payload = SessionCreateRequest(
            sellerId = "atelier-norte",
            sellerName = "Atelier Norte",
            collectionLabel = "Outono 26",
            productSkus = listOf("AN-104", "AN-217"),
            clientName = "Diego Albuquerque",
        )
        val raw = json.encodeToString(SessionCreateRequest.serializer(), payload)
        val decoded = json.decodeFromString(SessionCreateRequest.serializer(), raw)
        assertEquals(payload, decoded)
    }

    @Test
    fun sessionInfoSerializes() {
        val info = SessionInfo(
            token = "abc123",
            sellerName = "Atelier Norte",
            collectionLabel = "Outono 26",
            clientName = null,
            clientShop = null,
            scheduledFor = null,
            productCount = 5,
            createdAtMs = 1_700_000_000_000,
            expiresAtMs = 1_700_086_400_000,
        )
        val raw = json.encodeToString(SessionInfo.serializer(), info)
        assertTrue(raw.contains("\"token\":\"abc123\""))
        assertTrue(raw.contains("\"productCount\":5"))
    }
}
