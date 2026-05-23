package app.trovata.cast.protocol

import kotlinx.serialization.Serializable

@Serializable
data class OrderSubmissionRequest(
    val orderId: String,
    val sessionToken: String,
    val tsMs: Long,
    val totalCents: Long,
    val lines: List<OrderLine>,
    val source: OrderSource,
    val clientName: String? = null,
)

@Serializable
data class OrderSubmissionResponse(
    val orderId: String,
    val receivedAtMs: Long,
)

@Serializable
data class OrderRecord(
    val orderId: String,
    val sessionToken: String,
    val tsMs: Long,
    val receivedAtMs: Long,
    val totalCents: Long,
    val lines: List<OrderLine>,
    val source: OrderSource,
    val clientName: String?,
)

enum class OrderSource { Buyer, Seller }
