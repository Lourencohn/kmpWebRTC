package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.AppointmentOutcome
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.local.LinkedOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class AgendaRulesTest {

    private val now = at(Wednesday, 10)

    @Test
    fun statusFollowsTheClockWhileScheduled() {
        val later = appointment("a", at(Wednesday, 14))
        val soon = appointment("b", at(Wednesday, 10, 5))
        val past = appointment("c", at(Wednesday, 8))

        assertEquals(AgendaStatus.Upcoming, later.agendaStatus(now))
        assertEquals(AgendaStatus.Now, soon.agendaStatus(now))
        assertEquals(AgendaStatus.Pending, past.agendaStatus(now))
    }

    @Test
    fun orderClosedInTheLinkedSessionCountsAsDone() {
        val past = appointment("a", at(Wednesday, 8), sessionId = "ses_1")
            .copy(linkedOrder = LinkedOrder("CAR-9021", 420_000))

        assertEquals(AgendaStatus.Done, past.agendaStatus(now))
        assertEquals(CallResult.OrderPlaced, past.suggestedResult())
        assertTrue(pendingOf(listOf(past), now).isEmpty())
    }

    @Test
    fun registeredStatusesIgnoreTheClock() {
        val cancelled = appointment("a", at(Wednesday, 8), status = AppointmentStatus.Cancelled)
        val missed = appointment("b", at(Wednesday, 14), status = AppointmentStatus.Missed)

        assertEquals(AgendaStatus.Cancelled, cancelled.agendaStatus(now))
        assertEquals(AgendaStatus.Missed, missed.agendaStatus(now))
    }

    @Test
    fun noAnswerMarksMissedAndKeepsTheNotes() {
        val result = appointment("a", at(Wednesday, 8))
            .withResult(CallResult.NoAnswer, "  caixa postal  ", "tentar amanhã cedo", now)

        assertEquals(AppointmentStatus.Missed, result.status)
        assertNull(result.outcome)
        assertEquals("caixa postal", result.outcomeNotes)
        assertEquals("tentar amanhã cedo", result.nextStep)
    }

    @Test
    fun followUpResultIsDoneWithOutcome() {
        val result = appointment("a", at(Wednesday, 8)).withResult(CallResult.FollowUp, "", " ", now)

        assertEquals(AppointmentStatus.Done, result.status)
        assertEquals(AppointmentOutcome.FollowUp, result.outcome)
        assertNull(result.outcomeNotes)
        assertNull(result.nextStep)
    }

    @Test
    fun rejectsSlotInThePastOnlyWhenTheTimeIsBeingChosen() {
        val request = slotRequest(start = at(Wednesday, 9), requireFuture = true)
        assertEquals(SlotProblem.InPast, slotProblem(request, emptyList(), now, SaoPaulo))

        val editingNotesOfPastCall = request.copy(requireFuture = false)
        assertNull(slotProblem(editingNotesOfPastCall, emptyList(), now, SaoPaulo))
    }

    @Test
    fun rejectsOverlapWithAnotherOpenCall() {
        val other = appointment("other", at(Wednesday, 14), clienteId = 202, clientName = "Concept Maré", clientEmail = "mare@loja.com")
        val request = slotRequest(start = at(Wednesday, 14, 15))

        val problem = slotProblem(request, listOf(other), now, SaoPaulo)

        assertIs<SlotProblem.Overlaps>(problem)
        assertEquals("other", problem.other.appointmentId)
    }

    @Test
    fun backToBackCallsDoNotOverlap() {
        val other = appointment("other", at(Wednesday, 14), clienteId = 202, clientName = "Concept Maré", clientEmail = null)
        val request = slotRequest(start = at(Wednesday, 14, 30))

        assertNull(slotProblem(request, listOf(other), now, SaoPaulo))
    }

    @Test
    fun cancelledOrFinishedCallsDoNotBlockTheSlot() {
        val cancelled = appointment("x", at(Wednesday, 14), clienteId = 202, status = AppointmentStatus.Cancelled)
        val request = slotRequest(start = at(Wednesday, 14))

        assertNull(slotProblem(request, listOf(cancelled), now, SaoPaulo))
    }

    @Test
    fun rejectsSecondOpenCallWithTheSameClientOnTheSameDay() {
        val morning = appointment("morning", at(Wednesday, 11))
        val request = slotRequest(start = at(Wednesday, 16))

        val problem = slotProblem(request, listOf(morning), now, SaoPaulo)

        assertIs<SlotProblem.SameClientSameDay>(problem)
    }

    @Test
    fun editingAnAppointmentDoesNotConflictWithItself() {
        val existing = appointment("same", at(Wednesday, 14))
        val request = slotRequest(start = at(Wednesday, 14, 15)).copy(appointmentId = "same")

        assertNull(slotProblem(request, listOf(existing), now, SaoPaulo))
    }

    @Test
    fun clientIdentityPrefersClienteIdOverEmail() {
        assertTrue(sameClient(101, "a@x.com", "A", 101, "b@y.com", "B"))
        assertEquals(false, sameClient(101, "a@x.com", "A", 202, "a@x.com", "A"))
        assertTrue(sameClient(null, " Compras@Bella.com.br ", "A", 101, "compras@bella.com.br", "B"))
        assertTrue(sameClient(null, null, "Loja  Bella", null, null, "loja bella"))
    }

    @Test
    fun cartsNeedingCallUseTheIdleWindowAndSkipClientsAlreadyScheduled() {
        val warm = cart(1, updatedAtMs = now - 3 * DayMs, clienteId = 101)
        val tooFresh = cart(2, updatedAtMs = now - 2 * 3_600_000L, clienteId = 303, clientEmail = "x@x.com")
        val abandoned = cart(3, updatedAtMs = now - 45 * DayMs, clienteId = 404, clientEmail = "y@y.com")
        val empty = cart(4, updatedAtMs = now - 3 * DayMs, clienteId = 505, clientEmail = "z@z.com", itemCount = 0)
        val unscheduled = cart(5, updatedAtMs = now - 5 * DayMs, clienteId = 606, clientEmail = "w@w.com", totalCents = 900_000)
        val scheduled = appointment("a", at(Wednesday, 15), clienteId = 101)

        val result = cartsNeedingCall(listOf(warm, tooFresh, abandoned, empty, unscheduled), listOf(scheduled), now)

        assertEquals(listOf(5L), result.map { it.carrinhoId })
    }

    @Test
    fun cartWithoutClienteIdMatchesByEmail() {
        val scheduled = appointment("a", at(Wednesday, 15), clienteId = 101, clientEmail = "compras@bella.com.br")
        val unlinked = cart(7, updatedAtMs = now - 3 * DayMs, clienteId = null, clientEmail = "COMPRAS@bella.com.br")

        assertTrue(scheduled.isSameClientAs(unlinked))
        assertTrue(cartsNeedingCall(listOf(unlinked), listOf(scheduled), now).isEmpty())
    }

    @Test
    fun followUpDisappearsOnceAReturnIsScheduled() {
        val done = appointment("done", at(Wednesday, 8))
            .withResult(CallResult.FollowUp, null, "mandar tabela nova", now)
        assertEquals(listOf("done"), followUpsOf(listOf(done)).map { it.appointmentId })

        val returnCall = appointment("ret", at(LocalDate(2026, 10, 23), 9), followUpOfId = "done")
        assertTrue(followUpsOf(listOf(done, returnCall)).isEmpty())
    }

    @Test
    fun summaryCountsTodayAndFindsTheNextCall() {
        val donePast = appointment("1", at(Wednesday, 8)).withResult(CallResult.OrderPlaced, null, null, now)
        val pending = appointment("2", at(Wednesday, 9), clienteId = 202)
        val next = appointment("3", at(Wednesday, 14), clienteId = 303)
        val cancelled = appointment("4", at(Wednesday, 16), clienteId = 404, status = AppointmentStatus.Cancelled)
        val tomorrow = appointment("5", at(LocalDate(2026, 10, 22), 9), clienteId = 505)

        val summary = summarize(listOf(donePast, pending, next, cancelled, tomorrow), now, SaoPaulo)

        assertEquals(3, summary.today)
        assertEquals(1, summary.doneToday)
        assertEquals(1, summary.pending)
        assertEquals("3", summary.next?.appointmentId)
    }

    @Test
    fun weekStartsOnMondayAndMonthGridCoversWholeWeeks() {
        val week = weekOf(Wednesday)
        assertEquals(LocalDate(2026, 10, 19), week.first())
        assertEquals(LocalDate(2026, 10, 25), week.last())

        val grid = monthGridOf(Wednesday)
        assertEquals(0, grid.size % 7)
        assertEquals(LocalDate(2026, 9, 28), grid.first())
        assertEquals(LocalDate(2026, 11, 1), grid.last())
    }

    @Test
    fun freeGapsSkipBusyTimeAndThePastOfToday() {
        val call = appointment("a", at(Wednesday, 11), durationMinutes = 60)

        val gaps = freeGaps(Wednesday, listOf(call), now, SaoPaulo)

        assertEquals(listOf(10 * 60 until 11 * 60, 12 * 60 until WorkdayEndMinute), gaps)
        assertTrue(freeGaps(LocalDate(2026, 10, 20), emptyList(), now, SaoPaulo).isEmpty())
    }

    @Test
    fun nextBusinessDaySkipsTheWeekend() {
        assertEquals(LocalDate(2026, 10, 26), nextBusinessDay(LocalDate(2026, 10, 23)))
        assertEquals(LocalDate(2026, 10, 22), nextBusinessDay(Wednesday))
    }

    private fun slotRequest(start: Long, requireFuture: Boolean = true) = SlotRequest(
        appointmentId = null,
        clienteId = 101,
        clientName = "Loja Bella",
        clientEmail = "compras@bella.com.br",
        startsAtMs = start,
        durationMinutes = 30,
        requireFuture = requireFuture,
    )
}
