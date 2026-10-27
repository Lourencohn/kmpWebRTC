package app.trovata.cast.feature.aftercall

import app.trovata.cast.data.local.Appointment
import app.trovata.cast.data.local.CallInterest
import app.trovata.cast.data.local.CallInterestEvent
import app.trovata.cast.data.local.CartProduct
import app.trovata.cast.data.local.CatalogClient
import app.trovata.cast.data.local.OrderLine
import app.trovata.cast.data.local.StoredOrder
import app.trovata.cast.data.local.StoredSessionRecord
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.data.remote.sfa.CartItemLine
import app.trovata.cast.data.remote.sfa.ProductLabel
import app.trovata.cast.feature.agenda.appointment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AfterCallScreenModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Harness(interests: List<CallInterest>, orders: List<StoredOrder> = emptyList()) {
        val interests = MutableStateFlow(interests)
        val orders = MutableStateFlow(orders)
        val labelRequests = mutableListOf<Long>()
        val carts = mutableListOf<List<CartProduct>>()

        fun saveCart(products: List<CartProduct>) {
            carts += products
            interests.update { current ->
                val units = products.associate { it.produtoPreId to it.units }
                val known = current.map { it.copy(cartUnits = units[it.produtoPreId] ?: 0) }
                known + products.filter { product -> current.none { it.produtoPreId == product.produtoPreId } }
                    .map { interest(it.produtoPreId, cartUnits = it.units, name = it.name) }
            }
        }

        fun name(event: CallInterestEvent.Named) {
            interests.update { current ->
                current.map { if (it.produtoPreId == event.produtoPreId) it.copy(name = event.name, ref = event.ref) else it }
            }
        }
    }

    private fun model(
        harness: Harness,
        session: StoredSessionRecord? = session(),
        linked: Appointment? = null,
        client: CatalogClient? = null,
        carrinhoId: Long? = 9021,
        cart: List<CartItemLine> = listOf(cartLine(77, "Vestido Midi", 8)),
    ) = AfterCallScreenModel(
        spec = AfterCallSpec(sessionId = "ses_1", carrinhoId = carrinhoId),
        findSession = { session },
        findAppointment = { linked },
        findClientByEmail = { client },
        observeInterests = { harness.interests },
        observeOrders = { harness.orders },
        loadCartItems = { _, _, _ -> ApiResult.Ok(cart) },
        loadProductLabel = { _, _, produtoPreId ->
            harness.labelRequests += produtoPreId
            ApiResult.Ok(ProductLabel(produtoPreId, ref = "REF-$produtoPreId", name = "Produto nomeado $produtoPreId", imageUrl = null))
        },
        recordCart = { products, _ -> harness.saveCart(products) },
        recordName = harness::name,
        catalogWebUrl = "https://trovata.app.br",
        nowMs = { 1_000 },
    )

    @Test
    fun cartIsRefreshedAndOnlyWhatLeftItIsNamed() = runTest(dispatcher) {
        val harness = Harness(listOf(interest(77, clientOpens = 1), interest(88, clientOpens = 2)))
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(CartProduct(77, "AN-104", "Vestido Midi", null, 8)), harness.carts.single())
        assertEquals(listOf(88L), harness.labelRequests)
        val leftOut = sm.state.value.leftOut.single()
        assertEquals("Produto nomeado 88", leftOut.label)
        assertEquals(1, sm.state.value.inCartCount)
    }

    @Test
    fun orderSummaryGoesToTheSessionPhone() = runTest(dispatcher) {
        val harness = Harness(listOf(interest(77, clientOpens = 1)), orders = listOf(order()))
        val sm = model(harness)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Oi, Marina! Seu pedido ficou pronto: 8 unidades, total de R$ 960,00. " +
                "O resumo completo está aqui: https://trovata.app.br/catalogo-link-resume/buba/uuid-1/carrinho/9021",
            sm.messageForClient(),
        )
        assertTrue(sm.whatsAppLink().orEmpty().startsWith("https://wa.me/5511988887777?text=Oi%2C%20Marina"))
    }

    @Test
    fun phoneFallsBackToTheClientRegistry() = runTest(dispatcher) {
        val harness = Harness(emptyList())
        val sm = model(harness, session = session(phone = null), client = client(phone = "(21) 97777-6666"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("5521977776666", sm.state.value.whatsAppNumber)
    }

    @Test
    fun withoutAnyPhoneThereIsNoDirectLink() = runTest(dispatcher) {
        val harness = Harness(emptyList())
        val sm = model(harness, session = session(phone = null))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(sm.whatsAppLink())
        assertEquals("Oi, Marina! Obrigado pela conversa. Qualquer coisa, é só me chamar.", sm.messageForClient())
    }

    @Test
    fun followUpContinuesTheLinkedAppointmentWithTheLeftOutNotes() = runTest(dispatcher) {
        val harness = Harness(listOf(interest(88, clientOpens = 1)))
        val linked = appointment("apt-1", startsAtMs = 0, sessionId = "ses_1")
        val sm = model(harness, linked = linked)
        dispatcher.scheduler.advanceUntilIdle()

        val draft = sm.followUpDraft()
        assertEquals("apt-1", draft.followUpOfId)
        assertEquals(101L, draft.clienteId)
        assertEquals(
            "Ficou de fora do pedido na última chamada:\n- Produto nomeado 88 · ref. REF-88 · o cliente abriu",
            draft.notes,
        )
        assertEquals("Retomar os produtos que ficaram de fora", draft.objective)
    }

    @Test
    fun followUpWithoutAppointmentCarriesTheClientOfTheSession() = runTest(dispatcher) {
        val harness = Harness(emptyList())
        val sm = model(harness, carrinhoId = null)
        dispatcher.scheduler.advanceUntilIdle()

        val draft = sm.followUpDraft()
        assertNull(draft.followUpOfId)
        assertEquals("Marina Couto", draft.clientName)
        assertEquals("marina@bella.com.br", draft.clientEmail)
        assertEquals("(11) 98888-7777", draft.clientPhone)
        assertNull(draft.notes)
        assertNull(draft.objective)
        assertTrue(harness.carts.isEmpty())
    }

    private fun session(phone: String? = "(11) 98888-7777") = StoredSessionRecord(
        sessionId = "ses_1",
        token = "kP3xq9",
        url = "https://trovata.app.br/catalogo-link-view/buba/uuid-1?live=kP3xq9",
        sellerName = "Lourenço",
        empresaSlug = "buba",
        catalogoUuid = "uuid-1",
        catalogoNome = "Verão 27",
        catalogoLinkId = 4471,
        clientName = "Marina Couto",
        clientEmail = "marina@bella.com.br",
        clientShop = null,
        scheduledFor = null,
        createdAtMs = 0,
        expiresAtMs = 10_000,
        clientPhone = phone,
    )

    private fun client(phone: String?) = CatalogClient(
        id = 101,
        ref = "C-101",
        name = "Loja Bella",
        legalName = null,
        document = null,
        phone = phone,
        email = "marina@bella.com.br",
        contact = null,
        active = true,
    )

    private fun order() = StoredOrder(
        orderId = "CAR-9021",
        sessionId = "ses_1",
        sessionToken = "kP3xq9",
        clientName = "Marina Couto",
        clientShop = null,
        sellerName = "Lourenço",
        totalCents = 96_000,
        confirmedByMe = true,
        createdAtMs = 0,
        lines = listOf(OrderLine(productId = "AN-104", size = "P", units = 8, unitPriceCents = 12_000)),
    )

    private fun cartLine(produtoPreId: Long, name: String, units: Int) = CartItemLine(
        itemId = produtoPreId,
        produtoPreId = produtoPreId,
        ref = "AN-104",
        name = name,
        color = null,
        imageUrl = null,
        quantity = units,
        totalCents = 96_000,
        sizes = emptyList(),
    )
}

private fun interest(
    produtoPreId: Long,
    clientOpens: Int = 0,
    cartUnits: Int = 0,
    name: String? = null,
) = CallInterest(
    produtoPreId = produtoPreId,
    clientOpens = clientOpens,
    sellerShows = 0,
    draftUnits = 0,
    cartUnits = cartUnits,
    ref = null,
    name = name,
    imageUrl = null,
    firstAtMs = produtoPreId,
    lastAtMs = produtoPreId,
)
