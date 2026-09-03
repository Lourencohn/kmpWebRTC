package app.trovata.cast.data.remote.sfa

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

sealed class SfaApiResult<out T> {
    data class Ok<T>(val value: T) : SfaApiResult<T>()
    data class Fail(val code: String, val message: String, val status: Int) : SfaApiResult<Nothing>()
}

class SfaApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val empresaIdProvider: () -> Long = { SfaConfig.empresaId },
    private val baseUrl: String = SfaConfig.baseUrl,
) {
    suspend fun fetchPageRaw(
        resource: String,
        page: Int,
        perPage: Int,
        updatedAt: String? = null,
    ): SfaApiResult<SfaListEnvelope<JsonElement>> {
        return try {
            val response = requestWithRetry(resource, page, perPage, updatedAt)
                ?: return SfaApiResult.Fail("unauthenticated", "Faça login novamente", 401)
            if (response.status.isSuccess()) {
                SfaApiResult.Ok(response.body())
            } else {
                SfaApiResult.Fail("http_${response.status.value}", "HTTP ${response.status.value}", response.status.value)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
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
