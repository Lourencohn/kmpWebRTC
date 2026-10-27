package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProdutoVariacaoDto(
    val arquivos: List<VitrineArquivoDto> = emptyList(),
)

@Serializable
data class ProdutoComGradeDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    val apelido: String? = null,
    val variacao: ProdutoVariacaoDto? = null,
    val variacoes: List<ProdutoVariacaoDto> = emptyList(),
)
