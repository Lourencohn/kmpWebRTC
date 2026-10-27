package app.trovata.cast.data.local

enum class InterestSource { Client, Seller }

sealed interface CallInterestEvent {
    val produtoPreId: Long

    data class Focused(
        override val produtoPreId: Long,
        val source: InterestSource,
        val atMs: Long,
    ) : CallInterestEvent

    data class Drafted(
        override val produtoPreId: Long,
        val units: Int,
        val atMs: Long,
    ) : CallInterestEvent

    data class Named(
        override val produtoPreId: Long,
        val ref: String?,
        val name: String?,
        val imageUrl: String?,
    ) : CallInterestEvent
}

data class CartProduct(
    val produtoPreId: Long,
    val ref: String?,
    val name: String?,
    val imageUrl: String?,
    val units: Int,
)

data class CallInterest(
    val produtoPreId: Long,
    val clientOpens: Int,
    val sellerShows: Int,
    val draftUnits: Int,
    val cartUnits: Int,
    val ref: String?,
    val name: String?,
    val imageUrl: String?,
    val firstAtMs: Long,
    val lastAtMs: Long,
) {
    val inCart: Boolean get() = cartUnits > 0

    val leftOut: Boolean get() = !inCart && (clientOpens > 0 || draftUnits > 0 || sellerShows > 0)

    val isNamed: Boolean get() = name != null

    val label: String get() = name ?: ref?.let { "Ref. $it" } ?: "Produto $produtoPreId"
}

fun List<CallInterest>.leftOut(): List<CallInterest> =
    filter { it.leftOut }.sortedWith(
        compareByDescending<CallInterest> { it.clientOpens > 0 || it.draftUnits > 0 }
            .thenByDescending { it.draftUnits }
            .thenByDescending { it.clientOpens }
            .thenBy { it.firstAtMs },
    )
