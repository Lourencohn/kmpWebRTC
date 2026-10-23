package app.trovata.cast.data.signaling

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.withTimeoutOrNull

private const val CLOSE_REASON_WAIT_MS = 1_000L

class KtorSignalingTransport(
    private val httpClient: HttpClient,
    private val wsUrl: String,
) : SignalingTransport {

    private var session: DefaultClientWebSocketSession? = null

    override suspend fun connect() {
        session = httpClient.webSocketSession { url(wsUrl) }
    }

    override suspend fun send(raw: String) {
        session?.send(raw)
    }

    override suspend fun close(reason: String?) {
        session?.close()
        session = null
    }

    override suspend fun listen(onMessage: suspend (String) -> Unit) {
        val active = session ?: return
        for (frame in active.incoming) {
            if (frame is Frame.Text) onMessage(frame.readText())
        }
    }

    override suspend fun closeReason(): String? {
        val active = session ?: return null
        val reason = withTimeoutOrNull(CLOSE_REASON_WAIT_MS) {
            runCatching { active.closeReason.await() }.getOrNull()
        }
        return reason?.message?.takeIf { it.isNotBlank() }
    }
}

fun signalingUrlFor(baseHttpUrl: String, token: String): String {
    val wsBase = baseHttpUrl
        .replaceFirst(Regex("^http://"), "ws://")
        .replaceFirst(Regex("^https://"), "wss://")
        .trimEnd('/')
    return "$wsBase/ws/session/$token"
}
