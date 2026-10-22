package app.trovata.cast.data.remote.sfa

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.remote.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement

class SfaApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val empresaIdProvider: () -> Long = { AppConfig.defaultEmpresaId },
    private val baseUrl: String = AppConfig.sfaApiUrl,
) {
    suspend fun fetchPageRaw(
        resource: String,
        page: Int,
        perPage: Int,
        updatedAt: String? = null,
    ): ApiResult<SfaListEnvelope<JsonElement>> {
        return try {
            val response = requestWithRetry(resource, page, perPage, updatedAt)
                ?: return ApiResult.Fail("unauthenticated", "Faça login novamente", 401)
            if (response.status.isSuccess()) {
                ApiResult.Ok(response.body())
            } else {
                ApiResult.Fail("http_${response.status.value}", "HTTP ${response.status.value}", response.status.value)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            ApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }
    }

    private suspend fun requestWithRetry(
        resource: String,
        page: Int,
        perPage: Int,
        updatedAt: String?,
    ): HttpResponse? {
        var attempt = 0
        while (true) {
            val token = tokenProvider() ?: return null
            val empresaId = empresaIdProvider()
            val response = client.get("$baseUrl/v2/empresas/$empresaId/$resource") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("page", page)
                parameter("per_page", perPage)
                updatedAt?.let { parameter("updated_at", it) }
            }
            if (response.status.value in 500..599 && attempt < MAX_RETRIES) {
                attempt++
                delay(BASE_BACKOFF_MS * attempt)
                continue
            }
            return response
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val BASE_BACKOFF_MS = 350L
    }
}
