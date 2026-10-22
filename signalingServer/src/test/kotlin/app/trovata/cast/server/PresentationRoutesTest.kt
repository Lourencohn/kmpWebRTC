package app.trovata.cast.server

import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.PresentationCreateRequest
import app.trovata.cast.protocol.PresentationCreateResponse
import app.trovata.cast.protocol.PresentationInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class PresentationRoutesTest {

    private val catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f"

    private val presigner = S3Presigner(
        endpoint = "http://localhost:9000",
        bucket = "trovatacast-presentations",
        region = "auto",
        accessKey = "local",
        secretKey = "local-secret",
        now = { Instant.parse("2026-09-27T12:00:00Z") },
    )

    private fun request(durationMs: Long = 95_000, audioContentType: String = "audio/mp4") =
        PresentationCreateRequest(
            empresaSlug = "atelier-norte",
            catalogoUuid = catalogoUuid,
            sellerId = "vend-31",
            sellerName = "Marina Prado",
            durationMs = durationMs,
            catalogoNome = "Outono 26",
            audioContentType = audioContentType,
        )

    private fun ApplicationTestBuilder.appWith(
        storage: S3Presigner? = presigner,
        validator: CatalogLinkValidator = DisabledCatalogLinkValidator,
    ): HttpClient {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
            presentationRoutes(storage, "https://staging.trovata.app.br", validator, now = { 1_000L })
        }
        return createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun createReturnsShareLinkAndSignedUploadsForBothObjects() = testApplication {
        val client = appWith()
        val response = client.post("/presentations") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer seller-token")
            setBody(request())
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body: PresentationCreateResponse = response.body()
        assertTrue(PresentationIds.isWellFormed(body.presentationId))
        assertEquals(
            "https://staging.trovata.app.br/catalogo-link-view/atelier-norte/$catalogoUuid?apresentacao=${body.presentationId}",
            body.shareUrl,
        )
        assertTrue(
            body.timeline.url.startsWith(
                "http://localhost:9000/trovatacast-presentations/presentations/${body.presentationId}/timeline.json?",
            ),
        )
        assertTrue(
            body.audio.url.startsWith(
                "http://localhost:9000/trovatacast-presentations/presentations/${body.presentationId}/audio.m4a?",
            ),
        )
        assertEquals("application/json", body.timeline.contentType)
        assertEquals("audio/mp4", body.audio.contentType)
        assertEquals(1_000L + 15 * 60 * 1000, body.uploadExpiresAtMs)
    }

    @Test
    fun createWithoutSellerTokenIsRejected() = testApplication {
        val client = appWith()
        val response = client.post("/presentations") {
            contentType(ContentType.Application.Json)
            setBody(request())
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("missing_auth", response.body<ErrorResponse>().code)
    }

    @Test
    fun createRejectsEmptyAndTooLongRecordings() = testApplication {
        val client = appWith()
        listOf(0L to "empty_recording", 60L * 60 * 1000 + 1 to "recording_too_long").forEach { (duration, code) ->
            val response = client.post("/presentations") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer seller-token")
                setBody(request(durationMs = duration))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(code, response.body<ErrorResponse>().code)
        }
    }

    @Test
    fun createRejectsAudioFormatsThePlayerDoesNotExpect() = testApplication {
        val client = appWith()
        val response = client.post("/presentations") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer seller-token")
            setBody(request(audioContentType = "audio/wav"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("unsupported_audio", response.body<ErrorResponse>().code)
    }

    @Test
    fun createFollowsTheCatalogValidator() = testApplication {
        val rejecting = object : CatalogLinkValidator {
            override suspend fun check(empresaSlug: String, catalogoUuid: String, bearerToken: String?) =
                CatalogLinkCheck.Rejected("catalogo_indisponivel", "Catálogo expirado.")
        }
        val client = appWith(validator = rejecting)
        val response = client.post("/presentations") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer seller-token")
            setBody(request())
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals("catalogo_indisponivel", response.body<ErrorResponse>().code)
    }

    @Test
    fun fetchSignsPlaybackUrlsForTheSameObjects() = testApplication {
        val client = appWith()
        val id = "p7Kq2mZr8XwT4nBc"
        val response = client.get("/presentations/$id")
        assertEquals(HttpStatusCode.OK, response.status)
        val info: PresentationInfo = response.body()
        assertTrue(info.timelineUrl.contains("/presentations/$id/timeline.json?"))
        assertTrue(info.audioUrl.contains("/presentations/$id/audio.m4a?"))
        assertTrue(info.audioUrl.contains("X-Amz-Expires=21600"))
    }

    @Test
    fun fetchRefusesMalformedIdsWithoutSigningAnything() = testApplication {
        val client = appWith()
        val response = client.get("/presentations/..%2Fsecret")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun routesAnswerUnavailableWhenStorageIsNotConfigured() = testApplication {
        val client = appWith(storage = null)
        val response = client.get("/presentations/p7Kq2mZr8XwT4nBc")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("presentations_disabled", response.body<ErrorResponse>().code)
    }
}
