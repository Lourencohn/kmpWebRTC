package app.trovata.cast.server

import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionInfo
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val TOKEN_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private const val TOKEN_LENGTH = 9
private const val SESSION_TTL_MS = 4L * 60 * 60 * 1000

data class StoredSession(
    val sessionId: String,
    val token: String,
    val request: SessionCreateRequest,
    val createdAtMs: Long,
    val expiresAtMs: Long,
) {
    fun toInfo(): SessionInfo = SessionInfo(
        token = token,
        empresaSlug = request.empresaSlug,
        catalogoUuid = request.catalogoUuid,
        sellerName = request.sellerName,
        catalogoNome = request.catalogoNome,
        catalogoLinkId = request.catalogoLinkId,
        carrinhoId = request.carrinhoId,
        clientName = request.clientName,
        clientEmail = request.clientEmail,
        createdAtMs = createdAtMs,
        expiresAtMs = expiresAtMs,
    )
}

class SessionStore(
    private val ttlMs: Long = SESSION_TTL_MS,
    private val random: Random = Random.Default,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val byToken = ConcurrentHashMap<String, StoredSession>()

    fun create(request: SessionCreateRequest): StoredSession {
        val createdAt = now()
        val sessionId = "ses_${generate(12)}"
        val token = generate(TOKEN_LENGTH)
        val stored = StoredSession(
            sessionId = sessionId,
            token = token,
            request = request,
            createdAtMs = createdAt,
            expiresAtMs = createdAt + ttlMs,
        )
        byToken[token] = stored
        return stored
    }

    fun get(token: String): StoredSession? {
        val stored = byToken[token] ?: return null
        if (stored.expiresAtMs < now()) {
            byToken.remove(token)
            return null
        }
        return stored
    }

    fun size(): Int = byToken.size

    private fun generate(length: Int): String = buildString(length) {
        repeat(length) { append(TOKEN_ALPHABET[random.nextInt(TOKEN_ALPHABET.length)]) }
    }
}
