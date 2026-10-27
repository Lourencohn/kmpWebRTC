package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.ProdutoComGradeDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class ProdutoDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val payload = """
        {
          "data": {
            "id": 4932,
            "id_erp": "20889",
            "descricao": "PRATINHO EM SILICONE COM VENTOSA CLOUDY ELEFANTE",
            "apelido": "PRATINHO EM SILICONE COM VENTO",
            "situacao": "A",
            "validar_estoque": "S",
            "produto_indisponivel": false,
            "saldo_total_disponivel": "12.000",
            "produto_pre_1": { "id": 9, "produto_pre_id": 4932, "complemento_1_id": 3 },
            "variacoes": [],
            "variacao": {
              "complemento_1": { "id": 3, "descricao": "AZUL" },
              "grades": [],
              "arquivos": [
                {
                  "id": 9160,
                  "caminho_original": "https://cdn.exemplo/original/20889_00.jpg",
                  "caminho_thumb": "https://cdn.exemplo/thumb/20889_00.jpg",
                  "tipo": "imagem"
                }
              ],
              "descontos_por_origem": []
            },
            "tipos_vendas_ids": [1]
          }
        }
    """.trimIndent()

    @Test
    fun mapsTheProductLabelFromThePublicGradeRoute() {
        val produto = json.decodeFromString<SfaObjectEnvelope<ProdutoComGradeDto>>(payload).data!!

        assertEquals(
            ProductLabel(
                produtoPreId = 4932,
                ref = "20889",
                name = "PRATINHO EM SILICONE COM VENTOSA CLOUDY ELEFANTE",
                imageUrl = "https://cdn.exemplo/thumb/20889_00.jpg",
            ),
            produto.toProductLabel(),
        )
    }

    @Test
    fun nicknameAndMissingImageAreHandled() {
        val produto = ProdutoComGradeDto(id = 1, idErp = " ", descricao = "", apelido = "Blusa Tricô")

        val label = produto.toProductLabel()
        assertEquals("Blusa Tricô", label.name)
        assertNull(label.ref)
        assertNull(label.imageUrl)
    }
}
