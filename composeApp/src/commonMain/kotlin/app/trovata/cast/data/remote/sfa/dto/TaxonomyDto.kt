package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxonomyDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    val situacao: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ComplementoDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    @SerialName("tipo_complemento_id_erp") val tipoComplementoIdErp: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class GradePadraoDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val sequencia: Long? = null,
    @SerialName("complemento_2_id") val complemento2Id: Long? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
