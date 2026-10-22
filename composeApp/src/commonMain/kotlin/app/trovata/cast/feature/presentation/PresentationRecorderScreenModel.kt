package app.trovata.cast.feature.presentation

import app.trovata.cast.data.local.StoredPresentation
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.feature.call.LiveWebBridge
import app.trovata.cast.platform.AudioCapture
import app.trovata.cast.protocol.PresentationCreateRequest
import app.trovata.cast.protocol.PresentationCreateResponse
import app.trovata.cast.protocol.PresentationEvent
import app.trovata.cast.protocol.PresentationTimeline
import app.trovata.cast.protocol.PresentationUpload
import app.trovata.cast.protocol.encode
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

const val MaxPresentationMs: Long = 30L * 60 * 1000

const val CountdownSeconds: Int = 3

private const val MinPresentationMs: Long = 1_000

private const val TickMs: Long = 100

private const val CountdownStepMs: Long = 1_000

typealias CreatePresentationFn = suspend (PresentationCreateRequest) -> ApiResult<PresentationCreateResponse>

typealias UploadPresentationFn = suspend (PresentationUpload, ByteArray) -> ApiResult<Unit>

typealias PersistPresentationFn = suspend (StoredPresentation) -> Unit

fun presentationShareMessage(catalogoNome: String, url: String): String =
    "Olá! Gravei uma apresentação do catálogo $catalogoNome para você. É só abrir o link: $url"

private data class RecordedPresentation(
    val recordedAtMs: Long,
    val durationMs: Long,
    val events: List<PresentationEvent>,
    val audio: ByteArray,
)

class PresentationRecorderScreenModel(
    private val spec: PresentationSpec,
    pageUrl: String,
    private val audio: AudioCapture,
    private val createPresentation: CreatePresentationFn,
    private val upload: UploadPresentationFn,
    private val persist: PersistPresentationFn = {},
    private val clock: RecordingClock = RecordingClock(),
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val maxDurationMs: Long = MaxPresentationMs,
    private val audioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScreenModel {

    private val timeline = PresentationTimelineRecorder(clock)
    private val labeler = RecordedActionLabeler()
    private val pageMessages = Channel<String>(Channel.UNLIMITED)
    private var recorded: RecordedPresentation? = null
    private var recordedAtMs = 0L
    private var ticker: Job? = null
    private var countdownJob: Job? = null

    val webBridge = LiveWebBridge(onPageMessage = { payload -> pageMessages.trySend(payload) })

    private val _state = MutableStateFlow(
        PresentationRecorderUiState(pageUrl = pageUrl, catalogoNome = spec.catalogoNome),
    )
    val state: StateFlow<PresentationRecorderUiState> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            for (payload in pageMessages) {
                val event = timeline.record(payload) ?: continue
                val label = labeler.label(event)
                _state.update { current ->
                    current.copy(
                        eventCount = timeline.size,
                        lastAction = label?.let { RecordedAction(it, timeline.size) } ?: current.lastAction,
                    )
                }
            }
        }
    }

    fun requestStart() {
        if (_state.value.phase != RecorderPhase.Preparing) return
        _state.update { it.copy(phase = RecorderPhase.Countdown, countdown = CountdownSeconds, error = null) }
        countdownJob = screenModelScope.launch {
            for (remaining in CountdownSeconds - 1 downTo 1) {
                delay(CountdownStepMs)
                _state.update { it.copy(countdown = remaining) }
            }
            delay(CountdownStepMs)
            countdownJob = null
            startRecording()
        }
    }

    fun cancelCountdown() {
        if (_state.value.phase != RecorderPhase.Countdown) return
        stopCountdown()
        _state.update { it.copy(phase = RecorderPhase.Preparing, countdown = 0) }
    }

    fun startRecording() {
        val phase = _state.value.phase
        if (phase != RecorderPhase.Preparing && phase != RecorderPhase.Countdown) return
        stopCountdown()
        try {
            audio.start()
        } catch (_: Throwable) {
            _state.update {
                it.copy(
                    phase = RecorderPhase.Preparing,
                    countdown = 0,
                    error = "Não consegui abrir o microfone. Confira a permissão de áudio do app.",
                )
            }
            return
        }
        recordedAtMs = nowMs()
        clock.start()
        labeler.reset()
        timeline.begin()
        webBridge.clearDrawing()
        webBridge.resync()
        _state.update {
            it.copy(
                phase = RecorderPhase.Recording,
                countdown = 0,
                elapsedMs = 0,
                eventCount = 0,
                lastAction = null,
                error = null,
            )
        }
        startTicker()
    }

    fun pause() {
        if (_state.value.phase != RecorderPhase.Recording) return
        audio.pause()
        clock.pause()
        _state.update { it.copy(phase = RecorderPhase.Paused, elapsedMs = clock.elapsedMs(), micLevel = 0f) }
    }

    fun resume() {
        if (_state.value.phase != RecorderPhase.Paused) return
        audio.resume()
        clock.resume()
        _state.update { it.copy(phase = RecorderPhase.Recording) }
    }

    fun finish() {
        if (!_state.value.isCapturing) return
        stopTicker()
        clock.pause()
        val durationMs = clock.elapsedMs()
        val events = timeline.end()
        setDrawing(false)
        _state.update { it.copy(phase = RecorderPhase.Finishing, elapsedMs = durationMs, micLevel = 0f) }
        screenModelScope.launch {
            val bytes = try {
                withContext(audioDispatcher) { audio.stop() }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Throwable) {
                null
            }
            if (bytes == null || bytes.isEmpty() || durationMs < MinPresentationMs) {
                startOver(error = "A gravação ficou curta demais ou o áudio falhou. Grave de novo.")
                return@launch
            }
            recorded = RecordedPresentation(recordedAtMs, durationMs, events, bytes)
            _state.update { it.copy(phase = RecorderPhase.Review) }
        }
    }

    fun publish() {
        val pending = recorded ?: return
        if (_state.value.phase != RecorderPhase.Review) return
        _state.update { it.copy(phase = RecorderPhase.Uploading, uploadStep = UploadStep.Creating, error = null) }
        screenModelScope.launch {
            val failure = send(pending)
            if (failure != null) {
                _state.update { it.copy(phase = RecorderPhase.Review, uploadStep = null, error = failure) }
            }
        }
    }

    fun startOver(error: String? = null) {
        stopTicker()
        stopCountdown()
        audio.discard()
        timeline.end()
        recorded = null
        setDrawing(false)
        _state.update {
            it.copy(
                phase = RecorderPhase.Preparing,
                countdown = 0,
                elapsedMs = 0,
                micLevel = 0f,
                eventCount = 0,
                lastAction = null,
                uploadStep = null,
                shareUrl = null,
                savedToRecordings = false,
                error = error,
            )
        }
    }

    fun toggleDrawing() {
        setDrawing(!_state.value.drawing)
    }

    fun clearDrawing() {
        webBridge.clearDrawing()
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun send(pending: RecordedPresentation): String? {
        val created = when (
            val result = createPresentation(
                PresentationCreateRequest(
                    empresaSlug = spec.empresaSlug,
                    catalogoUuid = spec.catalogoUuid,
                    sellerId = spec.sellerId,
                    sellerName = spec.sellerName,
                    durationMs = pending.durationMs,
                    catalogoNome = spec.catalogoNome,
                ),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Fail -> return result.toUserMessage()
        }

        val timelineBytes = PresentationTimeline(
            presentationId = created.presentationId,
            empresaSlug = spec.empresaSlug,
            catalogoUuid = spec.catalogoUuid,
            sellerName = spec.sellerName,
            recordedAtMs = pending.recordedAtMs,
            durationMs = pending.durationMs,
            events = pending.events,
            catalogoNome = spec.catalogoNome,
        ).encode().encodeToByteArray()

        _state.update { it.copy(uploadStep = UploadStep.Audio) }
        (upload(created.audio, pending.audio) as? ApiResult.Fail)?.let { return it.toUserMessage() }
        _state.update { it.copy(uploadStep = UploadStep.Timeline) }
        (upload(created.timeline, timelineBytes) as? ApiResult.Fail)?.let { return it.toUserMessage() }

        recorded = null
        val saved = keepInRecordings(created, pending)
        _state.update {
            it.copy(
                phase = RecorderPhase.Shared,
                uploadStep = null,
                shareUrl = created.shareUrl,
                savedToRecordings = saved,
            )
        }
        return null
    }

    private suspend fun keepInRecordings(created: PresentationCreateResponse, pending: RecordedPresentation): Boolean =
        try {
            persist(
                StoredPresentation(
                    presentationId = created.presentationId,
                    shareUrl = created.shareUrl,
                    empresaSlug = spec.empresaSlug,
                    catalogoUuid = spec.catalogoUuid,
                    catalogoNome = spec.catalogoNome,
                    sellerId = spec.sellerId,
                    sellerName = spec.sellerName,
                    recordedAtMs = pending.recordedAtMs,
                    durationMs = pending.durationMs,
                    eventCount = pending.events.size,
                    uploadedAtMs = nowMs(),
                ),
            )
            true
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Throwable) {
            false
        }

    private fun setDrawing(enabled: Boolean) {
        if (_state.value.drawing == enabled) return
        _state.update { it.copy(drawing = enabled) }
        webBridge.setDrawing(enabled)
    }

    private fun startTicker() {
        stopTicker()
        ticker = screenModelScope.launch {
            while (isActive) {
                delay(TickMs)
                val elapsed = clock.elapsedMs()
                val level = if (_state.value.phase == RecorderPhase.Recording) audio.level() else 0f
                _state.update { it.copy(elapsedMs = elapsed, micLevel = level) }
                if (elapsed >= maxDurationMs) {
                    finish()
                    return@launch
                }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    override fun onDispose() {
        stopTicker()
        stopCountdown()
        audio.discard()
        pageMessages.close()
    }
}

private fun ApiResult.Fail.toUserMessage(): String = when (code) {
    "missing_auth" -> "Sua sessão expirou. Entre de novo para enviar a apresentação."
    "presentations_disabled" -> "A gravação de apresentações ainda não está ligada no servidor."
    "network_error" -> "Sem conexão. Confira a internet e tente enviar de novo."
    "upload_failed" -> "Não consegui enviar o áudio. Tente de novo."
    else -> message
}
