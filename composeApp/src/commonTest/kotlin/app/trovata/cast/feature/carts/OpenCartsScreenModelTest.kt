package app.trovata.cast.feature.carts

import app.trovata.cast.data.local.SessionClientNotes
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.SessionsApiResult
import app.trovata.cast.data.remote.sfa.OpenCartsPage
import app.trovata.cast.data.remote.sfa.SfaApiResult
import app.trovata.cast.feature.catalog.CreateSessionFn
import app.trovata.cast.feature.catalog.PersistSessionFn
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
class OpenCartsScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val agora = 1_756_800_000_000L

    private val retomavel = OpenCart(
        carrinhoId = 9021,
        catalogoLinkId = 4471,
        catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        catalogoNome = "Outono 26",
        catalogoAtivo = true,
        catalogoValidadeMs = agora + 30L * 24 * 3_600_000,
        clienteNome = "Loja Bella",
        clienteEmail = "compras@bella.com.br",
        situacao = CartSituacao.Digitando,
        itens = 12,
        quantidadeTotal = 48,
        valorTotalCents = 420_000,
        atualizadoEmMs = agora - 3L * 24 * 3_600_000,
    )

    private val semEmail = retomavel.copy(
        carrinhoId = 9022,
        clienteNome = "Loja Sem Contato",
        clienteEmail = null,
    )

    private val catalogoExpirado = retomavel.copy(
        carrinhoId = 9023,
        clienteNome = "Loja Atrasada",
        catalogoValidadeMs = agora - 24 * 3_600_000,
    )

    private val jaEnviado = retomavel.copy(
        carrinhoId = 9024,
        clienteNome = "Loja Fechada",
        situacao = CartSituacao.Enviado,
    )

    private val vazio = retomavel.copy(
        carrinhoId = 9025,
        clienteNome = "Loja Curiosa",
        itens = 0,
        quantidadeTotal = 0,
        valorTotalCents = 0,
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
        carts: List<OpenCart> = listOf(retomavel, semEmail, catalogoExpirado, jaEnviado, vazio),
        loadResult: SfaApiResult<OpenCartsPage>? = null,
        createSession: CreateSessionFn = { error("not called") },
        persistSession: PersistSessionFn = { _, _, _, _ -> error("not called") },
        empresaSlug: String = "buba-teste",
        onLoad: (String, CartSituacao?, OpenCartsOrder, String?) -> Unit = { _, _, _, _ -> },
    ) = OpenCartsScreenModel(
        loadOpenCarts = { slug, situacao, order, search ->
            onLoad(slug, situacao, order, search)
            loadResult ?: SfaApiResult.Ok(OpenCartsPage(carts = carts, total = carts.size, hasMore = false))
        },
        createSession = createSession,
        persistSession = persistSession,
        empresaSlugProvider = { empresaSlug },
        companyNameProvider = { "GRUPO MOAS" },
        nowMs = { agora },
    )

    private fun response(token: String = "kP3xq9Trz") = SessionCreateResponse(
        sessionId = "ses_$token",
        token = token,
        url = "https://trovata.app.br/catalogo-link-view/buba-teste/${retomavel.catalogoUuid}?live=$token",
        expiresAtMs = agora + 3_600_000,
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
    fun carregaCarrinhosDigitandoNaAbertura() = runTest(dispatcher) {
        var slugUsado: String? = null
        var situacaoUsada: CartSituacao? = null
        var ordemUsada: OpenCartsOrder? = null
        val sm = model(onLoad = { slug, situacao, order, _ ->
            slugUsado = slug
            situacaoUsada = situacao
            ordemUsada = order
        })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(sm.state.value.isLoading)
        assertEquals("buba-teste", slugUsado)
        assertEquals(CartSituacao.Digitando, situacaoUsada)
        assertEquals(OpenCartsOrder.MaisRecentes, ordemUsada)
        assertEquals(5, sm.state.value.carts.size)
    }

    @Test
    fun filtroRetomaveisMostraSoQuemAChamadaConsegueRetomar() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(retomavel.carrinhoId), sm.state.value.visibleCarts.map { it.carrinhoId })
        assertEquals(420_000L, sm.state.value.valorRetomavelCents)

        sm.setFilter(OpenCartsFilter.Todos)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, sm.state.value.visibleCarts.size)
    }

    @Test
    fun filtroTodosPedeListaSemSituacaoAoServidor() = runTest(dispatcher) {
        val situacoes = mutableListOf<CartSituacao?>()
        val sm = model(onLoad = { _, situacao, _, _ -> situacoes.add(situacao) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.setFilter(OpenCartsFilter.Todos)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(CartSituacao.Digitando, null), situacoes)
    }

    @Test
    fun trocaDeOrdemRecarregaComDirecaoInvertida() = runTest(dispatcher) {
        val ordens = mutableListOf<OpenCartsOrder>()
        val sm = model(onLoad = { _, _, order, _ -> ordens.add(order) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.toggleOrder()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(OpenCartsOrder.MaisRecentes, OpenCartsOrder.ParadosHaMaisTempo), ordens)
        assertEquals("asc", sm.state.value.order.direction)
    }

    @Test
    fun buscaVaiParaOServidor() = runTest(dispatcher) {
        val buscas = mutableListOf<String?>()
        val sm = model(onLoad = { _, _, _, search -> buscas.add(search) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.setSearch("  bella  ")
        sm.submitSearch()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(null, "bella"), buscas)
    }

    @Test
    fun semEmpresaSelecionadaNaoChamaApi() = runTest(dispatcher) {
        var chamou = false
        val sm = model(empresaSlug = "", onLoad = { _, _, _, _ -> chamou = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(chamou)
        assertEquals("Selecione uma empresa para ver os carrinhos", sm.state.value.error)
        assertFalse(sm.state.value.isLoading)
    }

    @Test
    fun falhaDaApiViraMensagemNaTela() = runTest(dispatcher) {
        val sm = model(
            loadResult = SfaApiResult.Fail("http_401", "Sessão expirada. Faça login novamente.", 401),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Sessão expirada. Faça login novamente.", sm.state.value.error)
        assertTrue(sm.state.value.carts.isEmpty())
    }

    @Test
    fun chamarAoVivoAbreSessaoNoCatalogoEComOEmailDoCarrinho() = runTest(dispatcher) {
        var captured: SessionCreateRequest? = null
        val resp = response()
        val sm = model(
            createSession = { request ->
                captured = request
                SessionsApiResult.Ok(resp)
            },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(retomavel, sellerId = "vend-31", sellerName = "Marina Prado")
        assertEquals(retomavel.carrinhoId, sm.state.value.callingCartId)
        dispatcher.scheduler.advanceUntilIdle()

        val request = assertNotNull(captured)
        assertEquals("buba-teste", request.empresaSlug)
        assertEquals(retomavel.catalogoUuid, request.catalogoUuid)
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
    fun carrinhoSemEmailNaoAbreSessaoEExplicaOMotivo() = runTest(dispatcher) {
        var chamou = false
        val sm = model(createSession = { chamou = true; SessionsApiResult.Ok(response()) })
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(semEmail)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(chamou)
        assertNull(sm.state.value.callingCartId)
        assertEquals(
            "Esse carrinho não tem e-mail de cliente. Sem ele a chamada não abre o carrinho.",
            sm.state.value.error,
        )
    }

    @Test
    fun carrinhoForaDeDigitandoAvisaQueAChamadaAbririaOutroCarrinho() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(jaEnviado)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Esse carrinho está enviado. A chamada abriria um carrinho novo, não esse.",
            sm.state.value.error,
        )
    }

    @Test
    fun catalogoExpiradoBloqueiaAChamada() = runTest(dispatcher) {
        val sm = model()
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(catalogoExpirado)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "O catálogo desse carrinho está expirado. Renove a validade no Catálogo Link.",
            sm.state.value.error,
        )
    }

    @Test
    fun erroDoServidorNaCriacaoLiberaOBotao() = runTest(dispatcher) {
        val sm = model(
            createSession = {
                SessionsApiResult.Fail("sessao_indisponivel", "Não foi possível abrir a sessão agora.", 503)
            },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(retomavel)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Não foi possível abrir a sessão agora.", sm.state.value.error)
        assertNull(sm.state.value.createdSession)
        assertNull(sm.state.value.callingCartId)
    }

    @Test
    fun consumeCreatedSessionLimpaOEstado() = runTest(dispatcher) {
        val sm = model(
            createSession = { SessionsApiResult.Ok(response()) },
            persistSession = { request, r, createdAt, notes -> record(request, r, createdAt, notes) },
        )
        dispatcher.scheduler.advanceUntilIdle()

        sm.callLive(retomavel)
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(sm.state.value.createdSession)

        sm.consumeCreatedSession()
        assertNull(sm.state.value.createdSession)
    }

    @Test
    fun rotuloDeTempoParadoCobreOsIntervalosDaLista() {
        assertEquals("mexeu agora", tempoParadoLabel(agora, agora))
        assertEquals("sem mexer há 5 min", tempoParadoLabel(agora - 5 * 60_000, agora))
        assertEquals("sem mexer há 3h", tempoParadoLabel(agora - 3 * 3_600_000, agora))
        assertEquals("sem mexer há 1 dia", tempoParadoLabel(agora - 24 * 3_600_000, agora))
        assertEquals("sem mexer há 3 dias", tempoParadoLabel(agora - 3L * 24 * 3_600_000, agora))
        assertEquals("sem mexer há 2 meses", tempoParadoLabel(agora - 61L * 24 * 3_600_000, agora))
        assertEquals("sem data de alteração", tempoParadoLabel(null, agora))
    }
}
