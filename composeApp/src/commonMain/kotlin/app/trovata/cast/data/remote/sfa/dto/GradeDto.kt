package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradeComplementoDto(
    val id: Long? = null,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
)
