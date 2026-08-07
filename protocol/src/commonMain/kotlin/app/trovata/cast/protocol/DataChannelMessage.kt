package app.trovata.cast.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class DataChannelMessage {
    abstract val ts: Long
    abstract val from: String

    @Serializable
    @SerialName("mute")
    data class Mute(
        val muted: Boolean,
        override val ts: Long,
        override val from: String,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("navigate")
    data class Navigate(
        val view: ViewState,
        override val ts: Long,
        override val from: String,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("scroll")
    data class Scroll(
        val anchor: ScrollAnchor,
        override val ts: Long,
        override val from: String,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("pointAt")
    data class PointAt(
        val target: String,
        val xRatio: Float = 0.5f,
        val yRatio: Float = 0.5f,
        override val ts: Long,
        override val from: String,
        val durationMs: Long = 3_000,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("cartInvalidated")
    data class CartInvalidated(
        val carrinhoId: Long,
        val reason: CartChangeReason,
        override val ts: Long,
        override val from: String,
        val hint: CartChangeHint? = null,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("orderPlaced")
    data class OrderPlaced(
        val carrinhoId: Long,
        override val ts: Long,
        override val from: String,
        val pedidoId: String? = null,
    ) : DataChannelMessage()
}

@Serializable
enum class CartChangeReason {
    @SerialName("itemAdded")
    ItemAdded,

    @SerialName("itemRemoved")
    ItemRemoved,

    @SerialName("quantityChanged")
    QuantityChanged,

    @SerialName("prazoChanged")
    PrazoChanged,

    @SerialName("cleared")
    Cleared,

    @SerialName("finalized")
    Finalized,
}

@Serializable
data class CartChangeHint(
    val produtoPreId: Long? = null,
    val unitsDelta: Int = 0,
    val label: String? = null,
)

val DataChannelJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun DataChannelMessage.encode(): String =
    DataChannelJson.encodeToString(DataChannelMessage.serializer(), this)

fun decodeDataChannel(raw: String): DataChannelMessage? =
    runCatching { DataChannelJson.decodeFromString(DataChannelMessage.serializer(), raw) }.getOrNull()
