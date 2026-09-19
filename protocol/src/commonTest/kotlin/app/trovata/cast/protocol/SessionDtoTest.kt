package app.trovata.cast.protocol

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            empresaSlug = "atelier-norte",
            catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
            sellerId = "vend-31",
            sellerName = "Marina Prado",
            catalogoNome = "Outono 26",
            carrinhoId = 90_112,
            clientName = "Diego Albuquerque",
            clientEmail = "diego@lojadiego.com.br",
        )
        val raw = json.encodeToString(SessionCreateRequest.serializer(), payload)
        val decoded = json.decodeFromString(SessionCreateRequest.serializer(), raw)
        assertEquals(payload, decoded)
    }

    @Test
    fun sessionCreateRequestCarriesNoProductSnapshot() {
        val payload = SessionCreateRequest(
            empresaSlug = "atelier-norte",
            catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
            sellerId = "vend-31",
            sellerName = "Marina Prado",
        )
        val raw = json.encodeToString(SessionCreateRequest.serializer(), payload)
        assertFalse(raw.contains("products"))
        assertFalse(raw.contains("productSkus"))
    }

    @Test
    fun sessionInfoSerializes() {
        val info = SessionInfo(
            token = "abc123",
            empresaSlug = "atelier-norte",
            catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
            sellerName = "Marina Prado",
            catalogoNome = "Outono 26",
            carrinhoId = null,
            clientName = null,
            clientEmail = null,
            createdAtMs = 1_700_000_000_000,
            expiresAtMs = 1_700_014_400_000,
        )
        val raw = json.encodeToString(SessionInfo.serializer(), info)
        assertTrue(raw.contains("\"token\":\"abc123\""))
        assertTrue(raw.contains("\"catalogoUuid\":\"5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f\""))
    }
}

class LiveUrlTest {

    @Test
    fun sellerEmbedUrlAppendsToInviteQuery() {
        val invite = buildLiveInviteUrl(
            catalogBaseUrl = "https://staging.trovata.app.br/",
            empresaSlug = "buba",
            catalogoUuid = "5f6c1d2e",
            token = "tok123",
        )
        assertEquals("https://staging.trovata.app.br/catalogo-link-view/buba/5f6c1d2e?live=tok123", invite)
        assertEquals("$invite&embed=seller", buildSellerEmbedUrl(invite))
    }

    @Test
    fun sellerEmbedUrlStartsQueryWhenInviteHasNone() {
        assertEquals(
            "https://staging.trovata.app.br/catalogo-link-view/buba/uuid?embed=seller",
            buildSellerEmbedUrl("https://staging.trovata.app.br/catalogo-link-view/buba/uuid"),
        )
    }
}
