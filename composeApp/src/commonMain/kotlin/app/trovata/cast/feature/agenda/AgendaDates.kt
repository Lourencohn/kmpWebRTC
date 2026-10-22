package app.trovata.cast.feature.agenda

import app.trovata.cast.ui.format.monthLong
import app.trovata.cast.ui.format.monthShort
import app.trovata.cast.ui.format.weekdayLong
import app.trovata.cast.ui.format.weekdayShort
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

const val MinuteMs: Long = 60_000
const val DayMs: Long = 24 * 60 * MinuteMs

fun LocalDate.startOfWeek(): LocalDate = minus(DatePeriod(days = dayOfWeek.isoDayNumber - 1))

fun weekOf(date: LocalDate): List<LocalDate> {
    val monday = date.startOfWeek()
    return (0 until 7).map { monday.plus(DatePeriod(days = it)) }
}

fun monthGridOf(date: LocalDate): List<LocalDate> {
    val first = LocalDate(date.year, date.month, 1)
    val last = first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    val gridStart = first.startOfWeek()
    val gridEnd = last.startOfWeek().plus(DatePeriod(days = 6))
    val days = gridEnd.toEpochDays() - gridStart.toEpochDays() + 1
    return (0 until days).map { gridStart.plus(DatePeriod(days = it)) }
}

fun LocalDate.atMinuteOfDay(minuteOfDay: Int, timeZone: TimeZone): Long =
    LocalDateTime(this, LocalTime(minuteOfDay / 60, minuteOfDay % 60))
        .toInstant(timeZone)
        .toEpochMilliseconds()

fun localDateTimeAt(epochMs: Long, timeZone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone)

fun localDateAt(epochMs: Long, timeZone: TimeZone): LocalDate = localDateTimeAt(epochMs, timeZone).date

fun minuteOfDayAt(epochMs: Long, timeZone: TimeZone): Int {
    val dateTime = localDateTimeAt(epochMs, timeZone)
    return dateTime.hour * 60 + dateTime.minute
}

fun nextBusinessDay(after: LocalDate): LocalDate {
    var candidate = after.plus(DatePeriod(days = 1))
    while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
        candidate = candidate.plus(DatePeriod(days = 1))
    }
    return candidate
}

fun relativeDayLabel(date: LocalDate, today: LocalDate): String = when (date.toEpochDays() - today.toEpochDays()) {
    0 -> "Hoje"
    1 -> "Amanhã"
    -1 -> "Ontem"
    else -> "${weekdayShort(date.dayOfWeek).replaceFirstChar { it.uppercase() }}, ${date.dayOfMonth} ${monthShort(date.month)}"
}

fun longDayLabel(date: LocalDate, today: LocalDate): String {
    val base = "${weekdayLong(date.dayOfWeek).replaceFirstChar { it.uppercase() }}, ${date.dayOfMonth} de ${monthLong(date.month)}"
    return when (date.toEpochDays() - today.toEpochDays()) {
        0 -> "Hoje · $base"
        1 -> "Amanhã · $base"
        else -> base
    }
}

fun weekRangeLabel(week: List<LocalDate>): String {
    val first = week.first()
    val last = week.last()
    return if (first.month == last.month) {
        "${first.dayOfMonth} a ${last.dayOfMonth} de ${monthShort(last.month)}"
    } else {
        "${first.dayOfMonth} ${monthShort(first.month)} a ${last.dayOfMonth} ${monthShort(last.month)}"
    }
}

fun dayMonthLabel(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}"

fun monthLabel(date: LocalDate): String =
    "${monthLong(date.month).replaceFirstChar { it.uppercase() }} ${date.year}"
