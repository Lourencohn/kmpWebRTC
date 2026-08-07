package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.CatalogoLinkDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogLinkDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val payloadDaListagem = """
        {
          "data": [
            {
              "id": 4471,
              "uuid": "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
              "descricao": "Outono 26",
              "situacao": "A",
              "cliente_novo": "N",
              "origin_source": "sistema",
              "catalogo_link_online": 1,
              "catalogo_link_offline": null,
              "vendedor_id": 31,
              "usuario_id": 12,
              "cpf_cnpj": "12345678000199",
              "nome_fantasia": "Trama Multimarcas",
              "razao_social": "Trama Comercio LTDA",
              "e_mail": "diego@trama.com.br",
              "celular_whatsapp": "51999998888",
              "observacao": null,
              "total_carrinhos": 3,
              "total_visualizacoes": 42,
              "expirado": false,
              "data_validade": "2026-12-31T00:00:00.000000Z",
              "created_at": "2026-08-01T10:00:00.000000Z",
              "updated_at": "2026-08-05T18:20:00.000000Z",
              "deleted_at": null,
              "usuario": { "id": 12, "nome": "Marina Prado" },
              "vendedor": {
                "id": 31,
                "id_erp": "V31",
                "situacao": "A",
                "nome_fantasia": "Marina Prado",
                "razao_social": "Marina Prado ME"
              },
              "clientes": [
                { "id": 88, "nome_fantasia": "Trama Multimarcas", "razao_social": "Trama Comercio LTDA" }
              ]
            }
          ],
          "meta": { "current_page": 1, "last_page": 2, "per_page": 100, "total": 120 }
        }
    """.trimIndent()

    @Test
    fun decodificaListagemRealDoCatalogoLink() {
        val envelope = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(payloadDaListagem)
        val dto = envelope.data!!.single()

        assertEquals(4471, dto.id)
        assertEquals("5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f", dto.uuid)
        assertEquals("Outono 26", dto.descricao)
        assertEquals(3, dto.totalCarrinhos)
        assertEquals(42, dto.totalVisualizacoes)
        assertEquals(false, dto.expirado)
        assertEquals("Marina Prado", dto.vendedor?.nomeFantasia)
        assertEquals("Trama Multimarcas", dto.clientes.single().nomeFantasia)
    }

    @Test
    fun paginacaoFuncionaEmMetaEEmPagination() {
        val comMeta = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(payloadDaListagem)
        assertTrue(comMeta.hasNextPage())
        assertEquals(1, comMeta.page?.currentPage)

        val comPagination = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(
            """{"data":[],"pagination":{"current_page":2,"last_page":2,"per_page":100,"total":120}}""",
        )
        assertFalse(comPagination.hasNextPage())
        assertEquals(2, comPagination.page?.currentPage)

        val semPaginacao = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>("""{"data":[]}""")
        assertFalse(semPaginacao.hasNextPage())
    }

    @Test
    fun mapeiaParaModeloDeDominio() {
        val dto = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(payloadDaListagem).data!!.single()
        val link = dto.toSellerCatalogLink()

        assertEquals("Outono 26", link.nome)
        assertEquals("Trama Multimarcas", link.clienteNome)
        assertEquals("Marina Prado", link.vendedorNome)
        assertEquals("2026-12-31", link.validadeLabel)
        assertTrue(link.ativo)
        assertFalse(link.expirado)
        assertTrue(link.disponivel)
    }

    @Test
    fun catalogoExpiradoOuInativoNaoFicaDisponivel() {
        val base = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(payloadDaListagem).data!!.single()

        assertFalse(base.copy(expirado = true).toSellerCatalogLink().disponivel)
        assertFalse(base.copy(situacao = "I").toSellerCatalogLink().disponivel)
    }

    @Test
    fun catalogoSemDescricaoUsaClienteComoNome() {
        val base = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(payloadDaListagem).data!!.single()
        val semDescricao = base.copy(descricao = null).toSellerCatalogLink()
        assertEquals("Trama Multimarcas", semDescricao.nome)

        val semNada = base.copy(
            descricao = null,
            nomeFantasia = null,
            razaoSocial = null,
            clientes = emptyList(),
        ).toSellerCatalogLink()
        assertEquals("Catálogo 4471", semNada.nome)
        assertNull(semNada.clienteNome)
    }

    @Test
    fun campoDesconhecidoNoPayloadNaoQuebraODecode() {
        val comCampoNovo = """{"data":[{"id":1,"uuid":"u","campo_que_ainda_nao_existe":true}]}"""
        val dto = json.decodeFromString<SfaPaginatedEnvelope<CatalogoLinkDto>>(comCampoNovo).data!!.single()
        assertEquals(1, dto.id)
        assertTrue(dto.toSellerCatalogLink().ativo)
    }
}
