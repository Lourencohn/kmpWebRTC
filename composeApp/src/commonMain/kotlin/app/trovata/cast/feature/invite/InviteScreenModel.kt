package app.trovata.cast.feature.invite

import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.protocol.SessionInfo
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
)

class InviteScreenModel(
    private val record: StoredSessionRecord,
    private val fetchSession: FetchSessionFn,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ScreenModel {

    private val _state = MutableStateFlow(InviteUiState())
    val state: StateFlow<InviteUiState> = _state.asStateFlow()

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
