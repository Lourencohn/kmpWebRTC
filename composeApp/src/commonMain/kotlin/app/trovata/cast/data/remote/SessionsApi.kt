package app.trovata.cast.data.remote

import app.trovata.cast.platform.ServerConfig
import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import app.trovata.cast.protocol.SessionInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

sealed class SessionsApiResult<out T> {
    data class Ok<T>(val value: T) : SessionsApiResult<T>()
    data class Fail(val code: String, val message: String, val status: Int) : SessionsApiResult<Nothing>()
}

class SessionsApi(
    private val client: HttpClient,
    private val baseUrl: String = ServerConfig.baseUrl,
) {
    suspend fun createSession(request: SessionCreateRequest): SessionsApiResult<SessionCreateResponse> {
        val response = client.post("$baseUrl/session") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            SessionsApiResult.Ok(response.body())
        } else {
            failureFromBody(response.status, response.body<ErrorResponse?>())
        }
    }

    suspend fun fetchSession(token: String): SessionsApiResult<SessionInfo> {
        val response = client.get("$baseUrl/session/$token")
        return if (response.status.isSuccess()) {
            SessionsApiResult.Ok(response.body())
        } else {
            failureFromBody(response.status, response.body<ErrorResponse?>())
        }
    }

    private fun failureFromBody(status: HttpStatusCode, body: ErrorResponse?): SessionsApiResult.Fail =
        SessionsApiResult.Fail(
            code = body?.code ?: "http_error",
            message = body?.message ?: "HTTP ${status.value}",
            status = status.value,
        )
}
