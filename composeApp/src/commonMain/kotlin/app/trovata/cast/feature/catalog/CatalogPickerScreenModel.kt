package app.trovata.cast.feature.catalog

import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.SessionsApiResult
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.sample.ProductTag
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class CatalogFilter(val label: String) {
    Todos("Todos"),
    Novos("Novos"),
    TopVenda("Top venda"),
    PreVenda("Pré-venda"),
}

data class ClientDraft(
    val name: String? = null,
    val shop: String? = null,
    val scheduledFor: String? = null,
)

data class CatalogPickerUiState(
    val products: List<Product>,
    val selectedSkus: Set<String>,
    val filter: CatalogFilter,
    val client: ClientDraft,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdSession: StoredSessionRecord? = null,
) {
    val visibleProducts: List<Product>
        get() = when (filter) {
            CatalogFilter.Todos -> products
            CatalogFilter.Novos -> products.filter { it.tag == ProductTag.Novo }
            CatalogFilter.TopVenda -> products.filter { it.tag == ProductTag.TopVenda }
            CatalogFilter.PreVenda -> products.filter { it.tag == ProductTag.PreVenda }
        }

    val skuCount: Int get() = visibleProducts
        .filter { it.ref in selectedSkus }
        .sumOf { it.sizes.size * it.colorCount }
}

typealias CreateSessionFn = suspend (SessionCreateRequest) -> SessionsApiResult<SessionCreateResponse>
typealias PersistSessionFn = suspend (SessionCreateRequest, SessionCreateResponse, Long) -> StoredSessionRecord

class CatalogPickerScreenModel(
    private val createSession: CreateSessionFn,
    private val persistSession: PersistSessionFn,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    initialClient: ClientDraft = ClientDraft(),
) : ScreenModel {

    private val _state = MutableStateFlow(
        CatalogPickerUiState(
            products = SampleCatalog.products,
            selectedSkus = emptySet(),
            filter = CatalogFilter.Todos,
            client = initialClient,
        ),
    )
    val state: StateFlow<CatalogPickerUiState> = _state.asStateFlow()

    fun toggle(sku: String) {
        _state.update { current ->
            val next = current.selectedSkus.toMutableSet().apply {
                if (!add(sku)) remove(sku)
            }
            current.copy(selectedSkus = next, error = null)
        }
    }

    fun setFilter(filter: CatalogFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun consumeCreatedSession() {
        _state.update { it.copy(createdSession = null) }
    }

    fun generateLink(sellerId: String = "atelier-norte", sellerName: String = "Atelier Norte") {
        val snapshot = _state.value
        if (snapshot.selectedSkus.isEmpty()) {
            _state.update { it.copy(error = "Escolha ao menos um produto") }
            return
        }
        if (snapshot.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        val request = SessionCreateRequest(
            sellerId = sellerId,
            sellerName = sellerName,
            collectionLabel = SampleCatalog.collection,
            productSkus = snapshot.selectedSkus.toList(),
            clientName = snapshot.client.name,
            clientShop = snapshot.client.shop,
            scheduledFor = snapshot.client.scheduledFor,
        )
        screenModelScope.launch {
            when (val result = createSession(request)) {
                is SessionsApiResult.Ok -> {
                    val record = persistSession(request, result.value, nowMs())
                    _state.update { it.copy(isSubmitting = false, createdSession = record) }
                }
                is SessionsApiResult.Fail -> {
                    _state.update { it.copy(isSubmitting = false, error = result.message) }
                }
            }
        }
    }
}
