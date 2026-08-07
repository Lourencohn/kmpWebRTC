package app.trovata.cast.feature.catalog

import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.SessionsApiResult
import app.trovata.cast.data.remote.sfa.SfaApiResult
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

data class ClientDraft(
    val name: String? = null,
    val shop: String? = null,
    val scheduledFor: String? = null,
)

enum class CatalogLinkFilter(val label: String) {
    Disponiveis("Disponíveis"),
    Todos("Todos"),
}

data class CatalogLinkPickerUiState(
    val links: List<SellerCatalogLink> = emptyList(),
    val selectedUuid: String? = null,
    val filter: CatalogLinkFilter = CatalogLinkFilter.Disponiveis,
    val client: ClientDraft = ClientDraft(),
    val companyName: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdSession: StoredSessionRecord? = null,
) {
    val visibleLinks: List<SellerCatalogLink>
        get() = when (filter) {
            CatalogLinkFilter.Disponiveis -> links.filter { it.disponivel }
            CatalogLinkFilter.Todos -> links
        }

    val selected: SellerCatalogLink?
        get() = links.firstOrNull { it.uuid == selectedUuid }

    val canGenerate: Boolean
        get() = selected?.disponivel == true && !isSubmitting

    val unavailableCount: Int get() = links.count { !it.disponivel }
}

typealias LoadCatalogLinksFn = suspend (empresaSlug: String) -> SfaApiResult<List<SellerCatalogLink>>
typealias CreateSessionFn = suspend (SessionCreateRequest) -> SessionsApiResult<SessionCreateResponse>
typealias PersistSessionFn = suspend (
    SessionCreateRequest,
    SessionCreateResponse,
    Long,
    SessionClientNotes,
) -> StoredSessionRecord

class CatalogLinkPickerScreenModel(
    private val loadCatalogLinks: LoadCatalogLinksFn,
    private val createSession: CreateSessionFn,
    private val persistSession: PersistSessionFn,
    private val empresaSlugProvider: () -> String,
    private val companyNameProvider: () -> String = { "" },
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    initialClient: ClientDraft = ClientDraft(),
) : ScreenModel {

    private val _state = MutableStateFlow(
        CatalogLinkPickerUiState(client = initialClient, companyName = companyNameProvider()),
    )
    val state: StateFlow<CatalogLinkPickerUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val empresaSlug = empresaSlugProvider()
        if (empresaSlug.isBlank()) {
            _state.update {
                it.copy(isLoading = false, error = "Selecione uma empresa para ver seus catálogos")
            }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        screenModelScope.launch {
            when (val result = loadCatalogLinks(empresaSlug)) {
                is SfaApiResult.Ok -> _state.update { current ->
                    val links = result.value
                    current.copy(
                        links = links,
                        isLoading = false,
                        error = null,
                        selectedUuid = current.selectedUuid?.takeIf { uuid ->
                            links.any { it.uuid == uuid }
                        },
                    )
                }
                is SfaApiResult.Fail -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun select(link: SellerCatalogLink) {
        if (!link.disponivel) {
            _state.update { it.copy(error = motivoIndisponivel(link)) }
            return
        }
        _state.update {
            val alreadySelected = it.selectedUuid == link.uuid
            it.copy(selectedUuid = if (alreadySelected) null else link.uuid, error = null)
        }
    }

    fun setFilter(filter: CatalogLinkFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun consumeCreatedSession() {
        _state.update { it.copy(createdSession = null) }
    }

    fun generateLink(sellerId: String = "seller", sellerName: String = "Vendedor") {
        val snapshot = _state.value
        val link = snapshot.selected
        if (link == null) {
            _state.update { it.copy(error = "Escolha um catálogo link para convidar o cliente") }
            return
        }
        if (!link.disponivel) {
            _state.update { it.copy(error = motivoIndisponivel(link)) }
            return
        }
        if (snapshot.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }

        val request = SessionCreateRequest(
            empresaSlug = empresaSlugProvider(),
            catalogoUuid = link.uuid,
            sellerId = sellerId,
            sellerName = sellerName,
            catalogoNome = link.nome,
            clientName = snapshot.client.name ?: link.clienteNome,
        )
        val notes = SessionClientNotes(
            shop = snapshot.client.shop ?: link.clienteNome,
            scheduledFor = snapshot.client.scheduledFor,
        )
        screenModelScope.launch {
            when (val result = createSession(request)) {
                is SessionsApiResult.Ok -> {
                    val record = persistSession(request, result.value, nowMs(), notes)
                    _state.update { it.copy(isSubmitting = false, createdSession = record) }
                }
                is SessionsApiResult.Fail -> {
                    _state.update { it.copy(isSubmitting = false, error = result.message) }
                }
            }
        }
    }

    private fun motivoIndisponivel(link: SellerCatalogLink): String = when {
        link.expirado -> "Esse catálogo está expirado. Renove a validade no Catálogo Link."
        !link.ativo -> "Esse catálogo está inativo."
        else -> "Esse catálogo não está disponível."
    }
}
