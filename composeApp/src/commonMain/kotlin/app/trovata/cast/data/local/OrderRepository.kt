package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.db.TrovataDatabase
import app.trovata.cast.protocol.OrderLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class StoredOrder(
    val orderId: String,
    val sessionId: String?,
    val sessionToken: String?,
    val clientName: String?,
    val clientShop: String?,
    val sellerName: String,
    val totalCents: Long,
    val confirmedByMe: Boolean,
    val createdAtMs: Long,
    val lines: List<OrderLine>,
)

class OrderRepository(private val db: TrovataDatabase) {

    suspend fun persist(
        orderId: String,
        sessionId: String?,
        sessionToken: String?,
        clientName: String?,
        clientShop: String?,
        sellerName: String,
        totalCents: Long,
        confirmedByMe: Boolean,
        createdAtMs: Long,
        lines: List<OrderLine>,
    ) = withContext(Dispatchers.Default) {
        db.transaction {
            db.ordersQueries.insertOrder(
                orderId = orderId,
                sessionId = sessionId,
                sessionToken = sessionToken,
                clientName = clientName,
                clientShop = clientShop,
                sellerName = sellerName,
                totalCents = totalCents,
                confirmedByMe = if (confirmedByMe) 1 else 0,
                createdAtMs = createdAtMs,
            )
            lines.forEach { line ->
                db.ordersQueries.insertOrderLine(
                    orderId = orderId,
                    productId = line.productId,
                    size = line.size,
                    units = line.units.toLong(),
                    unitPriceCents = line.unitPriceCents,
                )
            }
        }
    }

    fun observeBetween(fromMs: Long, untilMs: Long): Flow<List<StoredOrder>> =
        db.ordersQueries.selectOrdersBetween(fromMs, untilMs)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toStoredOrder() } }

    fun observeAll(): Flow<List<StoredOrder>> =
        db.ordersQueries.selectAllOrders()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toStoredOrder() } }

    suspend fun get(orderId: String): StoredOrder? = withContext(Dispatchers.Default) {
        db.ordersQueries.selectOrderById(orderId).executeAsOneOrNull()?.toStoredOrder()
    }

    private fun app.trovata.cast.db.OrderEntity.toStoredOrder(): StoredOrder {
        val lineRows = db.ordersQueries.selectOrderLines(orderId).executeAsList()
        return StoredOrder(
            orderId = orderId,
            sessionId = sessionId,
            sessionToken = sessionToken,
            clientName = clientName,
            clientShop = clientShop,
            sellerName = sellerName,
            totalCents = totalCents,
            confirmedByMe = confirmedByMe == 1L,
            createdAtMs = createdAtMs,
            lines = lineRows.map {
                OrderLine(
                    productId = it.productId,
                    size = it.size,
                    units = it.units.toInt(),
                    unitPriceCents = it.unitPriceCents,
                )
            },
        )
    }
}
