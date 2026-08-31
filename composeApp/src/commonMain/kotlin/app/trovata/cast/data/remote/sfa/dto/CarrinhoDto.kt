package app.trovata.cast.data.remote.sfa.dto

import app.trovata.cast.data.remote.sfa.LenientDoubleSerializer
import kotlin.math.roundToLong
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarrinhoLoginRequest(
    @SerialName("e_mail") val eMail: String,
    @SerialName("cpf_cnpj") val cpfCnpj: String? = null,
    val telefone: String? = null,
    @SerialName("nome_cliente") val nomeCliente: String? = null,
)

@Serializable
data class CarrinhoDto(
    val id: Long,
    @SerialName("e_mail") val eMail: String? = null,
    @SerialName("cpf_cnpj") val cpfCnpj: String? = null,
    val descricao: String? = null,
    val situacao: String? = null,
    @SerialName("prazo_id") val prazoId: Long? = null,
    @SerialName("cliente_id") val clienteId: Long? = null,
    @SerialName("catalogo_link_id") val catalogoLinkId: Long? = null,
    @SerialName("nome_cliente") val nomeCliente: String? = null,
    val itens: Int? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("quantidade_total") val quantidadeTotal: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("valor_total") val valorTotal: Double? = null,
)

@Serializable
data class CarrinhoEnvelope(val data: CarrinhoDto? = null)

@Serializable
data class ContextoComercialDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
)

@Serializable
data class ContextoComercialEnvelope(val data: ContextoComercialDto? = null)

@Serializable
data class CarrinhoGradeItemPayload(
    @SerialName("complemento_2_id") val complemento2Id: Long,
    val qtde: Double,
    val id: Long? = null,
)

@Serializable
data class CarrinhoItemPayload(
    @SerialName("complemento_1_id") val complemento1Id: Long,
    @SerialName("complemento_3_id") val complemento3Id: Long? = null,
    @SerialName("catalogos_carrinhos_grades_itens") val gradesItens: List<CarrinhoGradeItemPayload>,
    val id: Long? = null,
)

@Serializable
data class CarrinhoItensRequest(
    @SerialName("produto_pre_id") val produtoPreId: Long,
    @SerialName("prazo_id") val prazoId: Long,
    @SerialName("tipo_venda_id") val tipoVendaId: Long,
    @SerialName("tabela_preco_id") val tabelaPrecoId: Long,
    val items: List<CarrinhoItemPayload>,
)

@Serializable
data class PrecoDto(
    val numerador: String? = null,
    val denominador: String? = null,
    val valor: String? = null,
    val formatado: String? = null,
) {
    val cents: Long?
        get() {
            val num = numerador?.trim()?.toLongOrNull()
            val den = denominador?.trim()?.toLongOrNull()
            if (num != null && den != null && den != 0L) {
                val escalado = num * 100
                val truncado = escalado / den
                val resto = escalado % den
                return if (resto * 2 >= den) truncado + 1 else truncado
            }
            val decimal = valor?.trim()?.toDoubleOrNull() ?: return null
            return (decimal * 100).roundToLong()
        }
}

@Serializable
data class CarrinhoItemValoresDto(
    val unitario: PrecoDto? = null,
    val total: PrecoDto? = null,
)

@Serializable
data class CarrinhoItemQuantidadesDto(
    @Serializable(with = LenientDoubleSerializer::class)
    val total: Double? = null,
)

@Serializable
data class CarrinhoItemProdutoDto(
    val id: Long? = null,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    val apelido: String? = null,
)

@Serializable
data class CarrinhoItemVariacaoDto(
    @SerialName("complemento_1") val complemento1: GradeComplementoDto? = null,
    @SerialName("complemento_3") val complemento3: GradeComplementoDto? = null,
)

@Serializable
data class CarrinhoItemGradeSelecaoDto(
    val id: Long? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    val quantidade: Double? = null,
)

@Serializable
data class CarrinhoItemGradeDto(
    @SerialName("complemento_2") val complemento2: GradeComplementoDto? = null,
    @SerialName("carrinho_item_grade") val carrinhoItemGrade: CarrinhoItemGradeSelecaoDto? = null,
    val selecao: CarrinhoItemGradeSelecaoDto? = null,
)

@Serializable
data class CarrinhoItemDto(
    val id: Long,
    @SerialName("produto_pre_id") val produtoPreId: Long? = null,
    @SerialName("produto_pre_1_id") val produtoPre1Id: Long? = null,
    val quantidades: CarrinhoItemQuantidadesDto? = null,
    val valores: CarrinhoItemValoresDto? = null,
    val produto: CarrinhoItemProdutoDto? = null,
    val arquivo: VitrineArquivoDto? = null,
    val variacao: CarrinhoItemVariacaoDto? = null,
    val grades: List<CarrinhoItemGradeDto> = emptyList(),
)
