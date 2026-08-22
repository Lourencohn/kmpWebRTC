package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VitrineApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(
        body: String = """{"data":[],"current_page":1,"per_page":30,"total":0,"last_page":1}""",
        status: HttpStatusCode = HttpStatusCode.OK,
        capture: MutableList<HttpRequestData> = mutableListOf(),
    ): VitrineApi {
        val engine = MockEngine { request ->
            capture += request
            if (status.value >= 400) respondError(status) else respond(body, status, jsonHeaders)
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(HttpClientFactory.sfaJson) }
        }
        return VitrineApi(client = client, baseUrl = "https://api.example/api")
    }

    @Test
    fun montaOCaminhoPublicoDaVitrine() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        api.produtos(empresaSlug = "buba", catalogoUuid = "uuid-1", page = 2, carrinhoId = 77)

        val request = requests.single()
        assertEquals("/api/catalogos-links/buba/uuid-1/vitrine", request.url.encodedPath)
        assertEquals("2", request.url.parameters["page"])
        assertEquals("30", request.url.parameters["total"])
        assertEquals("77", request.url.parameters["catalogo_carrinho"])
        assertNull(request.url.parameters["search"])
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun naoChamaARedeSemIdentidadeDoCatalogo() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        val result = api.produtos(empresaSlug = "", catalogoUuid = "uuid-1")

        assertEquals("missing_catalogo", (result as SfaApiResult.Fail).code)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun mapeiaProdutoComPrecoNumericoEImagem() = runTest {
        val body = """
        {
          "data": [{
            "id": 4821, "id_erp": "22587", "produto_pre_1_id": 9012, "complemento_1_id": 3,
            "descricao": "KIT PROTETORES DE TOMADA", "apelido": "KIT PROTETORES",
            "descricao_categoria": "CUIDAR", "descricao_complemento_1": "AZUL",
            "preco_final": 144.9, "preco_de": 189.9, "teve_desconto": true, "percentual_desconto": 23.7,
            "saldo_disponivel": 12, "total_quantidade": null, "is_carrinho": true, "is_favorito": false,
            "lista_multiplo_venda": "6", "exibe_produto_indisponivel": false, "sequencia": 4,
            "arquivos": { "caminho_thumb": "https://cdn/t.jpg", "caminho_detail": "https://cdn/d.jpg", "sequencia": 1 }
          }],
          "current_page": 1, "per_page": 30, "total": 979, "last_page": 33
        }
        """.trimIndent()

        val page = (api(body = body).produtos("buba", "uuid-1") as SfaApiResult.Ok).value

        assertEquals(33, page.lastPage)
        assertEquals(979, page.total)
        assertTrue(page.hasNextPage)
        val produto = page.produtos.single()
        assertEquals(4821L, produto.produtoPreId)
        assertEquals(9012L, produto.produtoPre1Id)
        assertEquals(3L, produto.complemento1Id)
        assertEquals("22587", produto.ref)
        assertEquals("KIT PROTETORES DE TOMADA", produto.nome)
        assertEquals("AZUL", produto.cor)
        assertEquals(14490L, produto.precoCents)
        assertEquals(18990L, produto.precoDeCents)
        assertEquals(12, produto.saldoDisponivel)
        assertEquals(6, produto.multiploVenda)
        assertTrue(produto.noCarrinho)
        assertEquals("https://cdn/t.jpg", produto.imageUrl)
    }

    @Test
    fun aceitaPrecoComoStringNoPadraoBrasileiro() = runTest {
        val body = """
        {
          "data": [{ "id": 7, "preco_final": "1.189,90", "descricao": "Produto sete" }],
          "current_page": 1, "per_page": 30, "total": 1, "last_page": 1
        }
        """.trimIndent()

        val produto = (api(body = body).produtos("buba", "u") as SfaApiResult.Ok).value.produtos.single()

        assertEquals(118990L, produto.precoCents)
        assertEquals("7", produto.ref)
        assertEquals(1, produto.multiploVenda)
        assertNull(produto.precoDeCents)
    }

    @Test
    fun ignoraPrecoDeQuandoNaoHouveDesconto() = runTest {
        val body = """
        {
          "data": [{ "id": 8, "preco_final": 50, "preco_de": 50, "teve_desconto": false, "descricao": "Produto oito" }],
          "current_page": 1, "per_page": 30, "total": 1, "last_page": 1
        }
        """.trimIndent()

        val produto = (api(body = body).produtos("buba", "u") as SfaApiResult.Ok).value.produtos.single()

        assertEquals(5000L, produto.precoCents)
        assertNull(produto.precoDeCents)
    }

    @Test
    fun traduzErroDeCatalogoInexistente() = runTest {
        val fail = api(status = HttpStatusCode.NotFound).produtos("buba", "u") as SfaApiResult.Fail

        assertEquals("http_404", fail.code)
        assertEquals(404, fail.status)
    }

    @Test
    fun erroDeRedeViraFalhaDeDominio() = runTest {
        val engine = MockEngine { throw kotlin.RuntimeException("sem rede") }
        val client = HttpClient(engine) { expectSuccess = false }
        val api = VitrineApi(client = client, baseUrl = "https://api.example/api")

        val fail = api.produtos("buba", "u") as SfaApiResult.Fail

        assertEquals("network_error", fail.code)
    }
}
