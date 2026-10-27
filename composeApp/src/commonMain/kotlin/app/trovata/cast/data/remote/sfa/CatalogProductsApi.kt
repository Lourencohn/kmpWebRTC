package app.trovata.cast.data.remote.sfa

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.dto.ProdutoComGradeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

data class ProductLabel(
    val produtoPreId: Long,
    val ref: String?,
    val name: String?,
    val imageUrl: String?,
)

class CatalogProductsApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.laravelApiUrl,
) {
    suspend fun label(
        empresaSlug: String,
        catalogoUuid: String,
        produtoPreId: Long,
    ): ApiResult<ProductLabel> = try {
        val response = client.get(
            "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/produtos/$produtoPreId/grades",
        )
        if (!response.status.isSuccess()) {
            ApiResult.Fail("http_${response.status.value}", "O servidor respondeu ${response.status.value}", response.status.value)
        } else {
            val produto = response.body<SfaObjectEnvelope<ProdutoComGradeDto>>().data
            if (produto == null) {
                ApiResult.Fail("produto_ausente", "Produto não encontrado", 0)
            } else {
                ApiResult.Ok(produto.toProductLabel())
            }
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        ApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
    }
}

fun ProdutoComGradeDto.toProductLabel(): ProductLabel = ProductLabel(
    produtoPreId = id,
    ref = idErp?.takeIf { it.isNotBlank() },
    name = descricao?.takeIf { it.isNotBlank() } ?: apelido?.takeIf { it.isNotBlank() },
    imageUrl = (listOfNotNull(variacao) + variacoes)
        .firstNotNullOfOrNull { variation -> variation.arquivos.firstNotNullOfOrNull { it.melhorImagem } },
)
