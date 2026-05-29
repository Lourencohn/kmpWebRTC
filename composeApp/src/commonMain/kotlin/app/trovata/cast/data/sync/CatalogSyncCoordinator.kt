package app.trovata.cast.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CatalogSyncCoordinator(
    private val catalogSyncService: CatalogSyncService,
    private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch { catalogSyncService.syncEssentials() }
    }
}
