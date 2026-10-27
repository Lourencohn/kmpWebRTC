package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentOwner
import app.trovata.cast.data.local.AppointmentPriority
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.CatalogClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentFormScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = at(Wednesday, 10)
    private val owner = AppointmentOwner(empresaSlug = "buba", sellerId = "42")

    private val bella = CatalogClient(
        id = 101,
        ref = "C-101",
        name = "Loja Bella",
        legalName = "Bella Comércio de Roupas Ltda",
        document = "12.345.678/0001-90",
        phone = "(11) 98888-7777",
        email = "compras@bella.com.br",
        contact = "Marina",
        active = true,
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(
        spec: AppointmentFormSpec = AppointmentFormSpec(),
        existing: List<Appointment> = emptyList(),
        saved: MutableList<Appointment> = mutableListOf(),
        owner: AppointmentOwner? = this.owner,
        interests: Map<String, List<CallInterest>> = emptyMap(),
    ) = AppointmentFormScreenModel(
        spec = spec,
        ownerProvider = { owner },
        loadAppointments = { existing },
        searchClients = { query -> if (query.isBlank() || "bella" in query.lowercase()) listOf(bella) else emptyList() },
        saveAppointment = { saved += it },
        loadCallInterests = { sessionId -> interests[sessionId].orEmpty() },
        newId = { "apt-new" },
        nowMs = { now },
        timeZone = SaoPaulo,
    )

    @Test
    fun createsAScheduledAppointmentForTheOwner() = runTest(dispatcher) {
        val saved = mutableListOf<Appointment>()
        val sm = model(saved = saved)
        dispatcher.scheduler.advanceUntilIdle()

        sm.pickClient(bella)
        sm.selectSlot(14 * 60)
        sm.selectDuration(45)
        sm.applyObjective("Reposição")
        sm.togglePriority()
        sm.save()
        dispatcher.scheduler.advanceUntilIdle()

        val appointment = saved.single()
        assertEquals("apt-new", appointment.appointmentId)
        assertEquals("buba", appointment.empresaSlug)
        assertEquals("42", appointment.sellerId)
        assertEquals(101L, appointment.clienteId)
        assertEquals("Bella Comércio de Roupas Ltda", appointment.clientShop)
        assertEquals(at(Wednesday, 14), appointment.startsAtMs)
        assertEquals(45, appointment.durationMinutes)
        assertEquals("Reposição", appointment.objective)
        assertEquals(AppointmentPriority.High, appointment.priority)
        assertEquals(AppointmentStatus.Scheduled, appointment.status)
        assertEquals("apt-new", sm.state.value.savedAppointmentId)
    }

    @Test
    fun blocksSavingWithoutClientOrTime() = runTest(dispatcher) {
        val saved = mutableListOf<Appointment>()
        val sm = model(saved = saved)
        dispatcher.scheduler.advanceUntilIdle()

        sm.save()
        assertEquals("Escolha o cliente da chamada.", sm.state.value.problem)

        sm.pickClient(bella)
        sm.save()
        assertEquals("Escolha o horário da chamada.", sm.state.value.problem)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun hidesPastSlotsOfTodayAndMarksBusyOnes() = runTest(dispatcher) {
        val other = appointment("other", at(Wednesday, 14), clienteId = 202, clientName = "Concept Maré", clientEmail = null)
        val sm = model(existing = listOf(other))
        dispatcher.scheduler.advanceUntilIdle()

        val slots = sm.state.value.slots
        assertEquals(10 * 60, slots.first().minuteOfDay)
        assertEquals(SlotState.Busy, slots.first { it.minuteOfDay == 14 * 60 }.state)
        assertEquals(SlotState.Free, slots.first { it.minuteOfDay == 13 * 60 + 30 }.state)

        sm.selectDuration(60)
        assertEquals(SlotState.Busy, sm.state.value.slots.first { it.minuteOfDay == 13 * 60 + 30 }.state)
    }

    @Test
    fun conflictIsExplainedBeforeSaving() = runTest(dispatcher) {
        val other = appointment("other", at(Wednesday, 14), clienteId = 202, clientName = "Concept Maré", clientEmail = null)
        val sm = model(existing = listOf(other))
        dispatcher.scheduler.advanceUntilIdle()

        sm.pickClient(bella)
        sm.selectSlot(13 * 60 + 45)

        assertEquals("Bate com a chamada de Concept Maré, das 14:00 às 14:30.", sm.state.value.conflictPreview)
    }

    @Test
    fun reschedulingMovesTheSameAppointmentAndCountsIt() = runTest(dispatcher) {
        val original = appointment("apt-1", at(Wednesday, 15)).copy(objective = "Coleção nova")
        val saved = mutableListOf<Appointment>()
        val sm = model(spec = AppointmentFormSpec(appointmentId = "apt-1", reschedule = true), existing = listOf(original), saved = saved)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(FormMode.Reschedule, sm.state.value.mode)
        assertEquals(15 * 60, sm.state.value.minuteOfDay)
        sm.selectDate(LocalDate(2026, 10, 22))
        sm.selectSlot(9 * 60)
        sm.save()
        dispatcher.scheduler.advanceUntilIdle()

        val moved = saved.single()
        assertEquals("apt-1", moved.appointmentId)
        assertEquals(at(LocalDate(2026, 10, 22), 9), moved.startsAtMs)
        assertEquals(1, moved.rescheduleCount)
        assertEquals("Coleção nova", moved.objective)
    }

    @Test
    fun editingNotesOfAPendingCallKeepsItsPastTime() = runTest(dispatcher) {
        val pending = appointment("apt-1", at(Wednesday, 8))
        val saved = mutableListOf<Appointment>()
        val sm = model(spec = AppointmentFormSpec(appointmentId = "apt-1"), existing = listOf(pending), saved = saved)
        dispatcher.scheduler.advanceUntilIdle()

        sm.setNotes("Levar tabela de inverno")
        sm.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(sm.state.value.problem)
        assertEquals(at(Wednesday, 8), saved.single().startsAtMs)
        assertEquals(0, saved.single().rescheduleCount)
    }

    @Test
    fun followUpStartsFromThePreviousCall() = runTest(dispatcher) {
        val previous = appointment("prev", at(Wednesday, 9))
            .withResult(CallResult.FollowUp, "pediu prazo", "Mandar tabela de inverno", now)
        val saved = mutableListOf<Appointment>()
        val sm = model(spec = AppointmentFormSpec(followUpOfId = "prev"), existing = listOf(previous), saved = saved)
        dispatcher.scheduler.advanceUntilIdle()

        val state = sm.state.value
        assertEquals(FormMode.FollowUp, state.mode)
        assertEquals("Loja Bella", state.client?.name)
        assertEquals(LocalDate(2026, 10, 22), state.date)
        assertEquals(9 * 60, state.minuteOfDay)
        assertEquals("Mandar tabela de inverno", state.objective)

        sm.save()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("prev", saved.single().followUpOfId)
    }

    @Test
    fun clientFromACartStaysLinkedToTheCart() = runTest(dispatcher) {
        val fromCart = cart(9021, updatedAtMs = now - 3 * DayMs, clienteId = null).toAppointmentClient()
        val saved = mutableListOf<Appointment>()
        val sm = model(
            spec = AppointmentFormSpec(client = fromCart, carrinhoId = 9021, objective = "Retomar carrinho parado"),
            saved = saved,
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.clearClient()
        assertNotNull(sm.state.value.client)
        sm.selectSlot(16 * 60)
        sm.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(9021L, saved.single().carrinhoId)
        assertEquals("compras@bella.com.br", saved.single().clientEmail)
        assertNull(saved.single().clienteId)
    }

    @Test
    fun withoutCompanyNothingIsSaved() = runTest(dispatcher) {
        val saved = mutableListOf<Appointment>()
        val sm = model(owner = null, saved = saved)
        dispatcher.scheduler.advanceUntilIdle()

        sm.pickClient(bella)
        sm.selectSlot(14 * 60)
        sm.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(saved.isEmpty())
        assertEquals("Selecione uma empresa para agendar chamadas.", sm.state.value.problem)
    }

    @Test
    fun followUpOfACallListsWhatWasLeftOut() = runTest(dispatcher) {
        val previous = appointment("prev", at(Wednesday, 9), sessionId = "ses_old")
        val leftOut = CallInterest(
            produtoPreId = 77,
            clientOpens = 2,
            sellerShows = 0,
            draftUnits = 0,
            cartUnits = 0,
            ref = "AN-104",
            name = "Vestido Midi",
            imageUrl = null,
            firstAtMs = 0,
            lastAtMs = 0,
        )
        val saved = mutableListOf<Appointment>()
        val sm = model(
            spec = AppointmentFormSpec(followUpOfId = "prev"),
            existing = listOf(previous),
            saved = saved,
            interests = mapOf("ses_old" to listOf(leftOut)),
        )
        dispatcher.scheduler.advanceUntilIdle()

        val expected = "Ficou de fora do pedido na última chamada:\n- Vestido Midi · ref. AN-104 · abriu 2 vezes"
        assertEquals(expected, sm.state.value.notes)

        sm.save()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(expected, saved.single().notes)
    }

    @Test
    fun notesFromTheSpecArePrefilledAndWin() = runTest(dispatcher) {
        val previous = appointment("prev", at(Wednesday, 9), sessionId = "ses_old")
        val sm = model(
            spec = AppointmentFormSpec(followUpOfId = "prev", notes = "Levar a tabela nova"),
            existing = listOf(previous),
            interests = mapOf("ses_old" to emptyList()),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Levar a tabela nova", sm.state.value.notes)
    }
}
