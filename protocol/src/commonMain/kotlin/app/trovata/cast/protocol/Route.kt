package app.trovata.cast.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    @SerialName("catalog")
    data class Catalog(val collectionId: String) : Route()

    @Serializable
    @SerialName("product")
    data class Product(val sku: String) : Route()
}
