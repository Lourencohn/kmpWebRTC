package app.trovata.cast.data.remote.sfa

data class SellerCatalogLink(
    val id: Long,
    val uuid: String,
    val name: String,
    val clientName: String?,
    val clientEmail: String?,
    val clientDocument: String?,
    val sellerName: String?,
    val active: Boolean,
    val expired: Boolean,
    val validityLabel: String?,
    val cartCount: Int,
    val viewCount: Int,
) {
    val isAvailable: Boolean get() = active && !expired

    val hasRecipient: Boolean
        get() = !clientName.isNullOrBlank() || !clientEmail.isNullOrBlank() || !clientDocument.isNullOrBlank()

    val recipientLabel: String?
        get() = clientName?.takeIf { it.isNotBlank() } ?: clientEmail?.takeIf { it.isNotBlank() }
}
