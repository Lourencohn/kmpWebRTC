package app.trovata.cast.feature.presentation

import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.PresentationEvent
import app.trovata.cast.protocol.decodeDataChannel
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class RecordingClock(private val timeSource: TimeSource = TimeSource.Monotonic) {
    private var accumulated: Duration = Duration.ZERO
    private var runningSince: TimeMark? = null

    fun start() {
        accumulated = Duration.ZERO
        runningSince = timeSource.markNow()
    }

    fun pause() {
        runningSince?.let { accumulated += it.elapsedNow() }
        runningSince = null
    }

    fun resume() {
        if (runningSince == null) runningSince = timeSource.markNow()
    }

    fun elapsedMs(): Long = (accumulated + (runningSince?.elapsedNow() ?: Duration.ZERO)).inWholeMilliseconds
}

class PresentationTimelineRecorder(private val clock: RecordingClock) {
    private val events = mutableListOf<PresentationEvent>()
    private var accepting = false

    val size: Int get() = events.size

    fun begin() {
        events.clear()
        accepting = true
    }

    fun record(payload: String): PresentationEvent? {
        if (!accepting) return null
        val message = decodeDataChannel(payload) ?: return null
        if (!message.isReplayable()) return null
        return PresentationEvent(t = clock.elapsedMs(), message = message).also { events += it }
    }

    fun end(): List<PresentationEvent> {
        accepting = false
        return events.toList()
    }
}

private fun DataChannelMessage.isReplayable(): Boolean = when (this) {
    is DataChannelMessage.Mute,
    is DataChannelMessage.CartInvalidated,
    is DataChannelMessage.OrderPlaced,
    -> false
    else -> true
}
