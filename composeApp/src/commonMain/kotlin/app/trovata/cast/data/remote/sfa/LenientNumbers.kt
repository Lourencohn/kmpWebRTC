package app.trovata.cast.data.remote.sfa

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToLong

private fun Decoder.readPrimitive(): JsonPrimitive? {
    val json = this as? JsonDecoder ?: return null
    val element = json.decodeJsonElement()
    if (element is JsonNull) return null
    return element as? JsonPrimitive
}

object LenientCentsSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientCents", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long? {
        val primitive = decoder.readPrimitive() ?: return null
        primitive.doubleOrNull?.let { return (it * 100).roundToLong() }
        return SfaParse.parseCents(primitive.content)
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value / 100.0)
    }
}

object LenientDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Double? {
        val primitive = decoder.readPrimitive() ?: return null
        primitive.doubleOrNull?.let { return it }
        val normalized = primitive.content.trim().replace(".", "").replace(",", ".")
        return normalized.toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}
