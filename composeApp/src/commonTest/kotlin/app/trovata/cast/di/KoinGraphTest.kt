package app.trovata.cast.di

import app.trovata.cast.feature.call.CallSpec
import app.trovata.cast.feature.call.PeerSession
import app.trovata.cast.feature.call.newSellerPeerId
import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KoinGraphTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun modulesLoadWithoutConflicts() {
        val koin = startKoin { modules(appModules()) }.koin
        assertTrue(appModules().isNotEmpty())
        assertNotNull(koin.get<CoroutineScope>(AppScope))
    }

    @Test
    fun callScopeWiresSignalingGraph() {
        val koin = startKoin { modules(appModules()) }.koin
        val spec = CallSpec(
            token = "tok-test",
            sessionId = "sess-test",
            empresaSlug = "buba-teste",
            catalogoUuid = "uuid-test",
            clientName = "Cliente",
            sellerName = "Vendedor",
            collectionLabel = "Coleção",
            sellerPeerId = newSellerPeerId(),
        )
        val scope = koin.createScope<CallSession>(spec.sellerPeerId)
        val peer = scope.get<PeerSession> { parametersOf(spec) }
        assertNotNull(peer)
        scope.close()
    }
}
