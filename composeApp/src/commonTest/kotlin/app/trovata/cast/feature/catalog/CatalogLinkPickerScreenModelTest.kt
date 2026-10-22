package app.trovata.cast.feature.catalog

import app.trovata.cast.data.auth.SellerIdentity
import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CatalogLinkPage
import app.trovata.cast.data.remote.sfa.SellerCatalogLink
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
class CatalogLinkPickerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val activeLink = SellerCatalogLink(
        id = 4471,
        uuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        name = "Outono 26",
        clientName = "Trama Multimarcas",
        clientEmail = "compras@trama.com.br",
        clientDocument = "12.345.678/0001-99",
        sellerName = "Marina Prado",
        active = true,
        expired = false,
        validityLabel = "2026-12-31",
        cartCount = 3,
        viewCount = 42,
    )

    private val expired = activeLink.copy(
        id = 4472,
        uuid = "aaaa1111-8a41-4f0b-9c3d-77b2a0e14c9f",
        name = "Verão 25",
        expired = true,
    )

    private val inactive = activeLink.copy(
        id = 4473,
        uuid = "bbbb2222-8a41-4f0b-9c3d-77b2a0e14c9f",
        name = "Inverno 25",
        active = false,
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
        links: List<SellerCatalogLink> = listOf(activeLink, expired, inactive),
        loadResult: ApiResult<CatalogLinkPage> =
            ApiResult.Ok(CatalogLinkPage(links = links, total = links.size, hasMore = false)),
        createSession: CreateSessionFn = { error("not called") },
        persistSession: PersistSessionFn = { _, _, _, _ -> error("not called") },
        empresaSlug: String = "buba-teste",
        onLoad: (String) -> Unit = {},
        initialClient: ClientDraft = ClientDraft(),
        seller: SellerIdentity = SellerIdentity(id = "seller", name = "Vendedor"),
    ) = CatalogLinkPickerScreenModel(
        loadCatalogLinks = { slug, _ ->
            onLoad(slug)
            loadResult
        },
        createSession = createSession,
        persistSession = persistSession,
        empresaSlugProvider = { empresaSlug },
        companyNameProvider = { "GRUPO MOAS" },
        sellerProvider = { seller },
        nowMs = { 9_999 },
        initialClient = initialClient,
    )

    private fun response(token: String = "kP3xq9Trz") = SessionCreateResponse(
        sessionId = "ses_$token",
        token = token,
        url = "https://trovata.app.br/catalogo-link-view/buba-teste/${activeLink.uuid}?live=$token",
        expiresAtMs = 1_700_014_400_000,
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
    fun loadsSellerCatalogsOnOpen() = runTest(dispatcher) {
        var slugUsado: String? = null
        val sm = model(onLoad = { slugUsado = it })
        dispatcher.scheduler.advanceUntilIdle()

        val state = sm.state.value
        assertFalse(state.isLoading)
        assertEquals(3, state.links.size)
        assertEquals("buba-teste", slugUsado)
        assertNull(state.error)
    }

    @Test
    fun defaultFilterHidesExpiredAndInactive() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(activeLink.uuid), sm.state.value.visibleLinks.map { it.uuid })
        assertEquals(2, sm.state.value.unavailableCount)

        sm.setFilter(CatalogLinkFilter.All)
        assertEquals(3, sm.state.value.visibleLinks.size)
    }

    @Test
    fun withoutSelectedCompanyDoesNotCallApi() = runTest(dispatcher) {
        var chamou = false
        val sm = model(empresaSlug = "", onLoad = { chamou = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(chamou)
        assertEquals("Selecione uma empresa para ver seus catálogos", sm.state.value.error)
        assertFalse(sm.state.value.isLoading)
    }

    @Test
    fun apiFailureBecomesScreenMessage() = runTest(dispatcher) {
        val sm = model(
            loadResult = ApiResult.Fail("http_401", "Sessão expirada. Faça login novamente.", 401),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sessão expirada. Faça login novamente.", sm.state.value.error)
        assertTrue(sm.state.value.links.isEmpty())
    }

    @Test
    fun selectingExpiredExplainsWhy() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(expired)
        assertNull(sm.state.value.selectedUuid)
        assertEquals(
            "Esse catálogo está expirado. Renove a validade no Catálogo Link.",
            sm.state.value.error,
        )

        sm.clearError()
        sm.select(inactive)
        assertEquals("Esse catálogo está inativo.", sm.state.value.error)
    }

    @Test
    fun selectingTwiceDeselects() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        assertEquals(activeLink.uuid, sm.state.value.selectedUuid)
        assertTrue(sm.state.value.canGenerate)

        sm.select(activeLink)
        assertNull(sm.state.value.selectedUuid)
        assertFalse(sm.state.value.canGenerate)
    }

    @Test
    fun generatingLinkWithoutSelectionWarns() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.generateLink()
        assertEquals("Escolha um catálogo link para convidar o cliente", sm.state.value.error)
        assertFalse(sm.state.value.isSubmitting)
    }

    @Test
    fun generatingLinkSendsChosenCatalogIdentity() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val resp = response()
        val sm = model(
            createSession = { request ->
                captured = request
                ApiResult.Ok(resp)
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
            initialClient = ClientDraft(name = "Diego Albuquerque"),
            seller = SellerIdentity(id = "vend-31", name = "Marina Prado"),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        sm.generateLink()
        assertTrue(sm.state.value.isSubmitting)
        dispatcher.scheduler.advanceUntilIdle()

        val request = assertNotNull(captured)
        assertEquals("buba-teste", request.empresaSlug)
        assertEquals(activeLink.uuid, request.catalogoUuid)
        assertEquals("Outono 26", request.catalogoNome)
        assertEquals("vend-31", request.sellerId)
        assertEquals("Diego Albuquerque", request.clientName)

        val created = assertNotNull(sm.state.value.createdSession)
        assertEquals(resp.token, created.token)
        assertTrue(created.url.contains("/catalogo-link-view/buba-teste/${activeLink.uuid}?live="))
        assertEquals("Trama Multimarcas", created.clientShop)
        assertFalse(sm.state.value.isSubmitting)
    }

    @Test
    fun catalogClientBecomesNameWhenThereIsNoDraft() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val sm = model(
            createSession = { request ->
                captured = request
                ApiResult.Ok(response())
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Trama Multimarcas", assertNotNull(captured).clientName)
    }

    @Test
    fun serverErrorOnCreationShowsOnScreen() = runTest(dispatcher) {
        val sm = model(
            createSession = {
                ApiResult.Fail(
                    code = "catalogo_indisponivel",
                    message = "Catálogo expirado. Renove a validade no Catálogo Link.",
                    status = 422,
                )
            },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Catálogo expirado. Renove a validade no Catálogo Link.", sm.state.value.error)
        assertNull(sm.state.value.createdSession)
        assertFalse(sm.state.value.isSubmitting)
    }

    @Test
    fun refreshKeepsSelectionWhenCatalogStillExists() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()
        sm.select(activeLink)

        sm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(activeLink.uuid, sm.state.value.selectedUuid)
    }

    @Test
    fun chosenClientEmailWinsInTheCall() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val sm = model(
            createSession = { request ->
                captured = request
                ApiResult.Ok(response())
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
            initialClient = ClientDraft(
                name = "Loja Bella",
                document = "99.999.999/0001-99",
                email = "compras@bella.com.br",
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        assertEquals("compras@bella.com.br", sm.state.value.callEmail)
        assertEquals("Loja Bella", sm.state.value.callRecipient)

        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()

        val request = assertNotNull(captured)
        assertEquals("compras@bella.com.br", request.clientEmail)
        assertEquals("Loja Bella", request.clientName)
    }

    @Test
    fun withoutChosenClientTheCatalogWins() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val sm = model(
            createSession = { request ->
                captured = request
                ApiResult.Ok(response())
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()

        val request = assertNotNull(captured)
        assertEquals("compras@trama.com.br", request.clientEmail)
        assertEquals("Trama Multimarcas", request.clientName)
        assertNull(sm.state.value.recipientWarning)
    }

    @Test
    fun warnsThatCatalogIsRegisteredToAnotherClient() = runTest(dispatcher) {
        val sm = model(
            initialClient = ClientDraft(
                name = "Loja Bella",
                document = "99.999.999/0001-99",
                email = "compras@bella.com.br",
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        assertEquals(
            "Esse catálogo está cadastrado para Trama Multimarcas. A chamada vai abrir o carrinho de Loja Bella, com o e-mail dele.",
            sm.state.value.recipientWarning,
        )
    }

    @Test
    fun doesNotWarnWhenDocumentMatches() = runTest(dispatcher) {
        val sm = model(
            initialClient = ClientDraft(
                name = "Trama Multimarcas",
                document = "12345678000199",
                email = "compras@trama.com.br",
            ),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        assertNull(sm.state.value.recipientWarning)
    }

    @Test
    fun clientWithoutEmailFallsBackToCatalogEmailAndWarns() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val sm = model(
            createSession = { request ->
                captured = request
                ApiResult.Ok(response())
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
            initialClient = ClientDraft(name = "Loja Bella", document = "99.999.999/0001-99"),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        assertEquals(
            "Loja Bella não tem e-mail cadastrado. A chamada vai abrir o carrinho de Trama Multimarcas.",
            sm.state.value.recipientWarning,
        )

        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("compras@trama.com.br", assertNotNull(captured).clientEmail)
    }

    @Test
    fun withoutEmailOnEitherSideWarnsThatNoCartOpens() = runTest(dispatcher) {
        val semEmail = activeLink.copy(clientEmail = null)
        val sm = model(
            links = listOf(semEmail),
            initialClient = ClientDraft(name = "Loja Bella", document = "99.999.999/0001-99"),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(semEmail)
        assertEquals(
            "Nem Loja Bella nem esse catálogo têm e-mail. A chamada não vai conseguir abrir o carrinho.",
            sm.state.value.recipientWarning,
        )
    }

    @Test
    fun consumeCreatedSessionClearsTheState() = runTest(dispatcher) {
        val sm = model(
            createSession = { ApiResult.Ok(response()) },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(activeLink)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sm.state.value.createdSession)

        sm.consumeCreatedSession()
        assertNull(sm.state.value.createdSession)
    }
}
