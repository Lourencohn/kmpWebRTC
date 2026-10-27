package app.trovata.cast.ui.format

import io.ktor.http.encodeURLParameter

private const val BrazilCountryCode = "55"

fun whatsAppNumberOf(raw: String?): String? {
    val digits = raw?.filter { it.isDigit() }?.trimStart('0')?.takeIf { it.isNotEmpty() } ?: return null
    val national = when {
        digits.length in 10..11 -> digits
        digits.startsWith(BrazilCountryCode) && digits.length in 12..13 -> digits.drop(BrazilCountryCode.length)
        else -> return null
    }
    return (BrazilCountryCode + national).takeIf { isPlausibleBrazilianNumber(national) }
}

fun whatsAppUrl(number: String, text: String): String =
    "https://wa.me/$number?text=${text.encodeURLParameter()}"

fun formatWhatsAppNumber(number: String): String {
    val local = number.removePrefix(BrazilCountryCode)
    val areaCode = local.take(2)
    val subscriber = local.drop(2)
    val split = subscriber.length - 4
    return "($areaCode) ${subscriber.take(split)}-${subscriber.drop(split)}"
}

private fun isPlausibleBrazilianNumber(national: String): Boolean {
    val areaCode = national.take(2)
    val subscriber = national.drop(2)
    val validAreaCode = areaCode.none { it == '0' }
    val validLine = when (subscriber.length) {
        9 -> subscriber.first() == '9'
        8 -> subscriber.first() in '2'..'9'
        else -> false
    }
    val placeholder = subscriber.drop(1).toSet().size == 1
    return validAreaCode && validLine && !placeholder
}
