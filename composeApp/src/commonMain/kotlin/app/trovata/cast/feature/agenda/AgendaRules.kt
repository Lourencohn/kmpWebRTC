package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentOutcome
import app.trovata.cast.data.local.AppointmentPriority
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.ui.format.hourMinuteAt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

const val NowLeadMs: Long = 10 * MinuteMs
const val MinDurationMinutes: Int = 5
const val MaxDurationMinutes: Int = 240
const val FollowUpMinIdleMs: Long = DayMs
const val FollowUpMaxIdleMs: Long = 30 * DayMs
const val WorkdayStartMinute: Int = 8 * 60
const val WorkdayEndMinute: Int = 19 * 60
const val MinFreeGapMinutes: Int = 30

enum class AgendaStatus(val label: String) {
    Upcoming("Agendada"),
    Now("Agora"),
    Pending("Pendente"),
    Done("Realizada"),
    Missed("Não atendeu"),
    Cancelled("Cancelada"),
}

enum class CallResult(val label: String) {
    OrderPlaced("Pedido fechado"),
    FollowUp("Retornar depois"),
    NotInterested("Sem interesse agora"),
    NoAnswer("Não atendeu"),
}

enum class PrepTask(val label: String) {
    ReviewCart("Revisei o carrinho em aberto"),
    ReviewLastContact("Reli o último atendimento"),
    PickCatalog("Separei o que vou mostrar"),
}

fun Appointment.agendaStatus(nowMs: Long): AgendaStatus = when (status) {
    AppointmentStatus.Cancelled -> AgendaStatus.Cancelled
    AppointmentStatus.Missed -> AgendaStatus.Missed
    AppointmentStatus.Done -> AgendaStatus.Done
    AppointmentStatus.Scheduled -> when {
        linkedOrder != null -> AgendaStatus.Done
        nowMs >= endsAtMs -> AgendaStatus.Pending
        nowMs >= startsAtMs - NowLeadMs -> AgendaStatus.Now
        else -> AgendaStatus.Upcoming
    }
}

val Appointment.isOpen: Boolean get() = status == AppointmentStatus.Scheduled && linkedOrder == null

val Appointment.canCall: Boolean get() = status == AppointmentStatus.Scheduled

val Appointment.canRegisterResult: Boolean get() = status == AppointmentStatus.Scheduled

val Appointment.canReschedule: Boolean get() = isOpen

val Appointment.canCancel: Boolean get() = isOpen

val Appointment.isPriority: Boolean get() = priority == AppointmentPriority.High

fun Appointment.suggestedResult(): CallResult? = if (linkedOrder != null) CallResult.OrderPlaced else null

fun Appointment.withResult(result: CallResult, notes: String?, nextStep: String?, nowMs: Long): Appointment {
    val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
    val cleanNextStep = nextStep?.trim()?.takeIf { it.isNotEmpty() }
    return when (result) {
        CallResult.NoAnswer -> copy(
            status = AppointmentStatus.Missed,
            outcome = null,
            outcomeNotes = cleanNotes,
            nextStep = cleanNextStep,
            updatedAtMs = nowMs,
        )
        else -> copy(
            status = AppointmentStatus.Done,
            outcome = result.toOutcome(),
            outcomeNotes = cleanNotes,
            nextStep = cleanNextStep,
            updatedAtMs = nowMs,
        )
    }
}

fun Appointment.cancelled(nowMs: Long): Appointment = copy(status = AppointmentStatus.Cancelled, updatedAtMs = nowMs)

fun Appointment.withPrepToggled(task: PrepTask, nowMs: Long): Appointment {
    val key = task.name
    val next = if (key in prepDone) prepDone - key else prepDone + key
    return copy(prepDone = next, updatedAtMs = nowMs)
}

fun Appointment.isPrepDone(task: PrepTask): Boolean = task.name in prepDone

private fun CallResult.toOutcome(): AppointmentOutcome? = when (this) {
    CallResult.OrderPlaced -> AppointmentOutcome.OrderPlaced
    CallResult.FollowUp -> AppointmentOutcome.FollowUp
    CallResult.NotInterested -> AppointmentOutcome.NotInterested
    CallResult.NoAnswer -> null
}

fun AppointmentOutcome.label(): String = when (this) {
    AppointmentOutcome.OrderPlaced -> CallResult.OrderPlaced.label
    AppointmentOutcome.FollowUp -> CallResult.FollowUp.label
    AppointmentOutcome.NotInterested -> CallResult.NotInterested.label
}

fun sameClient(
    clienteId: Long?,
    email: String?,
    name: String?,
    otherClienteId: Long?,
    otherEmail: String?,
    otherName: String?,
): Boolean {
    if (clienteId != null && otherClienteId != null) return clienteId == otherClienteId
    val normalizedEmail = normalizeEmail(email)
    val otherNormalizedEmail = normalizeEmail(otherEmail)
    if (normalizedEmail != null && otherNormalizedEmail != null) return normalizedEmail == otherNormalizedEmail
    val normalizedName = normalizeName(name)
    return normalizedName != null && normalizedName == normalizeName(otherName)
}

fun Appointment.isSameClientAs(other: Appointment): Boolean =
    sameClient(clienteId, clientEmail, clientName, other.clienteId, other.clientEmail, other.clientName)

fun Appointment.isSameClientAs(cart: OpenCart): Boolean {
    if (clienteId != null && cart.clienteId != null) return clienteId == cart.clienteId
    val email = normalizeEmail(clientEmail) ?: return false
    return email == normalizeEmail(cart.clientEmail)
}

fun normalizeEmail(raw: String?): String? = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

private fun normalizeName(raw: String?): String? =
    raw?.trim()?.lowercase()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotEmpty() }

data class SlotRequest(
    val appointmentId: String?,
    val clienteId: Long?,
    val clientName: String?,
    val clientEmail: String?,
    val startsAtMs: Long?,
    val durationMinutes: Int,
    val requireFuture: Boolean,
)

sealed interface SlotProblem {
    data object MissingClient : SlotProblem
    data object MissingTime : SlotProblem
    data object InvalidDuration : SlotProblem
    data object InPast : SlotProblem
    data class SameClientSameDay(val other: Appointment) : SlotProblem
    data class Overlaps(val other: Appointment) : SlotProblem
}

fun slotProblem(
    request: SlotRequest,
    existing: List<Appointment>,
    nowMs: Long,
    timeZone: TimeZone,
): SlotProblem? {
    if (request.clientName.isNullOrBlank()) return SlotProblem.MissingClient
    val startsAtMs = request.startsAtMs ?: return SlotProblem.MissingTime
    if (request.durationMinutes !in MinDurationMinutes..MaxDurationMinutes) return SlotProblem.InvalidDuration
    if (request.requireFuture && startsAtMs < nowMs) return SlotProblem.InPast
    val endsAtMs = startsAtMs + request.durationMinutes * MinuteMs
    val day = localDateAt(startsAtMs, timeZone)
    val others = existing.filter { it.isOpen && it.appointmentId != request.appointmentId }
    others.firstOrNull { other ->
        localDateAt(other.startsAtMs, timeZone) == day &&
            sameClient(request.clienteId, request.clientEmail, request.clientName, other.clienteId, other.clientEmail, other.clientName)
    }?.let { return SlotProblem.SameClientSameDay(it) }
    others.firstOrNull { other -> startsAtMs < other.endsAtMs && other.startsAtMs < endsAtMs }
        ?.let { return SlotProblem.Overlaps(it) }
    return null
}

fun SlotProblem.message(timeZone: TimeZone): String = when (this) {
    SlotProblem.MissingClient -> "Escolha o cliente da chamada."
    SlotProblem.MissingTime -> "Escolha o horário da chamada."
    SlotProblem.InvalidDuration -> "A duração precisa ficar entre $MinDurationMinutes minutos e ${MaxDurationMinutes / 60} horas."
    SlotProblem.InPast -> "Esse horário já passou. Escolha um horário a partir de agora."
    is SlotProblem.SameClientSameDay ->
        "Você já tem chamada com ${other.clientName} nesse dia, às ${hourMinuteAt(other.startsAtMs, timeZone)}. Reagende aquela em vez de criar outra."
    is SlotProblem.Overlaps ->
        "Bate com a chamada de ${other.clientName}, das ${hourMinuteAt(other.startsAtMs, timeZone)} às ${hourMinuteAt(other.endsAtMs, timeZone)}."
}

data class AgendaSummary(
    val today: Int,
    val doneToday: Int,
    val pending: Int,
    val followUps: Int,
    val next: Appointment?,
)

fun summarize(appointments: List<Appointment>, nowMs: Long, timeZone: TimeZone): AgendaSummary {
    val today = localDateAt(nowMs, timeZone)
    val ofToday = appointments.filter {
        localDateAt(it.startsAtMs, timeZone) == today && it.status != AppointmentStatus.Cancelled
    }
    return AgendaSummary(
        today = ofToday.size,
        doneToday = ofToday.count { it.agendaStatus(nowMs) == AgendaStatus.Done },
        pending = pendingOf(appointments, nowMs).size,
        followUps = followUpsOf(appointments).size,
        next = nextOf(appointments, nowMs),
    )
}

fun pendingOf(appointments: List<Appointment>, nowMs: Long): List<Appointment> =
    appointments.filter { it.agendaStatus(nowMs) == AgendaStatus.Pending }.sortedBy { it.startsAtMs }

fun nextOf(appointments: List<Appointment>, nowMs: Long): Appointment? =
    appointments.filter { it.isOpen && it.endsAtMs > nowMs }.minByOrNull { it.startsAtMs }

fun followUpsOf(appointments: List<Appointment>): List<Appointment> =
    appointments
        .filter { it.status == AppointmentStatus.Done && it.outcome == AppointmentOutcome.FollowUp }
        .filter { done ->
            appointments.none { other ->
                other.followUpOfId == done.appointmentId ||
                    (other.isOpen && other.startsAtMs > done.startsAtMs && other.isSameClientAs(done))
            }
        }
        .sortedBy { it.startsAtMs }

fun cartsNeedingCall(carts: List<OpenCart>, appointments: List<Appointment>, nowMs: Long): List<OpenCart> =
    carts
        .filter { it.status == CartStatus.Editing && it.hasItems }
        .filter { cart ->
            val idleMs = cart.updatedAtMs?.let { nowMs - it } ?: return@filter false
            idleMs in FollowUpMinIdleMs..FollowUpMaxIdleMs
        }
        .filter { cart -> appointments.none { it.isOpen && it.isSameClientAs(cart) } }
        .sortedByDescending { it.totalCents ?: 0L }

fun clientHistory(appointment: Appointment, appointments: List<Appointment>): List<Appointment> =
    appointments
        .filter { it.appointmentId != appointment.appointmentId && it.isSameClientAs(appointment) }
        .sortedByDescending { it.startsAtMs }

fun startLabel(appointment: Appointment, nowMs: Long): String? {
    val minutesUntil = (appointment.startsAtMs - nowMs) / MinuteMs
    return when (appointment.agendaStatus(nowMs)) {
        AgendaStatus.Now -> if (minutesUntil > 0) "começa em $minutesUntil min" else "acontecendo agora"
        AgendaStatus.Upcoming -> when {
            minutesUntil < 60 -> "em $minutesUntil min"
            minutesUntil < 12 * 60 -> "em ${minutesUntil / 60}h"
            else -> null
        }
        AgendaStatus.Pending -> "sem resultado registrado"
        else -> null
    }
}

fun freeGaps(
    date: LocalDate,
    appointments: List<Appointment>,
    nowMs: Long,
    timeZone: TimeZone,
): List<IntRange> {
    val today = localDateAt(nowMs, timeZone)
    if (date < today) return emptyList()
    val earliest = if (date == today) roundUpToQuarter(minuteOfDayAt(nowMs, timeZone)) else WorkdayStartMinute
    var cursor = maxOf(WorkdayStartMinute, earliest)
    val busy = appointments
        .filter { it.status != AppointmentStatus.Cancelled && localDateAt(it.startsAtMs, timeZone) == date }
        .map { minuteOfDayAt(it.startsAtMs, timeZone) to minuteOfDayAt(it.startsAtMs, timeZone) + it.durationMinutes }
        .sortedBy { it.first }
    val gaps = mutableListOf<IntRange>()
    busy.forEach { (start, end) ->
        if (start - cursor >= MinFreeGapMinutes && start <= WorkdayEndMinute) gaps += cursor until start
        cursor = maxOf(cursor, end)
    }
    if (WorkdayEndMinute - cursor >= MinFreeGapMinutes) gaps += cursor until WorkdayEndMinute
    return gaps
}

private fun roundUpToQuarter(minuteOfDay: Int): Int = ((minuteOfDay + 14) / 15) * 15
