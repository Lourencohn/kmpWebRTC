package app.trovata.cast.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataChannelMessageTest {

    @Test
    fun mute_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.Mute(
            muted = true,
            ts = 1_700_000_000_000,
            from = "seller-1",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun scroll_roundtrip_anchored_on_product() {
        val original: DataChannelMessage = DataChannelMessage.Scroll(
            anchor = ScrollAnchor(
                page = 2,
                produtoPreId = 8813,
                itemOffsetRatio = 0.42f,
                viewportRatio = 0.31f,
            ),
            ts = 1_700_000_000_000,
            from = "seller-1",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun scroll_falls_back_to_viewport_ratio_without_product() {
        val original: DataChannelMessage = DataChannelMessage.Scroll(
            anchor = ScrollAnchor(viewportRatio = 0.75f),
            ts = 1L,
            from = "buyer-1",
        )
        val decoded = decodeDataChannel(original.encode()) as DataChannelMessage.Scroll
        assertNull(decoded.anchor.produtoPreId)
        assertEquals(1, decoded.anchor.page)
        assertEquals(0.75f, decoded.anchor.viewportRatio)
    }

    @Test
    fun pointAt_roundtrip_with_default_duration() {
        val original: DataChannelMessage = DataChannelMessage.PointAt(
            target = LiveAnchor.product(produtoPreId = 8813, complemento1Id = 44),
            xRatio = 0.2f,
            yRatio = 0.8f,
            ts = 1_700_000_000_000,
            from = "seller-1",
        )
        val raw = original.encode()
        val decoded = decodeDataChannel(raw) as DataChannelMessage.PointAt
        assertEquals(original, decoded)
        assertEquals(3_000L, decoded.durationMs)
        assertEquals(8813L, LiveAnchor.produtoPreIdOf(decoded.target))
    }

    @Test
    fun pointAt_targets_an_action() {
        val original: DataChannelMessage = DataChannelMessage.PointAt(
            target = LiveAnchor.action("finalizar"),
            ts = 1_700_000_000_000,
            from = "seller-1",
            durationMs = 5_000,
        )
        val decoded = decodeDataChannel(original.encode()) as DataChannelMessage.PointAt
        assertEquals(original, decoded)
        assertNull(LiveAnchor.produtoPreIdOf(decoded.target))
    }

    @Test
    fun discriminator_uses_type_field() {
        val raw = DataChannelMessage.Mute(muted = false, ts = 1L, from = "buyer-2").encode()
        assertTrue(raw.contains("\"type\":\"mute\""))
    }

    @Test
    fun navigate_carries_route_query_and_focus() {
        val original: DataChannelMessage = DataChannelMessage.Navigate(
            view = ViewState(
                route = CatalogRoute.Secao(tabela = "grupo_produto", tabelaId = "17"),
                query = mapOf("search" to "camisa", "page" to "2"),
                focus = ProductFocus(produtoPreId = 8813, produtoPre1Id = 4410, complemento1Id = 44),
            ),
            ts = 1_700_000_000_500,
            from = "buyer-xyz",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun navigate_to_object_route_roundtrips() {
        val original: DataChannelMessage = DataChannelMessage.Navigate(
            view = ViewState(route = CatalogRoute.Carrinho),
            ts = 1L,
            from = "seller-1",
        )
        val decoded = decodeDataChannel(original.encode()) as DataChannelMessage.Navigate
        assertEquals(CatalogRoute.Carrinho, decoded.view.route)
        assertTrue(decoded.view.query.isEmpty())
        assertNull(decoded.view.focus)
    }

    @Test
    fun cartInvalidated_carries_no_cart_state() {
        val original: DataChannelMessage = DataChannelMessage.CartInvalidated(
            carrinhoId = 90_112,
            reason = CartChangeReason.ItemAdded,
            ts = 1_700_000_000_750,
            from = "buyer-xyz",
            hint = CartChangeHint(produtoPreId = 8813, unitsDelta = 12, label = "Camisa Linho"),
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
        assertTrue(raw.contains("\"reason\":\"itemAdded\""))
    }

    @Test
    fun cartInvalidated_hint_is_optional() {
        val original: DataChannelMessage = DataChannelMessage.CartInvalidated(
            carrinhoId = 90_112,
            reason = CartChangeReason.Cleared,
            ts = 1L,
            from = "seller-1",
        )
        val decoded = decodeDataChannel(original.encode()) as DataChannelMessage.CartInvalidated
        assertNull(decoded.hint)
    }

    @Test
    fun orderPlaced_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.OrderPlaced(
            carrinhoId = 90_112,
            ts = 1_700_000_000_900,
            from = "seller-1",
            pedidoId = "PED-2026-4471",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
    }

    @Test
    fun unknown_payload_returns_null() {
        assertNull(decodeDataChannel("{\"type\":\"unknown-thing\",\"foo\":1}"))
        assertNull(decodeDataChannel("not json"))
        assertNull(decodeDataChannel("{\"type\":\"cartUpdate\",\"productId\":\"AN-104\"}"))
    }
}
