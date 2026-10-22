package app.trovata.cast.feature.agenda

import app.trovata.cast.data.auth.SellerIdentity
import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.data.remote.sfa.OpenCartsOrder
import app.trovata.cast.data.remote.sfa.OpenCartsPage
import app.trovata.cast.feature.carts.LoadOpenCartsFn
import app.trovata.cast.feature.carts.liveCartSessionRequest
import app.trovata.cast.feature.catalog.CreateSessionFn
import app.trovata.cast.feature.catalog.PersistSessionFn
import app.trovata.cast.feature.presentation.minuteClock
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

typealias LoadCartItemsFn = suspend (
    empresaSlug: String,
    catalogoUuid: String,
    carrinhoId: Long,
) -> ApiResult<List<CartItemLine>>

data class OutcomeDraft(
    val result: CallResult? = null,
    val notes: String = "",
    val nextStep: String = "",
    val scheduleReturn: Boolean = false,
)

data class ReadinessCheck(val label: String, val ok: Boolean, val hint: String)

data class AppointmentDetailUiState(
    val appointment: Appointment? = null,
    val isLoading: Boolean = true,
    val nowMs: Long,
    val timeZone: TimeZone,
    val client: CatalogClient? = null,
    val history: List<Appointment> = emptyList(),
    val sessions: List<StoredSessionRecord> = emptyList(),
    val orders: List<StoredOrder> = emptyList(),
    val carts: List<OpenCart> = emptyList(),
    val cartsLoading: Boolean = false,
    val cartsError: String? = null,
    val expandedCartId: Long? = null,
    val cartItems: Map<Long, List<CartItemLine>> = emptyMap(),
    val loadingItemsFor: Long? = null,
    val callingCartId: Long? = null,
    val error: String? = null,
    val createdSession: StoredSessionRecord? = null,
    val showResultSheet: Boolean = false,
    val outcome: OutcomeDraft = OutcomeDraft(),
    val confirmingCancel: Boolean = false,
    val followUpRequestedFor: String? = null,
) {
    val notFound: Boolean get() = !isLoading && appointment == null

    val status: AgendaStatus? get() = appointment?.agendaStatus(nowMs)

    val openCarts: List<OpenCart> get() = carts.filter { it.status == CartStatus.Editing && it.hasItems }

    val ordersInProgress: List<OpenCart>
        get() = carts.filter {
            it.status == CartStatus.Finalized || it.status == CartStatus.ReadyToSend || it.status == CartStatus.Sent
        }

    val callableCart: OpenCart? get() = openCarts.firstOrNull { it.canCallLive(nowMs) }

    val lastContact: Appointment?
        get() {
            val current = appointment ?: return null
            return history.firstOrNull {
                it.startsAtMs < current.startsAtMs &&
                    (it.status == AppointmentStatus.Done || it.status == AppointmentStatus.Missed)
            }
        }

    val readiness: List<ReadinessCheck>
        get() {
            val current = appointment ?: return emptyList()
            return listOf(
                ReadinessCheck(
                    label = "E-mail para abrir o carrinho",
                    ok = !current.clientEmail.isNullOrBlank(),
                    hint = "Sem e-mail a chamada não abre carrinho. Peça antes de convidar.",
                ),
                ReadinessCheck(
                    label = "Objetivo da conversa",
                    ok = !current.objective.isNullOrBlank(),
                    hint = "Defina o objetivo para conduzir a chamada sem perder o foco.",
                ),
            )
        }

    val prepTasks: List<PrepTask>
        get() = listOfNotNull(
            PrepTask.ReviewCart.takeIf { openCarts.isNotEmpty() },
            PrepTask.ReviewLastContact.takeIf { lastContact != null || sessions.isNotEmpty() },
            PrepTask.PickCatalog,
        )

    val unlinkedSessions: List<StoredSessionRecord>
        get() {
            val linked = (history + listOfNotNull(appointment)).mapNotNull { it.sessionId }.toSet()
            return sessions.filter { it.sessionId !in linked }
        }

    fun orderFor(sessionId: String?): StoredOrder? = sessionId?.let { id -> orders.firstOrNull { it.sessionId == id } }
}

private data class DetailSnapshot(
    val appointment: Appointment?,
    val history: List<Appointment>,
    val sessions: List<StoredSessionRecord>,
    val orders: List<StoredOrder>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDetailScreenModel(
    private val appointmentId: String,
    observeAppointments: () -> Flow<List<Appointment>>,
    observeSessions: () -> Flow<List<StoredSessionRecord>>,
    private val observeOrders: (List<String>) -> Flow<List<StoredOrder>>,
    private val findClient: suspend (Long) -> CatalogClient?,
    private val loadOpenCarts: LoadOpenCartsFn,
    private val loadCartItems: LoadCartItemsFn,
    private val saveAppointment: suspend (Appointment) -> Unit,
    private val createSession: CreateSessionFn,
    private val persistSession: PersistSessionFn,
    private val sellerProvider: () -> SellerIdentity,
    clock: Flow<Long> = minuteClock(),
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ScreenModel {

    private val _state = MutableStateFlow(AppointmentDetailUiState(nowMs = nowMs(), timeZone = timeZone))
    val state: StateFlow<AppointmentDetailUiState> = _state.asStateFlow()

    private var contextLoadedFor: String? = null

    init {
        screenModelScope.launch {
            combine(observeAppointments(), observeSessions()) { appointments, sessions -> appointments to sessions }
                .flatMapLatest { (appointments, sessions) -> snapshotOf(appointments, sessions) }
                .collect { snapshot ->
                    _state.update {
                        it.copy(
                            appointment = snapshot.appointment,
                            history = snapshot.history,
                            sessions = snapshot.sessions,
                            orders = snapshot.orders,
                            isLoading = false,
                        )
                    }
                    snapshot.appointment?.let(::loadContextOnce)
                }
        }
        screenModelScope.launch {
            clock.collect { now -> _state.update { it.copy(nowMs = now) } }
        }
    }

    private fun snapshotOf(appointments: List<Appointment>, sessions: List<StoredSessionRecord>): Flow<DetailSnapshot> {
        val appointment = appointments.firstOrNull { it.appointmentId == appointmentId }
            ?: return flowOf(DetailSnapshot(null, emptyList(), emptyList(), emptyList()))
        val history = clientHistory(appointment, appointments)
        val linkedSessionIds = (history + appointment).mapNotNull { it.sessionId }.toSet()
        val email = normalizeEmail(appointment.clientEmail)
        val clientSessions = sessions.filter { session ->
            session.empresaSlug == appointment.empresaSlug &&
                (session.sessionId in linkedSessionIds || (email != null && normalizeEmail(session.clientEmail) == email))
        }
        val sessionIds = clientSessions.map { it.sessionId }
        val orders = if (sessionIds.isEmpty()) flowOf(emptyList()) else observeOrders(sessionIds)
        return orders.map { DetailSnapshot(appointment, history, clientSessions, it) }
    }

    private fun loadContextOnce(appointment: Appointment) {
        if (contextLoadedFor == appointment.appointmentId) return
        contextLoadedFor = appointment.appointmentId
        appointment.clienteId?.let { id ->
            screenModelScope.launch {
                val client = findClient(id)
                _state.update { it.copy(client = client) }
            }
        }
        refreshCarts()
    }

    fun refreshCarts() {
        val appointment = _state.value.appointment ?: return
        val terms = listOfNotNull(
            appointment.clientEmail?.trim()?.takeIf { it.isNotEmpty() },
            appointment.clientName.trim().takeIf { it.isNotEmpty() },
        ).distinct()
        if (terms.isEmpty()) return
        _state.update { it.copy(cartsLoading = true, cartsError = null) }
        screenModelScope.launch {
            val results = terms
                .map { term -> async { loadOpenCarts(appointment.empresaSlug, null, OpenCartsOrder.MostRecent, term) } }
                .awaitAll()
            val found = results.filterIsInstance<ApiResult.Ok<OpenCartsPage>>()
                .flatMap { it.value.carts }
                .distinctBy { it.carrinhoId }
                .filter { cart -> appointment.isSameClientAs(cart) && cart.status != CartStatus.Cancelled }
            val failure = results.filterIsInstance<ApiResult.Fail>().firstOrNull()
            _state.update {
                it.copy(
                    carts = found,
                    cartsLoading = false,
                    cartsError = if (found.isEmpty() && failure != null) failure.message else null,
                )
            }
        }
    }

    fun toggleCart(cart: OpenCart) {
        val snapshot = _state.value
        if (snapshot.expandedCartId == cart.carrinhoId) {
            _state.update { it.copy(expandedCartId = null) }
            return
        }
        _state.update { it.copy(expandedCartId = cart.carrinhoId) }
        val catalogoUuid = cart.catalogoUuid ?: return
        if (cart.carrinhoId in snapshot.cartItems) return
        val empresaSlug = snapshot.appointment?.empresaSlug ?: return
        _state.update { it.copy(loadingItemsFor = cart.carrinhoId) }
        screenModelScope.launch {
            when (val result = loadCartItems(empresaSlug, catalogoUuid, cart.carrinhoId)) {
                is ApiResult.Ok -> _state.update {
                    it.copy(cartItems = it.cartItems + (cart.carrinhoId to result.value), loadingItemsFor = null)
                }
                is ApiResult.Fail -> _state.update { it.copy(loadingItemsFor = null, error = result.message) }
            }
        }
    }

    fun callWithCart(cart: OpenCart) {
        val snapshot = _state.value
        val appointment = snapshot.appointment ?: return
        if (snapshot.callingCartId != null || !appointment.canCall) return
        val blockReason = cart.callBlockReason(nowMs())
        if (blockReason != null) {
            _state.update { it.copy(error = blockReason) }
            return
        }
        val catalogoUuid = cart.catalogoUuid ?: return
        _state.update { it.copy(callingCartId = cart.carrinhoId, error = null) }
        val request = liveCartSessionRequest(cart, catalogoUuid, appointment.empresaSlug, sellerProvider())
        val notes = SessionClientNotes(
            shop = appointment.clientShop ?: cart.clientName,
            scheduledFor = scheduledForOf(appointment),
        )
        screenModelScope.launch {
            when (val result = createSession(request)) {
                is ApiResult.Ok -> {
                    val now = nowMs()
                    val record = persistSession(request, result.value, now, notes)
                    saveAppointment(
                        appointment.copy(sessionId = record.sessionId, carrinhoId = cart.carrinhoId, updatedAtMs = now),
                    )
                    _state.update { it.copy(callingCartId = null, createdSession = record) }
                }
                is ApiResult.Fail -> _state.update { it.copy(callingCartId = null, error = result.message) }
            }
        }
    }

    fun consumeCreatedSession() {
        _state.update { it.copy(createdSession = null) }
    }

    fun togglePrep(task: PrepTask) {
        val appointment = _state.value.appointment ?: return
        screenModelScope.launch { saveAppointment(appointment.withPrepToggled(task, nowMs())) }
    }

    fun openResult() {
        val appointment = _state.value.appointment ?: return
        if (!appointment.canRegisterResult) return
        _state.update {
            it.copy(
                showResultSheet = true,
                outcome = OutcomeDraft(
                    result = appointment.suggestedResult(),
                    notes = appointment.outcomeNotes.orEmpty(),
                    nextStep = appointment.nextStep.orEmpty(),
                ),
            )
        }
    }

    fun selectResult(result: CallResult) {
        _state.update {
            it.copy(
                outcome = it.outcome.copy(
                    result = result,
                    scheduleReturn = result == CallResult.FollowUp || result == CallResult.NoAnswer,
                ),
            )
        }
    }

    fun setOutcomeNotes(text: String) {
        _state.update { it.copy(outcome = it.outcome.copy(notes = text)) }
    }

    fun setNextStep(text: String) {
        _state.update { it.copy(outcome = it.outcome.copy(nextStep = text)) }
    }

    fun toggleScheduleReturn() {
        _state.update { it.copy(outcome = it.outcome.copy(scheduleReturn = !it.outcome.scheduleReturn)) }
    }

    fun dismissResult() {
        _state.update { it.copy(showResultSheet = false) }
    }

    fun saveResult() {
        val snapshot = _state.value
        val appointment = snapshot.appointment ?: return
        val result = snapshot.outcome.result
        if (result == null) {
            _state.update { it.copy(error = "Escolha como foi a chamada.") }
            return
        }
        val updated = appointment.withResult(result, snapshot.outcome.notes, snapshot.outcome.nextStep, nowMs())
        val followUp = snapshot.outcome.scheduleReturn
        screenModelScope.launch {
            saveAppointment(updated)
            _state.update {
                it.copy(
                    showResultSheet = false,
                    followUpRequestedFor = if (followUp) appointment.appointmentId else null,
                )
            }
        }
    }

    fun consumeFollowUpRequest() {
        _state.update { it.copy(followUpRequestedFor = null) }
    }

    fun requestCancel() {
        if (_state.value.appointment?.canCancel != true) return
        _state.update { it.copy(confirmingCancel = true) }
    }

    fun dismissCancel() {
        _state.update { it.copy(confirmingCancel = false) }
    }

    fun confirmCancel() {
        val appointment = _state.value.appointment ?: return
        screenModelScope.launch {
            saveAppointment(appointment.cancelled(nowMs()))
            _state.update { it.copy(confirmingCancel = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

fun scheduledForOf(appointment: Appointment): String = Instant.fromEpochMilliseconds(appointment.startsAtMs).toString()
