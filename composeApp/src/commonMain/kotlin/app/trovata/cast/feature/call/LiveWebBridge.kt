package app.trovata.cast.feature.call

import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.encode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class LiveWebBridge(private val onPageMessage: (String) -> Unit) {

    private val pendingScripts = Channel<String>(Channel.UNLIMITED)
    val scripts: Flow<String> = pendingScripts.receiveAsFlow()

    var latestStatus: String = STATUS_NEGOTIATING
        private set

    var latestDrawing: Boolean = false
        private set

    fun receiveFromPage(payload: String) = onPageMessage(payload)

    fun deliver(message: DataChannelMessage) {
        deliverRaw(message.encode())
    }

    fun deliverRaw(payload: String) {
        run("window.$RECEIVE_FN(${jsString(payload)})")
    }

    fun updateStatus(status: String) {
        latestStatus = status
        run(statusCall(status))
    }

    fun setDrawing(enabled: Boolean) {
        latestDrawing = enabled
        run(drawingCall(enabled))
    }

    fun clearDrawing() {
        run("window.$CLEAR_DRAWING_FN()")
    }

    fun bootstrapScript(): String =
        guarded(SHIM_SCRIPT + statusCall(latestStatus) + drawingCall(latestDrawing))

    private fun run(js: String) {
        pendingScripts.trySend(guarded(js))
    }

    private fun statusCall(status: String) = "window.$STATUS_FN(${jsString(status)});"

    private fun drawingCall(enabled: Boolean) = "window.$SET_DRAWING_FN($enabled);"

    companion object {
        const val NATIVE_OBJECT = "TrovataLive"
        const val RECEIVE_FN = "__trovataLiveReceive"
        const val STATUS_FN = "__trovataLiveStatus"
        const val SET_DRAWING_FN = "__trovataLiveSetDrawing"
        const val CLEAR_DRAWING_FN = "__trovataLiveClearDrawing"

        const val STATUS_IDLE = "idle"
        const val STATUS_NEGOTIATING = "negotiating"
        const val STATUS_CONNECTED = "connected"
        const val STATUS_FAILED = "failed"
        const val STATUS_CLOSED = "closed"

        val SHIM_SCRIPT: String = """
            if (!window.__trovataLiveShim) {
              window.__trovataLiveShim = true;
              var hook = function (name) {
                if (typeof window[name] === 'function') return;
                var real = null;
                var queue = [];
                Object.defineProperty(window, name, {
                  configurable: true,
                  get: function () { return real || function (arg) { queue.push(arg); }; },
                  set: function (fn) {
                    real = typeof fn === 'function' ? fn : null;
                    if (!real) return;
                    var pending = queue;
                    queue = [];
                    pending.forEach(function (arg) { real(arg); });
                  }
                });
              };
              hook('$RECEIVE_FN');
              hook('$STATUS_FN');
              hook('$SET_DRAWING_FN');
              hook('$CLEAR_DRAWING_FN');
            }
        """.trimIndent() + "\n"

        fun guarded(js: String): String = "(function(){try{$js}catch(e){}})();"

        fun jsString(value: String): String {
            val escaped = buildString(value.length + 2) {
                append('"')
                value.forEach { ch ->
                    when (ch) {
                        '"' -> append("\\\"")
                        '\\' -> append("\\\\")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        ' ' -> append("\\u2028")
                        ' ' -> append("\\u2029")
                        '<' -> append("\\u003c")
                        else -> append(ch)
                    }
                }
                append('"')
            }
            return escaped
        }
    }
}
