package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.db.CallInterestEntity
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CallInterestRepository(private val db: TrovataDatabase) {
    private val queries = db.callInterestQueries

    suspend fun apply(sessionId: String, event: CallInterestEvent) = withContext(Dispatchers.Default) {
        when (event) {
            is CallInterestEvent.Focused -> db.transaction {
                queries.insertInterestIfAbsent(sessionId, event.produtoPreId, event.atMs, event.atMs)
                when (event.source) {
                    InterestSource.Client -> queries.addClientOpen(
                        atMs = event.atMs,
                        sessionId = sessionId,
                        produtoPreId = event.produtoPreId,
                    )
                    InterestSource.Seller -> queries.addSellerShow(
                        atMs = event.atMs,
                        sessionId = sessionId,
                        produtoPreId = event.produtoPreId,
                    )
                }
            }
            is CallInterestEvent.Drafted -> db.transaction {
                queries.insertInterestIfAbsent(sessionId, event.produtoPreId, event.atMs, event.atMs)
                queries.raiseDraftUnits(
                    units = event.units.toLong(),
                    atMs = event.atMs,
                    sessionId = sessionId,
                    produtoPreId = event.produtoPreId,
                )
            }
            is CallInterestEvent.Named -> queries.nameProduct(
                ref = event.ref,
                name = event.name,
                imageUrl = event.imageUrl,
                sessionId = sessionId,
                produtoPreId = event.produtoPreId,
            )
        }
    }

    suspend fun saveCart(sessionId: String, products: List<CartProduct>, atMs: Long) = withContext(Dispatchers.Default) {
        db.transaction {
            queries.clearCartUnits(sessionId)
            products.groupBy { it.produtoPreId }.forEach { (produtoPreId, lines) ->
                val first = lines.first()
                queries.insertInterestIfAbsent(sessionId, produtoPreId, atMs, atMs)
                queries.setCartUnits(units = lines.sumOf { it.units }.toLong(), sessionId = sessionId, produtoPreId = produtoPreId)
                queries.nameProduct(
                    ref = first.ref,
                    name = first.name,
                    imageUrl = first.imageUrl,
                    sessionId = sessionId,
                    produtoPreId = produtoPreId,
                )
            }
        }
    }

    fun observeForSession(sessionId: String): Flow<List<CallInterest>> =
        queries.selectInterestForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toInterest() } }

    suspend fun listForSession(sessionId: String): List<CallInterest> = withContext(Dispatchers.Default) {
        queries.selectInterestForSession(sessionId).executeAsList().map { it.toInterest() }
    }
}

private fun CallInterestEntity.toInterest(): CallInterest = CallInterest(
    produtoPreId = produtoPreId,
    clientOpens = clientOpens.toInt(),
    sellerShows = sellerShows.toInt(),
    draftUnits = draftUnits.toInt(),
    cartUnits = cartUnits.toInt(),
    ref = ref,
    name = name,
    imageUrl = imageUrl,
    firstAtMs = firstAtMs,
    lastAtMs = lastAtMs,
)
