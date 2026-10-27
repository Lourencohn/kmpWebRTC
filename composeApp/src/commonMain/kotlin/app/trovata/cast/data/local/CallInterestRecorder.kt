package app.trovata.cast.data.local

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class CallInterestRecorder(
    private val repository: CallInterestRepository,
    scope: CoroutineScope,
) {
    private val log = Logger.withTag("CallInterestRecorder")
    private val pending = Channel<suspend (CallInterestRepository) -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (write in pending) {
                try {
                    write(repository)
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (err: Throwable) {
                    log.w(err) { "interest write failed" }
                }
            }
        }
    }

    fun record(sessionId: String, event: CallInterestEvent) {
        pending.trySend { it.apply(sessionId, event) }
    }

    fun recordCart(sessionId: String, products: List<CartProduct>, atMs: Long) {
        pending.trySend { it.saveCart(sessionId, products, atMs) }
    }
}
