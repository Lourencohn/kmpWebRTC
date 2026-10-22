package app.trovata.cast.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trovata.cast.db.TrovataDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class StoredPresentation(
    val presentationId: String,
    val shareUrl: String,
    val empresaSlug: String,
    val catalogoUuid: String,
    val catalogoNome: String,
    val sellerId: String,
    val sellerName: String,
    val recordedAtMs: Long,
    val durationMs: Long,
    val eventCount: Int,
    val uploadedAtMs: Long,
)

class PresentationsRepository(private val db: TrovataDatabase) {

    fun observeForCompany(empresaSlug: String): Flow<List<StoredPresentation>> =
        db.presentationsQueries.selectPresentationsForCompany(empresaSlug)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map {
                    StoredPresentation(
                        presentationId = it.presentationId,
                        shareUrl = it.shareUrl,
                        empresaSlug = it.empresaSlug,
                        catalogoUuid = it.catalogoUuid,
                        catalogoNome = it.catalogoNome,
                        sellerId = it.sellerId,
                        sellerName = it.sellerName,
                        recordedAtMs = it.recordedAtMs,
                        durationMs = it.durationMs,
                        eventCount = it.eventCount.toInt(),
                        uploadedAtMs = it.uploadedAtMs,
                    )
                }
            }

    suspend fun save(presentation: StoredPresentation) = withContext(Dispatchers.Default) {
        db.presentationsQueries.insertPresentation(
            presentationId = presentation.presentationId,
            shareUrl = presentation.shareUrl,
            empresaSlug = presentation.empresaSlug,
            catalogoUuid = presentation.catalogoUuid,
            catalogoNome = presentation.catalogoNome,
            sellerId = presentation.sellerId,
            sellerName = presentation.sellerName,
            recordedAtMs = presentation.recordedAtMs,
            durationMs = presentation.durationMs,
            eventCount = presentation.eventCount.toLong(),
            uploadedAtMs = presentation.uploadedAtMs,
        )
    }

    suspend fun remove(presentationId: String) = withContext(Dispatchers.Default) {
        db.presentationsQueries.deletePresentation(presentationId)
    }
}
