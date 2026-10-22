package app.trovata.cast.server

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory

sealed class CatalogLinkCheck {
    data object Valid : CatalogLinkCheck()
    data object Skipped : CatalogLinkCheck()
    data class Rejected(val code: String, val message: String) : CatalogLinkCheck()
}

interface CatalogLinkValidator {
    suspend fun check(empresaSlug: String, catalogoUuid: String, bearerToken: String?): CatalogLinkCheck
}

object DisabledCatalogLinkValidator : CatalogLinkValidator {
    override suspend fun check(empresaSlug: String, catalogoUuid: String, bearerToken: String?): CatalogLinkCheck =
        CatalogLinkCheck.Skipped
}

class SfaCatalogLinkValidator(
    private val client: HttpClient,
    private val sfaBaseUrl: String,
) : CatalogLinkValidator {

    private val log = LoggerFactory.getLogger(SfaCatalogLinkValidator::class.java)

    override suspend fun check(
        empresaSlug: String,
        catalogoUuid: String,
        bearerToken: String?,
    ): CatalogLinkCheck {
        val url = buildString {
            append(sfaBaseUrl.trimEnd('/'))
            append("/catalogos-links/")
            append(empresaSlug.encodeURLPathPart())
            append('/')
            append(catalogoUuid.encodeURLPathPart())
            append('/')
            append(VALIDATION_RESOURCE)
        }
        return try {
            val response = client.get(url) {
                bearerToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
            when {
                response.status.isSuccess() -> CatalogLinkCheck.Valid
                response.status.value == 404 ->
                    CatalogLinkCheck.Rejected("catalogo_not_found", "Catálogo não encontrado para essa empresa")
                response.status.value == 400 ->
                    CatalogLinkCheck.Rejected("catalogo_indisponivel", rejectionMessage(response.bodyAsText()))
                response.status.value in 401..403 ->
                    CatalogLinkCheck.Rejected("catalogo_sem_acesso", "Sem acesso a esse catálogo")
                else -> {
                    log.warn("validação do catálogo indisponível: HTTP {}", response.status.value)
                    CatalogLinkCheck.Skipped
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            log.warn("validação do catálogo falhou, seguindo sem bloquear", t)
            CatalogLinkCheck.Skipped
        }
    }

    private companion object {
        const val VALIDATION_RESOURCE = "clientes-liberados"
    }

    private fun rejectionMessage(body: String): String = when {
        body.contains("expirado", ignoreCase = true) ->
            "Catálogo expirado. Renove a validade no Catálogo Link."
        body.contains("não está ativo", ignoreCase = true) || body.contains("nao esta ativo", ignoreCase = true) ->
            "Catálogo inativo."
        else -> "Catálogo indisponível."
    }
}
