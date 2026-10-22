package app.trovata.cast.server

import app.trovata.cast.protocol.IceServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class CloudflareTurnCredentials(
    private val client: HttpClient,
    private val keyId: String,
    private val apiToken: String,
    private val ttlSeconds: Long = DEFAULT_TTL_SECONDS,
    private val requestTimeoutMs: Long = REQUEST_TIMEOUT_MS,
) {

    private val log = LoggerFactory.getLogger(CloudflareTurnCredentials::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generate(): List<IceServerConfig> = try {
        val response = withTimeoutOrNull(requestTimeoutMs) {
            client.post("$API_BASE_URL/${keyId.encodeURLPathPart()}/credentials/generate-ice-servers") {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
                contentType(ContentType.Application.Json)
                setBody("""{"ttl":$ttlSeconds}""")
            }
        }
        when {
            response == null -> {
                log.warn("credencial TURN da Cloudflare sem resposta em {} ms, seguindo só com STUN", requestTimeoutMs)
                emptyList()
            }
            response.status.isSuccess() ->
                json.decodeFromString<CloudflareIceServers>(response.bodyAsText())
                    .iceServers
                    .withoutBrowserBlockedPorts()
            else -> {
                log.warn("credencial TURN da Cloudflare recusada: HTTP {}", response.status.value)
                emptyList()
            }
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        log.warn("credencial TURN da Cloudflare falhou, seguindo só com STUN", t)
        emptyList()
    }

    private fun List<IceServerConfig>.withoutBrowserBlockedPorts(): List<IceServerConfig> =
        mapNotNull { server ->
            server.copy(urls = server.urls.filterNot { it.port() == BROWSER_BLOCKED_PORT })
                .takeIf { it.urls.isNotEmpty() }
        }

    private fun String.port(): String = substringAfterLast(':').substringBefore('?')

    @Serializable
    private data class CloudflareIceServers(val iceServers: List<IceServerConfig>)

    private companion object {
        const val API_BASE_URL = "https://rtc.live.cloudflare.com/v1/turn/keys"
        const val DEFAULT_TTL_SECONDS = 6L * 60 * 60
        const val REQUEST_TIMEOUT_MS = 3_000L
        const val BROWSER_BLOCKED_PORT = "53"
    }
}
