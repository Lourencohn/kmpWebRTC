package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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
    val clientName: String?,
    val clientShop: String?,
    val scheduledFor: String?,
    val createdAtMs: Long,
    val expiresAtMs: Long,
) {
    val catalogLabel: String get() = catalogoNome.orEmpty()
}

class SessionsRepository(private val db: TrovataDatabase) {

    fun observeAll(): Flow<List<StoredSessionRecord>> =
        db.sessionsQueries.selectAllSessions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    StoredSessionRecord(
                        sessionId = it.sessionId,
                        token = it.token,
                        url = it.url,
                        sellerName = it.sellerName,
                        empresaSlug = it.empresaSlug,
                        catalogoUuid = it.catalogoUuid,
                        catalogoNome = it.catalogoNome,
                        clientName = it.clientName,
                        clientShop = it.clientShop,
                        scheduledFor = it.scheduledFor,
                        createdAtMs = it.createdAtMs,
                        expiresAtMs = it.expiresAtMs,
                    )
                }
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
            )
            StoredSessionRecord(
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
            )
        }
    }

    suspend fun selectedSkus(sessionId: String): List<String> = withContext(Dispatchers.Default) {
        db.sessionsQueries.selectSelectedProducts(sessionId).executeAsList()
    }
}

data class SessionClientNotes(
    val shop: String? = null,
    val scheduledFor: String? = null,
)
