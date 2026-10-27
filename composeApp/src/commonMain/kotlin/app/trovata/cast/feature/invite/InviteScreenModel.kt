package app.trovata.cast.feature.invite

import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.protocol.SessionInfo
import app.trovata.cast.ui.format.whatsAppNumberOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

typealias FetchSessionFn = suspend (token: String) -> ApiResult<SessionInfo>

private const val SESSION_NOT_FOUND = "not_found"
private const val EXPIRED_MESSAGE = "Esse convite expirou. Crie um novo convite para o cliente."
private const val GONE_MESSAGE = "Esse convite não vale mais. Crie um novo convite para o cliente."

data class InviteUiState(
    val isCheckingSession: Boolean = false,
    val callReady: Boolean = false,
    val error: String? = null,
    val clientPhone: String? = null,
) {
    val whatsAppNumber: String? get() = whatsAppNumberOf(clientPhone)
}

class InviteScreenModel(
    private val record: StoredSessionRecord,
    private val fetchSession: FetchSessionFn,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    findClientPhone: suspend (email: String?) -> String? = { null },
) : ScreenModel {

    private val _state = MutableStateFlow(InviteUiState(clientPhone = record.clientPhone))
    val state: StateFlow<InviteUiState> = _state.asStateFlow()

    init {
        if (record.clientPhone == null) {
            screenModelScope.launch {
                val phone = findClientPhone(record.clientEmail)
                _state.update { it.copy(clientPhone = phone) }
            }
        }
    }

    fun startCall() {
        if (_state.value.isCheckingSession) return
        if (record.expiresAtMs < nowMs()) {
            _state.update { it.copy(error = EXPIRED_MESSAGE) }
            return
        }
        _state.update { it.copy(isCheckingSession = true, error = null) }
        screenModelScope.launch {
            val result = fetchSession(record.token)
            val gone = result is ApiResult.Fail && result.code == SESSION_NOT_FOUND
            _state.update {
                it.copy(
                    isCheckingSession = false,
                    callReady = !gone,
                    error = if (gone) GONE_MESSAGE else null,
                )
            }
        }
    }

    fun consumeCallReady() {
        _state.update { it.copy(callReady = false) }
    }
}
