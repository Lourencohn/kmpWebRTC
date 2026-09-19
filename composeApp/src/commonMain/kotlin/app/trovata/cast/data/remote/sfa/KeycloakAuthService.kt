package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.auth.AuthTokens
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long = 0,
)

@Serializable
private data class TokenError(
    val error: String? = null,
    @SerialName("error_description") val description: String? = null,
)

private const val OFFLINE_SCOPE = "offline_access"
private val OFFLINE_SCOPE_REFUSALS = setOf("not_allowed", "invalid_scope")

class KeycloakAuthService(
    private val client: HttpClient,
    private val keycloakUrl: String = SfaConfig.keycloakUrl,
    private val realm: String = SfaConfig.realm,
    private val clientId: String = SfaConfig.clientId,
) {
    private val tokenUrl: String
        get() = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"

    suspend fun login(username: String, password: String): SfaApiResult<AuthTokens> {
        val persistent = submit(passwordGrant(username, password, scope = OFFLINE_SCOPE))
        val refusedOfflineScope = persistent is SfaApiResult.Fail && persistent.code in OFFLINE_SCOPE_REFUSALS
        return if (refusedOfflineScope) submit(passwordGrant(username, password, scope = null)) else persistent
    }

    suspend fun refresh(refreshToken: String): SfaApiResult<AuthTokens> = submit(
        Parameters.build {
            append("grant_type", "refresh_token")
            append("client_id", clientId)
            append("refresh_token", refreshToken)
        },
    )

    private fun passwordGrant(username: String, password: String, scope: String?) = Parameters.build {
        append("grant_type", "password")
        append("client_id", clientId)
        append("username", username)
        append("password", password)
        scope?.let { append("scope", it) }
    }

    private suspend fun submit(form: Parameters): SfaApiResult<AuthTokens> = try {
        val response = client.submitForm(url = tokenUrl, formParameters = form)
        if (response.status.isSuccess()) SfaApiResult.Ok(tokensOf(response.body())) else failureOf(response)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
    }

    private fun tokensOf(body: TokenResponse): AuthTokens {
        val now = Clock.System.now().toEpochMilliseconds()
        val refreshToken = body.refreshToken.orEmpty()
        val refreshExpiresAtMs = when {
            refreshToken.isEmpty() -> now
            body.refreshExpiresIn <= 0 -> AuthTokens.NEVER_EXPIRES
            else -> now + body.refreshExpiresIn * 1000
        }
        return AuthTokens(
            accessToken = body.accessToken,
            refreshToken = refreshToken,
            accessExpiresAtMs = now + body.expiresIn * 1000,
            refreshExpiresAtMs = refreshExpiresAtMs,
        )
    }

    private suspend fun failureOf(response: HttpResponse): SfaApiResult.Fail {
        val status = response.status.value
        val error = runCatching { response.body<TokenError>() }.getOrNull()
        val message = if (status == 401) "Credenciais inválidas" else "Falha na autenticação (HTTP $status)"
        return SfaApiResult.Fail(error?.error ?: "auth_$status", message, status)
    }
}
