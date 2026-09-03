package app.trovata.cast.feature.carts

enum class CartSituacao(val code: String, val label: String) {
    Digitando("D", "Digitando"),
    Finalizado("F", "Finalizado"),
    ProntoParaEnvio("PE", "Pronto para envio"),
    Enviado("E", "Enviado"),
    Cancelado("C", "Cancelado"),
    Desconhecida("", "Sem situação"),
    ;

    companion object {
        fun fromCode(raw: String?): CartSituacao {
            val code = raw?.trim().orEmpty()
            if (code.isEmpty()) return Desconhecida
            return entries.firstOrNull { it.code.isNotEmpty() && it.code.equals(code, ignoreCase = true) }
                ?: Desconhecida
        }
    }
}

data class OpenCart(
    val carrinhoId: Long,
    val catalogoLinkId: Long?,
    val catalogoUuid: String?,
    val catalogoNome: String?,
    val catalogoAtivo: Boolean,
    val catalogoValidadeMs: Long?,
    val clienteNome: String?,
    val clienteEmail: String?,
    val situacao: CartSituacao,
    val itens: Int,
    val quantidadeTotal: Int,
    val valorTotalCents: Long?,
    val atualizadoEmMs: Long?,
) {
    val temItens: Boolean get() = itens > 0

    val titulo: String
        get() = clienteNome?.takeIf { it.isNotBlank() }
            ?: clienteEmail?.takeIf { it.isNotBlank() }
            ?: "Carrinho $carrinhoId"

    fun catalogoExpiradoEm(nowMs: Long): Boolean {
        val validade = catalogoValidadeMs ?: return false
        return validade < nowMs
    }

    fun impedimentoParaChamar(nowMs: Long): String? = when {
        catalogoUuid == null -> "Esse carrinho não tem catálogo link associado."
        clienteEmail.isNullOrBlank() -> "Esse carrinho não tem e-mail de cliente. Sem ele a chamada não abre o carrinho."
        !catalogoAtivo -> "O catálogo desse carrinho está inativo."
        catalogoExpiradoEm(nowMs) -> "O catálogo desse carrinho está expirado. Renove a validade no Catálogo Link."
        situacao != CartSituacao.Digitando ->
            "Esse carrinho está ${situacao.label.lowercase()}. A chamada abriria um carrinho novo, não esse."
        else -> null
    }

    fun podeChamarAoVivo(nowMs: Long): Boolean = impedimentoParaChamar(nowMs) == null
}

enum class OpenCartsOrder(val label: String, val sort: String, val direction: String) {
    MaisRecentes("Mais recentes", "updated_at", "desc"),
    ParadosHaMaisTempo("Parados há mais tempo", "updated_at", "asc"),
}

fun tempoParadoLabel(atualizadoEmMs: Long?, nowMs: Long): String {
    if (atualizadoEmMs == null) return "sem data de alteração"
    val decorridoMs = nowMs - atualizadoEmMs
    if (decorridoMs < 0) return "mexeu agora"
    val minutos = decorridoMs / 60_000
    if (minutos < 1) return "mexeu agora"
    if (minutos < 60) return "sem mexer há $minutos min"
    val horas = minutos / 60
    if (horas < 24) return "sem mexer há ${horas}h"
    val dias = horas / 24
    if (dias < 30) return if (dias == 1L) "sem mexer há 1 dia" else "sem mexer há $dias dias"
    val meses = dias / 30
    return if (meses == 1L) "sem mexer há 1 mês" else "sem mexer há $meses meses"
}
