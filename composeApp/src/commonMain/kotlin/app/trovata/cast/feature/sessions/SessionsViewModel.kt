package app.trovata.cast.feature.sessions

import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.sample.LiveWaitingSession
import app.trovata.cast.data.sample.SampleSessions
import app.trovata.cast.data.sample.SellerHomeData
import app.trovata.cast.data.sample.SessionChecklistItem
import app.trovata.cast.data.sample.SessionPrepData
import app.trovata.cast.ui.components.SellerTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class SellerHomeUiState(
    val data: SellerHomeData,
    val activeTab: SellerTab,
    val closedToday: List<StoredOrder> = emptyList(),
)

data class IncomingCallUiState(
    val session: LiveWaitingSession,
    val accepted: Boolean,
    val declined: Boolean,
    val rescheduled: Boolean,
)

data class SessionPrepUiState(
    val data: SessionPrepData,
    val ready: Boolean,
)

class SessionsViewModel(
    private val orderRepository: OrderRepository? = null,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _home = MutableStateFlow(
        SellerHomeUiState(
            data = SampleSessions.home,
            activeTab = SellerTab.Sessoes,
        ),
    )
    val home: StateFlow<SellerHomeUiState> = _home.asStateFlow()

    init {
        val repo = orderRepository
        if (repo != null) {
            scope.launch {
                val (fromMs, untilMs) = todayWindow()
                repo.observeBetween(fromMs, untilMs)
                    .flowOn(Dispatchers.Default)
                    .collect { orders ->
                        _home.update { it.copy(closedToday = orders) }
                    }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun todayWindow(): Pair<Long, Long> {
        val today = Instant.fromEpochMilliseconds(now())
            .toLocalDateTime(timeZone).date
        val tomorrow = today.plus(DatePeriod(days = 1))
        val fromMs = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val untilMs = tomorrow.atStartOfDayIn(timeZone).toEpochMilliseconds()
        return fromMs to untilMs
    }

    private val _incoming = MutableStateFlow(
        IncomingCallUiState(
            session = SampleSessions.incoming,
            accepted = false,
            declined = false,
            rescheduled = false,
        ),
    )
    val incoming: StateFlow<IncomingCallUiState> = _incoming.asStateFlow()

    private val _prep = MutableStateFlow(
        SessionPrepUiState(
            data = SampleSessions.prep,
            ready = SampleSessions.prep.checklist.all { it.ready },
        ),
    )
    val prep: StateFlow<SessionPrepUiState> = _prep.asStateFlow()

    fun selectTab(tab: SellerTab) {
        _home.update { it.copy(activeTab = tab) }
    }

    fun acceptIncoming() {
        _incoming.update { it.copy(accepted = true, declined = false, rescheduled = false) }
    }

    fun declineIncoming() {
        _incoming.update { it.copy(declined = true, accepted = false, rescheduled = false) }
    }

    fun rescheduleIncoming() {
        _incoming.update { it.copy(rescheduled = true, accepted = false, declined = false) }
    }

    fun toggleChecklistItem(itemId: String) {
        _prep.update { state ->
            val next = state.data.checklist.map { item ->
                if (item.id == itemId) item.copy(ready = !item.ready) else item
            }
            state.copy(
                data = state.data.copy(checklist = next),
                ready = next.all(SessionChecklistItem::ready),
            )
        }
    }
}
