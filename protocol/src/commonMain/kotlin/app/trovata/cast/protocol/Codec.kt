package app.trovata.cast.protocol

import kotlinx.serialization.json.Json

val ProtocolJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun SessionEvent.encode(): String = ProtocolJson.encodeToString(SessionEvent.serializer(), this)

fun decodeSessionEvent(raw: String): SessionEvent =
    ProtocolJson.decodeFromString(SessionEvent.serializer(), raw)
