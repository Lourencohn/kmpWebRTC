package app.trovata.cast.server

import app.trovata.cast.protocol.OrderRecord
import app.trovata.cast.protocol.OrderSource
import app.trovata.cast.protocol.OrderSubmissionRequest
import java.util.concurrent.ConcurrentHashMap

class OrderStore(
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val byId = ConcurrentHashMap<String, OrderRecord>()
    private val byToken = ConcurrentHashMap<String, MutableList<String>>()

    fun submit(request: OrderSubmissionRequest): OrderRecord {
        val existing = byId[request.orderId]
        if (existing != null) return existing
        val record = OrderRecord(
            orderId = request.orderId,
            sessionToken = request.sessionToken,
            tsMs = request.tsMs,
            receivedAtMs = now(),
            totalCents = request.totalCents,
            lines = request.lines,
            source = request.source,
            clientName = request.clientName,
        )
        byId[request.orderId] = record
        byToken.computeIfAbsent(request.sessionToken) { mutableListOf() }.add(request.orderId)
        return record
    }

    fun get(orderId: String): OrderRecord? = byId[orderId]

    fun byToken(token: String): List<OrderRecord> =
        byToken[token].orEmpty().mapNotNull { byId[it] }

    fun size(): Int = byId.size

    companion object {
        const val MAX_LINES = 200
        const val MAX_UNITS_PER_LINE = 10_000
        fun isValid(request: OrderSubmissionRequest): Boolean {
            if (request.orderId.isBlank()) return false
            if (request.sessionToken.isBlank()) return false
            if (request.lines.isEmpty()) return false
            if (request.lines.size > MAX_LINES) return false
            if (request.lines.any { it.units <= 0 || it.units > MAX_UNITS_PER_LINE }) return false
            if (request.lines.any { it.unitPriceCents < 0 }) return false
            if (request.totalCents < 0) return false
            return when (request.source) {
                OrderSource.Buyer, OrderSource.Seller -> true
            }
        }
    }
}
