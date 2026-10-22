package app.trovata.cast.platform

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import kotlin.math.sqrt

private const val SAMPLE_RATE_HZ = 44_100
private const val BIT_RATE_BPS = 48_000
private const val MAX_AMPLITUDE = 32_767f

actual class PresentationAudioRecorder(private val context: Context) : AudioCapture {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    override fun start() {
        discard()
        val file = File.createTempFile("apresentacao-", ".m4a", context.cacheDir)
        val created = newMediaRecorder()
        try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioChannels(1)
            created.setAudioSamplingRate(SAMPLE_RATE_HZ)
            created.setAudioEncodingBitRate(BIT_RATE_BPS)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
        } catch (failure: Throwable) {
            created.release()
            file.delete()
            throw failure
        }
        recorder = created
        output = file
    }

    override fun pause() {
        recorder?.pause()
    }

    override fun resume() {
        recorder?.resume()
    }

    override fun stop(): ByteArray {
        val active = checkNotNull(recorder) { "Gravação não iniciada" }
        val file = checkNotNull(output) { "Gravação não iniciada" }
        recorder = null
        output = null
        try {
            active.stop()
        } finally {
            active.release()
        }
        return file.readBytes().also { file.delete() }
    }

    override fun discard() {
        recorder?.let { active ->
            runCatching { active.stop() }
            active.release()
        }
        recorder = null
        output?.delete()
        output = null
    }

    override fun level(): Float {
        val amplitude = recorder?.let { active -> runCatching { active.maxAmplitude }.getOrNull() } ?: return 0f
        return sqrt(amplitude.toFloat() / MAX_AMPLITUDE).coerceIn(0f, 1f)
    }

    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
