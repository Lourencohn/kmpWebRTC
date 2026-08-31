package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.CarrinhoDto
import app.trovata.cast.data.remote.sfa.dto.CarrinhoEnvelope
import app.trovata.cast.data.remote.sfa.dto.CarrinhoItemDto
import app.trovata.cast.data.remote.sfa.dto.CarrinhoGradeItemPayload
import app.trovata.cast.data.remote.sfa.dto.CarrinhoItemPayload
import app.trovata.cast.data.remote.sfa.dto.CarrinhoItensRequest
import app.trovata.cast.data.remote.sfa.dto.CarrinhoLoginRequest
import app.trovata.cast.data.remote.sfa.dto.ContextoComercialEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

data class CarrinhoSessao(
    val id: Long,
    val prazoId: Long?,
    val clienteNome: String?,
    val email: String?,
    val itens: Int,
    val situacao: String?,
)

data class ContextoComercial(
    val tabelaPrecoId: Long,
    val tipoVendaId: Long,
)

data class CarrinhoItemTamanho(
    val complemento2Id: Long?,
    val label: String,
    val quantidade: Int,
)

data class CarrinhoItemLinha(
    val itemId: Long,
    val produtoPreId: Long?,
    val ref: String,
    val nome: String,
    val cor: String?,
    val imageUrl: String?,
    val quantidade: Int,
    val unitarioCents: Long?,
    val totalCents: Long?,
    val tamanhos: List<CarrinhoItemTamanho>,
)

data class ItemParaCarrinho(
    val produtoPreId: Long,
    val complemento1Id: Long,
    val complemento3Id: Long? = null,
    val quantidadePorTamanho: Map<Long, Int>,
)

class CarrinhoApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String? = { null },
    private val baseUrl: String = SfaConfig.laravelApiUrl,
) {
    suspend fun abrirCarrinho(
        empresaSlug: String,
        catalogoUuid: String,
        email: String,
        cpfCnpj: String? = null,
        nomeCliente: String? = null,
    ): SfaApiResult<CarrinhoSessao> = safeCall {
        if (email.isBlank()) {
            return@safeCall SfaApiResult.Fail("missing_email", "Catálogo link sem e-mail do cliente", 0)
        }
        val response = client.post("$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/login") {
            contentType(ContentType.Application.Json)
            setBody(CarrinhoLoginRequest(eMail = email, cpfCnpj = cpfCnpj, nomeCliente = nomeCliente))
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        val carrinho = response.body<CarrinhoEnvelope>().data
            ?: return@safeCall SfaApiResult.Fail("carrinho_ausente", "O servidor não devolveu o carrinho", 0)
        SfaApiResult.Ok(carrinho.toCarrinhoSessao())
    }

    suspend fun contextoComercial(
        empresaSlug: String,
        catalogoUuid: String,
    ): SfaApiResult<ContextoComercial> = safeCall {
        val base = "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid"

        val tabelaResponse = client.get("$base/tabelas-precos/primeira")
        if (!tabelaResponse.status.isSuccess()) return@safeCall fail(tabelaResponse.status.value)
        val tabela = tabelaResponse.body<ContextoComercialEnvelope>().data
            ?: return@safeCall SfaApiResult.Fail("sem_tabela_preco", "Catálogo link sem tabela de preço", 0)

        val tipoVendaResponse = client.get("$base/tipos-vendas/primeiro")
        if (!tipoVendaResponse.status.isSuccess()) return@safeCall fail(tipoVendaResponse.status.value)
        val tipoVenda = tipoVendaResponse.body<ContextoComercialEnvelope>().data
            ?: return@safeCall SfaApiResult.Fail("sem_tipo_venda", "Catálogo link sem tipo de venda", 0)

        SfaApiResult.Ok(ContextoComercial(tabelaPrecoId = tabela.id, tipoVendaId = tipoVenda.id))
    }

    suspend fun salvarItem(
        empresaSlug: String,
        catalogoUuid: String,
        carrinhoId: Long,
        contexto: ContextoComercial,
        prazoId: Long,
        item: ItemParaCarrinho,
    ): SfaApiResult<Unit> = safeCall {
        val grades = item.quantidadePorTamanho
            .filterValues { it > 0 }
            .map { (complemento2Id, qtde) ->
                CarrinhoGradeItemPayload(complemento2Id = complemento2Id, qtde = qtde.toDouble())
            }
        if (grades.isEmpty()) {
            return@safeCall SfaApiResult.Fail("sem_quantidade", "Informe a quantidade de ao menos um tamanho", 0)
        }

        val response = client.post(
            "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/carrinhos/$carrinhoId/itens-multiple",
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                CarrinhoItensRequest(
                    produtoPreId = item.produtoPreId,
                    prazoId = prazoId,
                    tipoVendaId = contexto.tipoVendaId,
                    tabelaPrecoId = contexto.tabelaPrecoId,
                    items = listOf(
                        CarrinhoItemPayload(
                            complemento1Id = item.complemento1Id,
                            complemento3Id = item.complemento3Id,
                            gradesItens = grades,
                        ),
                    ),
                ),
            )
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        SfaApiResult.Ok(Unit)
    }

    suspend fun resumo(
        empresaSlug: String,
        catalogoUuid: String,
        carrinhoId: Long,
    ): SfaApiResult<CarrinhoSessao> = safeCall {
        val response = client.get(
            "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/carrinhos/$carrinhoId",
        )
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        val carrinho = response.body<CarrinhoEnvelope>().data
            ?: return@safeCall SfaApiResult.Fail("carrinho_ausente", "Carrinho não encontrado", 0)
        SfaApiResult.Ok(carrinho.toCarrinhoSessao())
    }

    suspend fun itens(
        empresaSlug: String,
        catalogoUuid: String,
        carrinhoId: Long,
        page: Int = 1,
        total: Int = ITENS_POR_PAGINA,
    ): SfaApiResult<List<CarrinhoItemLinha>> = safeCall {
        val response = client.get(
            "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/carrinhos/$carrinhoId/itens-para-rota-publica",
        ) {
            parameter("page", page)
            parameter("total", total)
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        val envelope = response.body<SfaPaginatedEnvelope<CarrinhoItemDto>>()
        SfaApiResult.Ok(envelope.data.orEmpty().map { it.toCarrinhoItemLinha() })
    }

    suspend fun marcarProntoParaEnvio(
        empresaSlug: String,
        catalogoLinkId: Long,
        carrinhoId: Long,
    ): SfaApiResult<Unit> = safeCall {
        val token = tokenProvider()
            ?: return@safeCall SfaApiResult.Fail("unauthenticated", "Faça login novamente", 401)
        val response = client.patch(
            "$baseUrl/empresa/$empresaSlug/catalogos-links/$catalogoLinkId/carrinhos/$carrinhoId",
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        SfaApiResult.Ok(Unit)
    }

    private suspend fun <T> safeCall(block: suspend () -> SfaApiResult<T>): SfaApiResult<T> =
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            SfaApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }

    private fun fail(status: Int): SfaApiResult.Fail = when (status) {
        400 -> SfaApiResult.Fail("invalid_request", "O servidor recusou os dados enviados", status)
        401, 403 -> SfaApiResult.Fail("unauthorized", "Sessão sem permissão", status)
        404 -> SfaApiResult.Fail("not_found", "Carrinho ou catálogo não encontrado", status)
        else -> SfaApiResult.Fail("http_$status", "O servidor respondeu $status", status)
    }

    private companion object {
        const val ITENS_POR_PAGINA = 50
    }
}

fun CarrinhoDto.toCarrinhoSessao(): CarrinhoSessao = CarrinhoSessao(
    id = id,
    prazoId = prazoId,
    clienteNome = nomeCliente?.takeIf { it.isNotBlank() },
    email = eMail?.takeIf { it.isNotBlank() },
    itens = itens ?: 0,
    situacao = situacao,
)

fun CarrinhoItemDto.toCarrinhoItemLinha(): CarrinhoItemLinha {
    val tamanhos = grades.mapNotNull { grade ->
        val quantidade = (grade.carrinhoItemGrade ?: grade.selecao)?.quantidade?.toInt() ?: 0
        if (quantidade <= 0) return@mapNotNull null
        CarrinhoItemTamanho(
            complemento2Id = grade.complemento2?.id,
            label = grade.complemento2?.descricao?.takeIf { it.isNotBlank() }
                ?: grade.complemento2?.idErp?.takeIf { it.isNotBlank() }
                ?: "Único",
            quantidade = quantidade,
        )
    }
    val quantidadeTotal = quantidades?.total?.toInt() ?: tamanhos.sumOf { it.quantidade }
    return CarrinhoItemLinha(
        itemId = id,
        produtoPreId = produtoPreId,
        ref = produto?.idErp?.takeIf { it.isNotBlank() } ?: produtoPreId?.toString().orEmpty(),
        nome = produto?.descricao?.takeIf { it.isNotBlank() }
            ?: produto?.apelido?.takeIf { it.isNotBlank() }
            ?: "Produto",
        cor = variacao?.complemento1?.descricao?.takeIf { it.isNotBlank() },
        imageUrl = arquivo?.melhorImagem,
        quantidade = quantidadeTotal,
        unitarioCents = valores?.unitario?.cents,
        totalCents = valores?.total?.cents,
        tamanhos = tamanhos,
    )
}
