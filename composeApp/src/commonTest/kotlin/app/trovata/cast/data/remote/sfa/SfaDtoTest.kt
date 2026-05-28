package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.dto.ProductDto
import app.trovata.cast.data.remote.sfa.dto.ProductPriceDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SfaDtoTest {

    private val json = HttpClientFactory.sfaJson

    @Test
    fun decodesProductEnvelopeIgnoringUnknownKeys() {
        val raw = """
        {
          "data": [{
            "id": 20964, "id_old": "22587", "id_erp": "22587",
            "grupo_produto_id_erp": "44", "categoria_id_erp": "CUIDAR",
            "descricao": "KIT PROTETORES DE TOMADA - 24 PECAS",
            "descricao_tipo_complemento_1": "COR", "descricao_tipo_complemento_2": "TAMANHO",
            "descricao_categoria": "CUIDAR", "descricao_grupo_produto": "CUIDAR",
            "apelido": "KIT PROTETORES DE TOMADA - 24 ", "abreviatura_unidade": "UN",
            "situacao": "A", "grade": null, "codigo_barras": "7908103825878",
            "preco_base": null, "ncm": "39269090", "lista_multiplo_venda": "6",
            "categoria_id": 242, "colecao_id": null, "grupo_produto_id": 29,
            "sequencia_catalogo_link": 99999,
            "updated_at": "2026-05-27T20:15:26Z", "deleted_at": null
          }],
          "deleted_ids": null,
          "pagination": { "current_page": 1, "last_page": 1063, "per_page": 2, "total": 2126 }
        }
        """.trimIndent()

        val env = json.decodeFromString<SfaListEnvelope<ProductDto>>(raw)
        val product = env.data!!.single()
        assertEquals(20964L, product.id)
        assertEquals("22587", product.idErp)
        assertEquals("KIT PROTETORES DE TOMADA - 24 PECAS", product.descricao)
        assertEquals("7908103825878", product.codigoBarras)
        assertEquals("COR", product.tipoComplemento1)
        assertEquals("TAMANHO", product.tipoComplemento2)
        assertEquals(242L, product.categoriaId)
        assertEquals("6", product.listaMultiploVenda)
        assertEquals(99999L, product.sequenciaCatalogoLink)
        assertEquals(2126, env.pagination?.total)
        assertEquals(1063, env.pagination?.lastPage)
    }

    @Test
    fun decodesPriceWithGradeAndParsesCents() {
        val raw = """
        {
          "id": 5437, "tabela_preco_id_erp": "25", "produto_id_erp": "16131",
          "prazo_medio": "", "grade": "", "agrupamento": "", "preco": "79",
          "lista_grade": [{
            "complemento_1": "U",
            "complemento_2": [{ "complemento_2_id": 1, "complemento_2_id_erp": "U", "complemento_2_descricao": "U" }],
            "complemento_3": "U", "complemento_1_id": 1, "complemento_3_id": 1,
            "complemento_1_descricao": "UNICO", "complemento_3_descricao": "UNICO"
          }],
          "produto_pre_id": 1686, "tabela_preco_id": 3,
          "updated_at": "2026-04-29T15:12:19Z", "deleted_at": null
        }
        """.trimIndent()

        val price = json.decodeFromString<ProductPriceDto>(raw)
        assertEquals(5437L, price.id)
        assertEquals(1686L, price.produtoPreId)
        assertEquals(3L, price.tabelaPrecoId)
        assertEquals(7900L, SfaParse.parseCents(price.preco))

        val break0 = price.listaGrade!!.single()
        assertEquals("UNICO", break0.complemento1Descricao)
        assertEquals("U", break0.complemento2!!.single().complemento2Descricao)
        assertTrue(price.listaGrade!!.isNotEmpty())
    }
}
