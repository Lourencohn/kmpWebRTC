package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class CartLine(
    val sku: String,
    val size: String,
    val units: Int,
    val updatedAtMs: Long,
)

class CartRepository(private val db: TrovataDatabase) {

    fun observe(sessionId: String): Flow<List<CartLine>> =
        db.sessionsQueries.selectCartLinesBySession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    CartLine(
                        sku = it.sku,
                        size = it.size,
                        units = it.units.toInt(),
                        updatedAtMs = it.updatedAtMs,
                    )
                }
            }

    suspend fun apply(sessionId: String, sku: String, size: String, units: Int, ts: Long) =
        withContext(Dispatchers.Default) {
            if (units <= 0) {
                db.sessionsQueries.deleteCartLine(sessionId, sku, size)
            } else {
                db.sessionsQueries.upsertCartLine(sessionId, sku, size, units.toLong(), ts)
            }
        }

    suspend fun snapshot(sessionId: String): List<CartLine> = withContext(Dispatchers.Default) {
        db.sessionsQueries.selectCartLinesBySession(sessionId).executeAsList().map {
            CartLine(it.sku, it.size, it.units.toInt(), it.updatedAtMs)
        }
    }

    suspend fun clear(sessionId: String) = withContext(Dispatchers.Default) {
        db.sessionsQueries.clearCart(sessionId)
    }
}
