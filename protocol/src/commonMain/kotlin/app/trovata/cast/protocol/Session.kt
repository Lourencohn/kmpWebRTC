package app.trovata.cast.protocol

import kotlinx.serialization.Serializable

@Serializable
data class SessionProductSize(
    val sizeId: Long,
    val label: String,
    val available: Int? = null,
)

@Serializable
data class SessionProduct(
    val productId: Long,
    val ref: String,
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val priceCents: Long,
    val available: Int? = null,
    val sizes: List<SessionProductSize> = emptyList(),
)

@Serializable
data class SessionCreateRequest(
    val sellerId: String,
    val sellerName: String,
    val collectionLabel: String,
    val productSkus: List<String>,
    val products: List<SessionProduct> = emptyList(),
    val clientName: String? = null,
    val clientShop: String? = null,
    val scheduledFor: String? = null,
)

@Serializable
data class SessionCreateResponse(
    val sessionId: String,
    val token: String,
    val url: String,
    val expiresAtMs: Long,
)

@Serializable
data class SessionInfo(
    val token: String,
    val sellerName: String,
    val collectionLabel: String,
    val clientName: String?,
    val clientShop: String?,
    val scheduledFor: String?,
    val productCount: Int,
    val products: List<SessionProduct> = emptyList(),
    val createdAtMs: Long,
    val expiresAtMs: Long,
)

@Serializable
data class ErrorResponse(val code: String, val message: String)
