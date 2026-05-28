package app.trovata.cast.feature.clients

import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.local.ClientsRepository
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientsUiState(
    val query: String = "",
    val results: List<CatalogClient> = emptyList(),
    val total: Long = 0,
    val isLoading: Boolean = true,
)

class ClientsScreenModel(
    private val clientsRepository: ClientsRepository,
) : ScreenModel {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(ClientsUiState())
    val state: StateFlow<ClientsUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            _state.update { it.copy(total = clientsRepository.count()) }
        }
        screenModelScope.launch {
            _query.collectLatest { query ->
                if (query.isNotEmpty()) delay(180)
                val results = clientsRepository.search(query)
                _state.update { it.copy(results = results, isLoading = false) }
            }
        }
    }

    fun setQuery(query: String) {
        _query.value = query
        _state.update { it.copy(query = query) }
    }
}
