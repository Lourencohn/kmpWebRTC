package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceTableDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    val situacao: String? = null,
    @SerialName("perc_desconto") val percDesconto: String? = null,
    @SerialName("perc_desconto_parceria") val percParceria: String? = null,
    @SerialName("perc_desconto_gerencial") val percGerencial: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ProductPriceDto(
    val id: Long,
    @SerialName("tabela_preco_id") val tabelaPrecoId: Long? = null,
    @SerialName("tabela_preco_id_erp") val tabelaPrecoIdErp: String? = null,
    @SerialName("produto_pre_id") val produtoPreId: Long? = null,
    @SerialName("produto_id_erp") val produtoIdErp: String? = null,
    val preco: String? = null,
    @SerialName("prazo_medio") val prazoMedio: String? = null,
    val agrupamento: String? = null,
    @SerialName("lista_grade") val listaGrade: List<GradeBreakDto>? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class GradeBreakDto(
    @SerialName("complemento_1") val complemento1: String? = null,
    @SerialName("complemento_1_id") val complemento1Id: Long? = null,
    @SerialName("complemento_1_descricao") val complemento1Descricao: String? = null,
    @SerialName("complemento_3") val complemento3: String? = null,
    @SerialName("complemento_3_id") val complemento3Id: Long? = null,
    @SerialName("complemento_3_descricao") val complemento3Descricao: String? = null,
    @SerialName("complemento_2") val complemento2: List<GradeBreakComplemento2Dto>? = null,
)

@Serializable
data class GradeBreakComplemento2Dto(
    @SerialName("complemento_2_id") val complemento2Id: Long? = null,
    @SerialName("complemento_2_id_erp") val complemento2IdErp: String? = null,
    @SerialName("complemento_2_descricao") val complemento2Descricao: String? = null,
)

@Serializable
data class PrazoDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    val parcelas: Long? = null,
    val tipo: String? = null,
    @SerialName("prazo_medio") val prazoMedio: Double? = null,
    @SerialName("dias_parcela_1") val diasParcela1: Long? = null,
    @SerialName("dias_parcela_2") val diasParcela2: Long? = null,
    @SerialName("dias_parcela_3") val diasParcela3: Long? = null,
    @SerialName("dias_parcela_4") val diasParcela4: Long? = null,
    @SerialName("dias_parcela_5") val diasParcela5: Long? = null,
    @SerialName("dias_parcela_6") val diasParcela6: Long? = null,
    val situacao: String? = null,
    @SerialName("perc_desconto") val percDesconto: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun diasParcelasCsv(): String? =
        listOfNotNull(diasParcela1, diasParcela2, diasParcela3, diasParcela4, diasParcela5, diasParcela6)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
}

@Serializable
data class SaleTypeDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    @SerialName("gera_comissao") val geraComissao: String? = null,
    @SerialName("gera_contas_receber") val geraContasReceber: String? = null,
    @SerialName("parcela_minima") val parcelaMinima: String? = null,
    @SerialName("baixa_estoque") val baixaEstoque: String? = null,
    @SerialName("origem_pedido") val origemPedido: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
