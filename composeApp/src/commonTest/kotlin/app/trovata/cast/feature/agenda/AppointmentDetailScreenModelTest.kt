package app.trovata.cast.feature.agenda

import app.trovata.cast.data.auth.SellerIdentity
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentOutcome
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.local.LinkedOrder
import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.data.remote.sfa.OpenCartsPage
import app.trovata.cast.feature.catalog.CreateSessionFn
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDetailScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = at(Wednesday, 10)

    private val current = appointment("apt-1", at(Wednesday, 14)).copy(objective = "Reposição")
    private val previous = appointment("apt-0", at(Wednesday, 9) - 7 * DayMs, sessionId = "ses_old")
        .withResult(CallResult.FollowUp, "achou caro o tricot", "Mandar tabela de inverno", now)
    private val otherClient = appointment("apt-x", at(Wednesday, 16), clienteId = 202, clientName = "Concept Maré", clientEmail = "mare@loja.com")

    private val linkedCart = cart(9021, updatedAtMs = now - 3 * DayMs, clienteId = 101, clientEmail = "marina.pessoal@gmail.com")
    private val unlinkedCart = cart(9022, updatedAtMs = now - 5 * DayMs, clienteId = null, clientEmail = "COMPRAS@bella.com.br")
    private val strangerCart = cart(9023, updatedAtMs = now - 2 * DayMs, clienteId = 202, clientEmail = "mare@loja.com")
    private val sentOrder = cart(9024, updatedAtMs = now - DayMs, clienteId = 101, status = CartStatus.Sent)
    private val cancelledCart = cart(9025, updatedAtMs = now - DayMs, clienteId = 101, status = CartStatus.Cancelled)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Harness {
        val appointments = MutableStateFlow<List<Appointment>>(emptyList())
        val searches = mutableListOf<String?>()
        val requests = mutableListOf<SessionCreateRequest>()
        val notes = mutableListOf<SessionClientNotes>()
    }

    private fun model(
        harness: Harness,
        appointments: List<Appointment> = listOf(current, previous, otherClient),
        sessions: List<StoredSessionRecord> = emptyList(),
        orders: List<StoredOrder> = emptyList(),
        carts: List<OpenCart> = listOf(linkedCart, unlinkedCart, strangerCart, sentOrder, cancelledCart),
        createSession: CreateSessionFn = { request -> ApiResult.Ok(response(request)) },
    ): AppointmentDetailScreenModel {
        harness.appointments.value = appointments
        return AppointmentDetailScreenModel(
            appointmentId = "apt-1",
            observeAppointments = { harness.appointments },
            observeSessions = { flowOf(sessions) },
            observeOrders = { ids -> flowOf(orders.filter { it.sessionId in ids }) },
            findClient = { null },
            loadOpenCarts = { _, _, _, search ->
                harness.searches += search
                ApiResult.Ok(OpenCartsPage(carts, carts.size, false))
            },
            loadCartItems = { _, _, _ -> ApiResult.Ok(listOf(itemLine())) },
            saveAppointment = { saved ->
                harness.appointments.value = harness.appointments.value.map { if (it.appointmentId == saved.appointmentId) saved else it }
            },
            createSession = { request ->
                harness.requests += request
                createSession(request)
            },
            persistSession = { request, resp, createdAt, clientNotes ->
                harness.notes += clientNotes
                record(request, resp, createdAt, clientNotes)
            },
            sellerProvider = { SellerIdentity(id = "42", name = "Lourenço") },
            clock = flowOf(now),
            nowMs = { now },
            timeZone = SaoPaulo,
        )
    }

    @Test
    fun buildsTheClientContextFromHistorySessionsAndOrders() = runTest(dispatcher) {
        val oldSession = session("ses_old", email = null)
        val byEmail = session("ses_mail", email = "compras@bella.com.br")
        val stranger = session("ses_other", email = "mare@loja.com")
        val order = order("CAR-8800", "ses_old")
        val sm = model(Harness(), sessions = listOf(oldSession, byEmail, stranger), orders = listOf(order))
        dispatcher.scheduler.advanceUntilIdle()

        val state = sm.state.value
        assertEquals(listOf("apt-0"), state.history.map { it.appointmentId })
        assertEquals("Mandar tabela de inverno", state.lastContact?.nextStep)
        assertEquals(setOf("ses_old", "ses_mail"), state.sessions.map { it.sessionId }.toSet())
        assertEquals(listOf("ses_mail"), state.unlinkedSessions.map { it.sessionId })
        assertEquals("CAR-8800", state.orderFor("ses_old")?.orderId)
    }

    @Test
    fun findsTheClientCartsByIdOrEmailOnly() = runTest(dispatcher) {
        val harness = Harness()
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        val state = sm.state.value
        assertEquals(setOf("compras@bella.com.br", "Loja Bella"), harness.searches.toSet())
        assertEquals(listOf(9021L, 9022L), state.openCarts.map { it.carrinhoId })
        assertEquals(listOf(9024L), state.ordersInProgress.map { it.carrinhoId })
        assertEquals(9021L, state.callableCart?.carrinhoId)
    }

    @Test
    fun callingWithTheCartOpensASessionAndLinksTheAppointment() = runTest(dispatcher) {
        val harness = Harness()
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        sm.callWithCart(linkedCart)
        dispatcher.scheduler.advanceUntilIdle()

        val request = harness.requests.single()
        assertEquals(9021L, request.carrinhoId)
        assertEquals("buba", request.empresaSlug)
        assertEquals("42", request.sellerId)
        assertEquals("2026-10-21T17:00:00Z", harness.notes.single().scheduledFor)
        val linked = harness.appointments.value.first { it.appointmentId == "apt-1" }
        assertEquals("ses_kP3xq9", linked.sessionId)
        assertEquals(9021L, linked.carrinhoId)
        assertEquals("ses_kP3xq9", sm.state.value.createdSession?.sessionId)
    }

    @Test
    fun blockedCartExplainsWhyAndOpensNoSession() = runTest(dispatcher) {
        val harness = Harness()
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        sm.callWithCart(linkedCart.copy(clientEmail = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(harness.requests.isEmpty())
        assertEquals(
            "Esse carrinho não tem e-mail de cliente. Sem ele a chamada não abre o carrinho.",
            sm.state.value.error,
        )
    }

    @Test
    fun followUpResultIsSavedAndAsksForTheReturnCall() = runTest(dispatcher) {
        val harness = Harness()
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        sm.openResult()
        sm.selectResult(CallResult.FollowUp)
        assertTrue(sm.state.value.outcome.scheduleReturn)
        sm.setOutcomeNotes("Quer prazo de 90 dias")
        sm.setNextStep("Ligar com a resposta do financeiro")
        sm.saveResult()
        dispatcher.scheduler.advanceUntilIdle()

        val saved = harness.appointments.value.first { it.appointmentId == "apt-1" }
        assertEquals(AppointmentStatus.Done, saved.status)
        assertEquals(AppointmentOutcome.FollowUp, saved.outcome)
        assertEquals("Quer prazo de 90 dias", saved.outcomeNotes)
        assertEquals("Ligar com a resposta do financeiro", saved.nextStep)
        assertEquals("apt-1", sm.state.value.followUpRequestedFor)
        assertFalse(sm.state.value.showResultSheet)
    }

    @Test
    fun resultNeedsAChoice() = runTest(dispatcher) {
        val sm = model(Harness())
        dispatcher.scheduler.advanceUntilIdle()

        sm.openResult()
        sm.saveResult()

        assertEquals("Escolha como foi a chamada.", sm.state.value.error)
    }

    @Test
    fun orderClosedInTheCallIsSuggestedAsTheResult() = runTest(dispatcher) {
        val withOrder = current.copy(sessionId = "ses_live", linkedOrder = LinkedOrder("CAR-9021", 420_000))
        val sm = model(Harness(), appointments = listOf(withOrder))
        dispatcher.scheduler.advanceUntilIdle()

        sm.openResult()

        assertEquals(CallResult.OrderPlaced, sm.state.value.outcome.result)
        assertEquals(AgendaStatus.Done, sm.state.value.status)
    }

    @Test
    fun cancellingNeedsConfirmation() = runTest(dispatcher) {
        val harness = Harness()
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        sm.requestCancel()
        assertTrue(sm.state.value.confirmingCancel)
        sm.confirmCancel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppointmentStatus.Cancelled, harness.appointments.value.first { it.appointmentId == "apt-1" }.status)
        assertFalse(sm.state.value.confirmingCancel)
    }

    @Test
    fun prepChecklistIsPersistedAndReadinessFlagsMissingEmail() = runTest(dispatcher) {
        val harness = Harness()
        val noEmail = current.copy(clientEmail = null)
        val sm = model(harness, appointments = listOf(noEmail, previous))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, sm.state.value.readiness.first { it.label == "E-mail para abrir o carrinho" }.ok)
        assertEquals(true, sm.state.value.readiness.first { it.label == "Objetivo da conversa" }.ok)

        sm.togglePrep(PrepTask.PickCatalog)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(harness.appointments.value.first { it.appointmentId == "apt-1" }.isPrepDone(PrepTask.PickCatalog))
    }

    @Test
    fun expandingACartLoadsItsItemsOnce() = runTest(dispatcher) {
        val sm = model(Harness())
        dispatcher.scheduler.advanceUntilIdle()

        sm.toggleCart(linkedCart)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(9021L, sm.state.value.expandedCartId)
        assertEquals("Vestido Midi", sm.state.value.cartItems[9021L]?.single()?.name)

        sm.toggleCart(linkedCart)
        assertNull(sm.state.value.expandedCartId)
    }

    private fun response(request: SessionCreateRequest) = SessionCreateResponse(
        sessionId = "ses_kP3xq9",
        token = "kP3xq9",
        url = "https://trovata.app.br/catalogo-link-view/${request.empresaSlug}/${request.catalogoUuid}?live=kP3xq9",
        expiresAtMs = now + 3_600_000,
    )

    private fun record(
        request: SessionCreateRequest,
        resp: SessionCreateResponse,
        createdAt: Long,
        notes: SessionClientNotes,
    ) = StoredSessionRecord(
        sessionId = resp.sessionId,
        token = resp.token,
        url = resp.url,
        sellerName = request.sellerName,
        empresaSlug = request.empresaSlug,
        catalogoUuid = request.catalogoUuid,
        catalogoNome = request.catalogoNome,
        catalogoLinkId = request.catalogoLinkId,
        clientName = request.clientName,
        clientEmail = request.clientEmail,
        clientShop = notes.shop,
        scheduledFor = notes.scheduledFor,
        createdAtMs = createdAt,
        expiresAtMs = resp.expiresAtMs,
    )

    private fun session(id: String, email: String?) = StoredSessionRecord(
        sessionId = id,
        token = id.removePrefix("ses_"),
        url = "https://trovata.app.br/x?live=$id",
        sellerName = "Lourenço",
        empresaSlug = "buba",
        catalogoUuid = "uuid",
        catalogoNome = "Verão 27",
        catalogoLinkId = 4471,
        clientName = "Loja Bella",
        clientEmail = email,
        clientShop = null,
        scheduledFor = null,
        createdAtMs = now - 7 * DayMs,
        expiresAtMs = now,
    )

    private fun order(id: String, sessionId: String) = StoredOrder(
        orderId = id,
        sessionId = sessionId,
        sessionToken = null,
        clientName = "Loja Bella",
        clientShop = null,
        sellerName = "Lourenço",
        totalCents = 320_000,
        confirmedByMe = true,
        createdAtMs = now - 7 * DayMs,
        lines = emptyList(),
    )

    private fun itemLine() = CartItemLine(
        itemId = 1,
        produtoPreId = 77,
        ref = "AN-104",
        name = "Vestido Midi",
        color = "Areia",
        imageUrl = null,
        quantity = 8,
        totalCents = 96_000,
        sizes = emptyList(),
    )
}
