package app.trovata.cast.feature.call

data class CallSpec(
    val token: String,
    val sessionId: String,
    val clientName: String?,
    val sellerName: String,
    val collectionLabel: String,
    val clientShop: String? = null,
    val priceTableId: Long? = null,
    val sellerPeerId: String,
)

fun newSellerPeerId(): String =
    "seller-" + (1..6).map { ('a'..'z').random() }.joinToString("")
