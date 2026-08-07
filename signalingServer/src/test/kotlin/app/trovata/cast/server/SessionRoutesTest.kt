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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionRoutesTest {

    private val catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f"

    private fun sampleRequest() = SessionCreateRequest(
        empresaSlug = "atelier-norte",
        catalogoUuid = catalogoUuid,
        sellerId = "vend-31",
        sellerName = "Marina Prado",
        catalogoNome = "Outono 26",
        clientName = "Diego Albuquerque",
        clientEmail = "diego@trama.com.br",
    )

    @Test
    fun createReturnsTokenAndCatalogInviteUrl() = testApplication {
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
        assertTrue(body.sessionId.startsWith("ses_"))
        assertTrue(body.url.endsWith("/catalogo-link-view/atelier-norte/$catalogoUuid?live=${body.token}"))
    }

    @Test
    fun inviteUrlNeverUsesTheReservedTokenParam() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest())
        }.body()
        assertFalse(created.url.contains("?token="))
        assertFalse(created.url.contains("&token="))
    }

    @Test
    fun fetchAfterCreateReturnsCatalogIdentity() = testApplication {
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
        assertEquals("Marina Prado", info.sellerName)
        assertEquals("atelier-norte", info.empresaSlug)
        assertEquals(catalogoUuid, info.catalogoUuid)
        assertNotNull(info.clientName)
    }

    @Test
    fun fetchCarriesNoProductSnapshot() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val created: SessionCreateResponse = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest())
        }.body()

        val raw = client.get("/session/${created.token}").bodyAsText()
        listOf("\"products\"", "\"productSkus\"", "\"productCount\"", "\"priceCents\"")
            .forEach { field -> assertFalse(raw.contains(field), "sessão ainda carrega $field: $raw") }
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
    fun createWithoutCatalogFails() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest().copy(catalogoUuid = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body: ErrorResponse = response.body()
        assertEquals("missing_catalogo", body.code)
    }

    @Test
    fun createWithoutEmpresaFails() = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.post("/session") {
            contentType(ContentType.Application.Json)
            setBody(sampleRequest().copy(empresaSlug = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("missing_empresa", response.body<ErrorResponse>().code)
    }
}
