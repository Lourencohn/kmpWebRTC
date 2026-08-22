package app.trovata.cast.data.remote.sfa.dto

import app.trovata.cast.data.remote.sfa.LenientDoubleSerializer
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
