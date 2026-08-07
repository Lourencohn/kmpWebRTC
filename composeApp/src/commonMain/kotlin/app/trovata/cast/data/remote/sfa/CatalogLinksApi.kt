package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.CatalogoLinkDto
import app.trovata.cast.feature.catalog.SellerCatalogLink
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
    private val baseUrl: String = SfaConfig.laravelApiUrl,
) {
    suspend fun listForSeller(
        empresaSlug: String,
        search: String? = null,
        maxPages: Int = MAX_PAGES,
    ): SfaApiResult<List<SellerCatalogLink>> {
        if (empresaSlug.isBlank()) {
            return SfaApiResult.Fail("missing_empresa", "Empresa ativa sem slug", 0)
        }
        return try {
            val token = tokenProvider()
                ?: return SfaApiResult.Fail("unauthenticated", "Faça login novamente", 401)
            val collected = mutableListOf<SellerCatalogLink>()
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
                    return SfaApiResult.Fail(
                        "http_${response.status.value}",
                        messageFor(response.status.value),
                        response.status.value,
                    )
                }
                val envelope = response.body<SfaPaginatedEnvelope<CatalogoLinkDto>>()
                envelope.data.orEmpty().forEach { collected.add(it.toSellerCatalogLink()) }
                if (!envelope.hasNextPage()) break
                page++
            }
            SfaApiResult.Ok(collected)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }
    }

    private fun messageFor(status: Int): String = when (status) {
        401, 403 -> "Sessão expirada. Faça login novamente."
        404 -> "Empresa não encontrada."
        else -> "O servidor respondeu $status."
    }

    private companion object {
        const val MAX_PAGES = 20
        const val PER_PAGE = 100
    }
}

fun CatalogoLinkDto.toSellerCatalogLink(): SellerCatalogLink {
    val clienteNome = nomeFantasia?.takeIf { it.isNotBlank() }
        ?: razaoSocial?.takeIf { it.isNotBlank() }
        ?: clientes.firstNotNullOfOrNull { it.nomeFantasia ?: it.razaoSocial }
    return SellerCatalogLink(
        id = id,
        uuid = uuid,
        nome = descricao?.takeIf { it.isNotBlank() } ?: clienteNome ?: "Catálogo $id",
        clienteNome = clienteNome,
        vendedorNome = vendedor?.nomeFantasia ?: vendedor?.razaoSocial,
        ativo = situacao == null || situacao.equals("A", ignoreCase = true),
        expirado = expirado == true,
        validadeLabel = dataValidade?.take(10),
        totalCarrinhos = totalCarrinhos,
        totalVisualizacoes = totalVisualizacoes,
    )
}
