package app.trovata.cast.data.remote.sfa.dto

import app.trovata.cast.data.remote.sfa.LenientCentsSerializer
import app.trovata.cast.data.remote.sfa.LenientDoubleSerializer
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

@Serializable
data class VitrineProdutoDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    @SerialName("produto_pre_1_id") val produtoPre1Id: Long? = null,
    @SerialName("complemento_1_id") val complemento1Id: Long? = null,
    @SerialName("categoria_id") val categoriaId: Long? = null,
    val descricao: String? = null,
    val apelido: String? = null,
    @SerialName("descricao_categoria") val descricaoCategoria: String? = null,
    @SerialName("descricao_complemento_1") val descricaoComplemento1: String? = null,
    @Serializable(with = LenientCentsSerializer::class)
    @SerialName("preco_final") val precoFinalCents: Long? = null,
    @Serializable(with = LenientCentsSerializer::class)
    @SerialName("preco_de") val precoDeCents: Long? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("percentual_desconto") val percentualDesconto: Double? = null,
    @SerialName("teve_desconto") val teveDesconto: Boolean? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("saldo_disponivel") val saldoDisponivel: Double? = null,
    @Serializable(with = LenientDoubleSerializer::class)
    @SerialName("total_quantidade") val totalQuantidade: Double? = null,
    @SerialName("is_carrinho") val isCarrinho: Boolean = false,
    @SerialName("is_favorito") val isFavorito: Boolean = false,
    @SerialName("lista_multiplo_venda") val listaMultiploVenda: String? = null,
    @SerialName("exibe_produto_indisponivel") val exibeProdutoIndisponivel: Boolean? = null,
    val sequencia: Long? = null,
    val arquivos: VitrineArquivoDto? = null,
)

@Serializable
data class VitrinePaginadaDto(
    val data: List<VitrineProdutoDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    val total: Int = 0,
    @SerialName("last_page") val lastPage: Int = 1,
)
