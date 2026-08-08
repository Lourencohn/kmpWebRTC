package app.trovata.cast.feature.catalog

import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.SessionsApiResult
import app.trovata.cast.data.remote.sfa.CatalogLinkPage
import app.trovata.cast.data.remote.sfa.SfaApiResult
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CatalogLinkPickerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val vigente = SellerCatalogLink(
        id = 4471,
        uuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        nome = "Outono 26",
        clienteNome = "Trama Multimarcas",
        vendedorNome = "Marina Prado",
        ativo = true,
        expirado = false,
        validadeLabel = "2026-12-31",
        totalCarrinhos = 3,
        totalVisualizacoes = 42,
    )

    private val expirado = vigente.copy(
        id = 4472,
        uuid = "aaaa1111-8a41-4f0b-9c3d-77b2a0e14c9f",
        nome = "Verão 25",
        expirado = true,
    )

    private val inativo = vigente.copy(
        id = 4473,
        uuid = "bbbb2222-8a41-4f0b-9c3d-77b2a0e14c9f",
        nome = "Inverno 25",
        ativo = false,
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
        links: List<SellerCatalogLink> = listOf(vigente, expirado, inativo),
        loadResult: SfaApiResult<CatalogLinkPage> =
            SfaApiResult.Ok(CatalogLinkPage(links = links, total = links.size, hasMore = false)),
        createSession: CreateSessionFn = { error("not called") },
        persistSession: PersistSessionFn = { _, _, _, _ -> error("not called") },
        empresaSlug: String = "buba-teste",
        onLoad: (String) -> Unit = {},
        initialClient: ClientDraft = ClientDraft(),
    ) = CatalogLinkPickerScreenModel(
        loadCatalogLinks = { slug, _ ->
            onLoad(slug)
            loadResult
        },
        createSession = createSession,
        persistSession = persistSession,
        empresaSlugProvider = { empresaSlug },
        companyNameProvider = { "GRUPO MOAS" },
        nowMs = { 9_999 },
        initialClient = initialClient,
    )

    private fun response(token: String = "kP3xq9Trz") = SessionCreateResponse(
        sessionId = "ses_$token",
        token = token,
        url = "https://trovata.app.br/catalogo-link-view/buba-teste/${vigente.uuid}?live=$token",
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
        clientName = request.clientName,
        clientShop = notes.shop,
        scheduledFor = notes.scheduledFor,
        createdAtMs = createdAt,
        expiresAtMs = resp.expiresAtMs,
    )

    @Test
    fun carregaCatalogosDoVendedorNaAbertura() = runTest(dispatcher) {
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
    fun filtroPadraoEscondeExpiradosEInativos() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(vigente.uuid), sm.state.value.visibleLinks.map { it.uuid })
        assertEquals(2, sm.state.value.unavailableCount)

        sm.setFilter(CatalogLinkFilter.Todos)
        assertEquals(3, sm.state.value.visibleLinks.size)
    }

    @Test
    fun semEmpresaSelecionadaNaoChamaApi() = runTest(dispatcher) {
        var chamou = false
        val sm = model(empresaSlug = "", onLoad = { chamou = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(chamou)
        assertEquals("Selecione uma empresa para ver seus catálogos", sm.state.value.error)
        assertFalse(sm.state.value.isLoading)
    }

    @Test
    fun falhaDaApiViraMensagemNaTela() = runTest(dispatcher) {
        val sm = model(
            loadResult = SfaApiResult.Fail("http_401", "Sessão expirada. Faça login novamente.", 401),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sessão expirada. Faça login novamente.", sm.state.value.error)
        assertTrue(sm.state.value.links.isEmpty())
    }

    @Test
    fun selecionarExpiradoExplicaOMotivo() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(expirado)
        assertNull(sm.state.value.selectedUuid)
        assertEquals(
            "Esse catálogo está expirado. Renove a validade no Catálogo Link.",
            sm.state.value.error,
        )

        sm.clearError()
        sm.select(inativo)
        assertEquals("Esse catálogo está inativo.", sm.state.value.error)
    }

    @Test
    fun selecionarDuasVezesDesmarca() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(vigente)
        assertEquals(vigente.uuid, sm.state.value.selectedUuid)
        assertTrue(sm.state.value.canGenerate)

        sm.select(vigente)
        assertNull(sm.state.value.selectedUuid)
        assertFalse(sm.state.value.canGenerate)
    }

    @Test
    fun gerarLinkSemSelecaoAvisa() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.generateLink()
        assertEquals("Escolha um catálogo link para convidar o cliente", sm.state.value.error)
        assertFalse(sm.state.value.isSubmitting)
    }

    @Test
    fun gerarLinkEnviaIdentidadeDoCatalogoEscolhido() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val resp = response()
        val sm = model(
            createSession = { request ->
                captured = request
                SessionsApiResult.Ok(resp)
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
            initialClient = ClientDraft(name = "Diego Albuquerque"),
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(vigente)
        sm.generateLink(sellerId = "vend-31", sellerName = "Marina Prado")
        assertTrue(sm.state.value.isSubmitting)
        dispatcher.scheduler.advanceUntilIdle()

        val request = assertNotNull(captured)
        assertEquals("buba-teste", request.empresaSlug)
        assertEquals(vigente.uuid, request.catalogoUuid)
        assertEquals("Outono 26", request.catalogoNome)
        assertEquals("vend-31", request.sellerId)
        assertEquals("Diego Albuquerque", request.clientName)

        val created = assertNotNull(sm.state.value.createdSession)
        assertEquals(resp.token, created.token)
        assertTrue(created.url.contains("/catalogo-link-view/buba-teste/${vigente.uuid}?live="))
        assertEquals("Trama Multimarcas", created.clientShop)
        assertFalse(sm.state.value.isSubmitting)
    }

    @Test
    fun clienteDoCatalogoViraNomeQuandoNaoHaRascunho() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val sm = model(
            createSession = { request ->
                captured = request
                SessionsApiResult.Ok(response())
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(vigente)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Trama Multimarcas", assertNotNull(captured).clientName)
    }

    @Test
    fun erroDoServidorNaCriacaoApareceNaTela() = runTest(dispatcher) {
        val sm = model(
            createSession = {
                SessionsApiResult.Fail(
                    code = "catalogo_indisponivel",
                    message = "Catálogo expirado. Renove a validade no Catálogo Link.",
                    status = 422,
                )
            },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(vigente)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Catálogo expirado. Renove a validade no Catálogo Link.", sm.state.value.error)
        assertNull(sm.state.value.createdSession)
        assertFalse(sm.state.value.isSubmitting)
    }

    @Test
    fun refreshPreservaSelecaoQuandoCatalogoAindaExiste() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()
        sm.select(vigente)

        sm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(vigente.uuid, sm.state.value.selectedUuid)
    }

    @Test
    fun consumeCreatedSessionLimpaOEstado() = runTest(dispatcher) {
        val sm = model(
            createSession = { SessionsApiResult.Ok(response()) },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.select(vigente)
        sm.generateLink()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sm.state.value.createdSession)

        sm.consumeCreatedSession()
        assertNull(sm.state.value.createdSession)
    }
}
