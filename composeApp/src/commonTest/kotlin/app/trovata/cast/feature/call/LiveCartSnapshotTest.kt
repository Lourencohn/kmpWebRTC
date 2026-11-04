package app.trovata.cast.feature.call

import app.trovata.cast.protocol.PeerRole
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class LiveCartSnapshotTest {
    private fun cart(id: Long, status: String) = CartSession(id, null, "Cliente", "cliente@local.test", 0, status)

    @Test
    fun editableCartDoesNotReopen() = runTest {
        val result = loadLiveCart(1, { ApiResult.Ok(cart(1, "D")) }, { error("must not open") }, {
            assertEquals(1L, it)
            ApiResult.Ok(emptyList())
        })
        val snapshot = assertIs<ApiResult.Ok<LiveCartSnapshot>>(result).value
        assertEquals(1L, snapshot.session.id)
        assertNull(snapshot.completedCarrinhoId)
    }

    @Test
    fun finalizedCartLoadsSuccessorItems() = runTest {
        for (status in listOf("F", "PE", "E", "C")) {
            val result = loadLiveCart(1, { ApiResult.Ok(cart(1, status)) }, { ApiResult.Ok(cart(2, "D")) }, {
                assertEquals(2L, it)
                ApiResult.Ok(emptyList())
            })
            val snapshot = assertIs<ApiResult.Ok<LiveCartSnapshot>>(result).value
            assertEquals(2L, snapshot.session.id)
            assertEquals(if (status == "C") null else 1L, snapshot.completedCarrinhoId)
        }
    }

    @Test
    fun summaryFailureDoesNotCreateCart() = runTest {
        val failure = ApiResult.Fail("offline", "offline", 0)
        assertEquals(failure, loadLiveCart(1, { failure }, { error("must not open") }, { error("must not load") }))
    }

    @Test
    fun successorMustBeEditable() = runTest {
        val result = loadLiveCart(1, { ApiResult.Ok(cart(1, "PE")) }, { ApiResult.Ok(cart(1, "PE")) }, { error("must not load") })
        assertEquals("cart_not_editable", assertIs<ApiResult.Fail>(result).code)
    }

    @Test
    fun itemFailureDoesNotPublishPartialSnapshot() = runTest {
        val failure = ApiResult.Fail("offline", "offline", 0)
        assertEquals(failure, loadLiveCart(1, { ApiResult.Ok(cart(1, "D")) }, { error("must not open") }, { failure }))
    }
    @Test
    fun reviewUsesCompletedCartWhenSuccessorIsEmpty() {
        val state = LiveCallUiState(token = "token", role = PeerRole.Seller, pageUrl = "http://localhost", carrinhoId = 2, completedCarrinhoId = 1)
        assertEquals(1L, state.reviewCarrinhoId)
    }

    @Test
    fun reviewUsesCurrentCartWithoutCompletion() {
        val state = LiveCallUiState(token = "token", role = PeerRole.Seller, pageUrl = "http://localhost", carrinhoId = 2)
        assertEquals(2L, state.reviewCarrinhoId)
    }

    @Test
    fun reviewUsesNewPurchaseWhenItHasItems() {
        val line = CartLineUi(1, 2101, "VA-2101", "Camisa", null, null, 1, 8990, emptyList())
        val state = LiveCallUiState(token = "token", role = PeerRole.Seller, pageUrl = "http://localhost", carrinhoId = 2, completedCarrinhoId = 1, cart = listOf(line))
        assertEquals(2L, state.reviewCarrinhoId)
    }

}
