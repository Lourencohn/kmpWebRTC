package app.trovata.cast.feature.catalog

data class SellerCatalogLink(
    val id: Long,
    val uuid: String,
    val nome: String,
    val clienteNome: String?,
    val clienteEmail: String?,
    val vendedorNome: String?,
    val ativo: Boolean,
    val expirado: Boolean,
    val validadeLabel: String?,
    val totalCarrinhos: Int,
    val totalVisualizacoes: Int,
) {
    val disponivel: Boolean get() = ativo && !expirado
}
