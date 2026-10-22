package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentOwner
import app.trovata.cast.data.local.AppointmentPriority
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCartsPage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = at(Wednesday, 10)
    private val owner = AppointmentOwner(empresaSlug = "buba", sellerId = "42")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(
        owners: MutableStateFlow<AppointmentOwner?> = MutableStateFlow(owner),
        byOwner: Map<AppointmentOwner, List<Appointment>> = emptyMap(),
        cartsResult: ApiResult<OpenCartsPage> = ApiResult.Ok(OpenCartsPage(emptyList(), 0, false)),
        onLoadCarts: (String, CartStatus?) -> Unit = { _, _ -> },
    ) = AgendaScreenModel(
        owners = owners,
        observe = { flowOf(byOwner[it].orEmpty()) },
        loadOpenCarts = { slug, status, _, _ ->
            onLoadCarts(slug, status)
            cartsResult
        },
        clock = flowOf(now),
        nowMs = { now },
        timeZone = SaoPaulo,
    )

    @Test
    fun opensOnTheCurrentWeekWithTheOwnersAppointments() = runTest(dispatcher) {
        val mine = appointment("a", at(Wednesday, 14))
        val sm = model(byOwner = mapOf(owner to listOf(mine)))
        dispatcher.scheduler.advanceUntilIdle()

        val state = sm.state.value
        assertEquals(AgendaView.Week, state.view)
        assertEquals(Wednesday, state.selectedDate)
        assertEquals(listOf("a"), state.appointmentsOn(Wednesday).map { it.appointmentId })
        assertEquals("1 chamada hoje · próxima às 14:00", state.subtitle)
        assertFalse(state.isLoading)
    }

    @Test
    fun switchingCompanyShowsOnlyThatOwnersAgenda() = runTest(dispatcher) {
        val other = AppointmentOwner(empresaSlug = "mart", sellerId = "42")
        val owners = MutableStateFlow<AppointmentOwner?>(owner)
        val slugs = mutableListOf<String>()
        val sm = model(
            owners = owners,
            byOwner = mapOf(
                owner to listOf(appointment("buba-call", at(Wednesday, 14))),
                other to listOf(appointment("mart-call", at(Wednesday, 15))),
            ),
            onLoadCarts = { slug, _ -> slugs += slug },
        )
        dispatcher.scheduler.advanceUntilIdle()
        owners.value = other
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("mart-call"), sm.state.value.appointments.map { it.appointmentId })
        assertEquals(listOf("buba", "mart"), slugs)
    }

    @Test
    fun withoutCompanyTheAgendaIsEmptyAndFlagged() = runTest(dispatcher) {
        val sm = model(owners = MutableStateFlow(null))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(sm.state.value.hasOwner)
        assertTrue(sm.state.value.appointments.isEmpty())
    }

    @Test
    fun tappingADayOnTheWeekOpensTheDayView() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.selectDate(LocalDate(2026, 10, 23))

        assertEquals(AgendaView.Day, sm.state.value.view)
        assertEquals(LocalDate(2026, 10, 23), sm.state.value.selectedDate)
    }

    @Test
    fun periodNavigationMovesByTheCurrentView() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.nextPeriod()
        assertEquals(LocalDate(2026, 10, 28), sm.state.value.selectedDate)
        assertFalse(sm.state.value.isShowingToday)

        sm.selectView(AgendaView.Month)
        sm.nextPeriod()
        assertEquals(LocalDate(2026, 11, 28), sm.state.value.selectedDate)

        sm.goToToday()
        assertEquals(Wednesday, sm.state.value.selectedDate)
        assertTrue(sm.state.value.isShowingToday)
    }

    @Test
    fun loadsEditingCartsAndOffersOnlyThoseWithoutACall() = runTest(dispatcher) {
        var requested: CartStatus? = null
        val scheduledClient = cart(1, updatedAtMs = now - 2 * DayMs, clienteId = 101)
        val free = cart(2, updatedAtMs = now - 2 * DayMs, clienteId = 202, clientEmail = "mare@loja.com")
        val sm = model(
            byOwner = mapOf(owner to listOf(appointment("a", at(Wednesday, 14), clienteId = 101))),
            cartsResult = ApiResult.Ok(OpenCartsPage(listOf(scheduledClient, free), 2, false)),
            onLoadCarts = { _, status -> requested = status },
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CartStatus.Editing, requested)
        assertEquals(listOf(2L), sm.state.value.cartsToCall.map { it.carrinhoId })
        assertEquals(1L, sm.state.value.openCartFor(sm.state.value.appointments.first())?.carrinhoId)
    }

    @Test
    fun cartFailureKeepsTheAgendaUsable() = runTest(dispatcher) {
        val sm = model(
            byOwner = mapOf(owner to listOf(appointment("a", at(Wednesday, 14)))),
            cartsResult = ApiResult.Fail("network_error", "Sem conexão com o servidor", 0),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sem conexão com o servidor", sm.state.value.cartsError)
        assertEquals(1, sm.state.value.appointments.size)
    }

    @Test
    fun dayLoadCountsPendingAndPriority() = runTest(dispatcher) {
        val pending = appointment("p", at(Wednesday, 8))
        val priority = appointment("h", at(Wednesday, 15), clienteId = 202, priority = AppointmentPriority.High)
        val sm = model(byOwner = mapOf(owner to listOf(pending, priority)))
        dispatcher.scheduler.advanceUntilIdle()

        val load = sm.state.value.loadOf(Wednesday)
        assertEquals(2, load.total)
        assertEquals(1, load.pending)
        assertTrue(load.priority)
    }
}
