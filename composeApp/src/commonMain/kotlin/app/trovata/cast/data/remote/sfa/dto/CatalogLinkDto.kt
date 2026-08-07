package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogoLinkVendedorDto(
    val id: Long? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
)

@Serializable
data class CatalogoLinkClienteDto(
    val id: Long? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
)

@Serializable
data class CatalogoLinkDto(
    val id: Long,
    val uuid: String,
    val descricao: String? = null,
    val situacao: String? = null,
    val expirado: Boolean? = null,
    @SerialName("data_validade") val dataValidade: String? = null,
    @SerialName("total_carrinhos") val totalCarrinhos: Int = 0,
    @SerialName("total_visualizacoes") val totalVisualizacoes: Int = 0,
    @SerialName("vendedor_id") val vendedorId: Long? = null,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
    @SerialName("e_mail") val eMail: String? = null,
    @SerialName("celular_whatsapp") val celularWhatsapp: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val vendedor: CatalogoLinkVendedorDto? = null,
    val clientes: List<CatalogoLinkClienteDto> = emptyList(),
)
