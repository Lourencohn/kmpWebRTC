package app.trovata.cast.protocol

import kotlinx.serialization.Serializable

@Serializable
data class OrderLine(
    val productId: String,
    val size: String,
    val units: Int,
    val unitPriceCents: Long,
) {
    val subtotalCents: Long get() = unitPriceCents * units
}
