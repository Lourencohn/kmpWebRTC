package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentOwner
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.data.remote.sfa.OpenCartsOrder
import app.trovata.cast.feature.carts.LoadOpenCartsFn
import app.trovata.cast.feature.presentation.minuteClock
import app.trovata.cast.ui.format.hourMinuteAt
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class AgendaView(val label: String) {
    Day("Dia"),
    Week("Semana"),
    Month("Mês"),
}

data class AgendaDayLoad(
    val total: Int,
    val pending: Int,
    val done: Int,
    val priority: Boolean,
)

data class AgendaUiState(
    val view: AgendaView = AgendaView.Week,
    val selectedDate: LocalDate,
    val nowMs: Long,
    val timeZone: TimeZone,
    val appointments: List<Appointment> = emptyList(),
    val hasOwner: Boolean = true,
    val isLoading: Boolean = true,
    val openCarts: List<OpenCart> = emptyList(),
    val cartsLoading: Boolean = false,
    val cartsError: String? = null,
) {
    private val byDate: Map<LocalDate, List<Appointment>> by lazy {
        appointments.groupBy { localDateAt(it.startsAtMs, timeZone) }
            .mapValues { (_, list) -> list.sortedBy { it.startsAtMs } }
    }

    val today: LocalDate get() = localDateAt(nowMs, timeZone)

    val week: List<LocalDate> get() = weekOf(selectedDate)

    val monthGrid: List<LocalDate> get() = monthGridOf(selectedDate)

    val periodLabel: String
        get() = when (view) {
            AgendaView.Day -> longDayLabel(selectedDate, today)
            AgendaView.Week -> weekRangeLabel(week)
            AgendaView.Month -> monthLabel(selectedDate)
        }

    val isShowingToday: Boolean
        get() = when (view) {
            AgendaView.Day -> selectedDate == today
            AgendaView.Week -> today in week
            AgendaView.Month -> selectedDate.year == today.year && selectedDate.month == today.month
        }

    val summary: AgendaSummary get() = summarize(appointments, nowMs, timeZone)

    val pending: List<Appointment> get() = pendingOf(appointments, nowMs)

    val followUps: List<Appointment> get() = followUpsOf(appointments)

    val cartsToCall: List<OpenCart> get() = cartsNeedingCall(openCarts, appointments, nowMs)

    val todayAppointments: List<Appointment>
        get() = appointmentsOn(today).filter { it.status != AppointmentStatus.Cancelled }

    val subtitle: String
        get() {
            if (isLoading) return "Carregando sua agenda..."
            val current = summary
            val next = current.next
            return when {
                next != null && localDateAt(next.startsAtMs, timeZone) == today ->
                    "${plural(current.today, "chamada", "chamadas")} hoje · próxima às ${startTime(next)}"
                current.today > 0 -> "${plural(current.today, "chamada", "chamadas")} hoje"
                next != null -> "Nada hoje · próxima ${relativeDayLabel(localDateAt(next.startsAtMs, timeZone), today).lowercase()}"
                else -> "Nenhuma chamada marcada"
            }
        }

    fun appointmentsOn(date: LocalDate): List<Appointment> = byDate[date].orEmpty()

    fun loadOf(date: LocalDate): AgendaDayLoad {
        val active = appointmentsOn(date).filter { it.status != AppointmentStatus.Cancelled }
        return AgendaDayLoad(
            total = active.size,
            pending = active.count { it.agendaStatus(nowMs) == AgendaStatus.Pending },
            done = active.count { it.agendaStatus(nowMs) == AgendaStatus.Done },
            priority = active.any { it.isPriority && it.isOpen },
        )
    }

    fun openCartFor(appointment: Appointment): OpenCart? =
        openCarts.firstOrNull { it.status == CartStatus.Editing && it.hasItems && appointment.isSameClientAs(it) }

    private fun startTime(appointment: Appointment): String =
        hourMinuteAt(appointment.startsAtMs, timeZone)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaScreenModel(
    owners: Flow<AppointmentOwner?>,
    observe: (AppointmentOwner) -> Flow<List<Appointment>>,
    private val loadOpenCarts: LoadOpenCartsFn,
    clock: Flow<Long> = minuteClock(),
    nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ScreenModel {

    private val _state = MutableStateFlow(
        AgendaUiState(
            selectedDate = localDateAt(nowMs(), timeZone),
            nowMs = nowMs(),
            timeZone = timeZone,
        ),
    )
    val state: StateFlow<AgendaUiState> = _state.asStateFlow()

    private var owner: AppointmentOwner? = null
    private var cartsJob: Job? = null

    init {
        screenModelScope.launch {
            owners.distinctUntilChanged()
                .onEach { current ->
                    owner = current
                    refreshCarts()
                }
                .flatMapLatest { current -> if (current == null) flowOf(null) else observe(current) }
                .collect { list ->
                    _state.update {
                        it.copy(appointments = list.orEmpty(), hasOwner = list != null, isLoading = false)
                    }
                }
        }
        screenModelScope.launch {
            clock.collect { now -> _state.update { it.copy(nowMs = now) } }
        }
    }

    fun selectView(view: AgendaView) {
        _state.update { it.copy(view = view) }
    }

    fun selectDate(date: LocalDate) {
        _state.update {
            val view = if (it.view == AgendaView.Week) AgendaView.Day else it.view
            it.copy(selectedDate = date, view = view)
        }
    }

    fun previousPeriod() = shiftPeriod(forward = false)

    fun nextPeriod() = shiftPeriod(forward = true)

    fun goToToday() {
        _state.update { it.copy(selectedDate = it.today) }
    }

    fun refreshCarts() {
        val current = owner
        if (current == null) {
            _state.update { it.copy(openCarts = emptyList(), cartsLoading = false, cartsError = null) }
            return
        }
        _state.update { it.copy(cartsLoading = true, cartsError = null) }
        cartsJob?.cancel()
        cartsJob = screenModelScope.launch {
            when (val result = loadOpenCarts(current.empresaSlug, CartStatus.Editing, OpenCartsOrder.MostRecent, null)) {
                is ApiResult.Ok -> _state.update {
                    it.copy(openCarts = result.value.carts, cartsLoading = false, cartsError = null)
                }
                is ApiResult.Fail -> _state.update {
                    it.copy(cartsLoading = false, cartsError = result.message)
                }
            }
        }
    }

    private fun shiftPeriod(forward: Boolean) {
        _state.update {
            val step = when (it.view) {
                AgendaView.Day -> DatePeriod(days = 1)
                AgendaView.Week -> DatePeriod(days = 7)
                AgendaView.Month -> DatePeriod(months = 1)
            }
            val next = if (forward) it.selectedDate.plus(step) else it.selectedDate.minus(step)
            it.copy(selectedDate = next)
        }
    }
}

internal fun plural(count: Int, singular: String, pluralForm: String): String =
    "$count ${if (count == 1) singular else pluralForm}"
