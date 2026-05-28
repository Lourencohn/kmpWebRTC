package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    @SerialName("vendedor_id_erp") val vendedorIdErp: String? = null,
    @SerialName("cidade_id_erp") val cidadeIdErp: String? = null,
    val situacao: String? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
    @SerialName("cpf_cnpj") val cpfCnpj: String? = null,
    val endereco: String? = null,
    val bairro: String? = null,
    val cep: String? = null,
    val telefone: String? = null,
    val celular: String? = null,
    @SerialName("e_mail") val email: String? = null,
    val contato: String? = null,
    @SerialName("tabela_preco_id") val tabelaPrecoId: Long? = null,
    @SerialName("prazo_id") val prazoId: Long? = null,
    @SerialName("perc_desconto") val percDesconto: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class SellerDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
    val email: String? = null,
    val telefone: String? = null,
    val situacao: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SellerClientDto(
    val id: Long,
    @SerialName("vendedor_id_erp") val vendedorIdErp: String? = null,
    @SerialName("cliente_id_erp") val clienteIdErp: String? = null,
    @SerialName("vendedor_id") val vendedorId: Long? = null,
    @SerialName("cliente_id") val clienteId: Long? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
