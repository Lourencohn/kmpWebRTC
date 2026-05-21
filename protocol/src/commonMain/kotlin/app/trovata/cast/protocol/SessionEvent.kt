package app.trovata.cast.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SessionEvent {
    abstract val ts: Long
    abstract val from: String

    @Serializable
    @SerialName("cursor")
    data class Cursor(
        val x: Float,
        val y: Float,
        val productId: String?,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("scroll")
    data class Scroll(
        val productInView: String,
        val offset: Float,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("point")
    data class PointAt(
        val productId: String,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("nav")
    data class Navigate(
        val route: Route,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("cart")
    data class CartUpdate(
        val sku: String,
        val sizes: Map<String, Int>,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("reaction")
    data class Reaction(
        val productId: String,
        val kind: ReactionKind,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("offer")
    data class OfferProposed(
        val sku: String,
        val priceCents: Long,
        val validUntilMs: Long,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("draw")
    data class DrawStroke(
        val productId: String,
        val points: List<Point>,
        val color: String,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("presence")
    data class Presence(
        val state: PresenceState,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()

    @Serializable
    @SerialName("snapshot")
    data class Snapshot(
        val state: SessionState,
        override val ts: Long,
        override val from: String,
    ) : SessionEvent()
}

@Serializable
enum class ReactionKind { Love, Wow, Doubt }

@Serializable
enum class PresenceState { Joining, Active, Idle, Leaving }

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
data class SessionState(
    val route: Route,
    val cart: Map<String, Map<String, Int>> = emptyMap(),
    val pointingAt: String? = null,
)
