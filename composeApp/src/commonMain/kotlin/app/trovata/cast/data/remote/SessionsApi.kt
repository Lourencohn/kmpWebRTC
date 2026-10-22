package app.trovata.cast.data.remote

import app.trovata.cast.config.AppConfig
import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import app.trovata.cast.protocol.SessionInfo
import com.shepeliev.webrtckmp.IceServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class SessionsApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.signalingUrl,
    private val tokenProvider: suspend () -> String? = { null },
) {
    suspend fun createSession(request: SessionCreateRequest): ApiResult<SessionCreateResponse> =
        safeCall {
            val token = tokenProvider()
            val response = client.post("$baseUrl/session") {
                contentType(ContentType.Application.Json)
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(request)
            }
            if (response.status.isSuccess()) {
                ApiResult.Ok(response.body())
            } else {
                failureFromBody(response.status, response.body<ErrorResponse?>())
            }
        }

    suspend fun fetchSession(token: String): ApiResult<SessionInfo> =
        safeCall {
            val response = client.get("$baseUrl/session/$token")
            if (response.status.isSuccess()) {
                ApiResult.Ok(response.body())
            } else {
                failureFromBody(response.status, response.body<ErrorResponse?>())
            }
        }

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            ApiResult.Fail(
                code = "network_error",
                message = t.message ?: "Sem conexão com o servidor",
                status = 0,
            )
        }

    private fun failureFromBody(status: HttpStatusCode, body: ErrorResponse?): ApiResult.Fail =
        ApiResult.Fail(
            code = body?.code ?: "http_error",
            message = body?.message ?: "HTTP ${status.value}",
            status = status.value,
        )
}

suspend fun SessionsApi.iceServersFor(token: String): List<IceServer> =
    when (val result = fetchSession(token)) {
        is ApiResult.Ok -> result.value.iceServers.map {
            IceServer(urls = it.urls, username = it.username.orEmpty(), password = it.credential.orEmpty())
        }
        is ApiResult.Fail -> emptyList()
    }
