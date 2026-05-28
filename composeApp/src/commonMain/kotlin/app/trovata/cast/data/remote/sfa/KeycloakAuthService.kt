package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.auth.AuthTokens
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
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

class KeycloakAuthService(
    private val client: HttpClient,
    private val keycloakUrl: String = SfaConfig.keycloakUrl,
    private val realm: String = SfaConfig.realm,
    private val clientId: String = SfaConfig.clientId,
) {
    private val tokenUrl: String
        get() = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"

    suspend fun login(username: String, password: String): SfaApiResult<AuthTokens> = submit(
        Parameters.build {
            append("grant_type", "password")
            append("client_id", clientId)
            append("username", username)
            append("password", password)
        },
    )

    suspend fun refresh(refreshToken: String): SfaApiResult<AuthTokens> = submit(
        Parameters.build {
            append("grant_type", "refresh_token")
            append("client_id", clientId)
            append("refresh_token", refreshToken)
        },
    )

    private suspend fun submit(form: Parameters): SfaApiResult<AuthTokens> = try {
        val response = client.submitForm(url = tokenUrl, formParameters = form)
        if (response.status.isSuccess()) {
            val body = response.body<TokenResponse>()
            val now = Clock.System.now().toEpochMilliseconds()
            SfaApiResult.Ok(
                AuthTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken ?: "",
                    accessExpiresAtMs = now + body.expiresIn * 1000,
                    refreshExpiresAtMs = now + body.refreshExpiresIn * 1000,
                ),
            )
        } else {
            val code = response.status.value
            val message = if (code == 401) "Credenciais inválidas" else "Falha na autenticação (HTTP $code)"
            SfaApiResult.Fail("auth_$code", message, code)
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
    }
}
