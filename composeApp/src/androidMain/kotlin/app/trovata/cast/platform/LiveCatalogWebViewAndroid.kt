package app.trovata.cast.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.trovata.cast.feature.call.LiveWebBridge
import co.touchlab.kermit.Logger

private val log = Logger.withTag("LiveCatalogWebView")

private class TrovataLiveJsInterface(private val bridge: LiveWebBridge) {
    @JavascriptInterface
    fun postMessage(payload: String) {
        bridge.receiveFromPage(payload)
    }
}

private class BootstrappingClient(private val bridge: LiveWebBridge) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        view.evaluateJavascript(bridge.bootstrapScript(), null)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        view.evaluateJavascript(bridge.bootstrapScript(), null)
    }
}

private class ConsoleForwardingClient : WebChromeClient() {
    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
        log.d { "[page] ${message.message()} (${message.sourceId()}:${message.lineNumber()})" }
        return true
    }
}

private fun Context.isDebuggable(): Boolean =
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun LiveCatalogWebView(
    url: String,
    bridge: LiveWebBridge,
    modifier: Modifier,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(webView, bridge) {
        val view = webView ?: return@LaunchedEffect
        bridge.scripts.collect { script -> view.evaluateJavascript(script, null) }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView.setWebContentsDebuggingEnabled(context.isDebuggable())
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                addJavascriptInterface(TrovataLiveJsInterface(bridge), LiveWebBridge.NATIVE_OBJECT)
                webViewClient = BootstrappingClient(bridge)
                webChromeClient = ConsoleForwardingClient()
                loadUrl(url)
                webView = this
            }
        },
    )
}
