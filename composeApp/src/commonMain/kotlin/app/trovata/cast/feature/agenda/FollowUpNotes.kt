package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.leftOut

private const val NotedProductsLimit = 6

fun leftOutNotes(interests: List<CallInterest>): String? {
    val leftOut = interests.leftOut()
    if (leftOut.isEmpty()) return null
    val lines = leftOut.take(NotedProductsLimit).map { "- ${noteLineOf(it)}" }
    val remaining = leftOut.size - NotedProductsLimit
    return buildString {
        append("Ficou de fora do pedido na última chamada:")
        lines.forEach { append('\n').append(it) }
        if (remaining > 0) append("\n- e mais $remaining")
    }
}

fun interestDetailOf(interest: CallInterest): String = when {
    interest.draftUnits > 0 -> "chegou a digitar ${interest.draftUnits} un"
    interest.clientOpens > 1 -> "abriu ${interest.clientOpens} vezes"
    interest.clientOpens == 1 -> "o cliente abriu"
    else -> "você mostrou"
}

private fun noteLineOf(interest: CallInterest): String =
    listOfNotNull(
        interest.label,
        interest.ref?.takeIf { interest.isNamed }?.let { "ref. $it" },
        interestDetailOf(interest),
    ).joinToString(" · ")
