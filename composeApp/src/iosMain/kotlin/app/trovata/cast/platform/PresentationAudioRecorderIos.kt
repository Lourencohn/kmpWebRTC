package app.trovata.cast.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

private const val SAMPLE_RATE_HZ = 44_100.0
private const val BIT_RATE_BPS = 48_000
private const val SILENCE_DB = -50f

@OptIn(ExperimentalForeignApi::class)
actual class PresentationAudioRecorder : AudioCapture {
    private var recorder: AVAudioRecorder? = null
    private var output: NSURL? = null

    override fun start() {
        discard()
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        session.setActive(true, error = null)
        val url = NSURL.fileURLWithPath("${NSTemporaryDirectory()}apresentacao-${NSUUID().UUIDString}.m4a")
        val settings: Map<Any?, Any?> = mapOf(
            AVFormatIDKey to kAudioFormatMPEG4AAC.toInt(),
            AVSampleRateKey to SAMPLE_RATE_HZ,
            AVNumberOfChannelsKey to 1,
            AVEncoderBitRateKey to BIT_RATE_BPS,
        )
        val created = AVAudioRecorder(uRL = url, settings = settings, error = null)
        created.meteringEnabled = true
        check(created.prepareToRecord() && created.record()) { "Não consegui abrir o microfone" }
        recorder = created
        output = url
    }

    override fun pause() {
        recorder?.pause()
    }

    override fun resume() {
        recorder?.record()
    }

    override fun stop(): ByteArray {
        val active = checkNotNull(recorder) { "Gravação não iniciada" }
        val url = checkNotNull(output) { "Gravação não iniciada" }
        recorder = null
        output = null
        active.stop()
        val data = checkNotNull(NSData.dataWithContentsOfURL(url)) { "Áudio da gravação não encontrado" }
        NSFileManager.defaultManager.removeItemAtURL(url, error = null)
        return data.toByteArray()
    }

    override fun discard() {
        recorder?.let { active ->
            active.stop()
            active.deleteRecording()
        }
        recorder = null
        output = null
    }

    override fun level(): Float {
        val active = recorder ?: return 0f
        active.updateMeters()
        val decibels = active.averagePowerForChannel(0u)
        return ((decibels - SILENCE_DB) / -SILENCE_DB).coerceIn(0f, 1f)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    }
    return bytes
}
