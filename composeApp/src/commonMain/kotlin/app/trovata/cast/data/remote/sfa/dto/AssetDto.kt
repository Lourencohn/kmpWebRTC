package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val nome: String? = null,
    @SerialName("caminho_original") val caminhoOriginal: String? = null,
    @SerialName("caminho_media") val caminhoMedia: String? = null,
    @SerialName("caminho_detail") val caminhoDetail: String? = null,
    @SerialName("caminho_thumb") val caminhoThumb: String? = null,
    val tipo: String? = null,
    @SerialName("tipo_mime") val tipoMime: String? = null,
    val sequencia: Long? = null,
    val situacao: String? = null,
    @SerialName("complemento_1_id_erp") val complemento1IdErp: String? = null,
    @SerialName("produto_id_erp") val produtoIdErp: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)
