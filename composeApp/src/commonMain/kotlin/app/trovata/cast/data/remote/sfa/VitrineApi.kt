package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.VitrinePaginadaDto
import app.trovata.cast.data.remote.sfa.dto.VitrineProdutoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

data class VitrineProduto(
    val produtoPreId: Long,
    val produtoPre1Id: Long?,
    val complemento1Id: Long?,
    val ref: String,
    val nome: String,
    val cor: String?,
    val categoria: String?,
    val precoCents: Long?,
    val precoDeCents: Long?,
    val saldoDisponivel: Int?,
    val multiploVenda: Int,
    val noCarrinho: Boolean,
    val favorito: Boolean,
    val imageUrl: String?,
)

data class VitrinePage(
    val produtos: List<VitrineProduto>,
    val currentPage: Int,
    val lastPage: Int,
    val total: Int,
) {
    val hasNextPage: Boolean get() = currentPage < lastPage
}

class VitrineApi(
    private val client: HttpClient,
    private val baseUrl: String = SfaConfig.laravelApiUrl,
) {
    suspend fun produtos(
        empresaSlug: String,
        catalogoUuid: String,
        page: Int = 1,
        total: Int = DEFAULT_PAGE_SIZE,
        search: String? = null,
        categoriaId: Long? = null,
        carrinhoId: Long? = null,
        destaques: Boolean = false,
    ): SfaApiResult<VitrinePage> {
        if (empresaSlug.isBlank() || catalogoUuid.isBlank()) {
            return SfaApiResult.Fail("missing_catalogo", "Sessão sem catálogo link", 0)
        }
        return try {
            val response = client.get("$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/vitrine") {
                parameter("page", page)
                parameter("total", total)
                search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
                categoriaId?.let { parameter("categoria", it) }
                carrinhoId?.let { parameter("catalogo_carrinho", it) }
                if (destaques) parameter("destaques", true)
            }
            if (!response.status.isSuccess()) {
                return SfaApiResult.Fail(
                    "http_${response.status.value}",
                    messageFor(response.status.value),
                    response.status.value,
                )
            }
            val envelope = response.body<VitrinePaginadaDto>()
            SfaApiResult.Ok(
                VitrinePage(
                    produtos = envelope.data.map { it.toVitrineProduto() },
                    currentPage = envelope.currentPage,
                    lastPage = envelope.lastPage,
                    total = envelope.total,
                ),
            )
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }
    }

    private fun messageFor(status: Int): String = when (status) {
        404 -> "Catálogo não encontrado."
        422 -> "Catálogo indisponível."
        else -> "O servidor respondeu $status."
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 30
    }
}

fun VitrineProdutoDto.toVitrineProduto(): VitrineProduto = VitrineProduto(
    produtoPreId = id,
    produtoPre1Id = produtoPre1Id,
    complemento1Id = complemento1Id,
    ref = idErp?.takeIf { it.isNotBlank() } ?: id.toString(),
    nome = descricao?.takeIf { it.isNotBlank() }
        ?: apelido?.takeIf { it.isNotBlank() }
        ?: "Produto $id",
    cor = descricaoComplemento1?.takeIf { it.isNotBlank() },
    categoria = descricaoCategoria?.takeIf { it.isNotBlank() },
    precoCents = precoFinalCents,
    precoDeCents = precoDeCents?.takeIf { teveDesconto == true && it != precoFinalCents },
    saldoDisponivel = saldoDisponivel?.toInt(),
    multiploVenda = listaMultiploVenda?.trim()?.toDoubleOrNull()?.toInt()?.coerceAtLeast(1) ?: 1,
    noCarrinho = isCarrinho,
    favorito = isFavorito,
    imageUrl = arquivos?.melhorImagem,
)
