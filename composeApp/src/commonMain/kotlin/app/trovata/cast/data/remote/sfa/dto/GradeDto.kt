package app.trovata.cast.data.remote.sfa.dto

import app.trovata.cast.data.remote.sfa.LenientDoubleSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradeComplementoDto(
    val id: Long? = null,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
)

@Serializable
data class TamanhoGradeDto(
    @SerialName("complemento_2") val complemento2: GradeComplementoDto? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    val disponivel: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("adicionados_count") val adicionadosCount: Double? = null,
)

@Serializable
data class GradeDoComplemento3Dto(
    @SerialName("complemento_3") val complemento3: GradeComplementoDto? = null,
    val tamanhos: List<TamanhoGradeDto> = emptyList(),
)

@Serializable
data class VariacaoDto(
    @SerialName("complemento_1") val complemento1: GradeComplementoDto? = null,
    val grades: List<GradeDoComplemento3Dto> = emptyList(),
    val arquivos: List<VitrineArquivoDto> = emptyList(),
)

@Serializable
data class ProdutoComGradeDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    val apelido: String? = null,
    @SerialName("lista_multiplo_venda") val listaMultiploVenda: String? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("saldo_total_disponivel") val saldoTotalDisponivel: Double? = null,
    @SerialName("produto_indisponivel") val produtoIndisponivel: Boolean = false,
    val variacoes: List<VariacaoDto> = emptyList(),
    val variacao: VariacaoDto? = null,
)

@Serializable
data class ProdutoComGradeEnvelope(
    val data: ProdutoComGradeDto? = null,
)
