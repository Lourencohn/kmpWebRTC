package app.trovata.cast.feature.sessions

import app.trovata.cast.data.auth.AuthRepository
import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.local.SessionsRepository
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.ui.components.SellerTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class SellerHomeUiState(
    val eyebrow: String = "Sessões",
    val title: String = "Suas sessões",
    val subtitle: String = "",
    val activeTab: SellerTab = SellerTab.Sessoes,
    val recentSessions: List<StoredSessionRecord> = emptyList(),
    val closedToday: List<StoredOrder> = emptyList(),
)

class SessionsViewModel(
    private val sessionsRepository: SessionsRepository,
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeTab = MutableStateFlow(SellerTab.Sessoes)
    private val _home = MutableStateFlow(SellerHomeUiState())
    val home: StateFlow<SellerHomeUiState> = _home.asStateFlow()

    init {
        val (fromMs, untilMs) = todayWindow()
        scope.launch {
            combine(
                sessionsRepository.observeAll(),
                orderRepository.observeBetween(fromMs, untilMs),
                authRepository.user,
                authRepository.activeCompany,
                _activeTab,
            ) { sessions, orders, user, company, tab ->
                SellerHomeUiState(
                    eyebrow = "Sessões",
                    title = greetingFor(user?.name),
                    subtitle = company?.name.orEmpty(),
                    activeTab = tab,
                    recentSessions = sessions,
                    closedToday = orders,
                )
            }.collect { state -> _home.value = state }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun greetingFor(name: String?): String {
        val first = name?.trim()?.split(' ')?.firstOrNull()?.takeIf { it.isNotBlank() }
        return if (first != null) "Olá, $first" else "Suas sessões"
    }

    private fun todayWindow(): Pair<Long, Long> {
        val today = Instant.fromEpochMilliseconds(now())
            .toLocalDateTime(timeZone).date
        val tomorrow = today.plus(DatePeriod(days = 1))
        val fromMs = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val untilMs = tomorrow.atStartOfDayIn(timeZone).toEpochMilliseconds()
        return fromMs to untilMs
    }

    fun selectTab(tab: SellerTab) {
        _activeTab.value = tab
    }
}
