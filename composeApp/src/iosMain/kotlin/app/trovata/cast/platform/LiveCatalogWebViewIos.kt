package app.trovata.cast.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import app.trovata.cast.feature.call.LiveWebBridge
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

private val nativeObjectScript: String = """
    window.${LiveWebBridge.NATIVE_OBJECT} = {
      postMessage: function (payload) {
        window.webkit.messageHandlers.${LiveWebBridge.NATIVE_OBJECT}.postMessage(payload);
      }
    };
""".trimIndent()

private class TrovataLiveMessageHandler(private val bridge: LiveWebBridge) :
    NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        val payload = didReceiveScriptMessage.body as? String ?: return
        bridge.receiveFromPage(payload)
    }
}

private class BootstrappingNavigationDelegate(private val bridge: LiveWebBridge) :
    NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        webView.evaluateJavaScript(bridge.bootstrapScript(), null)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LiveCatalogWebView(
    url: String,
    bridge: LiveWebBridge,
    modifier: Modifier,
) {
    val messageHandler = remember(bridge) { TrovataLiveMessageHandler(bridge) }
    val navigationDelegate = remember(bridge) { BootstrappingNavigationDelegate(bridge) }
    val webView = remember(url, bridge) {
        val configuration = WKWebViewConfiguration().apply {
            userContentController.addScriptMessageHandler(messageHandler, LiveWebBridge.NATIVE_OBJECT)
            userContentController.addUserScript(
                WKUserScript(
                    source = nativeObjectScript + "\n" + bridge.bootstrapScript(),
                    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                    forMainFrameOnly = true,
                ),
            )
        }
        WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
            this.navigationDelegate = navigationDelegate
            NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
        }
    }

    LaunchedEffect(webView, bridge) {
        bridge.scripts.collect { script -> webView.evaluateJavaScript(script, null) }
    }

    UIKitView(
        factory = { webView },
        modifier = modifier,
        onRelease = { view ->
            view.configuration.userContentController.removeScriptMessageHandlerForName(LiveWebBridge.NATIVE_OBJECT)
            view.navigationDelegate = null
        },
    )
}
