package app.trovata.cast.feature.carts

import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.SessionsApiResult
import app.trovata.cast.data.remote.sfa.OpenCartsPage
import app.trovata.cast.data.remote.sfa.SfaApiResult
import app.trovata.cast.feature.catalog.CreateSessionFn
import app.trovata.cast.feature.catalog.PersistSessionFn
import app.trovata.cast.protocol.SessionCreateRequest
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class OpenCartsFilter(val label: String) {
    Retomaveis("Retomáveis"),
    Todos("Todos"),
}

data class OpenCartsUiState(
    val carts: List<OpenCart> = emptyList(),
    val filter: OpenCartsFilter = OpenCartsFilter.Retomaveis,
    val order: OpenCartsOrder = OpenCartsOrder.MaisRecentes,
    val search: String = "",
    val companyName: String = "",
    val nowMs: Long = 0,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val callingCartId: Long? = null,
    val error: String? = null,
    val createdSession: StoredSessionRecord? = null,
) {
    val retomaveis: List<OpenCart>
        get() = carts.filter { it.temItens && it.podeChamarAoVivo(nowMs) }

    val visibleCarts: List<OpenCart>
        get() = when (filter) {
            OpenCartsFilter.Retomaveis -> retomaveis
            OpenCartsFilter.Todos -> carts
        }

    val valorRetomavelCents: Long
        get() = retomaveis.sumOf { it.valorTotalCents ?: 0L }
}

typealias LoadOpenCartsFn = suspend (
    empresaSlug: String,
    situacao: CartSituacao?,
    order: OpenCartsOrder,
    search: String?,
) -> SfaApiResult<OpenCartsPage>

class OpenCartsScreenModel(
    private val loadOpenCarts: LoadOpenCartsFn,
    private val createSession: CreateSessionFn,
    private val persistSession: PersistSessionFn,
    private val empresaSlugProvider: () -> String,
    private val companyNameProvider: () -> String = { "" },
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ScreenModel {

    private val _state = MutableStateFlow(
        OpenCartsUiState(
            companyName = companyNameProvider(),
            nowMs = nowMs(),
        ),
    )
    val state: StateFlow<OpenCartsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        val empresaSlug = empresaSlugProvider()
        if (empresaSlug.isBlank()) {
            _state.update {
                it.copy(isLoading = false, error = "Selecione uma empresa para ver os carrinhos")
            }
            return
        }
        val snapshot = _state.value
        val query = snapshot.search.trim().takeIf { it.isNotBlank() }
        _state.update { it.copy(isLoading = true, error = null, companyName = companyNameProvider()) }
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            val result = loadOpenCarts(empresaSlug, situacaoFiltrada(snapshot.filter), snapshot.order, query)
            when (result) {
                is SfaApiResult.Ok -> _state.update {
                    it.copy(
                        carts = result.value.carts,
                        total = result.value.total,
                        hasMore = result.value.hasMore,
                        nowMs = nowMs(),
                        isLoading = false,
                        error = null,
                    )
                }
                is SfaApiResult.Fail -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun setSearch(query: String) {
        _state.update { it.copy(search = query) }
    }

    fun submitSearch() = refresh()

    fun setFilter(filter: OpenCartsFilter) {
        if (_state.value.filter == filter) return
        _state.update { it.copy(filter = filter) }
        refresh()
    }

    fun toggleOrder() {
        val next = when (_state.value.order) {
            OpenCartsOrder.MaisRecentes -> OpenCartsOrder.ParadosHaMaisTempo
            OpenCartsOrder.ParadosHaMaisTempo -> OpenCartsOrder.MaisRecentes
        }
        _state.update { it.copy(order = next) }
        refresh()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun consumeCreatedSession() {
        _state.update { it.copy(createdSession = null) }
    }

    fun callLive(cart: OpenCart, sellerId: String = "seller", sellerName: String = "Vendedor") {
        val snapshot = _state.value
        if (snapshot.callingCartId != null) return
        val impedimento = cart.impedimentoParaChamar(nowMs())
        if (impedimento != null) {
            _state.update { it.copy(error = impedimento) }
            return
        }
        val catalogoUuid = cart.catalogoUuid ?: return
        _state.update { it.copy(callingCartId = cart.carrinhoId, error = null) }

        val request = SessionCreateRequest(
            empresaSlug = empresaSlugProvider(),
            catalogoUuid = catalogoUuid,
            sellerId = sellerId,
            sellerName = sellerName,
            catalogoNome = cart.catalogoNome,
            catalogoLinkId = cart.catalogoLinkId,
            carrinhoId = cart.carrinhoId,
            clientName = cart.clienteNome,
            clientEmail = cart.clienteEmail,
        )
        val notes = SessionClientNotes(shop = cart.clienteNome)
        screenModelScope.launch {
            when (val result = createSession(request)) {
                is SessionsApiResult.Ok -> {
                    val record = persistSession(request, result.value, nowMs(), notes)
                    _state.update { it.copy(callingCartId = null, createdSession = record) }
                }
                is SessionsApiResult.Fail -> _state.update {
                    it.copy(callingCartId = null, error = result.message)
                }
            }
        }
    }

    private fun situacaoFiltrada(filter: OpenCartsFilter): CartSituacao? = when (filter) {
        OpenCartsFilter.Retomaveis -> CartSituacao.Digitando
        OpenCartsFilter.Todos -> null
    }
}
