package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import app.trovata.cast.protocol.SessionInfo
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionRoutesTest {

    private fun sampleRequest() = SessionCreateRequest(
        sellerId = "atelier-norte",
        sellerName = "Atelier Norte",
        collectionLabel = "Outono 26",
        productSkus = listOf("AN-104", "AN-217"),
        clientName = "Diego Albuquerque",
        clientShop = "Trama Multimarcas",
        scheduledFor = "Hoje · 14:00",
    )

    @Test
    fun createReturnsTokenAndUrl() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest())
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body: SessionCreateResponse = response.body()
        assertTrue(body.token.length >= 8)
        assertTrue(body.url.contains("?t=${body.token}"))
        assertTrue(body.sessionId.startsWith("ses_"))
    }

    @Test
    fun fetchAfterCreate() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest())
        }.body()

        val info: SessionInfo = client.get("/session/${created.token}").body()
        assertEquals(created.token, info.token)
        assertEquals("Atelier Norte", info.sellerName)
        assertEquals(2, info.productCount)
        assertNotNull(info.clientName)
    }

    @Test
    fun fetchUnknownReturns404() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.get("/session/zzz000")
        assertEquals(HttpStatusCode.NotFound, response.status)
        val body: ErrorResponse = response.body()
        assertEquals("not_found", body.code)
    }

    @Test
    fun createWithEmptySelectionFails() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest().copy(productSkus = emptyList()))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body: ErrorResponse = response.body()
        assertEquals("empty_selection", body.code)
    }
}
