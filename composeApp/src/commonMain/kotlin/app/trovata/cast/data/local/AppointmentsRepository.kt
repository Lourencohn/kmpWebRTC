package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.db.AppointmentWithOrder
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppointmentsRepository(private val db: TrovataDatabase) {
    private val queries = db.appointmentsQueries

    fun observeForOwner(owner: AppointmentOwner): Flow<List<Appointment>> =
        queries.selectAppointmentsForOwner(owner.empresaSlug, owner.sellerId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toAppointment() } }

    suspend fun listForOwner(owner: AppointmentOwner): List<Appointment> = withContext(Dispatchers.Default) {
        queries.selectAppointmentsForOwner(owner.empresaSlug, owner.sellerId)
            .executeAsList()
            .map { it.toAppointment() }
    }

    suspend fun save(appointment: Appointment) = withContext(Dispatchers.Default) {
        queries.upsertAppointment(
            appointmentId = appointment.appointmentId,
            empresaSlug = appointment.empresaSlug,
            sellerId = appointment.sellerId,
            clienteId = appointment.clienteId,
            clientName = appointment.clientName,
            clientShop = appointment.clientShop,
            clientDocument = appointment.clientDocument,
            clientEmail = appointment.clientEmail,
            clientPhone = appointment.clientPhone,
            startsAtMs = appointment.startsAtMs,
            durationMinutes = appointment.durationMinutes.toLong(),
            objective = appointment.objective,
            notes = appointment.notes,
            priority = appointment.priority.code,
            status = appointment.status.code,
            outcome = appointment.outcome?.code,
            outcomeNotes = appointment.outcomeNotes,
            nextStep = appointment.nextStep,
            prepDone = appointment.prepDone.sorted().joinToString(PREP_SEPARATOR),
            carrinhoId = appointment.carrinhoId,
            sessionId = appointment.sessionId,
            followUpOfId = appointment.followUpOfId,
            rescheduleCount = appointment.rescheduleCount.toLong(),
            createdAtMs = appointment.createdAtMs,
            updatedAtMs = appointment.updatedAtMs,
        )
    }

    suspend fun linkSession(appointmentId: String, sessionId: String, atMs: Long) = withContext(Dispatchers.Default) {
        queries.linkAppointmentSession(sessionId = sessionId, updatedAtMs = atMs, appointmentId = appointmentId)
    }

    private fun AppointmentWithOrder.toAppointment(): Appointment = Appointment(
        appointmentId = appointmentId,
        empresaSlug = empresaSlug,
        sellerId = sellerId,
        clienteId = clienteId,
        clientName = clientName,
        clientShop = clientShop,
        clientDocument = clientDocument,
        clientEmail = clientEmail,
        clientPhone = clientPhone,
        startsAtMs = startsAtMs,
        durationMinutes = durationMinutes.toInt(),
        objective = objective,
        notes = notes,
        priority = AppointmentPriority.fromCode(priority),
        status = AppointmentStatus.fromCode(status),
        outcome = AppointmentOutcome.fromCode(outcome),
        outcomeNotes = outcomeNotes,
        nextStep = nextStep,
        prepDone = prepDone.split(PREP_SEPARATOR).filter { it.isNotBlank() }.toSet(),
        carrinhoId = carrinhoId,
        sessionId = sessionId,
        followUpOfId = followUpOfId,
        rescheduleCount = rescheduleCount.toInt(),
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
        linkedOrder = orderId?.let { LinkedOrder(orderId = it, totalCents = orderTotalCents ?: 0L) },
    )

    private companion object {
        const val PREP_SEPARATOR = ","
    }
}
