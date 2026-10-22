package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.HttpClientFactory
import app.trovata.cast.data.remote.sfa.dto.ClientDto
import kotlin.test.Test
import kotlin.test.assertEquals

class SfaDtoTest {

    private val json = HttpClientFactory.sfaJson

    @Test
    fun decodesClientEnvelopeIgnoringUnknownKeys() {
        val raw = """
        {
          "data": [{
            "id": 4821, "id_old": "9912", "id_erp": "9912",
            "vendedor_id_erp": "31", "cidade_id_erp": "Joinville", "situacao": "A",
            "nome_fantasia": "Loja do Diego", "razao_social": "Diego Comercio de Roupas LTDA",
            "cpf_cnpj": "12345678000190", "e_mail": "diego@loja.com.br",
            "tabela_preco_id": 3, "prazo_id": 7, "perc_desconto": "2.5",
            "grupo_cliente_id_erp": "VAREJO",
            "updated_at": "2026-05-27T20:15:26Z", "deleted_at": null
          }],
          "deleted_ids": [17, 18],
          "pagination": { "current_page": 1, "last_page": 1063, "per_page": 2, "total": 2126 }
        }
        """.trimIndent()

        val env = json.decodeFromString<SfaListEnvelope<ClientDto>>(raw)
        val client = env.data!!.single()
        assertEquals(4821L, client.id)
        assertEquals("9912", client.idErp)
        assertEquals("31", client.vendedorIdErp)
        assertEquals("Loja do Diego", client.nomeFantasia)
        assertEquals("diego@loja.com.br", client.email)
        assertEquals(3L, client.tabelaPrecoId)
        assertEquals("2.5", client.percDesconto)
        assertEquals(listOf(17L, 18L), env.deletedIds)
        assertEquals(2126, env.pagination?.total)
        assertEquals(1063, env.pagination?.lastPage)
    }
}
