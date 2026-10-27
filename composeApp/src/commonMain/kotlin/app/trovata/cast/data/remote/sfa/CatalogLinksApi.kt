package app.trovata.cast.data.remote.sfa

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.dto.CatalogoLinkDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class CatalogLinksApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = AppConfig.laravelApiUrl,
) {
    suspend fun listForSeller(
        empresaSlug: String,
        search: String? = null,
        maxPages: Int = MAX_PAGES,
    ): ApiResult<CatalogLinkPage> {
        if (empresaSlug.isBlank()) {
            return ApiResult.Fail("missing_empresa", "Empresa ativa sem slug", 0)
        }
        return try {
            val token = tokenProvider()
                ?: return ApiResult.Fail("unauthenticated", "Faça login novamente", 401)
            val collected = mutableListOf<SellerCatalogLink>()
            var total = 0
            var hasMore = false
            var page = 1
            while (page <= maxPages) {
                val response = client.get("$baseUrl/empresa/$empresaSlug/catalogos-links") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("page", page)
                    parameter("total", PER_PAGE)
                    parameter("sort", "updated_at")
                    parameter("direction", "desc")
                    search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
                }
                if (!response.status.isSuccess()) {
                    return ApiResult.Fail(
                        "http_${response.status.value}",
                        messageFor(response.status.value),
                        response.status.value,
                    )
                }
                val envelope = response.body<SfaPaginatedEnvelope<CatalogoLinkDto>>()
                envelope.data.orEmpty().forEach { collected.add(it.toSellerCatalogLink()) }
                total = envelope.page?.total ?: collected.size
                hasMore = envelope.hasNextPage()
                if (!hasMore) break
                page++
            }
            ApiResult.Ok(
                CatalogLinkPage(
                    links = collected,
                    total = maxOf(total, collected.size),
                    hasMore = hasMore,
                ),
            )
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            ApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }
    }

    private fun messageFor(status: Int): String = when (status) {
        401, 403 -> "Sessão expirada. Faça login novamente."
        404 -> "Empresa não encontrada."
        else -> "O servidor respondeu $status."
    }

    private companion object {
        const val MAX_PAGES = 1
        const val PER_PAGE = 100
    }
}

data class CatalogLinkPage(
    val links: List<SellerCatalogLink>,
    val total: Int,
    val hasMore: Boolean,
)

fun CatalogoLinkDto.toSellerCatalogLink(): SellerCatalogLink {
    val clientName = nomeFantasia?.takeIf { it.isNotBlank() }
        ?: razaoSocial?.takeIf { it.isNotBlank() }
        ?: clientes.firstNotNullOfOrNull { it.nomeFantasia ?: it.razaoSocial }
    return SellerCatalogLink(
        id = id,
        uuid = uuid,
        name = descricao?.takeIf { it.isNotBlank() } ?: clientName ?: "Catálogo $id",
        clientName = clientName,
        clientEmail = eMail?.takeIf { it.isNotBlank() },
        clientDocument = cpfCnpj?.takeIf { it.isNotBlank() },
        sellerName = vendedor?.nomeFantasia ?: vendedor?.razaoSocial,
        active = situacao == null || situacao.equals("A", ignoreCase = true),
        expired = expirado == true,
        validityLabel = dataValidade?.take(10),
        cartCount = totalCarrinhos,
        viewCount = totalVisualizacoes,
        clientWhatsapp = celularWhatsapp?.takeIf { it.isNotBlank() },
    )
}
