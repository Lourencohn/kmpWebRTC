package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.ui.format.hourMinuteAt
import kotlinx.datetime.TimeZone

fun appointmentReminderMessage(
    appointment: Appointment,
    sellerName: String,
    nowMs: Long,
    timeZone: TimeZone,
): String = buildString {
    append("Oi, ${firstNameOf(appointment.clientName)}!")
    firstNameOf(sellerName).takeIf { it.isNotEmpty() }?.let { append(" Aqui é $it.") }
    if (appointment.status != AppointmentStatus.Scheduled || appointment.endsAtMs <= nowMs) return@buildString
    append(" Combinamos de conversar ")
    append(dayReferenceOf(appointment.startsAtMs, nowMs, timeZone))
    append(" às ")
    append(hourMinuteAt(appointment.startsAtMs, timeZone))
    append(". Na hora eu te mando o link do catálogo, é só abrir no celular.")
}

private fun firstNameOf(name: String): String = name.trim().split(' ').first()

private fun dayReferenceOf(epochMs: Long, nowMs: Long, timeZone: TimeZone): String {
    val day = localDateAt(epochMs, timeZone)
    return when (day.toEpochDays() - localDateAt(nowMs, timeZone).toEpochDays()) {
        0 -> "hoje"
        1 -> "amanhã"
        else -> "no dia ${day.dayOfMonth.toString().padStart(2, '0')}/${day.monthNumber.toString().padStart(2, '0')}"
    }
}
