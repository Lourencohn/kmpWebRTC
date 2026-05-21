package app.trovata.cast.feature.sessions

import app.trovata.cast.data.sample.LiveWaitingSession
import app.trovata.cast.data.sample.SampleSessions
import app.trovata.cast.data.sample.SellerHomeData
import app.trovata.cast.data.sample.SessionChecklistItem
import app.trovata.cast.data.sample.SessionPrepData
import app.trovata.cast.ui.components.SellerTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SellerHomeUiState(
    val data: SellerHomeData,
    val activeTab: SellerTab,
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

class SessionsViewModel {
    private val _home = MutableStateFlow(
        SellerHomeUiState(
            data = SampleSessions.home,
            activeTab = SellerTab.Sessoes,
        ),
    )
    val home: StateFlow<SellerHomeUiState> = _home.asStateFlow()

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
