package app.trovata.cast.data.remote.sfa

import app.trovata.cast.config.AppConfig
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.dto.CarrinhoDto
import app.trovata.cast.data.remote.sfa.dto.CarrinhoEnvelope
import app.trovata.cast.data.remote.sfa.dto.CarrinhoItemDto
import app.trovata.cast.data.remote.sfa.dto.CarrinhoLoginRequest
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

data class CartSession(
    val id: Long,
    val prazoId: Long?,
    val clientName: String?,
    val email: String?,
    val itemCount: Int,
    val status: String?,
)

data class CartItemSize(
    val complemento2Id: Long?,
    val label: String,
    val quantity: Int,
)

data class CartItemLine(
    val itemId: Long,
    val produtoPreId: Long?,
    val ref: String,
    val name: String,
    val color: String?,
    val imageUrl: String?,
    val quantity: Int,
    val totalCents: Long?,
    val sizes: List<CartItemSize>,
)

class CartApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String? = { null },
    private val baseUrl: String = AppConfig.laravelApiUrl,
) {
    suspend fun openCart(
        empresaSlug: String,
        catalogoUuid: String,
        email: String,
        cpfCnpj: String? = null,
        nomeCliente: String? = null,
    ): ApiResult<CartSession> = safeCall {
        if (email.isBlank()) {
            return@safeCall ApiResult.Fail("missing_email", "Catálogo link sem e-mail do cliente", 0)
        }
        val response = client.post("$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/login") {
            contentType(ContentType.Application.Json)
            setBody(CarrinhoLoginRequest(eMail = email, cpfCnpj = cpfCnpj, nomeCliente = nomeCliente))
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        val cart = response.body<CarrinhoEnvelope>().data
            ?: return@safeCall ApiResult.Fail("carrinho_ausente", "O servidor não devolveu o carrinho", 0)
        ApiResult.Ok(cart.toCartSession())
    }

    suspend fun summary(
        empresaSlug: String,
        catalogoUuid: String,
        carrinhoId: Long,
    ): ApiResult<CartSession> = safeCall {
        val response = client.get(
            "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/carrinhos/$carrinhoId",
        )
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        val cart = response.body<CarrinhoEnvelope>().data
            ?: return@safeCall ApiResult.Fail("carrinho_ausente", "Carrinho não encontrado", 0)
        ApiResult.Ok(cart.toCartSession())
    }

    suspend fun items(
        empresaSlug: String,
        catalogoUuid: String,
        carrinhoId: Long,
        page: Int = 1,
        total: Int = ITEMS_PER_PAGE,
    ): ApiResult<List<CartItemLine>> = safeCall {
        val response = client.get(
            "$baseUrl/catalogos-links/$empresaSlug/$catalogoUuid/carrinhos/$carrinhoId/itens-para-rota-publica",
        ) {
            parameter("page", page)
            parameter("total", total)
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        val envelope = response.body<SfaPaginatedEnvelope<CarrinhoItemDto>>()
        ApiResult.Ok(envelope.data.orEmpty().map { it.toCartItemLine() })
    }

    suspend fun markReadyToSend(
        empresaSlug: String,
        catalogoLinkId: Long,
        carrinhoId: Long,
    ): ApiResult<Unit> = safeCall {
        val token = tokenProvider()
            ?: return@safeCall ApiResult.Fail("unauthenticated", "Faça login novamente", 401)
        val response = client.patch(
            "$baseUrl/empresa/$empresaSlug/catalogos-links/$catalogoLinkId/carrinhos/$carrinhoId",
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) return@safeCall fail(response.status.value)
        ApiResult.Ok(Unit)
    }

    private suspend fun <T> safeCall(block: suspend () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            ApiResult.Fail("network_error", t.message ?: "Sem conexão com o servidor", 0)
        }

    private fun fail(status: Int): ApiResult.Fail = when (status) {
        400 -> ApiResult.Fail("invalid_request", "O servidor recusou os dados enviados", status)
        401, 403 -> ApiResult.Fail("unauthorized", "Sessão sem permissão", status)
        404 -> ApiResult.Fail("not_found", "Carrinho ou catálogo não encontrado", status)
        else -> ApiResult.Fail("http_$status", "O servidor respondeu $status", status)
    }

    private companion object {
        const val ITEMS_PER_PAGE = 50
    }
}

fun CarrinhoDto.toCartSession(): CartSession = CartSession(
    id = id,
    prazoId = prazoId,
    clientName = nomeCliente?.takeIf { it.isNotBlank() },
    email = eMail?.takeIf { it.isNotBlank() },
    itemCount = itens ?: 0,
    status = situacao,
)

fun CarrinhoItemDto.toCartItemLine(): CartItemLine {
    val sizes = grades.mapNotNull { sizeEntry ->
        val quantity = (sizeEntry.carrinhoItemGrade ?: sizeEntry.selecao)?.quantidade?.toInt() ?: 0
        if (quantity <= 0) return@mapNotNull null
        CartItemSize(
            complemento2Id = sizeEntry.complemento2?.id,
            label = sizeEntry.complemento2?.descricao?.takeIf { it.isNotBlank() }
                ?: sizeEntry.complemento2?.idErp?.takeIf { it.isNotBlank() }
                ?: "Único",
            quantity = quantity,
        )
    }
    val totalQuantity = quantidades?.total?.toInt() ?: sizes.sumOf { it.quantity }
    return CartItemLine(
        itemId = id,
        produtoPreId = produtoPreId,
        ref = produto?.idErp?.takeIf { it.isNotBlank() } ?: produtoPreId?.toString().orEmpty(),
        name = produto?.descricao?.takeIf { it.isNotBlank() }
            ?: produto?.apelido?.takeIf { it.isNotBlank() }
            ?: "Produto",
        color = variacao?.complemento1?.descricao?.takeIf { it.isNotBlank() },
        imageUrl = arquivo?.melhorImagem,
        quantity = totalQuantity,
        totalCents = valores?.total?.cents,
        sizes = sizes,
    )
}
