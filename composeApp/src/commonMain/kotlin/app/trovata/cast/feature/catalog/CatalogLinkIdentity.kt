package app.trovata.cast.feature.catalog

data class CatalogLinkIdentity(
    val empresaSlug: String,
    val catalogoUuid: String,
    val nome: String? = null,
) {
    val isResolved: Boolean get() = empresaSlug.isNotBlank() && catalogoUuid.isNotBlank()
}
