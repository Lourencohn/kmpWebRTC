package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VitrineArquivoDto(
    @SerialName("caminho_thumb") val caminhoThumb: String? = null,
    @SerialName("caminho_detail") val caminhoDetail: String? = null,
    @SerialName("caminho_media") val caminhoMedia: String? = null,
    @SerialName("caminho_original") val caminhoOriginal: String? = null,
    val sequencia: Int? = null,
) {
    val melhorImagem: String?
        get() = caminhoThumb ?: caminhoDetail ?: caminhoMedia ?: caminhoOriginal
}
