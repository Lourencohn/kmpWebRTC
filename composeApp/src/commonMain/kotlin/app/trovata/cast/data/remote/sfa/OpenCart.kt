package app.trovata.cast.data.remote.sfa

enum class CartStatus(val code: String, val label: String) {
    Editing("D", "Digitando"),
    Finalized("F", "Finalizado"),
    ReadyToSend("PE", "Pronto para envio"),
    Sent("E", "Enviado"),
    Cancelled("C", "Cancelado"),
    Unknown("", "Sem situação"),
    ;

    companion object {
        fun fromCode(raw: String?): CartStatus {
            val code = raw?.trim().orEmpty()
            if (code.isEmpty()) return Unknown
            return entries.firstOrNull { it.code.isNotEmpty() && it.code.equals(code, ignoreCase = true) }
                ?: Unknown
        }
    }
}

data class OpenCart(
    val carrinhoId: Long,
    val catalogoLinkId: Long?,
    val catalogoUuid: String?,
    val catalogName: String?,
    val catalogActive: Boolean,
    val catalogExpiresAtMs: Long?,
    val clientName: String?,
    val clientEmail: String?,
    val status: CartStatus,
    val itemCount: Int,
    val totalQuantity: Int,
    val totalCents: Long?,
    val updatedAtMs: Long?,
    val clienteId: Long? = null,
    val clientPhone: String? = null,
) {
    val hasItems: Boolean get() = itemCount > 0

    val title: String
        get() = clientName?.takeIf { it.isNotBlank() }
            ?: clientEmail?.takeIf { it.isNotBlank() }
            ?: "Carrinho $carrinhoId"

    fun isCatalogExpiredAt(nowMs: Long): Boolean {
        val expiresAtMs = catalogExpiresAtMs ?: return false
        return expiresAtMs < nowMs
    }

    fun callBlockReason(nowMs: Long): String? = when {
        catalogoUuid == null -> "Esse carrinho não tem catálogo link associado."
        clientEmail.isNullOrBlank() -> "Esse carrinho não tem e-mail de cliente. Sem ele a chamada não abre o carrinho."
        !catalogActive -> "O catálogo desse carrinho está inativo."
        isCatalogExpiredAt(nowMs) -> "O catálogo desse carrinho está expirado. Renove a validade no Catálogo Link."
        status != CartStatus.Editing ->
            "Esse carrinho está ${status.label.lowercase()}. A chamada abriria um carrinho novo, não esse."
        else -> null
    }

    fun canCallLive(nowMs: Long): Boolean = callBlockReason(nowMs) == null
}

enum class OpenCartsOrder(val label: String, val sort: String, val direction: String) {
    MostRecent("Mais recentes", "updated_at", "desc"),
    LongestIdle("Parados há mais tempo", "updated_at", "asc"),
}
