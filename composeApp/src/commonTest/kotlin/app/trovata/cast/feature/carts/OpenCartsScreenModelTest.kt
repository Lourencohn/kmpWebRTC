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
import app.trovata.cast.protocol.SessionCreateResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class OpenCartsScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val now = 1_756_800_000_000L

    private val resumable = OpenCart(
        carrinhoId = 9021,
        catalogoLinkId = 4471,
        catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        catalogName = "Outono 26",
        catalogActive = true,
        catalogExpiresAtMs = now + 30L * 24 * 3_600_000,
        clientName = "Loja Bella",
        clientEmail = "compras@bella.com.br",
        status = CartStatus.Editing,
        itemCount = 12,
        totalQuantity = 48,
        totalCents = 420_000,
        updatedAtMs = now - 3L * 24 * 3_600_000,
    )

    private val cartWithoutEmail = resumable.copy(
        carrinhoId = 9022,
        clientName = "Loja Sem Contato",
        clientEmail = null,
    )

    private val expiredCatalog = resumable.copy(
        carrinhoId = 9023,
        clientName = "Loja Atrasada",
        catalogExpiresAtMs = now - 24 * 3_600_000,
    )

    private val alreadySent = resumable.copy(
        carrinhoId = 9024,
        clientName = "Loja Fechada",
        status = CartStatus.Sent,
    )

    private val emptyCart = resumable.copy(
        carrinhoId = 9025,
        clientName = "Loja Curiosa",
        itemCount = 0,
        totalQuantity = 0,
        totalCents = 0,
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(
        carts: List<OpenCart> = listOf(resumable, cartWithoutEmail, expiredCatalog, alreadySent, emptyCart),
        loadResult: ApiResult<OpenCartsPage>? = null,
        createSession: CreateSessionFn = { error("not called") },
        persistSession: PersistSessionFn = { _, _, _, _ -> error("not called") },
        empresaSlug: String = "buba-teste",
        onLoad: (String, CartStatus?, OpenCartsOrder, String?) -> Unit = { _, _, _, _ -> },
        seller: SellerIdentity = SellerIdentity(id = "seller", name = "Vendedor"),
    ) = OpenCartsScreenModel(
        loadOpenCarts = { slug, status, order, search ->
            onLoad(slug, status, order, search)
            loadResult ?: ApiResult.Ok(OpenCartsPage(carts = carts, total = carts.size, hasMore = false))
        },
        createSession = createSession,
        persistSession = persistSession,
        empresaSlugProvider = { empresaSlug },
        companyNameProvider = { "GRUPO MOAS" },
        sellerProvider = { seller },
        nowMs = { now },
    )

    private fun response(token: String = "kP3xq9Trz") = SessionCreateResponse(
        sessionId = "ses_$token",
        token = token,
        url = "https://trovata.app.br/catalogo-link-view/buba-teste/${resumable.catalogoUuid}?live=$token",
        expiresAtMs = now + 3_600_000,
    )

    private fun record(
        request: SessionCreateRequest,
        resp: SessionCreateResponse,
        createdAt: Long,
        notes: SessionClientNotes,
    ) = StoredSessionRecord(
        sessionId = resp.sessionId,
        token = resp.token,
        url = resp.url,
        sellerName = request.sellerName,
        empresaSlug = request.empresaSlug,
        catalogoUuid = request.catalogoUuid,
        catalogoNome = request.catalogoNome,
        catalogoLinkId = request.catalogoLinkId,
        clientName = request.clientName,
        clientEmail = request.clientEmail,
        clientShop = notes.shop,
        scheduledFor = notes.scheduledFor,
        createdAtMs = createdAt,
        expiresAtMs = resp.expiresAtMs,
    )

    @Test
    fun loadsEditingCartsOnOpen() = runTest(dispatcher) {
        var slugUsado: String? = null
        var requestedStatus: CartStatus? = null
        var requestedOrder: OpenCartsOrder? = null
        val sm = model(onLoad = { slug, status, order, _ ->
            slugUsado = slug
            requestedStatus = status
            requestedOrder = order
        })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(sm.state.value.isLoading)
        assertEquals("buba-teste", slugUsado)
        assertEquals(CartStatus.Editing, requestedStatus)
        assertEquals(OpenCartsOrder.MostRecent, requestedOrder)
        assertEquals(5, sm.state.value.carts.size)
    }

    @Test
    fun resumableFilterShowsOnlyCartsTheCallCanResume() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(resumable.carrinhoId), sm.state.value.visibleCarts.map { it.carrinhoId })
        assertEquals(420_000L, sm.state.value.resumableTotalCents)

        sm.setFilter(OpenCartsFilter.All)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, sm.state.value.visibleCarts.size)
    }

    @Test
    fun allFilterRequestsListWithoutStatus() = runTest(dispatcher) {
        val requestedStatuses = mutableListOf<CartStatus?>()
        val sm = model(onLoad = { _, status, _, _ -> requestedStatuses.add(status) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.setFilter(OpenCartsFilter.All)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(CartStatus.Editing, null), requestedStatuses)
    }

    @Test
    fun changingOrderReloadsWithReversedDirection() = runTest(dispatcher) {
        val ordens = mutableListOf<OpenCartsOrder>()
        val sm = model(onLoad = { _, _, order, _ -> ordens.add(order) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.toggleOrder()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(OpenCartsOrder.MostRecent, OpenCartsOrder.LongestIdle), ordens)
        assertEquals("asc", sm.state.value.order.direction)
    }

    @Test
    fun searchGoesToTheServer() = runTest(dispatcher) {
        val searches = mutableListOf<String?>()
        val sm = model(onLoad = { _, _, _, search -> searches.add(search) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.setSearch("  bella  ")
        sm.submitSearch()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(null, "bella"), searches)
    }

    @Test
    fun withoutSelectedCompanyDoesNotCallApi() = runTest(dispatcher) {
        var chamou = false
        val sm = model(empresaSlug = "", onLoad = { _, _, _, _ -> chamou = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(chamou)
        assertEquals("Selecione uma empresa para ver os carrinhos", sm.state.value.error)
        assertFalse(sm.state.value.isLoading)
    }

    @Test
    fun apiFailureBecomesScreenMessage() = runTest(dispatcher) {
        val sm = model(
            loadResult = ApiResult.Fail("http_401", "Sessão expirada. Faça login novamente.", 401),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sessão expirada. Faça login novamente.", sm.state.value.error)
        assertTrue(sm.state.value.carts.isEmpty())
    }

    @Test
    fun callLiveOpensSessionOnCartCatalogAndEmail() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val resp = response()
        val sm = model(
            createSession = { request ->
                captured = request
                ApiResult.Ok(resp)
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
            seller = SellerIdentity(id = "vend-31", name = "Marina Prado"),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(resumable)
        assertEquals(resumable.carrinhoId, sm.state.value.callingCartId)
        dispatcher.scheduler.advanceUntilIdle()

        val request = assertNotNull(captured)
        assertEquals("buba-teste", request.empresaSlug)
        assertEquals(resumable.catalogoUuid, request.catalogoUuid)
        assertEquals(4471, request.catalogoLinkId)
        assertEquals(9021, request.carrinhoId)
        assertEquals("compras@bella.com.br", request.clientEmail)
        assertEquals("Loja Bella", request.clientName)
        assertEquals("Outono 26", request.catalogoNome)

        val created = assertNotNull(sm.state.value.createdSession)
        assertEquals(resp.token, created.token)
        assertEquals("Loja Bella", created.clientShop)
        assertNull(sm.state.value.callingCartId)
    }

    @Test
    fun cartWithoutEmailDoesNotOpenSessionAndExplainsWhy() = runTest(dispatcher) {
        var chamou = false
        val sm = model(createSession = { chamou = true; ApiResult.Ok(response()) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(cartWithoutEmail)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(chamou)
        assertNull(sm.state.value.callingCartId)
        assertEquals(
            "Esse carrinho não tem e-mail de cliente. Sem ele a chamada não abre o carrinho.",
            sm.state.value.error,
        )
    }

    @Test
    fun cartNotEditingWarnsThatTheCallWouldOpenAnotherCart() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(alreadySent)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Esse carrinho está enviado. A chamada abriria um carrinho novo, não esse.",
            sm.state.value.error,
        )
    }

    @Test
    fun expiredCatalogBlocksTheCall() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(expiredCatalog)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "O catálogo desse carrinho está expirado. Renove a validade no Catálogo Link.",
            sm.state.value.error,
        )
    }

    @Test
    fun serverErrorOnCreationReleasesTheButton() = runTest(dispatcher) {
        val sm = model(
            createSession = {
                ApiResult.Fail("sessao_indisponivel", "Não foi possível abrir a sessão agora.", 503)
            },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(resumable)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Não foi possível abrir a sessão agora.", sm.state.value.error)
        assertNull(sm.state.value.createdSession)
        assertNull(sm.state.value.callingCartId)
    }

    @Test
    fun consumeCreatedSessionClearsTheState() = runTest(dispatcher) {
        val sm = model(
            createSession = { ApiResult.Ok(response()) },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(resumable)
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sm.state.value.createdSession)

        sm.consumeCreatedSession()
        assertNull(sm.state.value.createdSession)
    }

    @Test
    fun idleTimeLabelCoversTheListIntervals() {
        assertEquals("mexeu agora", idleTimeLabel(now, now))
        assertEquals("sem mexer há 5 min", idleTimeLabel(now - 5 * 60_000, now))
        assertEquals("sem mexer há 3h", idleTimeLabel(now - 3 * 3_600_000, now))
        assertEquals("sem mexer há 1 dia", idleTimeLabel(now - 24 * 3_600_000, now))
        assertEquals("sem mexer há 3 dias", idleTimeLabel(now - 3L * 24 * 3_600_000, now))
        assertEquals("sem mexer há 2 meses", idleTimeLabel(now - 61L * 24 * 3_600_000, now))
        assertEquals("sem data de alteração", idleTimeLabel(null, now))
    }
}
