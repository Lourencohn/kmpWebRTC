package app.trovata.cast.data.remote.sfa

import app.trovata.cast.data.remote.sfa.dto.CarrinhoListagemDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class OpenCartsDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }

    private val listingPayload = """
        {
          "data": [
            {
              "id": 9021,
              "e_mail": "compras@bella.com.br",
              "descricao": "Carrinho:2026-08-28 09:12:44",
              "cpf_cnpj": "12345678000199",
              "cpf_cnpj_numerico": "12345678000199",
              "nome_cliente": "Loja Bella",
              "cliente_id": 331,
              "catalogo_link_id": 4471,
              "situacao": "D",
              "tipo_add_item_carrinho": "G",
              "itens": 12,
              "quantidade_total": "48.000",
              "valor_total": "4200.00",
              "valor_icms_st": null,
              "valor_ipi": null,
              "valor_total_bruto": "4200.00",
              "valor_total_com_impostos": null,
              "valor_total_bruto_com_impostos": null,
              "desconto_valor": "0.00",
              "perc_desconto_total": "0.00",
              "desc_comissao": null,
              "divisor_desc_comissao": null,
              "perc_desc_comissao": null,
              "total_vendas": 0,
              "observacao_roma": null,
              "moeda": "BRL",
              "simbolo": "R$",
              "created_at": "2026-08-28 09:12:44",
              "updated_at": "2026-08-30 16:41:02",
              "deleted_at": null,
              "cliente": {
                "id": 331,
                "id_erp": "C331",
                "situacao": "A",
                "nome_fantasia": "Loja Bella",
                "razao_social": "Bella Comercio de Roupas LTDA",
                "endereco": "Rua das Flores, 120",
                "perc_desconto": "0.00",
                "cpf_cnpj": "12345678000199",
                "cpf_cnpj_numerico": "12345678000199",
                "e_mail": "compras@bella.com.br",
                "telefone": "5133334444",
                "celular": "51999998888",
                "created_at": "2025-02-10 08:00:00",
                "updated_at": "2026-07-01 08:00:00",
                "deleted_at": null
              },
              "catalogo_link": {
                "id": 4471,
                "uuid": "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
                "descricao": "Outono 26",
                "situacao": "A",
                "cliente_novo": "NO",
                "origin_source": "sistema",
                "catalogo_link_online": 1,
                "catalogo_link_offline": null,
                "vendedor_id": 31,
                "usuario_id": 12,
                "cpf_cnpj": "12345678000199",
                "e_mail": "compras@bella.com.br",
                "celular_whatsapp": "51999998888",
                "observacao": null,
                "data_validade": "2026-12-31 00:00:00",
                "created_at": "2026-08-01 10:00:00",
                "updated_at": "2026-08-05 18:20:00",
                "deleted_at": null,
                "usuario": { "id": 12, "nome": "Marina Prado" },
                "vendedor": { "id": 31, "id_erp": "V31", "nome_fantasia": "Marina Prado" }
              }
            },
            {
              "id": 9022,
              "e_mail": null,
              "nome_cliente": null,
              "cliente_id": null,
              "catalogo_link_id": 4472,
              "situacao": "PE",
              "itens": 0,
              "quantidade_total": null,
              "valor_total": null,
              "created_at": "2026-08-29 11:00:00",
              "updated_at": "2026-08-29 11:00:00",
              "cliente": null,
              "catalogo_link": null
            }
          ],
          "meta": { "current_page": 1, "last_page": 2, "per_page": 50, "total": 74 }
        }
    """.trimIndent()

    @Test
    fun decodesGeneralCartListing() {
        val envelope = json.decodeFromString<SfaPaginatedEnvelope<CarrinhoListagemDto>>(listingPayload)
        val lines = envelope.data.orEmpty()

        assertEquals(2, lines.size)
        assertEquals(74, envelope.page?.total)
        assertTrue(envelope.hasNextPage())

        val firstCart = lines.first()
        assertEquals(9021, firstCart.id)
        assertEquals("Loja Bella", firstCart.nomeCliente)
        assertEquals("D", firstCart.situacao)
        assertEquals(12, firstCart.itens)
        assertEquals(4200.0, firstCart.valorTotal)
        assertEquals(48.0, firstCart.quantidadeTotal)
        assertEquals("5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f", firstCart.catalogoLink?.uuid)
    }

    @Test
    fun mapsResumableCartToScreenModel() {
        val envelope = json.decodeFromString<SfaPaginatedEnvelope<CarrinhoListagemDto>>(listingPayload)
        val cart = envelope.data.orEmpty().first().toOpenCart()
        val now = SfaParse.parseTimestampToMs("2026-09-02 10:00:00")!!

        assertEquals(9021, cart.carrinhoId)
        assertEquals(4471, cart.catalogoLinkId)
        assertEquals("5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f", cart.catalogoUuid)
        assertEquals("Outono 26", cart.catalogName)
        assertEquals("Loja Bella", cart.clientName)
        assertEquals("compras@bella.com.br", cart.clientEmail)
        assertEquals("51999998888", cart.clientPhone)
        assertEquals(CartStatus.Editing, cart.status)
        assertEquals(12, cart.itemCount)
        assertEquals(48, cart.totalQuantity)
        assertEquals(420_000L, cart.totalCents)
        assertNotNull(cart.updatedAtMs)
        assertTrue(cart.canCallLive(now))
        assertNull(cart.callBlockReason(now))
    }

    @Test
    fun cartWithoutCatalogOrNotEditingCannotBeCalled() {
        val envelope = json.decodeFromString<SfaPaginatedEnvelope<CarrinhoListagemDto>>(listingPayload)
        val cart = envelope.data.orEmpty()[1].toOpenCart()
        val now = SfaParse.parseTimestampToMs("2026-09-02 10:00:00")!!

        assertEquals(CartStatus.ReadyToSend, cart.status)
        assertNull(cart.catalogoUuid)
        assertFalse(cart.hasItems)
        assertFalse(cart.canCallLive(now))
        assertNotNull(cart.callBlockReason(now))
    }
}
