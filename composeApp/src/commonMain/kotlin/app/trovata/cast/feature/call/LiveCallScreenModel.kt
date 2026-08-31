package app.trovata.cast.feature.call

import app.trovata.cast.data.local.OrderRepository
import app.trovata.cast.data.remote.sfa.CarrinhoApi
import app.trovata.cast.data.remote.sfa.CarrinhoItemLinha
import app.trovata.cast.data.remote.sfa.ContextoComercial
import app.trovata.cast.data.remote.sfa.ItemParaCarrinho
import app.trovata.cast.data.remote.sfa.SfaApiResult
import app.trovata.cast.data.remote.sfa.ProdutoGrade
import app.trovata.cast.data.remote.sfa.VitrineApi
import app.trovata.cast.data.sample.Product
import app.trovata.cast.data.signaling.SignalingClient
import app.trovata.cast.data.signaling.SignalingState
import app.trovata.cast.protocol.CartChangeHint
import app.trovata.cast.protocol.CartChangeReason
import app.trovata.cast.protocol.CatalogRoute
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.LiveAnchor
import app.trovata.cast.protocol.OrderLine
import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.protocol.ProductFocus
import app.trovata.cast.protocol.ScrollAnchor
import app.trovata.cast.protocol.ViewState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.scope.Scope

data class CartSizeUi(
    val complemento2Id: Long?,
    val label: String,
    val units: Int,
)

data class CartLineUi(
    val itemId: Long,
    val produtoPreId: Long?,
    val ref: String,
    val name: String,
    val color: String?,
    val units: Int,
    val totalCents: Long,
    val sizes: List<CartSizeUi>,
) {
    val sizesLabel: String get() = sizes.joinToString(" · ") { "${it.label} ${it.units}un" }
}

enum class CartStage {
    Idle,
    Opening,
    Ready,
    Failed,
}

data class CartToast(
    val text: String,
    val createdAtMs: Long,
)

data class OrderSummaryUi(
    val orderId: String,
    val ts: Long,
    val lines: List<OrderLine>,
    val totalCents: Long,
    val confirmedByMe: Boolean,
)

data class LiveCallUiState(
    val signaling: SignalingState = SignalingState.Disconnected,
    val peer: PeerSessionState = PeerSessionState.Idle,
    val token: String,
    val role: PeerRole,
    val localMuted: Boolean = false,
    val remoteMuted: Boolean = false,
    val cart: List<CartLineUi> = emptyList(),
    val cartStage: CartStage = CartStage.Idle,
    val cartError: String? = null,
    val carrinhoId: Long? = null,
    val cartClientName: String? = null,
    val isSavingItem: Boolean = false,
    val isFinishingCart: Boolean = false,
    val products: List<Product> = emptyList(),
    val isLoadingCatalog: Boolean = true,
    val catalogError: String? = null,
    val focusedGrade: ProdutoGrade? = null,
    val isLoadingGrade: Boolean = false,
    val gradeError: String? = null,
    val catalogPage: Int = 1,
    val catalogLastPage: Int = 1,
    val catalogTotal: Int = 0,
    val collectionLabel: String = "",
    val focusedProductId: String? = null,
    val showProductSheet: Boolean = false,
    val showCartDrawer: Boolean = false,
    val toast: CartToast? = null,
    val summary: OrderSummaryUi? = null,
) {
    val isLive: Boolean get() = peer is PeerSessionState.Connected
    val isNegotiating: Boolean get() =
        signaling is SignalingState.Connecting ||
        peer is PeerSessionState.Negotiating
    val errorMessage: String? get() = when {
        signaling is SignalingState.Failed -> signaling.message
        peer is PeerSessionState.Failed -> peer.reason
        else -> null
    }
    val cartCount: Int get() = cart.sumOf { it.units }
    val cartTotalCents: Long get() = cart.sumOf { it.totalCents }
    val canSellToCart: Boolean get() = cartStage == CartStage.Ready && carrinhoId != null
}

class LiveCallScreenModel(
    private val spec: CallSpec,
    private val signaling: SignalingClient,
    private val peer: PeerSession,
    private val orderRepository: OrderRepository,
    private val vitrineApi: VitrineApi,
    private val carrinhoApi: CarrinhoApi,
    private val callScope: Scope,
) : ScreenModel {

    private val token = spec.token
    private val sessionId = spec.sessionId
    private val clientName = spec.clientName
    private val clientEmail = spec.clientEmail
    private val catalogoLinkId = spec.catalogoLinkId
    private val collectionLabel = spec.collectionLabel
    private val sellerName = spec.sellerName
    private val clientShop = spec.clientShop
    private val empresaSlug = spec.empresaSlug
    private val catalogoUuid = spec.catalogoUuid

    private var contexto: ContextoComercial? = null
    private var prazoId: Long = 0

    private val _state = MutableStateFlow(
        LiveCallUiState(
            token = token,
            role = PeerRole.Seller,
            collectionLabel = collectionLabel,
            carrinhoId = spec.carrinhoId,
        ),
    )
    val state: StateFlow<LiveCallUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            openCart()
            loadVitrine()
        }
        screenModelScope.launch {
            signaling.state.collect { s -> _state.update { it.copy(signaling = s) } }
        }
        screenModelScope.launch {
            peer.state.collect { p -> _state.update { it.copy(peer = p) } }
        }
        screenModelScope.launch {
            peer.localMuted.collect { m -> _state.update { it.copy(localMuted = m) } }
        }
        screenModelScope.launch {
            peer.remoteMuted.collect { m -> _state.update { it.copy(remoteMuted = m) } }
        }
        screenModelScope.launch {
            peer.remoteNavigate.collect { msg ->
                val ref = refOf(msg.view.focus?.produtoPreId) ?: return@collect
                _state.update { it.copy(focusedProductId = ref, showProductSheet = true) }
            }
        }
        screenModelScope.launch {
            peer.remoteCartInvalidated.collect { msg -> handleRemoteCartInvalidated(msg) }
        }
        screenModelScope.launch {
            peer.remoteOrderPlaced.collect {
                _state.update { it.copy(showProductSheet = false, showCartDrawer = false) }
            }
        }
    }

    private suspend fun openCart() {
        val email = clientEmail?.trim().orEmpty()
        if (email.isBlank()) {
            _state.update {
                it.copy(
                    cartStage = CartStage.Failed,
                    cartError = "Esse catálogo link não tem e-mail de cliente. Cadastre no Catálogo Link para vender na chamada.",
                )
            }
            return
        }
        _state.update { it.copy(cartStage = CartStage.Opening, cartError = null) }

        when (val sessao = carrinhoApi.abrirCarrinho(empresaSlug, catalogoUuid, email)) {
            is SfaApiResult.Fail -> {
                _state.update { it.copy(cartStage = CartStage.Failed, cartError = sessao.message) }
                return
            }
            is SfaApiResult.Ok -> {
                prazoId = sessao.value.prazoId ?: 0
                _state.update {
                    it.copy(carrinhoId = sessao.value.id, cartClientName = sessao.value.clienteNome)
                }
            }
        }

        when (val resolved = carrinhoApi.contextoComercial(empresaSlug, catalogoUuid)) {
            is SfaApiResult.Fail -> {
                _state.update { it.copy(cartStage = CartStage.Failed, cartError = resolved.message) }
                return
            }
            is SfaApiResult.Ok -> contexto = resolved.value
        }

        _state.update { it.copy(cartStage = CartStage.Ready, cartError = null) }
        refreshCart()
    }

    fun retryCart() {
        if (_state.value.cartStage == CartStage.Opening) return
        screenModelScope.launch {
            openCart()
            loadVitrine(_state.value.catalogPage)
        }
    }

    private suspend fun refreshCart() {
        val carrinhoId = _state.value.carrinhoId ?: return
        when (val result = carrinhoApi.itens(empresaSlug, catalogoUuid, carrinhoId)) {
            is SfaApiResult.Fail -> _state.update { it.copy(cartError = result.message) }
            is SfaApiResult.Ok -> _state.update {
                it.copy(cart = result.value.map { linha -> linha.toCartLineUi() }, cartError = null)
            }
        }
    }

    private suspend fun loadVitrine(page: Int = 1) {
        _state.update { it.copy(isLoadingCatalog = true) }
        when (
            val result = vitrineApi.produtos(
                empresaSlug = empresaSlug,
                catalogoUuid = catalogoUuid,
                page = page,
                carrinhoId = _state.value.carrinhoId,
            )
        ) {
            is SfaApiResult.Fail -> _state.update {
                it.copy(isLoadingCatalog = false, catalogError = result.message)
            }
            is SfaApiResult.Ok -> {
                val produtos = result.value.produtos
                _state.update { current ->
                    current.copy(
                        isLoadingCatalog = false,
                        catalogError = null,
                        products = produtos.map { produto -> produto.toUiProduct() },
                        catalogPage = result.value.currentPage,
                        catalogLastPage = result.value.lastPage,
                        catalogTotal = result.value.total,
                    )
                }
            }
        }
    }

    fun reloadVitrine() {
        screenModelScope.launch { loadVitrine(_state.value.catalogPage) }
    }

    fun nextCatalogPage() {
        val snapshot = _state.value
        if (snapshot.catalogPage >= snapshot.catalogLastPage) return
        screenModelScope.launch { loadVitrine(snapshot.catalogPage + 1) }
    }

    fun prevCatalogPage() {
        val snapshot = _state.value
        if (snapshot.catalogPage <= 1) return
        screenModelScope.launch { loadVitrine(snapshot.catalogPage - 1) }
    }

    fun start() {
        screenModelScope.launch {
            peer.start()
            signaling.start()
        }
    }

    fun toggleMute() {
        peer.setLocalMuted(!_state.value.localMuted)
    }

    fun publishScroll(productId: String, offset: Float) {
        peer.publishScroll(
            ScrollAnchor(
                produtoPreId = produtoPreIdOf(productId),
                itemOffsetRatio = offset,
            ),
        )
    }

    fun publishPointAt(productId: String) {
        val id = produtoPreIdOf(productId) ?: return
        peer.publishPointAt(LiveAnchor.product(id))
    }

    fun openProductDetail(productId: String) {
        _state.update {
            it.copy(
                focusedProductId = productId,
                showProductSheet = true,
                focusedGrade = null,
                gradeError = null,
            )
        }
        val id = produtoPreIdOf(productId) ?: return
        loadGrade(id)
        peer.publishNavigate(
            ViewState(
                route = CatalogRoute.Todos,
                focus = ProductFocus(produtoPreId = id),
            ),
        )
    }

    private fun loadGrade(produtoPreId: Long) {
        _state.update { it.copy(isLoadingGrade = true, gradeError = null) }
        screenModelScope.launch {
            when (
                val result = vitrineApi.grade(
                    empresaSlug = empresaSlug,
                    catalogoUuid = catalogoUuid,
                    produtoPreId = produtoPreId,
                    carrinhoId = _state.value.carrinhoId,
                )
            ) {
                is SfaApiResult.Fail -> _state.update {
                    it.copy(isLoadingGrade = false, gradeError = result.message)
                }
                is SfaApiResult.Ok -> _state.update {
                    if (produtoPreIdOf(it.focusedProductId.orEmpty()) != produtoPreId) it
                    else it.copy(isLoadingGrade = false, focusedGrade = result.value, gradeError = null)
                }
            }
        }
    }

    fun addFocusedProductToCart(complemento1Id: Long?, unitsBySize: Map<Long, Int>) {
        val snapshot = _state.value
        val carrinhoId = snapshot.carrinhoId
        val contextoComercial = contexto
        val produtoPreId = snapshot.focusedGrade?.produtoPreId
            ?: produtoPreIdOf(snapshot.focusedProductId.orEmpty())
        if (carrinhoId == null || contextoComercial == null || produtoPreId == null) {
            _state.update { it.copy(cartError = "O carrinho do cliente ainda não está pronto") }
            return
        }
        if (complemento1Id == null) {
            _state.update { it.copy(cartError = "Escolha uma cor para lançar no pedido") }
            return
        }
        val quantidades = unitsBySize.filterValues { it > 0 }
        if (quantidades.isEmpty()) {
            _state.update { it.copy(cartError = "Informe a quantidade de ao menos um tamanho") }
            return
        }
        if (snapshot.isSavingItem) return
        _state.update { it.copy(isSavingItem = true, cartError = null) }

        screenModelScope.launch {
            val result = carrinhoApi.salvarItem(
                empresaSlug = empresaSlug,
                catalogoUuid = catalogoUuid,
                carrinhoId = carrinhoId,
                contexto = contextoComercial,
                prazoId = prazoId,
                item = ItemParaCarrinho(
                    produtoPreId = produtoPreId,
                    complemento1Id = complemento1Id,
                    quantidadePorTamanho = quantidades,
                ),
            )
            when (result) {
                is SfaApiResult.Fail -> _state.update {
                    it.copy(isSavingItem = false, cartError = result.message)
                }
                is SfaApiResult.Ok -> {
                    val units = quantidades.values.sum()
                    val label = refOf(produtoPreId)
                    peer.publishCartInvalidated(
                        carrinhoId = carrinhoId,
                        reason = CartChangeReason.ItemAdded,
                        hint = CartChangeHint(
                            produtoPreId = produtoPreId,
                            unitsDelta = units,
                            label = label,
                        ),
                    )
                    _state.update {
                        it.copy(
                            isSavingItem = false,
                            cartError = null,
                            toast = CartToast(
                                text = "${units}un de ${label ?: "produto"} no pedido",
                                createdAtMs = Clock.System.now().toEpochMilliseconds(),
                            ),
                        )
                    }
                    refreshCart()
                    loadGrade(produtoPreId)
                }
            }
        }
    }

    private fun produtoPreIdOf(ref: String): Long? =
        _state.value.products.firstOrNull { it.ref == ref }?.produtoPreId

    private fun refOf(produtoPreId: Long?): String? {
        if (produtoPreId == null) return null
        return _state.value.products.firstOrNull { it.produtoPreId == produtoPreId }?.ref
    }

    fun openCartDrawer() {
        _state.update { it.copy(showCartDrawer = true) }
        screenModelScope.launch { refreshCart() }
    }

    fun dismissCartDrawer() {
        _state.update { it.copy(showCartDrawer = false) }
    }

    fun dismissProductSheet() {
        _state.update { it.copy(showProductSheet = false, focusedGrade = null) }
        peer.publishNavigate(ViewState(route = CatalogRoute.Todos))
    }

    fun dismissToast() {
        _state.update { it.copy(toast = null) }
    }

    fun clearCartError() {
        _state.update { it.copy(cartError = null) }
    }

    fun confirmOrder() {
        val current = _state.value
        if (current.summary != null || current.isFinishingCart) return
        if (current.cart.isEmpty()) return
        val carrinhoId = current.carrinhoId ?: return
        val linkId = catalogoLinkId
        if (linkId == null) {
            _state.update {
                it.copy(cartError = "Sessão sem o identificador do catálogo link. Gere o convite novamente.")
            }
            return
        }
        _state.update { it.copy(isFinishingCart = true, cartError = null) }

        screenModelScope.launch {
            when (val result = carrinhoApi.marcarProntoParaEnvio(empresaSlug, linkId, carrinhoId)) {
                is SfaApiResult.Fail -> _state.update {
                    it.copy(isFinishingCart = false, cartError = result.message)
                }
                is SfaApiResult.Ok -> {
                    val ts = Clock.System.now().toEpochMilliseconds()
                    val lines = current.cart.flatMap { it.toOrderLines() }
                    val totalCents = current.cartTotalCents
                    val orderId = "CAR-$carrinhoId"
                    peer.publishOrderPlaced(carrinhoId = carrinhoId, pedidoId = orderId)
                    persistOrder(
                        orderId = orderId,
                        ts = ts,
                        lines = lines,
                        totalCents = totalCents,
                        confirmedByMe = true,
                    )
                    _state.update {
                        it.copy(
                            isFinishingCart = false,
                            summary = OrderSummaryUi(
                                orderId = orderId,
                                ts = ts,
                                lines = lines,
                                totalCents = totalCents,
                                confirmedByMe = true,
                            ),
                            showCartDrawer = false,
                            showProductSheet = false,
                        )
                    }
                }
            }
        }
    }

    private suspend fun persistOrder(
        orderId: String,
        ts: Long,
        lines: List<OrderLine>,
        totalCents: Long,
        confirmedByMe: Boolean,
    ) {
        if (orderRepository.get(orderId) != null) return
        orderRepository.persist(
            orderId = orderId,
            sessionId = sessionId,
            sessionToken = token,
            clientName = _state.value.cartClientName ?: clientName,
            clientShop = clientShop,
            sellerName = sellerName,
            totalCents = totalCents,
            confirmedByMe = confirmedByMe,
            createdAtMs = ts,
            lines = lines,
        )
    }

    fun hangup() {
        screenModelScope.launch {
            peer.close("hangup")
            signaling.stop("hangup")
        }
    }

    private fun handleRemoteCartInvalidated(msg: DataChannelMessage.CartInvalidated) {
        val who = clientName?.split(' ')?.firstOrNull() ?: "Cliente"
        val name = msg.hint?.label
            ?: msg.hint?.produtoPreId?.let { refOf(it) }
            ?: "o carrinho"
        val units = msg.hint?.unitsDelta ?: 0
        val text = when (msg.reason) {
            CartChangeReason.ItemAdded -> "$who adicionou ${units}un de $name"
            CartChangeReason.ItemRemoved -> "$who removeu $name"
            CartChangeReason.QuantityChanged -> "$who ajustou $name"
            CartChangeReason.PrazoChanged -> "$who mudou o prazo"
            CartChangeReason.Cleared -> "$who esvaziou o carrinho"
            CartChangeReason.Finalized -> "$who finalizou o carrinho"
        }
        _state.update {
            it.copy(toast = CartToast(text = text, createdAtMs = Clock.System.now().toEpochMilliseconds()))
        }
        screenModelScope.launch { refreshCart() }
    }

    override fun onDispose() {
        callScope.close()
    }
}

fun CarrinhoItemLinha.toCartLineUi(): CartLineUi = CartLineUi(
    itemId = itemId,
    produtoPreId = produtoPreId,
    ref = ref,
    name = nome,
    color = cor,
    units = quantidade,
    totalCents = totalCents ?: 0L,
    sizes = tamanhos.map { CartSizeUi(it.complemento2Id, it.label, it.quantidade) },
)

private fun CartLineUi.toOrderLines(): List<OrderLine> {
    val unitPriceCents = if (units > 0) totalCents / units else 0L
    if (sizes.isEmpty()) {
        return listOf(
            OrderLine(
                productId = ref,
                size = "Único",
                units = units,
                unitPriceCents = unitPriceCents,
            ),
        )
    }
    return sizes.map { size ->
        OrderLine(
            productId = ref,
            size = size.label,
            units = size.units,
            unitPriceCents = unitPriceCents,
        )
    }
}
