package app.trovata.cast.protocol

import kotlinx.serialization.Serializable

@Serializable
data class SessionCreateRequest(
    val sellerId: String,
    val sellerName: String,
    val collectionLabel: String,
    val productSkus: List<String>,
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
    val createdAtMs: Long,
    val expiresAtMs: Long,
)

@Serializable
data class ErrorResponse(val code: String, val message: String)
