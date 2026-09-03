package app.trovata.cast.data.remote.sfa.dto

import app.trovata.cast.data.remote.sfa.LenientDoubleSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarrinhoListagemClienteDto(
    val id: Long? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
    @SerialName("e_mail") val eMail: String? = null,
    val telefone: String? = null,
    val celular: String? = null,
)

@Serializable
data class CarrinhoListagemCatalogoLinkDto(
    val id: Long,
    val uuid: String,
    val descricao: String? = null,
    val situacao: String? = null,
    @SerialName("e_mail") val eMail: String? = null,
    @SerialName("data_validade") val dataValidade: String? = null,
)

@Serializable
data class CarrinhoListagemDto(
    val id: Long,
    @SerialName("e_mail") val eMail: String? = null,
    val descricao: String? = null,
    @SerialName("nome_cliente") val nomeCliente: String? = null,
    @SerialName("cliente_id") val clienteId: Long? = null,
    @SerialName("catalogo_link_id") val catalogoLinkId: Long? = null,
    val situacao: String? = null,
    val itens: Int? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("quantidade_total") val quantidadeTotal: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("valor_total") val valorTotal: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("desconto_valor") val descontoValor: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val cliente: CarrinhoListagemClienteDto? = null,
    @SerialName("catalogo_link") val catalogoLink: CarrinhoListagemCatalogoLinkDto? = null,
)
