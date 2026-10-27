package app.trovata.cast.feature.catalog

import app.trovata.cast.data.auth.SellerIdentity
import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CatalogLinkPage
import app.trovata.cast.data.remote.sfa.SellerCatalogLink
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
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
    val document: String? = null,
    val email: String? = null,
    val phone: String? = null,
) {
    val isIdentified: Boolean get() = !document.isNullOrBlank() || !email.isNullOrBlank()
}

fun recipientMatches(client: ClientDraft, link: SellerCatalogLink): Boolean? {
    val clientDocumentDigits = digitsOnly(client.document)
    val linkDocumentDigits = digitsOnly(link.clientDocument)
    if (clientDocumentDigits != null && linkDocumentDigits != null) return clientDocumentDigits == linkDocumentDigits
    val normalizedClientEmail = client.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val emailLink = link.clientEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (normalizedClientEmail != null && emailLink != null) return normalizedClientEmail == emailLink
    return null
}

enum class CatalogLinkFilter(val label: String) {
    Available("Disponíveis"),
    All("Todos"),
}

data class CatalogLinkPickerUiState(
    val links: List<SellerCatalogLink> = emptyList(),
    val selectedUuid: String? = null,
    val filter: CatalogLinkFilter = CatalogLinkFilter.Available,
    val client: ClientDraft = ClientDraft(),
    val companyName: String = "",
    val companySlug: String = "",
    val search: String = "",
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdSession: StoredSessionRecord? = null,
) {
    val visibleLinks: List<SellerCatalogLink>
        get() = when (filter) {
            CatalogLinkFilter.Available -> links.filter { it.isAvailable }
            CatalogLinkFilter.All -> links
        }

    val selected: SellerCatalogLink?
        get() = links.firstOrNull { it.uuid == selectedUuid }

    val callEmail: String?
        get() = client.email?.takeIf { it.isNotBlank() } ?: selected?.clientEmail

    val callRecipient: String?
        get() = client.name?.takeIf { it.isNotBlank() } ?: selected?.recipientLabel

    val recipientWarning: String?
        get() {
            val link = selected ?: return null
            val chosenClientName = client.name ?: "o cliente escolhido"
            val chosenClientEmail = client.email?.takeIf { it.isNotBlank() }
            if (chosenClientEmail == null) {
                if (!client.isIdentified && client.name == null) return null
                return if (link.clientEmail != null) {
                    "$chosenClientName não tem e-mail cadastrado. A chamada vai abrir o carrinho de ${link.recipientLabel ?: "quem está no catálogo"}."
                } else {
                    "Nem $chosenClientName nem esse catálogo têm e-mail. A chamada não vai conseguir abrir o carrinho."
                }
            }
            if (link.hasRecipient && recipientMatches(client, link) == false) {
                return "Esse catálogo está cadastrado para ${link.recipientLabel}. A chamada vai abrir o carrinho de $chosenClientName, com o e-mail dele."
            }
            return null
        }

    val canGenerate: Boolean
        get() = selected?.isAvailable == true && !isSubmitting

    val unavailableCount: Int get() = links.count { !it.isAvailable }
}

typealias LoadCatalogLinksFn = suspend (empresaSlug: String, search: String?) -> ApiResult<CatalogLinkPage>
typealias CreateSessionFn = suspend (SessionCreateRequest) -> ApiResult<SessionCreateResponse>
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
    private val sellerProvider: () -> SellerIdentity,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    initialClient: ClientDraft = ClientDraft(),
) : ScreenModel {

    private val _state = MutableStateFlow(
        CatalogLinkPickerUiState(
            client = initialClient,
            companyName = companyNameProvider(),
            companySlug = empresaSlugProvider(),
        ),
    )
    val state: StateFlow<CatalogLinkPickerUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

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
        val query = _state.value.search.trim().takeIf { it.isNotBlank() }
        _state.update { it.copy(isLoading = true, error = null) }
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            when (val result = loadCatalogLinks(empresaSlug, query)) {
                is ApiResult.Ok -> _state.update { current ->
                    val page = result.value
                    current.copy(
                        links = page.links,
                        total = page.total,
                        hasMore = page.hasMore,
                        isLoading = false,
                        error = null,
                        selectedUuid = current.selectedUuid?.takeIf { uuid ->
                            page.links.any { it.uuid == uuid }
                        },
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

    fun select(link: SellerCatalogLink) {
        if (!link.isAvailable) {
            _state.update { it.copy(error = unavailableReason(link)) }
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

    fun generateLink() {
        val snapshot = _state.value
        val link = snapshot.selected
        if (link == null) {
            _state.update { it.copy(error = "Escolha um catálogo link para convidar o cliente") }
            return
        }
        if (!link.isAvailable) {
            _state.update { it.copy(error = unavailableReason(link)) }
            return
        }
        if (snapshot.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }

        val seller = sellerProvider()
        val request = SessionCreateRequest(
            empresaSlug = empresaSlugProvider(),
            catalogoUuid = link.uuid,
            sellerId = seller.id,
            sellerName = seller.name,
            catalogoNome = link.name,
            catalogoLinkId = link.id,
            clientName = snapshot.client.name ?: link.clientName,
            clientEmail = snapshot.callEmail,
        )
        val notes = SessionClientNotes(
            shop = snapshot.client.shop ?: link.clientName,
            scheduledFor = snapshot.client.scheduledFor,
            phone = snapshot.client.phone ?: link.clientWhatsapp.takeIf { linkBelongsTo(snapshot.client, link) },
        )
        screenModelScope.launch {
            when (val result = createSession(request)) {
                is ApiResult.Ok -> {
                    val record = persistSession(request, result.value, nowMs(), notes)
                    _state.update { it.copy(isSubmitting = false, createdSession = record) }
                }
                is ApiResult.Fail -> {
                    _state.update { it.copy(isSubmitting = false, error = result.message) }
                }
            }
        }
    }

    private fun unavailableReason(link: SellerCatalogLink): String = when {
        link.expired -> "Esse catálogo está expirado. Renove a validade no Catálogo Link."
        !link.active -> "Esse catálogo está inativo."
        else -> "Esse catálogo não está disponível."
    }
}

private fun linkBelongsTo(client: ClientDraft, link: SellerCatalogLink): Boolean =
    client.name == null || recipientMatches(client, link) == true

private fun digitsOnly(raw: String?): String? =
    raw?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
