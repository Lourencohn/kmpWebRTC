package app.trovata.cast.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IceConfigTest {

    private fun env(vars: Map<String, String>): (String) -> String? = { vars[it] }

    @Test
    fun fallsBackToPublicStunWhenNothingIsConfigured() {
        val servers = IceConfig.fromEnv(env(emptyMap()))

        val único = servers.single()
        assertEquals(2, único.urls.size)
        assertTrue(único.urls.all { it.startsWith("stun:") })
        assertNull(único.username)
    }

    @Test
    fun buildsTurnWithCredentials() {
        val servers = IceConfig.fromEnv(
            env(
                mapOf(
                    "ICE_TURN_URLS" to "turn:turn.example:3478?transport=udp, turns:turn.example:5349",
                    "ICE_TURN_USERNAME" to "trovata",
                    "ICE_TURN_CREDENTIAL" to "segredo",
                ),
            ),
        )

        assertEquals(2, servers.size)
        val turn = servers.last()
        assertEquals(listOf("turn:turn.example:3478?transport=udp", "turns:turn.example:5349"), turn.urls)
        assertEquals("trovata", turn.username)
        assertEquals("segredo", turn.credential)
    }

    @Test
    fun replacesDefaultStunWhenProvided() {
        val servers = IceConfig.fromEnv(env(mapOf("ICE_STUN_URLS" to "stun:stun.trovata.app.br:3478")))

        assertEquals(listOf("stun:stun.trovata.app.br:3478"), servers.single().urls)
    }

    @Test
    fun providerWithoutCloudflareKeepsFixedServersAndCallsNothing() = runBlocking {
        var calls = 0
        val client = HttpClient(MockEngine { calls++; respondError(HttpStatusCode.InternalServerError) })

        val servers = IceConfig.providerFromEnv(client, env(emptyMap())).current()

        assertEquals(IceConfig.fromEnv(env(emptyMap())), servers)
        assertEquals(0, calls)
    }

    @Test
    fun providerAppendsCloudflareTurnToFixedServers() = runBlocking {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"iceServers":[{"urls":["turns:turn.cloudflare.com:443?transport=tcp"],"username":"u","credential":"c"}]}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        val vars = env(
            mapOf(
                "ICE_CLOUDFLARE_TURN_KEY_ID" to "chave-turn",
                "ICE_CLOUDFLARE_TURN_API_TOKEN" to "token-api",
            ),
        )

        val servers = IceConfig.providerFromEnv(client, vars).current()

        assertEquals(2, servers.size)
        assertTrue(servers.first().urls.all { it.startsWith("stun:") })
        assertEquals("c", servers.last().credential)
    }

    @Test
    fun providerKeepsFixedServersWhenCloudflareFails() = runBlocking {
        val client = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        val vars = env(
            mapOf(
                "ICE_CLOUDFLARE_TURN_KEY_ID" to "chave-turn",
                "ICE_CLOUDFLARE_TURN_API_TOKEN" to "token-api",
            ),
        )

        val servers = IceConfig.providerFromEnv(client, vars).current()

        assertEquals(IceConfig.fromEnv(vars), servers)
    }

    @Test
    fun ignoraEntradasVaziasEEspacos() {
        val servers = IceConfig.fromEnv(
            env(mapOf("ICE_STUN_URLS" to " , ", "ICE_TURN_URLS" to " turn:a:3478 , ,")),
        )

        assertEquals(2, servers.size)
        assertEquals(listOf("turn:a:3478"), servers.last().urls)
        assertNull(servers.last().username)
    }
}
