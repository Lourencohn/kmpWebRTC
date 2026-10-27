package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.db.SessionEntity
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.protocol.SessionCreateRequest
import app.trovata.cast.protocol.SessionCreateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class StoredSessionRecord(
    val sessionId: String,
    val token: String,
    val url: String,
    val sellerName: String,
    val empresaSlug: String,
    val catalogoUuid: String,
    val catalogoNome: String?,
    val catalogoLinkId: Long?,
    val clientName: String?,
    val clientEmail: String?,
    val clientShop: String?,
    val scheduledFor: String?,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val clientPhone: String? = null,
) {
    val catalogLabel: String get() = catalogoNome.orEmpty()
}

class SessionsRepository(private val db: TrovataDatabase) {

    fun observeAll(): Flow<List<StoredSessionRecord>> =
        db.sessionsQueries.selectAllSessions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toRecord() } }

    suspend fun findById(sessionId: String): StoredSessionRecord? = withContext(Dispatchers.Default) {
        db.sessionsQueries.selectSessionById(sessionId).executeAsOneOrNull()?.toRecord()
    }

    suspend fun persistCreated(
        request: SessionCreateRequest,
        response: SessionCreateResponse,
        createdAtMs: Long,
        client: SessionClientNotes,
    ): StoredSessionRecord = withContext(Dispatchers.Default) {
        db.transactionWithResult {
            db.sessionsQueries.insertSession(
                sessionId = response.sessionId,
                token = response.token,
                url = response.url,
                sellerName = request.sellerName,
                empresaSlug = request.empresaSlug,
                catalogoUuid = request.catalogoUuid,
                catalogoNome = request.catalogoNome,
                clientName = request.clientName,
                clientShop = client.shop,
                scheduledFor = client.scheduledFor,
                createdAtMs = createdAtMs,
                expiresAtMs = response.expiresAtMs,
                clientEmail = request.clientEmail,
                catalogoLinkId = request.catalogoLinkId,
                clientPhone = client.phone,
            )
            StoredSessionRecord(
                sessionId = response.sessionId,
                token = response.token,
                url = response.url,
                sellerName = request.sellerName,
                empresaSlug = request.empresaSlug,
                catalogoUuid = request.catalogoUuid,
                catalogoNome = request.catalogoNome,
                catalogoLinkId = request.catalogoLinkId,
                clientName = request.clientName,
                clientEmail = request.clientEmail,
                clientShop = client.shop,
                scheduledFor = client.scheduledFor,
                createdAtMs = createdAtMs,
                expiresAtMs = response.expiresAtMs,
                clientPhone = client.phone,
            )
        }
    }
}

private fun SessionEntity.toRecord(): StoredSessionRecord = StoredSessionRecord(
    sessionId = sessionId,
    token = token,
    url = url,
    sellerName = sellerName,
    empresaSlug = empresaSlug,
    catalogoUuid = catalogoUuid,
    catalogoNome = catalogoNome,
    catalogoLinkId = catalogoLinkId,
    clientName = clientName,
    clientEmail = clientEmail,
    clientShop = clientShop,
    scheduledFor = scheduledFor,
    createdAtMs = createdAtMs,
    expiresAtMs = expiresAtMs,
    clientPhone = clientPhone,
)

data class SessionClientNotes(
    val shop: String? = null,
    val scheduledFor: String? = null,
    val phone: String? = null,
)
