package app.trovata.cast.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudflareTurnCredentialsTest {

    private val cloudflareResponse = """
        {
          "iceServers": [
            { "urls": ["stun:stun.cloudflare.com:3478", "stun:stun.cloudflare.com:53"] },
            {
              "urls": [
                "turn:turn.cloudflare.com:3478?transport=udp",
                "turn:turn.cloudflare.com:53?transport=udp",
                "turns:turn.cloudflare.com:443?transport=tcp"
              ],
              "username": "usuario-temporario",
              "credential": "senha-temporaria"
            }
          ]
        }
    """.trimIndent()

    private fun credentials(
        requestTimeoutMs: Long = 3_000,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<CloudflareTurnCredentials, MutableList<HttpRequestData>> {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests.add(request)
            handler(request)
        }
        val subject = CloudflareTurnCredentials(
            client = HttpClient(engine),
            keyId = "chave-turn",
            apiToken = "token-api",
            ttlSeconds = 3600,
            requestTimeoutMs = requestTimeoutMs,
        )
        return subject to requests
    }

    private fun MockRequestHandleScope.respondJson(body: String) = respond(
        content = body,
        status = HttpStatusCode.Created,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    @Test
    fun requestsCredentialsForTheKeyWithBearerAndTtl() = runBlocking {
        val (subject, requests) = credentials { respondJson(cloudflareResponse) }

        subject.generate()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(
            "https://rtc.live.cloudflare.com/v1/turn/keys/chave-turn/credentials/generate-ice-servers",
            request.url.toString(),
        )
        assertEquals("Bearer token-api", request.headers[HttpHeaders.Authorization])
        assertEquals("""{"ttl":3600}""", (request.body as TextContent).text)
    }

    @Test
    fun returnsTurnWithTemporaryCredentials() = runBlocking {
        val (subject, _) = credentials { respondJson(cloudflareResponse) }

        val turn = subject.generate().last()

        assertEquals("usuario-temporario", turn.username)
        assertEquals("senha-temporaria", turn.credential)
        assertTrue(turn.urls.contains("turns:turn.cloudflare.com:443?transport=tcp"))
    }

    @Test
    fun dropsPort53BecauseBrowsersBlockIt() = runBlocking {
        val (subject, _) = credentials { respondJson(cloudflareResponse) }

        val servers = subject.generate()

        assertEquals(listOf("stun:stun.cloudflare.com:3478"), servers.first().urls)
        assertEquals(
            listOf("turn:turn.cloudflare.com:3478?transport=udp", "turns:turn.cloudflare.com:443?transport=tcp"),
            servers.last().urls,
        )
    }

    @Test
    fun dropsServerLeftWithoutUrls() = runBlocking {
        val (subject, _) = credentials {
            respondJson("""{"iceServers":[{"urls":["stun:stun.cloudflare.com:53"]},{"urls":["turn:a:3478"]}]}""")
        }

        assertEquals(listOf("turn:a:3478"), subject.generate().single().urls)
    }

    @Test
    fun rejectedKeyFallsBackToNothing() = runBlocking {
        val (subject, _) = credentials { respondError(HttpStatusCode.Unauthorized) }

        assertTrue(subject.generate().isEmpty())
    }

    @Test
    fun unexpectedBodyFallsBackToNothing() = runBlocking {
        val (subject, _) = credentials { respondJson("""{"erro":"inesperado"}""") }

        assertTrue(subject.generate().isEmpty())
    }

    @Test
    fun slowCloudflareFallsBackToNothing() = runBlocking {
        val (subject, _) = credentials(requestTimeoutMs = 50) {
            delay(1_000)
            respondJson(cloudflareResponse)
        }

        assertTrue(subject.generate().isEmpty())
    }
}
