package app.trovata.cast.feature.carts

import app.trovata.cast.data.auth.SellerIdentity
import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartStatus
import app.trovata.cast.data.remote.sfa.OpenCart
import app.trovata.cast.data.remote.sfa.OpenCartsOrder
import app.trovata.cast.data.remote.sfa.OpenCartsPage
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
    Resumable("Retomáveis"),
    All("Todos"),
}

data class OpenCartsUiState(
    val carts: List<OpenCart> = emptyList(),
    val filter: OpenCartsFilter = OpenCartsFilter.Resumable,
    val order: OpenCartsOrder = OpenCartsOrder.MostRecent,
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
    val resumable: List<OpenCart>
        get() = carts.filter { it.hasItems && it.canCallLive(nowMs) }

    val visibleCarts: List<OpenCart>
        get() = when (filter) {
            OpenCartsFilter.Resumable -> resumable
            OpenCartsFilter.All -> carts
        }

    val resumableTotalCents: Long
        get() = resumable.sumOf { it.totalCents ?: 0L }
}

fun liveCartSessionRequest(
    cart: OpenCart,
    catalogoUuid: String,
    empresaSlug: String,
    seller: SellerIdentity,
): SessionCreateRequest = SessionCreateRequest(
    empresaSlug = empresaSlug,
    catalogoUuid = catalogoUuid,
    sellerId = seller.id,
    sellerName = seller.name,
    catalogoNome = cart.catalogName,
    catalogoLinkId = cart.catalogoLinkId,
    carrinhoId = cart.carrinhoId,
    clientName = cart.clientName,
    clientEmail = cart.clientEmail,
)

typealias LoadOpenCartsFn = suspend (
    empresaSlug: String,
    situacao: CartStatus?,
    order: OpenCartsOrder,
    search: String?,
) -> ApiResult<OpenCartsPage>

class OpenCartsScreenModel(
    private val loadOpenCarts: LoadOpenCartsFn,
    private val createSession: CreateSessionFn,
    private val persistSession: PersistSessionFn,
    private val empresaSlugProvider: () -> String,
    private val companyNameProvider: () -> String = { "" },
    private val sellerProvider: () -> SellerIdentity,
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
            val result = loadOpenCarts(empresaSlug, statusFilterFor(snapshot.filter), snapshot.order, query)
            when (result) {
                is ApiResult.Ok -> _state.update {
                    it.copy(
                        carts = result.value.carts,
                        total = result.value.total,
                        hasMore = result.value.hasMore,
                        nowMs = nowMs(),
                        isLoading = false,
                        error = null,
                    )
                }
                is ApiResult.Fail -> _state.update {
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
            OpenCartsOrder.MostRecent -> OpenCartsOrder.LongestIdle
            OpenCartsOrder.LongestIdle -> OpenCartsOrder.MostRecent
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

    fun callLive(cart: OpenCart) {
        val snapshot = _state.value
        if (snapshot.callingCartId != null) return
        val blockReason = cart.callBlockReason(nowMs())
        if (blockReason != null) {
            _state.update { it.copy(error = blockReason) }
            return
        }
        val catalogoUuid = cart.catalogoUuid ?: return
        _state.update { it.copy(callingCartId = cart.carrinhoId, error = null) }

        val request = liveCartSessionRequest(cart, catalogoUuid, empresaSlugProvider(), sellerProvider())
        val notes = SessionClientNotes(shop = cart.clientName)
        screenModelScope.launch {
            when (val result = createSession(request)) {
                is ApiResult.Ok -> {
                    val record = persistSession(request, result.value, nowMs(), notes)
                    _state.update { it.copy(callingCartId = null, createdSession = record) }
                }
                is ApiResult.Fail -> _state.update {
                    it.copy(callingCartId = null, error = result.message)
                }
            }
        }
    }

    private fun statusFilterFor(filter: OpenCartsFilter): CartStatus? = when (filter) {
        OpenCartsFilter.Resumable -> CartStatus.Editing
        OpenCartsFilter.All -> null
    }
}
