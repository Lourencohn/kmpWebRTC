package app.trovata.cast.data.remote.sfa

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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CarrinhoApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(
        respostas: Map<String, String> = emptyMap(),
        status: HttpStatusCode = HttpStatusCode.OK,
        capture: MutableList<HttpRequestData> = mutableListOf(),
        token: String? = "tok-vendedor",
    ): CarrinhoApi {
        val engine = MockEngine { request ->
            capture += request
            if (status.value >= 400) {
                respondError(status)
            } else {
                val corpo = respostas.entries.firstOrNull { request.url.encodedPath.endsWith(it.key) }?.value
                    ?: """{"data":{"id":1}}"""
                respond(corpo, status, jsonHeaders)
            }
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(HttpClientFactory.sfaJson) }
        }
        return CarrinhoApi(client = client, tokenProvider = { token }, baseUrl = "https://api.example/api")
    }

    private fun corpoDe(request: HttpRequestData): String = (request.body as TextContent).text

    @Test
    fun abreCarrinhoPeloLoginPublicoComOEmailDoCatalogo() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(
            respostas = mapOf(
                "/login" to """{"data":{"id":907,"e_mail":"diego@loja.com.br","prazo_id":4,"situacao":"Digitando","itens":3,"nome_cliente":"Loja do Diego"}}""",
            ),
            capture = requests,
        )

        val carrinho = (api.abrirCarrinho("buba", "uuid-1", "diego@loja.com.br") as SfaApiResult.Ok).value

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/catalogos-links/buba/uuid-1/login", request.url.encodedPath)
        assertTrue(corpoDe(request).contains("\"e_mail\":\"diego@loja.com.br\""))
        assertNull(request.headers[HttpHeaders.Authorization])

        assertEquals(907L, carrinho.id)
        assertEquals(4L, carrinho.prazoId)
        assertEquals(3, carrinho.itens)
        assertEquals("Loja do Diego", carrinho.clienteNome)
    }

    @Test
    fun naoChamaARedeSemEmailDoCliente() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        val fail = api.abrirCarrinho("buba", "uuid-1", "  ") as SfaApiResult.Fail

        assertEquals("missing_email", fail.code)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun resolveTabelaDePrecoETipoDeVendaDoCatalogo() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(
            respostas = mapOf(
                "/tabelas-precos/primeira" to """{"data":{"id":1,"id_erp":"1","descricao":"TABELA PADRAO"}}""",
                "/tipos-vendas/primeiro" to """{"data":{"id":7,"descricao":"VENDA"}}""",
            ),
            capture = requests,
        )

        val contexto = (api.contextoComercial("buba", "uuid-1") as SfaApiResult.Ok).value

        assertEquals(1L, contexto.tabelaPrecoId)
        assertEquals(7L, contexto.tipoVendaId)
        assertEquals(2, requests.size)
    }

    @Test
    fun avisaQuandoOCatalogoNaoTemTabelaDePreco() = runTest {
        val api = api(respostas = mapOf("/tabelas-precos/primeira" to """{"data":null}"""))

        val fail = api.contextoComercial("buba", "uuid-1") as SfaApiResult.Fail

        assertEquals("sem_tabela_preco", fail.code)
    }

    @Test
    fun lancaItemComGradePorTamanho() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        val result = api.salvarItem(
            empresaSlug = "buba",
            catalogoUuid = "uuid-1",
            carrinhoId = 907,
            contexto = ContextoComercial(tabelaPrecoId = 1, tipoVendaId = 7),
            prazoId = 4,
            item = ItemParaCarrinho(
                produtoPreId = 4821,
                complemento1Id = 3,
                quantidadePorTamanho = mapOf(11L to 12, 12L to 6),
            ),
        )

        assertTrue(result is SfaApiResult.Ok)
        val request = requests.single()
        assertEquals("/api/catalogos-links/buba/uuid-1/carrinhos/907/itens-multiple", request.url.encodedPath)
        val corpo = corpoDe(request)
        assertTrue(corpo.contains("\"produto_pre_id\":4821"))
        assertTrue(corpo.contains("\"tabela_preco_id\":1"))
        assertTrue(corpo.contains("\"tipo_venda_id\":7"))
        assertTrue(corpo.contains("\"prazo_id\":4"))
        assertTrue(corpo.contains("\"complemento_1_id\":3"))
        assertTrue(corpo.contains("\"complemento_2_id\":11"))
        assertTrue(corpo.contains("\"qtde\":12"))
    }

    @Test
    fun recusaItemSemNenhumTamanho() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        val fail = api.salvarItem(
            empresaSlug = "buba",
            catalogoUuid = "uuid-1",
            carrinhoId = 907,
            contexto = ContextoComercial(1, 7),
            prazoId = 4,
            item = ItemParaCarrinho(produtoPreId = 1, complemento1Id = 1, quantidadePorTamanho = emptyMap()),
        ) as SfaApiResult.Fail

        assertEquals("sem_quantidade", fail.code)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun resumoLeOCarrinhoPelaRotaPublicaDoCatalogo() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(
            respostas = mapOf(
                "/carrinhos/907" to """{"data":{"id":907,"itens":2,"situacao":"Digitando","nome_cliente":"Loja do Diego"}}""",
            ),
            capture = requests,
        )

        val carrinho = (api.resumo("buba", "uuid-1", 907) as SfaApiResult.Ok).value

        assertEquals("/api/catalogos-links/buba/uuid-1/carrinhos/907", requests.single().url.encodedPath)
        assertEquals(2, carrinho.itens)
    }

    @Test
    fun listaItensDoCarrinhoComTamanhosEValores() = runTest {
        val corpo = """
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
        val api = api(respostas = mapOf("itens-para-rota-publica" to corpo), capture = requests)

        val itens = (api.itens("buba", "uuid-1", 907) as SfaApiResult.Ok).value

        assertEquals(
            "/api/catalogos-links/buba/uuid-1/carrinhos/907/itens-para-rota-publica",
            requests.single().url.encodedPath,
        )
        val linha = itens.single()
        assertEquals(51L, linha.itemId)
        assertEquals(4821L, linha.produtoPreId)
        assertEquals("22587", linha.ref)
        assertEquals("Blusa Tricot", linha.nome)
        assertEquals("AZUL", linha.cor)
        assertEquals("https://cdn/t.jpg", linha.imageUrl)
        assertEquals(18, linha.quantidade)
        assertEquals(14490L, linha.unitarioCents)
        assertEquals(260820L, linha.totalCents)
        assertEquals(listOf("P", "M"), linha.tamanhos.map { it.label })
        assertEquals(listOf(12, 6), linha.tamanhos.map { it.quantidade })
    }

    @Test
    fun carrinhoVazioNaoViraErro() = runTest {
        val api = api(respostas = mapOf("itens-para-rota-publica" to """{"data":[],"meta":{"total":0}}"""))

        val itens = (api.itens("buba", "uuid-1", 907) as SfaApiResult.Ok).value

        assertTrue(itens.isEmpty())
    }

    @Test
    fun marcarProntoUsaARotaPrivadaComBearer() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests)

        api.marcarProntoParaEnvio(empresaSlug = "buba", catalogoLinkId = 811, carrinhoId = 907)

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/api/empresa/buba/catalogos-links/811/carrinhos/907", request.url.encodedPath)
        assertEquals("Bearer tok-vendedor", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun marcarProntoExigeSessaoAutenticada() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = api(capture = requests, token = null)

        val fail = api.marcarProntoParaEnvio("buba", 811, 907) as SfaApiResult.Fail

        assertEquals("unauthenticated", fail.code)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun traduzRecusaDoServidorAoLancarItem() = runTest {
        val fail = api(status = HttpStatusCode.BadRequest).salvarItem(
            empresaSlug = "buba",
            catalogoUuid = "uuid-1",
            carrinhoId = 907,
            contexto = ContextoComercial(1, 7),
            prazoId = 4,
            item = ItemParaCarrinho(produtoPreId = 1, complemento1Id = 1, quantidadePorTamanho = mapOf(2L to 6)),
        ) as SfaApiResult.Fail

        assertEquals("invalid_request", fail.code)
        assertEquals(400, fail.status)
    }
}
