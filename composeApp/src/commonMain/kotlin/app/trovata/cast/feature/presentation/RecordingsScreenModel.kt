package app.trovata.cast.feature.presentation

import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.local.StoredPresentation
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

const val PresentationRetentionDays: Int = 30
private const val DayMs: Long = 24L * 60 * 60 * 1000
private const val ExpiringSoonDays: Int = 3
private const val ClockTickMs: Long = 60_000

enum class RecordingAvailability { Available, ExpiringSoon, Expired }

data class RecordingItem(
    val presentation: StoredPresentation,
    val availability: RecordingAvailability,
    val recordedLabel: String,
    val availabilityLabel: String,
    val durationLabel: String,
    val actionsLabel: String,
) {
    val presentationId: String get() = presentation.presentationId
    val catalogoNome: String get() = presentation.catalogoNome
    val shareUrl: String get() = presentation.shareUrl
    val shareMessage: String get() = presentationShareMessage(presentation.catalogoNome, presentation.shareUrl)
}

data class RecordingsUiState(
    val companyName: String = "",
    val available: List<RecordingItem> = emptyList(),
    val expired: List<RecordingItem> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && available.isEmpty() && expired.isEmpty()

    val subtitle: String
        get() = when {
            isLoading -> "Carregando suas gravações..."
            isEmpty -> "Grave uma vez e mande para vários clientes"
            else -> listOfNotNull(
                plural(available.size, "disponível", "disponíveis"),
                expired.size.takeIf { it > 0 }?.let { plural(it, "expirada", "expiradas") },
            ).joinToString(" · ")
        }
}

private data class CompanyRecordings(val company: Company?, val presentations: List<StoredPresentation>)

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsScreenModel(
    companies: Flow<Company?>,
    observe: (empresaSlug: String) -> Flow<List<StoredPresentation>>,
    private val removePresentation: suspend (presentationId: String) -> Unit,
    clock: Flow<Long> = minuteClock(),
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ScreenModel {

    private val _state = MutableStateFlow(RecordingsUiState())
    val state: StateFlow<RecordingsUiState> = _state.asStateFlow()

    init {
        val recordings = companies.flatMapLatest { company ->
            val slug = company?.slug?.takeIf { it.isNotBlank() }
            if (slug == null) {
                flowOf(CompanyRecordings(company, emptyList()))
            } else {
                observe(slug).map { CompanyRecordings(company, it) }
            }
        }
        screenModelScope.launch {
            combine(recordings, clock) { current, nowMs -> buildState(current, nowMs) }
                .collect { _state.value = it }
        }
    }

    fun removeFromList(presentationId: String) {
        screenModelScope.launch { removePresentation(presentationId) }
    }

    private fun buildState(current: CompanyRecordings, nowMs: Long): RecordingsUiState {
        val items = current.presentations
            .sortedByDescending { it.recordedAtMs }
            .map { toItem(it, nowMs) }
        return RecordingsUiState(
            companyName = current.company?.name.orEmpty(),
            available = items.filter { it.availability != RecordingAvailability.Expired },
            expired = items.filter { it.availability == RecordingAvailability.Expired },
            isLoading = false,
        )
    }

    private fun toItem(presentation: StoredPresentation, nowMs: Long): RecordingItem {
        val expiresAtMs = presentation.uploadedAtMs + PresentationRetentionDays * DayMs
        val today = local(nowMs)
        val expiresAt = local(expiresAtMs)
        val daysLeft = expiresAt.date.toEpochDays() - today.date.toEpochDays()
        val availability = when {
            nowMs >= expiresAtMs -> RecordingAvailability.Expired
            daysLeft <= ExpiringSoonDays -> RecordingAvailability.ExpiringSoon
            else -> RecordingAvailability.Available
        }
        return RecordingItem(
            presentation = presentation,
            availability = availability,
            recordedLabel = recordedLabel(local(presentation.recordedAtMs), today),
            availabilityLabel = when (availability) {
                RecordingAvailability.Expired -> "Expirou em ${dayMonth(expiresAt)}"
                RecordingAvailability.ExpiringSoon -> when (daysLeft) {
                    0 -> "Expira hoje"
                    1 -> "Expira amanhã"
                    else -> "Expira em $daysLeft dias"
                }
                RecordingAvailability.Available -> "Disponível até ${dayMonth(expiresAt)}"
            },
            durationLabel = formatDuration(presentation.durationMs),
            actionsLabel = plural(presentation.eventCount, "ação", "ações"),
        )
    }

    private fun local(ms: Long): LocalDateTime = Instant.fromEpochMilliseconds(ms).toLocalDateTime(timeZone)
}

fun minuteClock(nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }): Flow<Long> = flow {
    while (true) {
        emit(nowMs())
        delay(ClockTickMs)
    }
}

private fun recordedLabel(recordedAt: LocalDateTime, today: LocalDateTime): String {
    val time = "${pad(recordedAt.hour)}:${pad(recordedAt.minute)}"
    return when (today.date.toEpochDays() - recordedAt.date.toEpochDays()) {
        0 -> "Gravada hoje às $time"
        1 -> "Gravada ontem às $time"
        else -> {
            val year = if (recordedAt.year != today.year) "/${recordedAt.year}" else ""
            "Gravada em ${dayMonth(recordedAt)}$year às $time"
        }
    }
}

private fun dayMonth(dateTime: LocalDateTime): String = "${pad(dateTime.dayOfMonth)}/${pad(dateTime.monthNumber)}"

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${pad((totalSeconds / 60).toInt())}:${pad((totalSeconds % 60).toInt())}"
}

private fun pad(value: Int): String = value.toString().padStart(2, '0')

private fun plural(count: Int, singular: String, pluralForm: String): String =
    "$count ${if (count == 1) singular else pluralForm}"
