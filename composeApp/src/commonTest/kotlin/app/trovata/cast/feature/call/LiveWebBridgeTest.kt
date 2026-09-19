package app.trovata.cast.feature.call

import app.trovata.cast.protocol.DataChannelMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveWebBridgeTest {

    @Test
    fun deliversMessagesAsGuardedReceiveCalls() = runTest {
        val bridge = LiveWebBridge(onPageMessage = {})
        bridge.deliver(DataChannelMessage.Mute(muted = true, ts = 1L, from = "buyer-1"))
        val script = bridge.scripts.first()
        assertTrue(script.startsWith("(function(){try{"))
        assertTrue(script.contains("window.__trovataLiveReceive(\"{\\\"type\\\":\\\"mute\\\""))
    }

    @Test
    fun rawPayloadsReachThePageUntouched() = runTest {
        val bridge = LiveWebBridge(onPageMessage = {})
        bridge.deliverRaw("""{"type":"somethingNewer","ts":1,"from":"buyer-1"}""")
        val script = bridge.scripts.first()
        assertTrue(script.contains("window.__trovataLiveReceive(\"{\\\"type\\\":\\\"somethingNewer\\\""))
    }

    @Test
    fun statusIsRememberedForTheNextPageLoad() = runTest {
        val bridge = LiveWebBridge(onPageMessage = {})
        bridge.updateStatus(LiveWebBridge.STATUS_CONNECTED)
        assertEquals(LiveWebBridge.STATUS_CONNECTED, bridge.latestStatus)
        assertTrue(bridge.bootstrapScript().contains("window.__trovataLiveStatus(\"connected\")"))
        assertTrue(bridge.bootstrapScript().contains("hook('__trovataLiveReceive')"))
    }

    @Test
    fun drawingModeSurvivesAPageReload() = runTest {
        val bridge = LiveWebBridge(onPageMessage = {})
        bridge.setDrawing(true)
        assertTrue(bridge.latestDrawing)
        assertTrue(bridge.bootstrapScript().contains("window.__trovataLiveSetDrawing(true)"))
        bridge.setDrawing(false)
        assertTrue(bridge.bootstrapScript().contains("window.__trovataLiveSetDrawing(false)"))
    }

    @Test
    fun scriptsArriveInOrderAndWaitForAConsumer() = runTest {
        val bridge = LiveWebBridge(onPageMessage = {})
        bridge.setDrawing(true)
        bridge.clearDrawing()
        val scripts = bridge.scripts.take(2).toList()
        assertTrue(scripts[0].contains("__trovataLiveSetDrawing(true)"))
        assertTrue(scripts[1].contains("__trovataLiveClearDrawing()"))
    }

    @Test
    fun pageMessagesReachTheHandler() {
        var received: String? = null
        val bridge = LiveWebBridge(onPageMessage = { received = it })
        bridge.receiveFromPage("{\"type\":\"mute\"}")
        assertEquals("{\"type\":\"mute\"}", received)
    }

    @Test
    fun jsStringEscapesQuotesAndScriptBreakers() {
        assertEquals("\"a\\\"b\\\\c\\u003cd\\n\"", LiveWebBridge.jsString("a\"b\\c<d\n"))
    }
}
