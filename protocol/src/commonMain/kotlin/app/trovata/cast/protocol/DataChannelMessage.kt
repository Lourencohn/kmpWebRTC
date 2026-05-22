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
