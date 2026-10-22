package app.trovata.cast.ui.format

fun formatBrl(cents: Long): String {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val reais = abs / 100
    val centavos = (abs % 100).toString().padStart(2, '0')
    val reaisStr = reais.toString().reversed().chunked(3).joinToString(".").reversed()
    return "${if (negative) "-" else ""}R$ $reaisStr,$centavos"
}
