package app.trovata.cast.feature.aftercall

import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.ui.format.formatBrl

private const val NamedProductsLimit = 3

fun cartSummaryUrl(catalogWebUrl: String, empresaSlug: String, catalogoUuid: String, carrinhoId: Long): String =
    "${catalogWebUrl.trimEnd('/')}/catalogo-link-resume/$empresaSlug/$catalogoUuid/carrinho/$carrinhoId"

fun orderSummaryMessage(clientName: String?, order: StoredOrder, summaryUrl: String?): String = buildString {
    append(greetingFor(clientName))
    append(" Seu pedido ficou pronto: ")
    append(order.lines.sumOf { it.units })
    append(" unidades, total de ")
    append(formatBrl(order.totalCents))
    append('.')
    if (summaryUrl != null) {
        append(" O resumo completo está aqui: ")
        append(summaryUrl)
    }
}

fun leftOutMessage(clientName: String?, leftOut: List<CallInterest>): String = buildString {
    append(greetingFor(clientName))
    append(" Obrigado pela conversa.")
    if (leftOut.isEmpty()) {
        append(" Qualquer coisa, é só me chamar.")
        return@buildString
    }
    append(" Anotei os produtos que você olhou: ")
    append(namesOf(leftOut))
    append(". Se quiser incluir algum no pedido, é só me falar.")
}

private fun greetingFor(clientName: String?): String {
    val firstName = clientName?.trim()?.split(' ')?.firstOrNull()?.takeIf { it.isNotEmpty() }
    return if (firstName != null) "Oi, $firstName!" else "Olá!"
}

private fun namesOf(interests: List<CallInterest>): String {
    val names = interests.take(NamedProductsLimit).map { it.label }
    val remaining = interests.size - names.size
    return when {
        remaining > 0 -> names.joinToString(", ") + " e mais $remaining"
        names.size == 1 -> names.single()
        else -> names.dropLast(1).joinToString(", ") + " e " + names.last()
    }
}
