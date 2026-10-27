package app.trovata.cast.feature.aftercall

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.CallInterestEvent
import app.trovata.cast.data.local.CartProduct
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.local.leftOut
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.ProductLabel
import app.trovata.cast.feature.agenda.LoadCartItemsFn
import app.trovata.cast.feature.agenda.leftOutNotes
import app.trovata.cast.ui.format.whatsAppNumberOf
import app.trovata.cast.ui.format.whatsAppUrl
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

typealias LoadProductLabelFn = suspend (
    empresaSlug: String,
    catalogoUuid: String,
    produtoPreId: Long,
) -> ApiResult<ProductLabel>

data class AfterCallSpec(val sessionId: String, val carrinhoId: Long?)

data class FollowUpDraft(
    val followUpOfId: String?,
    val clienteId: Long?,
    val clientName: String?,
    val clientShop: String?,
    val clientDocument: String?,
    val clientEmail: String?,
    val clientPhone: String?,
    val objective: String?,
    val notes: String?,
)

data class AfterCallUiState(
    val isLoading: Boolean = true,
    val session: StoredSessionRecord? = null,
    val appointment: Appointment? = null,
    val client: CatalogClient? = null,
    val order: StoredOrder? = null,
    val interests: List<CallInterest> = emptyList(),
) {
    val leftOut: List<CallInterest> get() = interests.leftOut()

    val inCartCount: Int get() = interests.count { it.inCart }

    val clientName: String? get() = session?.clientName ?: appointment?.clientName ?: client?.name

    val clientPhone: String? get() = session?.clientPhone ?: appointment?.clientPhone ?: client?.phone

    val whatsAppNumber: String? get() = whatsAppNumberOf(clientPhone)
}

class AfterCallScreenModel(
    private val spec: AfterCallSpec,
    private val findSession: suspend (String) -> StoredSessionRecord?,
    private val findAppointment: suspend (String) -> Appointment?,
    private val findClientByEmail: suspend (String?) -> CatalogClient?,
    observeInterests: (String) -> Flow<List<CallInterest>>,
    observeOrders: (List<String>) -> Flow<List<StoredOrder>>,
    private val loadCartItems: LoadCartItemsFn,
    private val loadProductLabel: LoadProductLabelFn,
    private val recordCart: (List<CartProduct>, Long) -> Unit,
    private val recordName: (CallInterestEvent.Named) -> Unit,
    private val catalogWebUrl: String,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ScreenModel {

    private val _state = MutableStateFlow(AfterCallUiState())
    val state: StateFlow<AfterCallUiState> = _state.asStateFlow()

    private val requestedNames = mutableSetOf<Long>()

    init {
        screenModelScope.launch {
            combine(observeInterests(spec.sessionId), observeOrders(listOf(spec.sessionId))) { interests, orders ->
                interests to orders.firstOrNull()
            }.collect { (interests, order) ->
                _state.update { it.copy(interests = interests, order = order) }
                nameLeftOutProducts()
            }
        }
        screenModelScope.launch { loadContext() }
    }

    private suspend fun loadContext() {
        val session = findSession(spec.sessionId)
        val appointment = findAppointment(spec.sessionId)
        val client = findClientByEmail(session?.clientEmail ?: appointment?.clientEmail)
        _state.update { it.copy(session = session, appointment = appointment, client = client, isLoading = false) }
        if (session != null) refreshCart(session)
        nameLeftOutProducts()
    }

    private suspend fun refreshCart(session: StoredSessionRecord) {
        val carrinhoId = spec.carrinhoId ?: return
        val result = loadCartItems(session.empresaSlug, session.catalogoUuid, carrinhoId)
        if (result !is ApiResult.Ok) return
        val products = result.value.mapNotNull { it.toCartProduct() }
        requestedNames += products.map { it.produtoPreId }
        recordCart(products, nowMs())
    }

    private fun nameLeftOutProducts() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        val missing = snapshot.leftOut
            .filter { !it.isNamed && it.produtoPreId !in requestedNames }
            .take(NamedProductsPerCall)
        missing.forEach { interest ->
            requestedNames += interest.produtoPreId
            screenModelScope.launch {
                val result = loadProductLabel(session.empresaSlug, session.catalogoUuid, interest.produtoPreId)
                if (result is ApiResult.Ok) {
                    recordName(
                        CallInterestEvent.Named(
                            produtoPreId = interest.produtoPreId,
                            ref = result.value.ref,
                            name = result.value.name,
                            imageUrl = result.value.imageUrl,
                        ),
                    )
                }
            }
        }
    }

    fun messageForClient(): String {
        val snapshot = _state.value
        val order = snapshot.order ?: return leftOutMessage(snapshot.clientName, snapshot.leftOut)
        return orderSummaryMessage(snapshot.clientName, order, summaryUrl(snapshot))
    }

    fun whatsAppLink(): String? = _state.value.whatsAppNumber?.let { whatsAppUrl(it, messageForClient()) }

    fun followUpDraft(): FollowUpDraft {
        val snapshot = _state.value
        val appointment = snapshot.appointment
        val client = snapshot.client
        return FollowUpDraft(
            followUpOfId = appointment?.appointmentId,
            clienteId = appointment?.clienteId ?: client?.id,
            clientName = snapshot.clientName,
            clientShop = appointment?.clientShop ?: snapshot.session?.clientShop ?: client?.legalName,
            clientDocument = appointment?.clientDocument ?: client?.document,
            clientEmail = snapshot.session?.clientEmail ?: appointment?.clientEmail ?: client?.email,
            clientPhone = snapshot.clientPhone,
            objective = if (snapshot.leftOut.isNotEmpty()) FollowUpObjective else null,
            notes = leftOutNotes(snapshot.interests),
        )
    }

    private fun summaryUrl(snapshot: AfterCallUiState): String? {
        val session = snapshot.session ?: return null
        val carrinhoId = spec.carrinhoId ?: snapshot.order?.orderId?.removePrefix(OrderIdPrefix)?.toLongOrNull() ?: return null
        return cartSummaryUrl(catalogWebUrl, session.empresaSlug, session.catalogoUuid, carrinhoId)
    }

    private companion object {
        const val NamedProductsPerCall = 20
        const val OrderIdPrefix = "CAR-"
        const val FollowUpObjective = "Retomar os produtos que ficaram de fora"
    }
}

private fun CartItemLine.toCartProduct(): CartProduct? {
    val produtoPreId = produtoPreId ?: return null
    return CartProduct(
        produtoPreId = produtoPreId,
        ref = ref.takeIf { it.isNotBlank() },
        name = name.takeIf { it.isNotBlank() },
        imageUrl = imageUrl,
        units = quantity,
    )
}
