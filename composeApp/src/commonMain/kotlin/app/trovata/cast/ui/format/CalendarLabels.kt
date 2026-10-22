package app.trovata.cast.ui.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun weekdayShort(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "seg"
    DayOfWeek.TUESDAY -> "ter"
    DayOfWeek.WEDNESDAY -> "qua"
    DayOfWeek.THURSDAY -> "qui"
    DayOfWeek.FRIDAY -> "sex"
    DayOfWeek.SATURDAY -> "sáb"
    else -> "dom"
}

fun weekdayLong(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "segunda"
    DayOfWeek.TUESDAY -> "terça"
    DayOfWeek.WEDNESDAY -> "quarta"
    DayOfWeek.THURSDAY -> "quinta"
    DayOfWeek.FRIDAY -> "sexta"
    DayOfWeek.SATURDAY -> "sábado"
    else -> "domingo"
}

fun monthShort(month: Month): String = monthLong(month).take(3)

fun monthLong(month: Month): String = when (month) {
    Month.JANUARY -> "janeiro"
    Month.FEBRUARY -> "fevereiro"
    Month.MARCH -> "março"
    Month.APRIL -> "abril"
    Month.MAY -> "maio"
    Month.JUNE -> "junho"
    Month.JULY -> "julho"
    Month.AUGUST -> "agosto"
    Month.SEPTEMBER -> "setembro"
    Month.OCTOBER -> "outubro"
    Month.NOVEMBER -> "novembro"
    else -> "dezembro"
}

fun hourMinute(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

fun hourMinuteAt(epochMs: Long, timeZone: TimeZone): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone)
    return hourMinute(dateTime.hour, dateTime.minute)
}

fun durationLabel(minutes: Int): String {
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "${hours}h" else "${hours}h${rest.toString().padStart(2, '0')}"
}
