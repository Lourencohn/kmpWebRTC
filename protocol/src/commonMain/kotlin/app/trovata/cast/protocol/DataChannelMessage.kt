package app.trovata.cast.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class DataChannelMessage {

    @Serializable
    @SerialName("mute")
    data class Mute(
        val muted: Boolean,
        val from: String,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("scroll")
    data class Scroll(
        val productId: String,
        val offset: Float,
        val ts: Long,
        val from: String,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("pointAt")
    data class PointAt(
        val productId: String,
        val ts: Long,
        val from: String,
        val durationMs: Long = 3_000,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("navigate")
    data class Navigate(
        val productId: String,
        val ts: Long,
        val from: String,
    ) : DataChannelMessage()

    @Serializable
    @SerialName("cartUpdate")
    data class CartUpdate(
        val productId: String,
        val size: String,
        val units: Int,
        val ts: Long,
        val from: String,
    ) : DataChannelMessage()
}

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
