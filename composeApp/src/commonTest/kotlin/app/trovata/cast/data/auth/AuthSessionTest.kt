package app.trovata.cast.data.auth

import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.AccountApi
import app.trovata.cast.data.remote.sfa.KeycloakAuthService
import app.trovata.cast.data.remote.sfa.SfaApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class InMemoryAuthStorage(initial: Map<String, String> = emptyMap()) : AuthStorage {
    val values = initial.toMutableMap()
    override fun get(key: String): String? = values[key]
    override fun put(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private fun clientOf(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
    HttpClient(MockEngine { request -> handler(request) }) {
        expectSuccess = false
        install(ContentNegotiation) { json(HttpClientFactory.sfaJson) }
    }

private fun HttpRequestData.formField(name: String): String? =
    (body as? FormDataContent)?.formData?.get(name)

private const val NOW = 1_800_000_000_000L

private fun storedSession(accessExpiresAtMs: Long, refreshExpiresAtMs: Long) = mapOf(
    "access_token" to "access-antigo",
    "refresh_token" to "refresh-offline",
    "access_expires_at" to accessExpiresAtMs.toString(),
    "refresh_expires_at" to refreshExpiresAtMs.toString(),
    "empresa_id" to "97",
    "company_name" to "BUBA",
    "company_slug" to "buba",
)

class KeycloakAuthServiceTest {

    @Test
    fun loginPedeTokenOfflineETrataRefreshSemPrazoComoPermanente() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val service = KeycloakAuthService(
            clientOf { request ->
                requests += request
                respond(
                    """{"access_token":"a1","refresh_token":"r1","expires_in":300,"refresh_expires_in":0}""",
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            },
        )

        val result = service.login("vendedor@buba.com.br", "segredo") as SfaApiResult.Ok

        assertEquals("offline_access", requests.single().formField("scope"))
        assertEquals(AuthTokens.NEVER_EXPIRES, result.value.refreshExpiresAtMs)
        assertEquals("r1", result.value.refreshToken)
    }

    @Test
    fun loginRecuaParaSessaoComumQuandoOfflineNaoEPermitido() = runTest {
        val scopes = mutableListOf<String?>()
        val service = KeycloakAuthService(
            clientOf { request ->
                scopes += request.formField("scope")
                if (request.formField("scope") != null) {
                    respond(
                        """{"error":"not_allowed","error_description":"Offline tokens not allowed for the user or client"}""",
                        HttpStatusCode.BadRequest,
                        jsonHeaders,
                    )
                } else {
                    respond(
                        """{"access_token":"a1","refresh_token":"r1","expires_in":300,"refresh_expires_in":1800}""",
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                }
            },
        )

        val result = service.login("vendedor@buba.com.br", "segredo") as SfaApiResult.Ok

        assertEquals(listOf<String?>("offline_access", null), scopes)
        assertTrue(result.value.refreshExpiresAtMs < AuthTokens.NEVER_EXPIRES)
    }

    @Test
    fun credencialInvalidaNaoGeraSegundaTentativa() = runTest {
        var attempts = 0
        val service = KeycloakAuthService(
            clientOf {
                attempts += 1
                respond(
                    """{"error":"invalid_grant","error_description":"Invalid user credentials"}""",
                    HttpStatusCode.Unauthorized,
                    jsonHeaders,
                )
            },
        )

        val result = service.login("vendedor@buba.com.br", "errada") as SfaApiResult.Fail

        assertEquals(1, attempts)
        assertEquals("Credenciais inválidas", result.message)
    }
}

class AuthRepositorySessionTest {

    private fun repository(storage: AuthStorage, client: HttpClient) = AuthRepository(
        store = storage,
        keycloak = KeycloakAuthService(client),
        account = AccountApi(client),
        nowMs = { NOW },
    )

    @Test
    fun sessaoOfflineContinuaLogadaDiasDepois() = runTest {
        val storage = InMemoryAuthStorage(
            storedSession(accessExpiresAtMs = NOW - 86_400_000, refreshExpiresAtMs = AuthTokens.NEVER_EXPIRES),
        )
        val repo = repository(
            storage,
            clientOf {
                respond(
                    """{"access_token":"access-novo","refresh_token":"refresh-rotacionado","expires_in":300,"refresh_expires_in":0}""",
                    HttpStatusCode.OK,
                    jsonHeaders,
                )
            },
        )

        assertTrue(repo.isAuthenticated.value)
        assertEquals("access-novo", repo.accessToken())
        assertEquals("refresh-rotacionado", storage.values["refresh_token"])
        assertEquals("BUBA", repo.activeCompany.value?.name)
    }

    @Test
    fun faltaDeRedeNoRefreshNaoDerrubaOLogin() = runTest {
        val storage = InMemoryAuthStorage(
            storedSession(accessExpiresAtMs = NOW - 1_000, refreshExpiresAtMs = AuthTokens.NEVER_EXPIRES),
        )
        val repo = repository(storage, clientOf { throw IllegalStateException("sem rede") })

        assertNull(repo.accessToken())
        assertTrue(repo.isAuthenticated.value)
        assertEquals("refresh-offline", storage.values["refresh_token"])
    }

    @Test
    fun refreshRecusadoPeloServidorEncerraASessao() = runTest {
        val storage = InMemoryAuthStorage(
            storedSession(accessExpiresAtMs = NOW - 1_000, refreshExpiresAtMs = AuthTokens.NEVER_EXPIRES),
        )
        val repo = repository(
            storage,
            clientOf {
                respond(
                    """{"error":"invalid_grant","error_description":"Offline session not active"}""",
                    HttpStatusCode.BadRequest,
                    jsonHeaders,
                )
            },
        )

        assertNull(repo.accessToken())
        assertFalse(repo.isAuthenticated.value)
        assertNull(storage.values["refresh_token"])
    }

    @Test
    fun tokenDeAcessoValidoNaoChamaOServidor() = runTest {
        var calls = 0
        val storage = InMemoryAuthStorage(
            storedSession(accessExpiresAtMs = NOW + 120_000, refreshExpiresAtMs = AuthTokens.NEVER_EXPIRES),
        )
        val repo = repository(
            storage,
            clientOf {
                calls += 1
                respond("{}", HttpStatusCode.OK, jsonHeaders)
            },
        )

        assertEquals("access-antigo", repo.accessToken())
        assertEquals(0, calls)
    }
}
