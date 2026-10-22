package app.trovata.cast.platform

interface AudioCapture {
    fun start()
    fun pause()
    fun resume()
    fun stop(): ByteArray
    fun discard()
    fun level(): Float
}

expect class PresentationAudioRecorder : AudioCapture
