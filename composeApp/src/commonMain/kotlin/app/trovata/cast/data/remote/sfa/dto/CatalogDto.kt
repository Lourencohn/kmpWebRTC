package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    val descricao: String? = null,
    @SerialName("descricao_2") val descricao2: String? = null,
    val apelido: String? = null,
    @SerialName("abreviatura_unidade") val unidade: String? = null,
    @SerialName("codigo_barras") val codigoBarras: String? = null,
    val ncm: String? = null,
    val situacao: String? = null,
    @SerialName("preco_base") val precoBase: String? = null,
    @SerialName("preco_final") val precoFinal: String? = null,
    @SerialName("lista_multiplo_venda") val listaMultiploVenda: String? = null,
    @SerialName("descricao_tipo_complemento_1") val tipoComplemento1: String? = null,
    @SerialName("descricao_tipo_complemento_2") val tipoComplemento2: String? = null,
    @SerialName("descricao_tipo_complemento_3") val tipoComplemento3: String? = null,
    @SerialName("categoria_id") val categoriaId: Long? = null,
    @SerialName("colecao_id") val colecaoId: Long? = null,
    @SerialName("marca_id") val marcaId: Long? = null,
    @SerialName("grupo_produto_id") val grupoProdutoId: Long? = null,
    @SerialName("genero_id") val generoId: Long? = null,
    @SerialName("linha_id") val linhaId: Long? = null,
    @SerialName("descricao_categoria") val descricaoCategoria: String? = null,
    @SerialName("descricao_colecao") val descricaoColecao: String? = null,
    @SerialName("descricao_marca") val descricaoMarca: String? = null,
    @SerialName("descricao_grupo_produto") val descricaoGrupoProduto: String? = null,
    @SerialName("sequencia_catalogo_link") val sequenciaCatalogoLink: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class CommercialProductDto(
    val id: Long,
    @SerialName("id_erp") val idErp: String? = null,
    @SerialName("produto_pre_id") val produtoPreId: Long? = null,
    val nivel: Long? = null,
    @SerialName("venda_liberada") val vendaLiberada: String? = null,
    @SerialName("lista_multiplo_venda") val listaMultiploVenda: String? = null,
    @SerialName("multiplo_venda") val multiploVenda: String? = null,
    @SerialName("qtde_minima_venda") val qtdeMinimaVenda: String? = null,
    @SerialName("qtde_minima_item_venda") val qtdeMinimaItemVenda: String? = null,
    @SerialName("qtde_maxima_venda") val qtdeMaximaVenda: String? = null,
    @SerialName("valor_minimo_venda") val valorMinimoVenda: String? = null,
    @SerialName("permite_sortir") val permiteSortir: String? = null,
    @SerialName("perc_desconto") val percDesconto: String? = null,
    @SerialName("data_validade_desconto") val dataValidadeDesconto: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
