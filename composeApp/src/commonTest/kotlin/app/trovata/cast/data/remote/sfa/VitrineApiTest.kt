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
    fun montaOCaminhoDaGradeDoProduto() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(body = """{"data":{"id":10,"variacoes":[]}}""", capture = requests)

        api.grade(empresaSlug = "buba", catalogoUuid = "uuid-1", produtoPreId = 10, carrinhoId = 5)

        val request = requests.single()
        assertEquals("/api/catalogos-links/buba/uuid-1/produtos/10/grades", request.url.encodedPath)
        assertEquals("5", request.url.parameters["catalogo_carrinho"])
    }

    @Test
    fun mapeiaCoresTamanhosESaldo() = runTest {
        val body = """
        {
          "data": {
            "id": 4821, "id_erp": "22587", "descricao": "Blusa Tricot",
            "lista_multiplo_venda": "6", "saldo_total_disponivel": 30, "produto_indisponivel": false,
            "variacoes": [
              {
                "complemento_1": { "id": 3, "id_erp": "AZ", "descricao": "AZUL" },
                "grades": [
                  {
                    "complemento_3": null,
                    "tamanhos": [
                      { "complemento_2": { "id": 11, "id_erp": "P", "descricao": "P" }, "disponivel": 12, "adicionados_count": 0 },
                      { "complemento_2": { "id": 12, "id_erp": "M", "descricao": "M" }, "disponivel": "8", "adicionados_count": 6 }
                    ]
                  }
                ],
                "arquivos": [ { "caminho_thumb": "https://cdn/azul.jpg" } ]
              },
              {
                "complemento_1": { "id": 4, "descricao": "PRETO" },
                "grades": [ { "tamanhos": [ { "complemento_2": { "id": 13, "descricao": "G" }, "disponivel": 10 } ] } ],
                "arquivos": []
              }
            ]
          }
        }
        """.trimIndent()

        val grade = (api(body = body).grade("buba", "u", 4821) as SfaApiResult.Ok).value

        assertEquals(4821L, grade.produtoPreId)
        assertEquals(6, grade.multiploVenda)
        assertEquals(30, grade.saldoTotal)
        assertEquals(2, grade.cores.size)

        val azul = grade.cores.first()
        assertEquals(3L, azul.complemento1Id)
        assertEquals("AZUL", azul.descricao)
        assertEquals("https://cdn/azul.jpg", azul.imageUrl)
        assertEquals(listOf("P", "M"), azul.tamanhos.map { it.label })
        assertEquals(listOf(12, 8), azul.tamanhos.map { it.disponivel })
        assertEquals(6, azul.tamanhos.last().adicionados)
        assertEquals(20, azul.saldoTotal)

        val preto = grade.cores.last()
        assertEquals("PRETO", preto.descricao)
        assertNull(preto.imageUrl)
    }

    @Test
    fun usaVariacaoUnicaQuandoNaoVemListaDeVariacoes() = runTest {
        val body = """
        {
          "data": {
            "id": 9, "descricao": "Produto nove",
            "variacoes": [],
            "variacao": {
              "complemento_1": { "id": 1, "descricao": "ÚNICA" },
              "grades": [ { "tamanhos": [ { "complemento_2": { "id": 2, "descricao": "U" }, "disponivel": 4 } ] } ],
              "arquivos": []
            }
          }
        }
        """.trimIndent()

        val grade = (api(body = body).grade("buba", "u", 9) as SfaApiResult.Ok).value

        assertEquals(1, grade.cores.size)
        assertEquals("ÚNICA", grade.cores.single().descricao)
        assertEquals(4, grade.cores.single().tamanhos.single().disponivel)
    }

    @Test
    fun gradeSemDadosViraFalhaDeDominio() = runTest {
        val fail = api(body = """{"data":null}""").grade("buba", "u", 1) as SfaApiResult.Fail

        assertEquals("grade_vazia", fail.code)
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
