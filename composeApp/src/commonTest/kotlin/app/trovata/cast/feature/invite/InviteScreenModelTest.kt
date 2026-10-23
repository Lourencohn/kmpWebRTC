package app.trovata.cast.feature.invite

import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.protocol.SessionInfo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class InviteScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val now = 1_756_800_000_000L

    private val record = StoredSessionRecord(
        sessionId = "ses_kP3xq9Trz",
        token = "kP3xq9Trz",
        url = "https://trovata.app.br/catalogo-link-view/buba-teste/uuid?live=kP3xq9Trz",
        sellerName = "Oswaldo",
        empresaSlug = "buba-teste",
        catalogoUuid = "uuid",
        catalogoNome = "Outono 26",
        catalogoLinkId = 4471,
        clientName = "Loja Bella",
        clientEmail = "compras@bella.com.br",
        clientShop = null,
        scheduledFor = null,
        createdAtMs = now - 3_600_000,
        expiresAtMs = now + 3_600_000,
    )

    private val sessionInfo = SessionInfo(
        token = record.token,
        empresaSlug = record.empresaSlug,
        catalogoUuid = record.catalogoUuid,
        sellerName = record.sellerName,
        createdAtMs = record.createdAtMs,
        expiresAtMs = record.expiresAtMs,
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
        stored: StoredSessionRecord = record,
        fetchSession: FetchSessionFn = { ApiResult.Ok(sessionInfo) },
    ) = InviteScreenModel(
        record = stored,
        fetchSession = fetchSession,
        nowMs = { now },
    )

    @Test
    fun liveSessionOpensTheCall() = runTest(dispatcher) {
        val checkedTokens = mutableListOf<String>()
        val sm = model(fetchSession = { token ->
            checkedTokens += token
            ApiResult.Ok(sessionInfo)
        })

        sm.startCall()
        assertTrue(sm.state.value.isCheckingSession)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(record.token), checkedTokens)
        assertTrue(sm.state.value.callReady)
        assertFalse(sm.state.value.isCheckingSession)
        assertNull(sm.state.value.error)

        sm.consumeCallReady()
        assertFalse(sm.state.value.callReady)
    }

    @Test
    fun expiredInviteIsBlockedWithoutAskingTheServer() = runTest(dispatcher) {
        val sm = model(
            stored = record.copy(expiresAtMs = now - 1),
            fetchSession = { error("not called") },
        )

        sm.startCall()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(sm.state.value.callReady)
        assertEquals("Esse convite expirou. Crie um novo convite para o cliente.", sm.state.value.error)
    }

    @Test
    fun sessionMissingOnServerIsBlocked() = runTest(dispatcher) {
        val sm = model(fetchSession = {
            ApiResult.Fail(code = "not_found", message = "Sessão não encontrada ou expirada", status = 404)
        })

        sm.startCall()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(sm.state.value.callReady)
        assertFalse(sm.state.value.isCheckingSession)
        assertEquals("Esse convite não vale mais. Crie um novo convite para o cliente.", sm.state.value.error)
    }

    @Test
    fun inconclusiveCheckStillOpensTheCall() = runTest(dispatcher) {
        val sm = model(fetchSession = {
            ApiResult.Fail(code = "network_error", message = "timeout", status = 0)
        })

        sm.startCall()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(sm.state.value.callReady)
        assertNull(sm.state.value.error)
    }

    @Test
    fun repeatedTapWhileCheckingAsksTheServerOnce() = runTest(dispatcher) {
        var calls = 0
        val sm = model(fetchSession = {
            calls++
            ApiResult.Ok(sessionInfo)
        })

        sm.startCall()
        sm.startCall()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, calls)
    }
}
