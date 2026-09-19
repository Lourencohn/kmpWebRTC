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

class DrawMessageTest {

    @Test
    fun draw_roundtrip_anchored_on_product() {
        val original: DataChannelMessage = DataChannelMessage.Draw(
            strokeId = "seller-embed-7",
            target = LiveAnchor.product(produtoPreId = 8813),
            phase = DrawPhase.Move,
            points = listOf(DrawPoint(0.12f, 0.4f), DrawPoint(0.5f, 0.55f)),
            ts = 1_700_000_000_000,
            from = "seller-embed",
            color = "#2456E0",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
        assertTrue(raw.contains("\"type\":\"draw\""))
        assertTrue(raw.contains("\"phase\":\"move\""))
    }

    @Test
    fun draw_points_may_leave_the_anchor_box() {
        val original: DataChannelMessage = DataChannelMessage.Draw(
            strokeId = "s1",
            target = LiveAnchor.product(produtoPreId = 8813),
            phase = DrawPhase.End,
            points = listOf(DrawPoint(-0.3f, 1.8f)),
            ts = 1L,
            from = "seller-embed",
        )
        val decoded = decodeDataChannel(original.encode()) as DataChannelMessage.Draw
        assertEquals(-0.3f, decoded.points.single().x)
        assertEquals(1.8f, decoded.points.single().y)
        assertNull(decoded.color)
    }

    @Test
    fun draw_can_fall_back_to_viewport_anchor() {
        val original: DataChannelMessage = DataChannelMessage.Draw(
            strokeId = "s2",
            target = LiveAnchor.viewport(),
            phase = DrawPhase.Start,
            points = listOf(DrawPoint(0.5f, 0.5f)),
            ts = 1L,
            from = "buyer-1",
        )
        val decoded = decodeDataChannel(original.encode()) as DataChannelMessage.Draw
        assertTrue(LiveAnchor.isViewport(decoded.target))
        assertNull(LiveAnchor.produtoPreIdOf(decoded.target))
    }

    @Test
    fun product_modal_anchor_still_resolves_the_product() {
        val target = LiveAnchor.productModal(8813)
        assertEquals("produto:8813:modal", target)
        assertTrue(LiveAnchor.isProductModal(target))
        assertEquals(8813L, LiveAnchor.produtoPreIdOf(target))
        assertTrue(!LiveAnchor.isProductModal(LiveAnchor.product(8813, complemento1Id = 44)))
    }

    @Test
    fun image_anchor_keeps_the_source_url_intact() {
        val src = "https://cdn.example.com/empresa_97/PRATO_00.jpg?w=800"
        val target = LiveAnchor.image(src)
        assertEquals("imagem:$src", target)
        assertEquals(src, LiveAnchor.imageSrcOf(target))
        assertNull(LiveAnchor.imageSrcOf(LiveAnchor.product(8813)))
        assertNull(LiveAnchor.produtoPreIdOf(target))
    }

    @Test
    fun drawClear_defaults_to_everything() {
        val original: DataChannelMessage = DataChannelMessage.DrawClear(ts = 1L, from = "seller-embed")
        val raw = original.encode()
        val decoded = decodeDataChannel(raw) as DataChannelMessage.DrawClear
        assertNull(decoded.strokeId)
        assertTrue(raw.contains("\"type\":\"drawClear\""))
    }

    @Test
    fun drawClear_can_target_one_stroke() {
        val original: DataChannelMessage = DataChannelMessage.DrawClear(
            ts = 1L,
            from = "seller-embed",
            strokeId = "s1",
        )
        assertEquals(original, decodeDataChannel(original.encode()))
    }

    @Test
    fun draw_from_the_web_side_decodes_without_optional_fields() {
        val raw = """{"type":"draw","strokeId":"s9","target":"produto:8813","phase":"start","points":[{"x":0.1,"y":0.2}],"ts":5,"from":"seller-embed"}"""
        val decoded = decodeDataChannel(raw) as DataChannelMessage.Draw
        assertEquals("s9", decoded.strokeId)
        assertEquals(DrawPhase.Start, decoded.phase)
        assertEquals(1, decoded.points.size)
    }
}

class QuantityDraftTest {

    @Test
    fun quantityDraft_roundtrip() {
        val original: DataChannelMessage = DataChannelMessage.QuantityDraft(
            produtoPreId = 4933,
            gradeKey = "0:77:12",
            units = 2,
            ts = 1_700_000_000_000,
            from = "seller-embed",
        )
        val raw = original.encode()
        assertEquals(original, decodeDataChannel(raw))
        assertTrue(raw.contains("\"type\":\"quantityDraft\""))
    }

    @Test
    fun quantityDraft_from_the_web_side_decodes() {
        val raw = """{"type":"quantityDraft","produtoPreId":4933,"gradeKey":"0:77:12","units":0,"ts":5,"from":"buyer-1"}"""
        val decoded = decodeDataChannel(raw) as DataChannelMessage.QuantityDraft
        assertEquals(0, decoded.units)
        assertEquals("0:77:12", decoded.gradeKey)
    }
}

class DataChannelEnvelopeTest {

    @Test
    fun unknown_type_with_envelope_fields_is_still_an_envelope() {
        val raw = """{"type":"somethingNewer","payload":{"x":1},"ts":5,"from":"buyer-1"}"""
        assertTrue(isDataChannelEnvelope(raw))
        assertNull(decodeDataChannel(raw))
    }

    @Test
    fun envelope_requires_type_from_and_numeric_ts() {
        assertTrue(!isDataChannelEnvelope("""{"type":"mute","muted":true}"""))
        assertTrue(!isDataChannelEnvelope("""{"type":"","ts":1,"from":"a"}"""))
        assertTrue(!isDataChannelEnvelope("""{"type":"mute","ts":"x","from":"a"}"""))
        assertTrue(!isDataChannelEnvelope("not json"))
        assertTrue(!isDataChannelEnvelope("[1,2]"))
    }
}
