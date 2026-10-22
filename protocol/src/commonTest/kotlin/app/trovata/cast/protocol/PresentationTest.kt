package app.trovata.cast.protocol

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class PresentationTest {

    private val timeline = PresentationTimeline(
        presentationId = "p7Kq2mZr8XwT4nBc",
        empresaSlug = "atelier-norte",
        catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        sellerName = "Marina Prado",
        recordedAtMs = 1_790_000_000_000,
        durationMs = 42_500,
        catalogoNome = "Outono 26",
        events = listOf(
            PresentationEvent(
                t = 0,
                message = DataChannelMessage.Navigate(
                    view = ViewState(route = CatalogRoute.Todos),
                    ts = 1_790_000_000_010,
                    from = "seller-embed",
                ),
            ),
            PresentationEvent(
                t = 1_250,
                message = DataChannelMessage.Draw(
                    strokeId = "stroke-1",
                    target = "produto:4932",
                    phase = DrawPhase.Start,
                    points = listOf(DrawPoint(0.25f, 0.5f)),
                    ts = 1_790_000_001_260,
                    from = "seller-embed",
                ),
            ),
        ),
    )

    @Test
    fun timelineRoundTripsThroughItsOwnCodec() {
        assertEquals(timeline, DataChannelJson.decodeFromString(PresentationTimeline.serializer(), timeline.encode()))
    }

    @Test
    fun timelineEventsKeepTheDataChannelWireFormat() {
        val events = DataChannelJson.parseToJsonElement(timeline.encode()).jsonObject["events"]!!.jsonArray
        val draw = events[1].jsonObject["message"]!!.jsonObject
        assertEquals("draw", draw["type"]!!.jsonPrimitive.content)
        assertEquals("produto:4932", draw["target"]!!.jsonPrimitive.content)
        assertEquals("1250", events[1].jsonObject["t"]!!.jsonPrimitive.content)
    }

    @Test
    fun timelineDefaultsToCurrentVersionAndAudioFormat() {
        assertEquals(PresentationTimelineVersion, timeline.version)
        assertEquals("audio/mp4", timeline.audioContentType)
    }

    @Test
    fun presentationUrlPointsToTheCatalogPage() {
        assertEquals(
            "https://staging.trovata.app.br/catalogo-link-view/atelier-norte/abc-123?apresentacao=p7Kq2m",
            buildPresentationUrl("https://staging.trovata.app.br/", "atelier-norte", "abc-123", "p7Kq2m"),
        )
    }

    @Test
    fun recorderUrlEmbedsThePageAsRecorder() {
        assertEquals(
            "http://localhost:5173/catalogo-link-view/atelier-norte/abc-123?embed=recorder",
            buildRecorderEmbedUrl("http://localhost:5173", "atelier-norte", "abc-123"),
        )
    }
}
