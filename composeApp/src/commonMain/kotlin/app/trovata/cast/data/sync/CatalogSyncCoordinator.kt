package app.trovata.cast.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogSyncCoordinator(
    private val catalogSyncService: CatalogSyncService,
    private val scope: CoroutineScope,
) {
    private var started = false

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun start() {
        if (started) return
        started = true
        scope.launch {
            _isSyncing.value = true
            try {
                catalogSyncService.syncEssentials()
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
