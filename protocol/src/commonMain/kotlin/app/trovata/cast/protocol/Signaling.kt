package app.trovata.cast.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class PeerRole { Seller, Buyer }

@Serializable
data class PeerSummary(
    val peerId: String,
    val role: PeerRole,
    val displayName: String? = null,
)

@Serializable
sealed class SignalingMessage {

    @Serializable
    @SerialName("hello")
    data class Hello(
        val role: PeerRole,
        val peerId: String,
        val displayName: String? = null,
    ) : SignalingMessage()

    @Serializable
    @SerialName("roomState")
    data class RoomState(
        val peers: List<PeerSummary>,
        val youAre: PeerSummary,
    ) : SignalingMessage()

    @Serializable
    @SerialName("peerJoined")
    data class PeerJoined(val peer: PeerSummary) : SignalingMessage()

    @Serializable
    @SerialName("peerLeft")
    data class PeerLeft(val peerId: String, val reason: String? = null) : SignalingMessage()

    @Serializable
    @SerialName("offer")
    data class Offer(
        val sdp: String,
        val from: String,
        val to: String? = null,
    ) : SignalingMessage()

    @Serializable
    @SerialName("answer")
    data class Answer(
        val sdp: String,
        val from: String,
        val to: String? = null,
    ) : SignalingMessage()

    @Serializable
    @SerialName("ice")
    data class IceCandidate(
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val candidate: String,
        val from: String,
        val to: String? = null,
    ) : SignalingMessage()

    @Serializable
    @SerialName("bye")
    data class Bye(val from: String, val reason: String? = null) : SignalingMessage()

    @Serializable
    @SerialName("presencePing")
    data class PresencePing(val from: String, val sentAtMs: Long) : SignalingMessage()

    @Serializable
    @SerialName("error")
    data class ProtocolError(val code: String, val message: String) : SignalingMessage()
}

val SignalingJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun SignalingMessage.encode(): String =
    SignalingJson.encodeToString(SignalingMessage.serializer(), this)

fun decodeSignaling(raw: String): SignalingMessage =
    SignalingJson.decodeFromString(SignalingMessage.serializer(), raw)
