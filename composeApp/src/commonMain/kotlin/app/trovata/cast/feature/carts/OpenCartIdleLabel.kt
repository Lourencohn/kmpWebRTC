package app.trovata.cast.feature.carts

fun idleTimeLabel(updatedAtMs: Long?, nowMs: Long): String {
    if (updatedAtMs == null) return "sem data de alteração"
    val decorridoMs = nowMs - updatedAtMs
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
