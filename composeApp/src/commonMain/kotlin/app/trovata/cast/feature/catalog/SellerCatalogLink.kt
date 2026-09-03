package app.trovata.cast.feature.catalog

data class SellerCatalogLink(
    val id: Long,
    val uuid: String,
    val nome: String,
    val clienteNome: String?,
    val clienteEmail: String?,
    val clienteDocumento: String?,
    val vendedorNome: String?,
    val ativo: Boolean,
    val expirado: Boolean,
    val validadeLabel: String?,
    val totalCarrinhos: Int,
    val totalVisualizacoes: Int,
) {
    val disponivel: Boolean get() = ativo && !expirado

    val temDestinatario: Boolean
        get() = !clienteNome.isNullOrBlank() || !clienteEmail.isNullOrBlank() || !clienteDocumento.isNullOrBlank()

    val destinatarioLabel: String?
        get() = clienteNome?.takeIf { it.isNotBlank() } ?: clienteEmail?.takeIf { it.isNotBlank() }
}

fun somenteDigitos(raw: String?): String? =
    raw?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
