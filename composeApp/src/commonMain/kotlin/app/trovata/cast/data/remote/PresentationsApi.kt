package app.trovata.cast.data.remote

import app.trovata.cast.config.AppConfig
import app.trovata.cast.protocol.ErrorResponse
import app.trovata.cast.protocol.PresentationCreateRequest
import app.trovata.cast.protocol.PresentationCreateResponse
import app.trovata.cast.protocol.PresentationUpload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

private const val UPLOAD_TIMEOUT_MS = 180_000L

class PresentationsApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.signalingUrl,
    private val tokenProvider: suspend () -> String? = { null },
) {
    suspend fun create(request: PresentationCreateRequest): ApiResult<PresentationCreateResponse> =
        safeCall {
            val token = tokenProvider()
            val response = client.post("$baseUrl/presentations") {
                contentType(ContentType.Application.Json)
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(request)
            }
            if (response.status.isSuccess()) {
                ApiResult.Ok(response.body())
            } else {
                val error = runCatching { response.body<ErrorResponse>() }.getOrNull()
                ApiResult.Fail(
                    code = error?.code ?: "http_error",
                    message = error?.message ?: "HTTP ${response.status.value}",
                    status = response.status.value,
                )
            }
        }

    suspend fun upload(target: PresentationUpload, bytes: ByteArray): ApiResult<Unit> =
        safeCall {
            val response = client.put(target.url) {
                timeout { requestTimeoutMillis = UPLOAD_TIMEOUT_MS }
                setBody(ByteArrayContent(bytes, ContentType.parse(target.contentType)))
            }
            if (response.status.isSuccess()) {
                ApiResult.Ok(Unit)
            } else {
                ApiResult.Fail(
                    code = "upload_failed",
                    message = "O armazenamento recusou o envio (HTTP ${response.status.value})",
                    status = response.status.value,
                )
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
}
