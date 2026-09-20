package app.trovata.cast.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

private val HEADSET_DEVICE_TYPES = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
)

actual class CallAudioController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var modeBeforeCall = AudioManager.MODE_NORMAL
    private var speakerphoneBeforeCall = false
    private var focusRequest: AudioFocusRequest? = null
    private var active = false

    actual fun activate() {
        if (!active) {
            active = true
            rememberStateBeforeCall()
            requestVoiceFocus()
        }
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        routeToSpeakerUnlessHeadsetIsConnected()
    }

    @Suppress("DEPRECATION")
    private fun rememberStateBeforeCall() {
        modeBeforeCall = audioManager.mode
        speakerphoneBeforeCall = audioManager.isSpeakerphoneOn
    }

    actual fun release() {
        if (!active) return
        active = false
        restoreRoute()
        audioManager.mode = modeBeforeCall
        focusRequest?.let(audioManager::abandonAudioFocusRequest)
        focusRequest = null
    }

    private fun requestVoiceFocus() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun routeToSpeakerUnlessHeadsetIsConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.availableCommunicationDevices
            if (devices.any { it.type in HEADSET_DEVICE_TYPES }) return
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let(audioManager::setCommunicationDevice)
            return
        }
        @Suppress("DEPRECATION")
        run {
            if (audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn) return
            audioManager.isSpeakerphoneOn = true
        }
    }

    private fun restoreRoute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            return
        }
        @Suppress("DEPRECATION")
        run { audioManager.isSpeakerphoneOn = speakerphoneBeforeCall }
    }
}
