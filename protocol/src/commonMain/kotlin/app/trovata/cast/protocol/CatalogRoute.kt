package app.trovata.cast.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class CatalogRoute {
    @Serializable
    @SerialName("inicio")
    data object Inicio : CatalogRoute()

    @Serializable
    @SerialName("menu")
    data object Menu : CatalogRoute()

    @Serializable
    @SerialName("todos")
    data object Todos : CatalogRoute()

    @Serializable
    @SerialName("secao")
    data class Secao(val tabela: String, val tabelaId: String) : CatalogRoute()

    @Serializable
    @SerialName("carrinho")
    data object Carrinho : CatalogRoute()

    @Serializable
    @SerialName("favoritos")
    data object Favoritos : CatalogRoute()
}

@Serializable
data class ProductFocus(
    val produtoPreId: Long,
    val produtoPre1Id: Long? = null,
    val complemento1Id: Long? = null,
)

@Serializable
data class ScrollAnchor(
    val page: Int = 1,
    val produtoPreId: Long? = null,
    val itemOffsetRatio: Float = 0f,
    val viewportRatio: Float = 0f,
)

@Serializable
data class ViewState(
    val route: CatalogRoute,
    val query: Map<String, String> = emptyMap(),
    val focus: ProductFocus? = null,
)

val SyncedQueryKeys: Set<String> = setOf(
    "categoria",
    "grupo_produto",
    "subgrupo_produto",
    "marca",
    "search",
    "page",
    "total",
    "sort",
    "direction",
)

fun Map<String, String>.retainSyncedQuery(): Map<String, String> =
    filterKeys { it in SyncedQueryKeys }

fun CatalogRoute.pathSegments(): List<String> = when (this) {
    CatalogRoute.Inicio -> emptyList()
    CatalogRoute.Menu -> listOf("menu")
    CatalogRoute.Todos -> listOf("todos")
    is CatalogRoute.Secao -> listOf(tabela, tabelaId)
    CatalogRoute.Carrinho -> listOf("carrinho")
    CatalogRoute.Favoritos -> listOf("favoritos")
}
