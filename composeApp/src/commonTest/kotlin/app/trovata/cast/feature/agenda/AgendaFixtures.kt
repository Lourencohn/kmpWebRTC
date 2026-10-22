package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentPriority
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

val SaoPaulo: TimeZone = TimeZone.of("America/Sao_Paulo")

val Wednesday: LocalDate = LocalDate(2026, 10, 21)

fun at(date: LocalDate, hour: Int, minute: Int = 0): Long = date.atMinuteOfDay(hour * 60 + minute, SaoPaulo)

fun appointment(
    id: String,
    startsAtMs: Long,
    durationMinutes: Int = 30,
    clienteId: Long? = 101,
    clientName: String = "Loja Bella",
    clientEmail: String? = "compras@bella.com.br",
    status: AppointmentStatus = AppointmentStatus.Scheduled,
    priority: AppointmentPriority = AppointmentPriority.Normal,
    sessionId: String? = null,
    followUpOfId: String? = null,
) = Appointment(
    appointmentId = id,
    empresaSlug = "buba",
    sellerId = "42",
    clienteId = clienteId,
    clientName = clientName,
    clientShop = null,
    clientDocument = null,
    clientEmail = clientEmail,
    clientPhone = null,
    startsAtMs = startsAtMs,
    durationMinutes = durationMinutes,
    objective = null,
    notes = null,
    priority = priority,
    status = status,
    outcome = null,
    outcomeNotes = null,
    nextStep = null,
    prepDone = emptySet(),
    carrinhoId = null,
    sessionId = sessionId,
    followUpOfId = followUpOfId,
    rescheduleCount = 0,
    createdAtMs = 0,
    updatedAtMs = 0,
)

fun cart(
    carrinhoId: Long,
    updatedAtMs: Long,
    clienteId: Long? = 101,
    clientEmail: String? = "compras@bella.com.br",
    status: CartStatus = CartStatus.Editing,
    itemCount: Int = 6,
    totalCents: Long = 180_000,
) = OpenCart(
    carrinhoId = carrinhoId,
    catalogoLinkId = 4471,
    catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
    catalogName = "Verão 27",
    catalogActive = true,
    catalogExpiresAtMs = null,
    clientName = "Loja Bella",
    clientEmail = clientEmail,
    status = status,
    itemCount = itemCount,
    totalQuantity = itemCount * 4,
    totalCents = totalCents,
    updatedAtMs = updatedAtMs,
    clienteId = clienteId,
)
