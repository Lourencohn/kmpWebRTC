package app.trovata.cast.feature.agenda

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.AppointmentOwner
import app.trovata.cast.data.local.AppointmentPriority
import app.trovata.cast.data.local.AppointmentStatus
import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.remote.sfa.OpenCart
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

const val FirstSlotMinute: Int = 7 * 60
const val LastSlotMinute: Int = 20 * 60 + 30
const val SlotStepMinutes: Int = 30
const val NudgeStepMinutes: Int = 15
const val DayOptionsCount: Int = 21

val DurationOptions: List<Int> = listOf(15, 30, 45, 60, 90)

val ObjectiveSuggestions: List<String> = listOf(
    "Reposição",
    "Apresentar coleção nova",
    "Retomar carrinho parado",
    "Negociar condições",
    "Primeiro contato",
)

data class AppointmentClient(
    val clienteId: Long?,
    val name: String,
    val shop: String?,
    val document: String?,
    val email: String?,
    val phone: String?,
)

fun CatalogClient.toAppointmentClient(): AppointmentClient = AppointmentClient(
    clienteId = id,
    name = name,
    shop = legalName?.takeIf { it != name },
    document = document,
    email = email,
    phone = phone,
)

fun OpenCart.toAppointmentClient(): AppointmentClient = AppointmentClient(
    clienteId = clienteId,
    name = title,
    shop = null,
    document = null,
    email = clientEmail,
    phone = null,
)

fun Appointment.toAppointmentClient(): AppointmentClient = AppointmentClient(
    clienteId = clienteId,
    name = clientName,
    shop = clientShop,
    document = clientDocument,
    email = clientEmail,
    phone = clientPhone,
)

data class AppointmentFormSpec(
    val appointmentId: String? = null,
    val followUpOfId: String? = null,
    val reschedule: Boolean = false,
    val client: AppointmentClient? = null,
    val carrinhoId: Long? = null,
    val objective: String? = null,
    val epochDay: Int? = null,
    val minuteOfDay: Int? = null,
    val notes: String? = null,
)

enum class FormMode(val title: String, val saveLabel: String) {
    New("Agendar chamada", "Agendar"),
    Edit("Editar compromisso", "Salvar"),
    Reschedule("Reagendar chamada", "Reagendar"),
    FollowUp("Agendar retorno", "Agendar retorno"),
}

enum class SlotState { Free, Busy }

data class TimeSlot(val minuteOfDay: Int, val state: SlotState)

data class AppointmentFormUiState(
    val mode: FormMode = FormMode.New,
    val isLoading: Boolean = true,
    val client: AppointmentClient? = null,
    val clientLocked: Boolean = false,
    val clientQuery: String = "",
    val clientResults: List<CatalogClient> = emptyList(),
    val date: LocalDate,
    val minuteOfDay: Int? = null,
    val durationMinutes: Int = 30,
    val objective: String = "",
    val notes: String = "",
    val priority: AppointmentPriority = AppointmentPriority.Normal,
    val carrinhoId: Long? = null,
    val others: List<Appointment> = emptyList(),
    val original: Appointment? = null,
    val followUpOf: Appointment? = null,
    val nowMs: Long,
    val timeZone: TimeZone,
    val showMonthPicker: Boolean = false,
    val problem: String? = null,
    val isSaving: Boolean = false,
    val savedAppointmentId: String? = null,
) {
    val today: LocalDate get() = localDateAt(nowMs, timeZone)

    val dayOptions: List<LocalDate>
        get() {
            val days = (0 until DayOptionsCount).map { today.plus(DatePeriod(days = it)) }
            return if (date in days || date < today) days else days + date
        }

    val slots: List<TimeSlot>
        get() {
            val nowMinute = if (date == today) minuteOfDayAt(nowMs, timeZone) else Int.MIN_VALUE
            val grid = (FirstSlotMinute..LastSlotMinute step SlotStepMinutes).toMutableList()
            minuteOfDay?.let { selected -> if (selected !in grid) grid += selected }
            return grid.sorted()
                .filter { it >= nowMinute || it == minuteOfDay }
                .map { TimeSlot(it, if (isBusy(it)) SlotState.Busy else SlotState.Free) }
        }

    val startsAtMs: Long? get() = minuteOfDay?.let { date.atMinuteOfDay(it, timeZone) }

    val clientWithoutEmail: Boolean get() = client != null && client.email.isNullOrBlank()

    val conflictPreview: String?
        get() {
            val start = startsAtMs ?: return null
            val problem = slotProblem(slotRequest(start), others, nowMs, timeZone)
            return when (problem) {
                is SlotProblem.Overlaps, is SlotProblem.SameClientSameDay -> problem.message(timeZone)
                else -> null
            }
        }

    fun loadOf(day: LocalDate): Int = others.count { it.isOpen && localDateAt(it.startsAtMs, timeZone) == day }

    fun slotRequest(start: Long?): SlotRequest = SlotRequest(
        appointmentId = original?.appointmentId,
        clienteId = client?.clienteId,
        clientName = client?.name,
        clientEmail = client?.email,
        startsAtMs = start,
        durationMinutes = durationMinutes,
        requireFuture = original == null || mode == FormMode.Reschedule || start != original.startsAtMs,
    )

    private fun isBusy(minute: Int): Boolean {
        val start = date.atMinuteOfDay(minute, timeZone)
        val end = start + durationMinutes * MinuteMs
        return others.any { it.isOpen && it.appointmentId != original?.appointmentId && start < it.endsAtMs && it.startsAtMs < end }
    }
}

class AppointmentFormScreenModel(
    private val spec: AppointmentFormSpec,
    private val ownerProvider: () -> AppointmentOwner?,
    private val loadAppointments: suspend (AppointmentOwner) -> List<Appointment>,
    private val searchClients: suspend (String) -> List<CatalogClient>,
    private val saveAppointment: suspend (Appointment) -> Unit,
    private val loadCallInterests: suspend (sessionId: String) -> List<CallInterest> = { emptyList() },
    private val newId: () -> String = ::newAppointmentId,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ScreenModel {

    private val _state = MutableStateFlow(
        AppointmentFormUiState(
            mode = modeOf(spec),
            date = spec.epochDay?.let { LocalDate.fromEpochDays(it) } ?: localDateAt(nowMs(), timeZone),
            minuteOfDay = spec.minuteOfDay,
            client = spec.client,
            clientLocked = spec.client != null && spec.carrinhoId != null,
            carrinhoId = spec.carrinhoId,
            objective = spec.objective.orEmpty(),
            notes = spec.notes.orEmpty(),
            nowMs = nowMs(),
            timeZone = timeZone,
        ),
    )
    val state: StateFlow<AppointmentFormUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        screenModelScope.launch { load() }
        if (spec.client == null && spec.appointmentId == null && spec.followUpOfId == null) searchNow("")
    }

    private suspend fun load() {
        val owner = ownerProvider()
        if (owner == null) {
            _state.update { it.copy(isLoading = false, problem = "Selecione uma empresa para agendar chamadas.") }
            return
        }
        val all = loadAppointments(owner)
        val original = spec.appointmentId?.let { id -> all.firstOrNull { it.appointmentId == id } }
        val previous = spec.followUpOfId?.let { id -> all.firstOrNull { it.appointmentId == id } }
        val others = all.filter { it.appointmentId != original?.appointmentId }
        val leftOutFromLastCall = previous?.sessionId
            ?.takeIf { spec.notes == null }
            ?.let { leftOutNotes(loadCallInterests(it)) }
        _state.update { current ->
            when {
                original != null -> current.copy(
                    client = original.toAppointmentClient(),
                    clientLocked = true,
                    date = localDateAt(original.startsAtMs, timeZone),
                    minuteOfDay = minuteOfDayAt(original.startsAtMs, timeZone),
                    durationMinutes = original.durationMinutes,
                    objective = original.objective.orEmpty(),
                    notes = original.notes.orEmpty(),
                    priority = original.priority,
                    carrinhoId = original.carrinhoId,
                    original = original,
                    others = others,
                    isLoading = false,
                )
                previous != null -> current.copy(
                    client = previous.toAppointmentClient(),
                    clientLocked = true,
                    date = nextBusinessDay(localDateAt(nowMs(), timeZone)),
                    minuteOfDay = minuteOfDayAt(previous.startsAtMs, timeZone),
                    durationMinutes = previous.durationMinutes,
                    objective = previous.nextStep ?: previous.objective.orEmpty(),
                    notes = leftOutFromLastCall ?: current.notes,
                    priority = previous.priority,
                    carrinhoId = previous.carrinhoId,
                    followUpOf = previous,
                    others = others,
                    isLoading = false,
                )
                else -> current.copy(others = others, isLoading = false)
            }
        }
    }

    fun setClientQuery(query: String) {
        _state.update { it.copy(clientQuery = query) }
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(query)
        }
    }

    fun pickClient(client: CatalogClient) {
        _state.update { it.copy(client = client.toAppointmentClient(), problem = null) }
    }

    fun clearClient() {
        if (_state.value.clientLocked) return
        _state.update { it.copy(client = null, problem = null) }
        searchNow(_state.value.clientQuery)
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(date = date, showMonthPicker = false, problem = null) }
    }

    fun toggleMonthPicker() {
        _state.update { it.copy(showMonthPicker = !it.showMonthPicker) }
    }

    fun selectSlot(minuteOfDay: Int) {
        _state.update { it.copy(minuteOfDay = minuteOfDay, problem = null) }
    }

    fun nudgeTime(forward: Boolean) {
        _state.update {
            val current = it.minuteOfDay ?: return@update it
            val next = current + if (forward) NudgeStepMinutes else -NudgeStepMinutes
            if (next < 0 || next > 23 * 60 + 45) it else it.copy(minuteOfDay = next, problem = null)
        }
    }

    fun selectDuration(minutes: Int) {
        _state.update { it.copy(durationMinutes = minutes, problem = null) }
    }

    fun setObjective(text: String) {
        _state.update { it.copy(objective = text) }
    }

    fun applyObjective(suggestion: String) {
        _state.update { it.copy(objective = suggestion) }
    }

    fun setNotes(text: String) {
        _state.update { it.copy(notes = text) }
    }

    fun togglePriority() {
        _state.update {
            val next = if (it.priority == AppointmentPriority.High) AppointmentPriority.Normal else AppointmentPriority.High
            it.copy(priority = next)
        }
    }

    fun save() {
        val snapshot = _state.value.copy(nowMs = nowMs())
        if (snapshot.isSaving || snapshot.isLoading) return
        val owner = ownerProvider()
        if (owner == null) {
            _state.update { it.copy(problem = "Selecione uma empresa para agendar chamadas.") }
            return
        }
        val start = snapshot.startsAtMs
        val problem = slotProblem(snapshot.slotRequest(start), snapshot.others, snapshot.nowMs, timeZone)
        if (problem != null || start == null) {
            _state.update { it.copy(problem = (problem ?: SlotProblem.MissingTime).message(timeZone)) }
            return
        }
        val client = snapshot.client ?: return
        val appointment = buildAppointment(snapshot, owner, client, start)
        _state.update { it.copy(isSaving = true, problem = null) }
        screenModelScope.launch {
            saveAppointment(appointment)
            _state.update { it.copy(isSaving = false, savedAppointmentId = appointment.appointmentId) }
        }
    }

    private fun buildAppointment(
        snapshot: AppointmentFormUiState,
        owner: AppointmentOwner,
        client: AppointmentClient,
        start: Long,
    ): Appointment {
        val objective = snapshot.objective.trim().takeIf { it.isNotEmpty() }
        val notes = snapshot.notes.trim().takeIf { it.isNotEmpty() }
        val original = snapshot.original
        if (original != null) {
            val moved = start != original.startsAtMs
            return original.copy(
                startsAtMs = start,
                durationMinutes = snapshot.durationMinutes,
                objective = objective,
                notes = notes,
                priority = snapshot.priority,
                rescheduleCount = original.rescheduleCount + if (moved) 1 else 0,
                updatedAtMs = snapshot.nowMs,
            )
        }
        return Appointment(
            appointmentId = newId(),
            empresaSlug = owner.empresaSlug,
            sellerId = owner.sellerId,
            clienteId = client.clienteId,
            clientName = client.name,
            clientShop = client.shop,
            clientDocument = client.document,
            clientEmail = client.email,
            clientPhone = client.phone,
            startsAtMs = start,
            durationMinutes = snapshot.durationMinutes,
            objective = objective,
            notes = notes,
            priority = snapshot.priority,
            status = AppointmentStatus.Scheduled,
            outcome = null,
            outcomeNotes = null,
            nextStep = null,
            prepDone = emptySet(),
            carrinhoId = snapshot.carrinhoId,
            sessionId = null,
            followUpOfId = snapshot.followUpOf?.appointmentId,
            rescheduleCount = 0,
            createdAtMs = snapshot.nowMs,
            updatedAtMs = snapshot.nowMs,
        )
    }

    private fun searchNow(query: String) {
        searchJob?.cancel()
        searchJob = screenModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(query: String) {
        val results = searchClients(query.trim())
        _state.update { it.copy(clientResults = results.take(CLIENT_RESULTS_LIMIT)) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 180L
        const val CLIENT_RESULTS_LIMIT = 8
    }
}

private fun modeOf(spec: AppointmentFormSpec): FormMode = when {
    spec.appointmentId != null && spec.reschedule -> FormMode.Reschedule
    spec.appointmentId != null -> FormMode.Edit
    spec.followUpOfId != null -> FormMode.FollowUp
    else -> FormMode.New
}

fun newAppointmentId(): String =
    "apt-" + (1..16).map { ("abcdefghijklmnopqrstuvwxyz0123456789").random() }.joinToString("")
