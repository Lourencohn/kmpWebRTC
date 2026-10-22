package app.trovata.cast.feature.presentation


data class PresentationSpec(
    val empresaSlug: String,
    val catalogoUuid: String,
    val catalogoNome: String,
    val sellerId: String,
    val sellerName: String,
)

enum class RecorderPhase { Preparing, Countdown, Recording, Paused, Finishing, Review, Uploading, Shared }

enum class UploadStep { Creating, Audio, Timeline }

data class PresentationRecorderUiState(
    val pageUrl: String,
    val catalogoNome: String,
    val phase: RecorderPhase = RecorderPhase.Preparing,
    val countdown: Int = 0,
    val elapsedMs: Long = 0,
    val micLevel: Float = 0f,
    val drawing: Boolean = false,
    val eventCount: Int = 0,
    val lastAction: RecordedAction? = null,
    val uploadStep: UploadStep? = null,
    val shareUrl: String? = null,
    val savedToRecordings: Boolean = false,
    val error: String? = null,
) {
    val isCapturing: Boolean get() = phase == RecorderPhase.Recording || phase == RecorderPhase.Paused

    val shareMessage: String?
        get() = shareUrl?.let { url -> presentationShareMessage(catalogoNome, url) }
}
