package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CartApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(
        responses: Map<String, String> = emptyMap(),
        status: HttpStatusCode = HttpStatusCode.OK,
        capture: MutableList<HttpRequestData> = mutableListOf(),
        token: String? = "tok-vendedor",
    ): CartApi {
        val engine = MockEngine { request ->
            capture += request
            if (status.value >= 400) {
                respondError(status)
            } else {
                val body = responses.entries.firstOrNull { request.url.encodedPath.endsWith(it.key) }?.value
                    ?: """{"data":{"id":1}}"""
                respond(body, status, jsonHeaders)
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(HttpClientFactory.sfaJson) }
        }
        return CartApi(client = client, tokenProvider = { token }, baseUrl = "https://api.example/api")
    }

    private fun bodyOf(request: HttpRequestData): String = (request.body as TextContent).text

    @Test
    fun opensCartThroughPublicLoginWithCatalogEmail() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(
            responses = mapOf(
                "/login" to """{"data":{"id":907,"e_mail":"diego@loja.com.br","prazo_id":4,"situacao":"Digitando","itens":3,"nome_cliente":"Loja do Diego"}}""",
            ),
            capture = requests,
        )

        val cart = (api.openCart("buba", "uuid-1", "diego@loja.com.br") as ApiResult.Ok).value

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/catalogos-links/buba/uuid-1/login", request.url.encodedPath)
        assertTrue(bodyOf(request).contains("\"e_mail\":\"diego@loja.com.br\""))
        assertNull(request.headers[HttpHeaders.Authorization])

        assertEquals(907L, cart.id)
        assertEquals(4L, cart.prazoId)
        assertEquals(3, cart.itemCount)
        assertEquals("Loja do Diego", cart.clientName)
    }

    @Test
    fun doesNotCallNetworkWithoutClientEmail() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        val fail = api.openCart("buba", "uuid-1", "  ") as ApiResult.Fail

        assertEquals("missing_email", fail.code)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun summaryReadsCartThroughPublicCatalogRoute() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(
            responses = mapOf(
                "/carrinhos/907" to """{"data":{"id":907,"itens":2,"situacao":"Digitando","nome_cliente":"Loja do Diego"}}""",
            ),
            capture = requests,
        )

        val cart = (api.summary("buba", "uuid-1", 907) as ApiResult.Ok).value

        assertEquals("/api/catalogos-links/buba/uuid-1/carrinhos/907", requests.single().url.encodedPath)
        assertEquals(2, cart.itemCount)
    }

    @Test
    fun listsCartItemsWithSizesAndValues() = runTest {
        val body = """
        {
          "data": [{
            "id": 51, "tipo": "produto", "catalogo_carrinho_id": 907,
            "produto_pre_id": 4821, "produto_pre_1_id": 9012, "sequencia": 1,
            "quantidades": { "total": 18, "por_caixa": null, "multiplicador": null },
            "valores": {
              "unitario": { "numerador": "1449", "denominador": "10", "valor": "144.900000", "formatado": "R$ 144.90" },
              "total": { "numerador": "26082", "denominador": "10", "valor": "2608.200000" }
            },
            "produto": { "id": 4821, "id_erp": "22587", "descricao": "Blusa Tricot" },
            "arquivo": { "caminho_thumb": "https://cdn/t.jpg" },
            "variacao": { "complemento_1": { "id": 3, "descricao": "AZUL" } },
            "grades": [
              { "complemento_2": { "id": 11, "descricao": "P" }, "carrinho_item_grade": { "id": 1, "quantidade": 12 } },
              { "complemento_2": { "id": 12, "descricao": "M" }, "carrinho_item_grade": { "id": 2, "quantidade": 6 } },
              { "complemento_2": { "id": 13, "descricao": "G" }, "carrinho_item_grade": null }
            ]
          }],
          "meta": { "current_page": 1, "last_page": 1, "per_page": 50, "total": 1 }
        }
        """.trimIndent()
        val requests = mutableListOf<HttpRequestData>()
        val api = api(responses = mapOf("itens-para-rota-publica" to body), capture = requests)

        val itemCount = (api.items("buba", "uuid-1", 907) as ApiResult.Ok).value

        assertEquals(
            "/api/catalogos-links/buba/uuid-1/carrinhos/907/itens-para-rota-publica",
            requests.single().url.encodedPath,
        )
        val line = itemCount.single()
        assertEquals(51L, line.itemId)
        assertEquals(4821L, line.produtoPreId)
        assertEquals("22587", line.ref)
        assertEquals("Blusa Tricot", line.name)
        assertEquals("AZUL", line.color)
        assertEquals("https://cdn/t.jpg", line.imageUrl)
        assertEquals(18, line.quantity)
        assertEquals(260820L, line.totalCents)
        assertEquals(listOf("P", "M"), line.sizes.map { it.label })
        assertEquals(listOf(12, 6), line.sizes.map { it.quantity })
    }

    @Test
    fun emptyCartIsNotAnError() = runTest {
        val api = api(responses = mapOf("itens-para-rota-publica" to """{"data":[],"meta":{"total":0}}"""))

        val itemCount = (api.items("buba", "uuid-1", 907) as ApiResult.Ok).value

        assertTrue(itemCount.isEmpty())
    }

    @Test
    fun markReadyUsesPrivateRouteWithBearer() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        api.markReadyToSend(empresaSlug = "buba", catalogoLinkId = 811, carrinhoId = 907)

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/api/empresa/buba/catalogos-links/811/carrinhos/907", request.url.encodedPath)
        assertEquals("Bearer tok-vendedor", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun markReadyRequiresAuthenticatedSession() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests, token = null)

        val fail = api.markReadyToSend("buba", 811, 907) as ApiResult.Fail

        assertEquals("unauthenticated", fail.code)
        assertTrue(requests.isEmpty())
    }

}
