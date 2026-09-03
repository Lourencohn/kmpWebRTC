package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.CarrinhoListagemDto
import app.trovata.cast.feature.carts.CartSituacao
import app.trovata.cast.feature.carts.OpenCart
import app.trovata.cast.feature.carts.OpenCartsOrder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class OpenCartsApi(
    private val client: HttpClient,
    private val tokenProvider: suspend () -> String?,
    private val baseUrl: String = SfaConfig.laravelApiUrl,
) {
    suspend fun listForSeller(
        empresaSlug: String,
        situacao: CartSituacao? = CartSituacao.Digitando,
        order: OpenCartsOrder = OpenCartsOrder.MaisRecentes,
        search: String? = null,
        perPage: Int = PER_PAGE,
    ): SfaApiResult<OpenCartsPage> {
        if (empresaSlug.isBlank()) {
            return SfaApiResult.Fail("missing_empresa", "Empresa ativa sem slug", 0)
        }
        return try {
            val token = tokenProvider()
                ?: return SfaApiResult.Fail("unauthenticated", "Faça login novamente", 401)
            val response = client.get("$baseUrl/empresa/$empresaSlug/carrinhos") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("page", 1)
                parameter("total", perPage)
                parameter("sort", order.sort)
                parameter("direction", order.direction)
                situacao?.takeIf { it.code.isNotEmpty() }?.let { parameter("situacao", it.code) }
                search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
            }
            if (!response.status.isSuccess()) {
                return SfaApiResult.Fail(
                    "http_${response.status.value}",
                    messageFor(response.status.value),
                    response.status.value,
                )
            }
            val envelope = response.body<SfaPaginatedEnvelope<CarrinhoListagemDto>>()
            val carts = envelope.data.orEmpty().map { it.toOpenCart() }
            SfaApiResult.Ok(
                OpenCartsPage(
                    carts = carts,
                    total = maxOf(envelope.page?.total ?: carts.size, carts.size),
                    hasMore = envelope.hasNextPage(),
                ),
            )
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
        const val PER_PAGE = 50
    }
}

data class OpenCartsPage(
    val carts: List<OpenCart>,
    val total: Int,
    val hasMore: Boolean,
)

fun CarrinhoListagemDto.toOpenCart(): OpenCart {
    val link = catalogoLink
    val clienteNome = nomeCliente?.takeIf { it.isNotBlank() }
        ?: cliente?.nomeFantasia?.takeIf { it.isNotBlank() }
        ?: cliente?.razaoSocial?.takeIf { it.isNotBlank() }
    val email = eMail?.takeIf { it.isNotBlank() }
        ?: cliente?.eMail?.takeIf { it.isNotBlank() }
        ?: link?.eMail?.takeIf { it.isNotBlank() }
    return OpenCart(
        carrinhoId = id,
        catalogoLinkId = catalogoLinkId ?: link?.id,
        catalogoUuid = link?.uuid,
        catalogoNome = link?.descricao?.takeIf { it.isNotBlank() }
            ?: link?.id?.let { "Catálogo $it" },
        catalogoAtivo = link?.situacao == null || link.situacao.equals("A", ignoreCase = true),
        catalogoValidadeMs = SfaParse.parseTimestampToMs(link?.dataValidade),
        clienteNome = clienteNome,
        clienteEmail = email,
        situacao = CartSituacao.fromCode(situacao),
        itens = itens ?: 0,
        quantidadeTotal = quantidadeTotal?.roundToInt() ?: 0,
        valorTotalCents = valorTotal?.let { (it * 100).roundToLong() },
        atualizadoEmMs = SfaParse.parseTimestampToMs(updatedAt)
            ?: SfaParse.parseTimestampToMs(createdAt),
    )
}
