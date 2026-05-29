package app.trovata.cast.feature.clients

import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.local.ClientsRepository
import app.trovata.cast.data.sync.CatalogSyncCoordinator
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientsUiState(
    val query: String = "",
    val results: List<CatalogClient> = emptyList(),
    val total: Long = 0,
    val isLoading: Boolean = true,
    val syncing: Boolean = false,
)

class ClientsScreenModel(
    private val clientsRepository: ClientsRepository,
    syncCoordinator: CatalogSyncCoordinator,
) : ScreenModel {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(ClientsUiState())
    val state: StateFlow<ClientsUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            combine(
                _query,
                clientsRepository.observeCount(),
                syncCoordinator.isSyncing,
            ) { query, total, syncing ->
                Triple(query, total, syncing)
            }.collectLatest { (query, total, syncing) ->
                if (query.isNotEmpty()) delay(180)
                val results = clientsRepository.search(query)
                _state.update {
                    it.copy(results = results, total = total, syncing = syncing, isLoading = false)
                }
            }
        }
    }

    fun setQuery(query: String) {
        _query.value = query
        _state.update { it.copy(query = query) }
    }
}
