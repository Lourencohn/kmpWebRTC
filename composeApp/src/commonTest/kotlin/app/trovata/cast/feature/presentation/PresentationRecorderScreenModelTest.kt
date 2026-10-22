package app.trovata.cast.feature.presentation

import app.trovata.cast.data.local.StoredPresentation
import app.trovata.cast.data.remote.ApiResult
import app.trovata.cast.platform.AudioCapture
import app.trovata.cast.protocol.DataChannelJson
import app.trovata.cast.protocol.DataChannelMessage
import app.trovata.cast.protocol.PresentationCreateRequest
import app.trovata.cast.protocol.PresentationCreateResponse
import app.trovata.cast.protocol.PresentationTimeline
import app.trovata.cast.protocol.PresentationUpload
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PresentationRecorderScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val time = TestTimeSource()

    private val spec = PresentationSpec(
        empresaSlug = "buba-teste",
        catalogoUuid = "5f6c1d2e-8a41-4f0b-9c3d-77b2a0e14c9f",
        catalogoNome = "Outono 26",
        sellerId = "31",
        sellerName = "Marina Prado",
    )

    private class FakeAudio(private val bytes: ByteArray = ByteArray(2_048) { 7 }) : AudioCapture {
        val calls = mutableListOf<String>()
        var failOnStart = false
        var currentLevel = 0f

        override fun level(): Float = currentLevel

        override fun start() {
            calls += "start"
            if (failOnStart) error("sem microfone")
        }

        override fun pause() {
            calls += "pause"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun stop(): ByteArray {
            calls += "stop"
            return bytes
        }

        override fun discard() {
            calls += "discard"
        }
    }

    private val created = PresentationCreateResponse(
        presentationId = "p7Kq2mZr8XwT4nBc",
        shareUrl = "https://staging.trovata.app.br/catalogo-link-view/buba-teste/uuid?apresentacao=p7Kq2mZr8XwT4nBc",
        timeline = PresentationUpload("https://storage/timeline", "application/json"),
        audio = PresentationUpload("https://storage/audio", "audio/mp4"),
        uploadExpiresAtMs = 0,
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun model(
        audio: AudioCapture = FakeAudio(),
        create: suspend (PresentationCreateRequest) -> ApiResult<PresentationCreateResponse> = {
            ApiResult.Ok(created)
        },
        upload: suspend (PresentationUpload, ByteArray) -> ApiResult<Unit> = { _, _ -> ApiResult.Ok(Unit) },
        persist: suspend (StoredPresentation) -> Unit = {},
    ) = PresentationRecorderScreenModel(
        spec = spec,
        pageUrl = "http://localhost:5173/catalogo-link-view/buba-teste/uuid?embed=recorder",
        audio = audio,
        createPresentation = create,
        upload = upload,
        persist = persist,
        clock = RecordingClock(time),
        nowMs = { 1_790_000_000_000 },
        audioDispatcher = dispatcher,
    )

    private fun navigate(ts: Long) =
        """{"type":"navigate","view":{"route":{"type":"todos"}},"ts":$ts,"from":"seller-embed"}"""

    private fun draw(ts: Long) =
        """{"type":"draw","strokeId":"s1","target":"produto:4932","phase":"start","points":[{"x":0.2,"y":0.4}],"ts":$ts,"from":"seller-embed"}"""

    private fun openProduct(ts: Long) =
        """{"type":"navigate","view":{"route":{"type":"todos"},"focus":{"produtoPreId":4932}},"ts":$ts,"from":"seller-embed"}"""

    private fun scroll(ts: Long) =
        """{"type":"scroll","anchor":{"page":1,"produtoPreId":4932,"itemOffsetRatio":0.1,"viewportRatio":0.3},"ts":$ts,"from":"seller-embed"}"""

    private fun cartInvalidated(ts: Long) =
        """{"type":"cartInvalidated","carrinhoId":9,"reason":"itemAdded","ts":$ts,"from":"seller-embed"}"""

    private fun TestScope.record(model: PresentationRecorderScreenModel, payload: String) {
        model.webBridge.receiveFromPage(payload)
        runCurrent()
    }

    @Test
    fun startingAsksThePageToClearAndResyncBeforeAnythingIsRecorded() = runTest(dispatcher) {
        val audio = FakeAudio()
        val model = model(audio = audio)
        record(model, navigate(1))
        model.startRecording()
        val scripts = model.webBridge.scripts.take(2).toList()
        assertTrue(scripts[0].contains("__trovataLiveClearDrawing()"))
        assertTrue(scripts[1].contains("__trovataLiveResync()"))
        assertEquals(listOf("start"), audio.calls)
        assertEquals(RecorderPhase.Recording, model.state.value.phase)
        assertEquals(0, model.state.value.eventCount)
        model.onDispose()
    }

    @Test
    fun eventsAreStampedWithRecordingTimeAndPausesDoNotCount() = runTest(dispatcher) {
        val uploads = mutableMapOf<String, ByteArray>()
        val model = model(upload = { target, bytes -> uploads[target.url] = bytes; ApiResult.Ok(Unit) })
        model.startRecording()
        record(model, navigate(10))
        time += 1_200.milliseconds
        record(model, draw(20))
        model.pause()
        time += 5_000.milliseconds
        record(model, cartInvalidated(30))
        model.resume()
        time += 800.milliseconds
        record(model, draw(40))
        model.finish()
        advanceUntilIdle()

        assertEquals(RecorderPhase.Review, model.state.value.phase)
        assertEquals(2_000, model.state.value.elapsedMs)
        model.publish()
        advanceUntilIdle()

        val timeline = DataChannelJson.decodeFromString(PresentationTimeline.serializer(), uploads.getValue("https://storage/timeline").decodeToString())
        assertEquals(listOf(0L, 1_200L, 2_000L), timeline.events.map { it.t })
        assertIs<DataChannelMessage.Navigate>(timeline.events[0].message)
        assertEquals("p7Kq2mZr8XwT4nBc", timeline.presentationId)
        assertEquals("Marina Prado", timeline.sellerName)
        assertEquals(2_000, timeline.durationMs)
        assertEquals(1_790_000_000_000, timeline.recordedAtMs)
        model.onDispose()
    }

    @Test
    fun publishingSendsAudioBeforeTheTimelineAndSharesTheLink() = runTest(dispatcher) {
        val order = mutableListOf<String>()
        var request: PresentationCreateRequest? = null
        val model = model(
            create = { request = it; ApiResult.Ok(created) },
            upload = { target, _ -> order += target.contentType; ApiResult.Ok(Unit) },
        )
        model.startRecording()
        time += 3_000.milliseconds
        model.finish()
        advanceUntilIdle()
        model.publish()
        advanceUntilIdle()

        assertEquals(listOf("audio/mp4", "application/json"), order)
        assertEquals(3_000, request?.durationMs)
        assertEquals("buba-teste", request?.empresaSlug)
        val state = model.state.first()
        assertEquals(RecorderPhase.Shared, state.phase)
        assertEquals(created.shareUrl, state.shareUrl)
        assertTrue(state.shareMessage!!.endsWith(created.shareUrl))
        model.onDispose()
    }

    @Test
    fun failedUploadKeepsTheRecordingForAnotherTry() = runTest(dispatcher) {
        var attempts = 0
        val model = model(
            upload = { _, _ ->
                attempts++
                if (attempts == 1) ApiResult.Fail("network_error", "timeout", 0) else ApiResult.Ok(Unit)
            },
        )
        model.startRecording()
        time += 4_000.milliseconds
        model.finish()
        advanceUntilIdle()
        model.publish()
        advanceUntilIdle()

        assertEquals(RecorderPhase.Review, model.state.value.phase)
        assertEquals("Sem conexão. Confira a internet e tente enviar de novo.", model.state.value.error)

        model.publish()
        advanceUntilIdle()
        assertEquals(RecorderPhase.Shared, model.state.value.phase)
        model.onDispose()
    }

    @Test
    fun microphoneFailureStaysInPreparationWithAMessage() = runTest(dispatcher) {
        val audio = FakeAudio().apply { failOnStart = true }
        val model = model(audio = audio)
        model.startRecording()
        assertEquals(RecorderPhase.Preparing, model.state.value.phase)
        assertTrue(model.state.value.error!!.contains("microfone"))
        model.onDispose()
    }

    @Test
    fun countdownOpensTheMicrophoneOnlyWhenItReachesZero() = runTest(dispatcher) {
        val audio = FakeAudio()
        val model = model(audio = audio)
        model.requestStart()
        assertEquals(RecorderPhase.Countdown, model.state.value.phase)
        assertEquals(3, model.state.value.countdown)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, model.state.value.countdown)
        assertTrue(audio.calls.isEmpty())

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(RecorderPhase.Recording, model.state.value.phase)
        assertEquals(listOf("start"), audio.calls)
        model.onDispose()
    }

    @Test
    fun cancellingTheCountdownNeverOpensTheMicrophone() = runTest(dispatcher) {
        val audio = FakeAudio()
        val model = model(audio = audio)
        model.requestStart()
        advanceTimeBy(1_500)
        model.cancelCountdown()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(RecorderPhase.Preparing, model.state.value.phase)
        assertEquals(0, model.state.value.countdown)
        assertTrue(audio.calls.isEmpty())
        model.onDispose()
    }

    @Test
    fun microphoneLevelIsShownWhileRecordingAndDropsOnPause() = runTest(dispatcher) {
        val audio = FakeAudio().apply { currentLevel = 0.6f }
        val model = model(audio = audio)
        model.startRecording()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(0.6f, model.state.value.micLevel)

        model.pause()
        assertEquals(0f, model.state.value.micLevel)
        model.onDispose()
    }

    @Test
    fun eachRecordedActionGetsALabelForTheSeller() = runTest(dispatcher) {
        val model = model()
        model.startRecording()
        record(model, navigate(1))
        assertEquals(StartingPointLabel, model.state.value.lastAction?.label)

        time += 300.milliseconds
        record(model, scroll(2))
        assertEquals(StartingPointLabel, model.state.value.lastAction?.label)

        time += 1_500.milliseconds
        record(model, openProduct(3))
        assertEquals("Abriu um produto", model.state.value.lastAction?.label)
        record(model, draw(4))
        assertEquals("Desenhou na vitrine", model.state.value.lastAction?.label)
        record(model, navigate(5))
        assertEquals("Fechou o produto", model.state.value.lastAction?.label)
        record(model, scroll(6))
        assertEquals("Rolou a vitrine", model.state.value.lastAction?.label)
        assertEquals(6, model.state.value.eventCount)
        model.onDispose()
    }

    @Test
    fun publishingReportsEachUploadStep() = runTest(dispatcher) {
        val steps = mutableListOf<UploadStep?>()
        lateinit var recorder: PresentationRecorderScreenModel
        recorder = model(
            create = { steps += recorder.state.value.uploadStep; ApiResult.Ok(created) },
            upload = { _, _ -> steps += recorder.state.value.uploadStep; ApiResult.Ok(Unit) },
        )
        recorder.startRecording()
        time += 2_000.milliseconds
        recorder.finish()
        advanceUntilIdle()
        recorder.publish()
        advanceUntilIdle()

        assertEquals(listOf<UploadStep?>(UploadStep.Creating, UploadStep.Audio, UploadStep.Timeline), steps)
        assertNull(recorder.state.value.uploadStep)
        assertEquals(RecorderPhase.Shared, recorder.state.value.phase)
        recorder.onDispose()
    }

    @Test
    fun tooShortRecordingIsDiscarded() = runTest(dispatcher) {
        val audio = FakeAudio()
        val model = model(audio = audio)
        model.startRecording()
        time += 300.milliseconds
        model.finish()
        advanceUntilIdle()
        assertEquals(RecorderPhase.Preparing, model.state.value.phase)
        assertTrue(audio.calls.containsAll(listOf("start", "stop", "discard")))
        model.onDispose()
    }

    @Test
    fun publishedRecordingIsKeptForTheRecordingsTab() = runTest(dispatcher) {
        val kept = mutableListOf<StoredPresentation>()
        val model = model(persist = { kept += it })
        model.startRecording()
        record(model, navigate(1))
        time += 3_000.milliseconds
        record(model, openProduct(2))
        model.finish()
        advanceUntilIdle()
        assertTrue(kept.isEmpty())

        model.publish()
        advanceUntilIdle()

        val stored = kept.single()
        assertEquals("p7Kq2mZr8XwT4nBc", stored.presentationId)
        assertEquals(created.shareUrl, stored.shareUrl)
        assertEquals("buba-teste", stored.empresaSlug)
        assertEquals("Outono 26", stored.catalogoNome)
        assertEquals("31", stored.sellerId)
        assertEquals(3_000, stored.durationMs)
        assertEquals(2, stored.eventCount)
        assertEquals(1_790_000_000_000, stored.recordedAtMs)
        assertEquals(1_790_000_000_000, stored.uploadedAtMs)
        assertTrue(model.state.value.savedToRecordings)
        model.onDispose()
    }

    @Test
    fun failedUploadKeepsNothingInTheRecordingsTab() = runTest(dispatcher) {
        val kept = mutableListOf<StoredPresentation>()
        val model = model(
            upload = { _, _ -> ApiResult.Fail("upload_failed", "HTTP 403", 403) },
            persist = { kept += it },
        )
        model.startRecording()
        time += 2_000.milliseconds
        model.finish()
        advanceUntilIdle()
        model.publish()
        advanceUntilIdle()

        assertEquals(RecorderPhase.Review, model.state.value.phase)
        assertTrue(kept.isEmpty())
        model.onDispose()
    }

    @Test
    fun linkIsSharedEvenWhenTheRecordingsTabCannotKeepIt() = runTest(dispatcher) {
        val model = model(persist = { error("disco cheio") })
        model.startRecording()
        time += 2_000.milliseconds
        model.finish()
        advanceUntilIdle()
        model.publish()
        advanceUntilIdle()

        assertEquals(RecorderPhase.Shared, model.state.value.phase)
        assertEquals(created.shareUrl, model.state.value.shareUrl)
        assertFalse(model.state.value.savedToRecordings)
        model.onDispose()
    }
}
