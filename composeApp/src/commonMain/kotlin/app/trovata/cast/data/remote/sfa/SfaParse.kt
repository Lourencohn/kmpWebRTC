package app.trovata.cast.data.remote.sfa

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.math.roundToLong

object SfaParse {

    fun parseCents(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim()
            .removePrefix("R$")
            .trim()
            .replace(".", "")
            .replace(",", ".")
        val value = normalized.toDoubleOrNull() ?: return null
        return (value * 100).roundToLong()
    }

    fun parseBool(raw: String?): Long {
        val v = raw?.trim()?.uppercase() ?: return 0L
        return if (v == "YES" || v == "S" || v == "SIM" || v == "TRUE" || v == "1") 1L else 0L
    }

    fun parseLong(raw: String?): Long? = raw?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull()

    fun parseInt(raw: String?): Long? = parseLong(raw)

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
