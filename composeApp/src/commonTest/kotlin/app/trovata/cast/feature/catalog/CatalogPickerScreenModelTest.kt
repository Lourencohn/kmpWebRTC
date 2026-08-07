package app.trovata.cast.feature.catalog

import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.SessionsApiResult
import app.trovata.cast.data.sample.SampleCatalog
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogPickerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val catalogLink = CatalogLinkIdentity(
        empresaSlug = "atelier-norte",
        catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        nome = "Outono 26",
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
        createSession: CreateSessionFn = { error("not called") },
        persistSession: PersistSessionFn = { _, _, _, _ -> error("not called") },
        catalogLink: CatalogLinkIdentity = this.catalogLink,
        nowMs: () -> Long = { 1_700_000_000_000 },
        initialClient: ClientDraft = ClientDraft(),
    ) = CatalogPickerScreenModel(
        createSession = createSession,
        persistSession = persistSession,
        catalogLink = catalogLink,
        nowMs = nowMs,
        initialClient = initialClient,
    )

    private fun sampleResponse(token: String = "abc12345X"): SessionCreateResponse =
        SessionCreateResponse(
            sessionId = "ses_$token",
            token = token,
            url = "http://localhost:5173/catalogo-link-view/atelier-norte/uuid?live=$token",
            expiresAtMs = 1_700_014_400_000,
        )

    private fun sampleRecord(
        request: SessionCreateRequest,
        response: SessionCreateResponse,
        createdAtMs: Long,
        notes: SessionClientNotes,
    ) = StoredSessionRecord(
        sessionId = response.sessionId,
        token = response.token,
        url = response.url,
        sellerName = request.sellerName,
        empresaSlug = request.empresaSlug,
        catalogoUuid = request.catalogoUuid,
        catalogoNome = request.catalogoNome,
        clientName = request.clientName,
        clientShop = notes.shop,
        scheduledFor = notes.scheduledFor,
        createdAtMs = createdAtMs,
        expiresAtMs = response.expiresAtMs,
    )

    @Test
    fun toggleAddsAndRemoves() = runTest {
        val sm = model()
        val item = SampleCatalog.products.first()
        sm.toggle(item)
        assertEquals(setOf(item.ref), sm.state.value.selectedSkus)
        sm.toggle(item)
        assertEquals(emptySet(), sm.state.value.selectedSkus)
    }

    @Test
    fun filterChangesVisibleProducts() = runTest {
        val sm = model()
        assertEquals(SampleCatalog.products.size, sm.state.value.visibleProducts.size)
        sm.setFilter(CatalogFilter.Novos)
        val novos = sm.state.value.visibleProducts
        assertTrue(novos.isNotEmpty())
        assertTrue(novos.all { it.tag?.name == "Novo" })
    }

    @Test
    fun skuCountIsSumOfSizesTimesColors() = runTest {
        val sm = model()
        val item = SampleCatalog.products.first()
        sm.toggle(item)
        assertEquals(item.sizes.size * item.colorCount, sm.state.value.skuCount)
    }

    @Test
    fun generateLinkWithoutCatalogLinkShowsError() = runTest {
        val sm = model(
            createSession = { error("não deveria ter sido chamado") },
            persistSession = { _, _, _, _ -> error("não deveria ter sido chamado") },
            catalogLink = CatalogLinkIdentity(empresaSlug = "", catalogoUuid = ""),
        )
        sm.generateLink()
        val state = sm.state.value
        assertEquals("Escolha um catálogo link para convidar o cliente", state.error)
        assertFalse(state.isSubmitting)
        assertNull(state.createdSession)
    }

    @Test
    fun generateLinkSendsCatalogIdentityWithoutProducts() = runTest(dispatcher) {
        var capturedRequest: SessionCreateRequest? = null
        val response = sampleResponse()
        val sm = model(
            createSession = { request ->
                capturedRequest = request
                SessionsApiResult.Ok(response)
            },
            persistSession = { request, resp, createdAt, notes ->
                sampleRecord(request, resp, createdAt, notes)
            },
            nowMs = { 9_999 },
            initialClient = ClientDraft(name = "Diego A", shop = "Trama"),
        )
        sm.toggle(SampleCatalog.products[0])
        sm.generateLink(sellerId = "vend-31", sellerName = "Marina Prado")
        assertTrue(sm.state.value.isSubmitting)
        dispatcher.scheduler.advanceUntilIdle()

        val state = sm.state.value
        assertFalse(state.isSubmitting)
        assertNull(state.error)
        assertNotNull(state.createdSession)
        assertEquals(response.token, state.createdSession!!.token)
        assertEquals(9_999L, state.createdSession!!.createdAtMs)
        assertEquals("Trama", state.createdSession!!.clientShop)

        val request = assertNotNull(capturedRequest)
        assertEquals("atelier-norte", request.empresaSlug)
        assertEquals(catalogLink.catalogoUuid, request.catalogoUuid)
        assertEquals("Outono 26", request.catalogoNome)
        assertEquals("vend-31", request.sellerId)
        assertEquals("Diego A", request.clientName)
    }

    @Test
    fun generateLinkServerFailurePublishesError() = runTest(dispatcher) {
        val sm = model(
            createSession = {
                SessionsApiResult.Fail(code = "missing_catalogo", message = "Bad", status = 400)
            },
            persistSession = { _, _, _, _ -> error("not called") },
        )
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()
        val state = sm.state.value
        assertFalse(state.isSubmitting)
        assertEquals("Bad", state.error)
        assertNull(state.createdSession)
    }

    @Test
    fun consumeCreatedSessionClearsIt() = runTest(dispatcher) {
        val response = sampleResponse()
        val sm = model(
            createSession = { SessionsApiResult.Ok(response) },
            persistSession = { request, resp, createdAt, notes ->
                sampleRecord(request, resp, createdAt, notes)
            },
        )
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sm.state.value.createdSession)
        sm.consumeCreatedSession()
        assertNull(sm.state.value.createdSession)
    }
}
