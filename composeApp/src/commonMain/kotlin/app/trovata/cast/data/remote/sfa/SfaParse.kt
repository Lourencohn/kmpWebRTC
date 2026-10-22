package app.trovata.cast.data.remote.sfa

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

object SfaParse {

    fun parseIsoToMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrNull()
    }

    fun parseTimestampToMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        parseIsoToMs(text)?.let { return it }
        return runCatching {
            LocalDateTime.parse(text.replace(' ', 'T'))
                .toInstant(laravelTimeZone)
                .toEpochMilliseconds()
        }.getOrNull()
    }

    private val laravelTimeZone: TimeZone by lazy {
        runCatching { TimeZone.of("America/Sao_Paulo") }.getOrDefault(TimeZone.currentSystemDefault())
    }
}
