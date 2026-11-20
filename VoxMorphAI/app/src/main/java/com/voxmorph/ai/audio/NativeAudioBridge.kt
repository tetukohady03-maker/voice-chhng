package com.voxmorph.ai.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.voxmorph.ai.data.model.ProcessingMode
import com.voxmorph.ai.data.model.VoicePreset

class NativeAudioBridge(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var nativeLoaded = false

    var onRouteChanged: ((String) -> Unit)? = null
    var onFocusLost: (() -> Unit)? = null

    init {
        nativeLoaded = try {
            System.loadLibrary("voxmorph_dsp")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native libvoxmorph_dsp", e)
            false
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            onRouteChanged?.invoke(currentOutputRouteName())
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            onRouteChanged?.invoke(currentOutputRouteName())
        }
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Pause engine cleanly on incoming phone calls or exclusive audio capture
                stop()
                onFocusLost?.invoke()
            }
        }
    }

    fun start(requestedSampleRate: Int = 0): Boolean {
        if (!nativeLoaded) return false
        requestAudioFocus()
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))

        val nativeSr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull() ?: 48000
        val nativeBurst = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            ?.toIntOrNull() ?: 192
        val targetSr = if (requestedSampleRate > 0) requestedSampleRate else nativeSr
        return nativeStart(targetSr, nativeBurst)
    }

    fun stop() {
        if (nativeLoaded) {
            nativeStop()
        }
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {
        }
        abandonAudioFocus()
    }

    fun isRunning(): Boolean = nativeLoaded && nativeIsRunning()

    fun applyPreset(
        preset: VoicePreset,
        monitoringVolume: Float,
        processingMode: ProcessingMode,
        muted: Boolean,
        bypassed: Boolean
    ) {
        if (!nativeLoaded) return
        nativeUpdateParams(
            preset.pitchSemitones,
            preset.formantShift,
            preset.bassDb,
            preset.trebleDb,
            preset.resonance,
            preset.brightness,
            preset.voiceDepth,
            preset.modulationIntensity,
            preset.noiseSuppression,
            preset.reverbMix,
            preset.distortionDrive,
            preset.robotModMix,
            monitoringVolume,
            preset.vocalFilterMode,
            processingMode.code,
            muted,
            bypassed,
            preset.spectralProfileEnabled,
            preset.spectralArray()
        )
    }

    fun getInputRms(): Float = if (nativeLoaded) nativeGetInputRms() else 0f
    fun getOutputRms(): Float = if (nativeLoaded) nativeGetOutputRms() else 0f
    fun getLatencyMs(): Float = if (nativeLoaded) nativeGetLatencyMs() else 0f
    fun getUnderrunCount(): Int = if (nativeLoaded) nativeGetUnderrunCount() else 0

    fun getWaveform(size: Int = 64): FloatArray {
        val out = FloatArray(size)
        if (nativeLoaded) nativeGetWaveform(out)
        return out
    }

    fun processOffline(samples: FloatArray): FloatArray =
        if (nativeLoaded) nativeProcessOffline(samples) ?: samples else samples

    fun currentOutputRouteName(): String {
        val outs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return when {
            outs.any { it.type == AudioDeviceInfo.TYPE_USB_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_DEVICE } ->
                "USB Audio Interface / Headset"
            outs.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } ->
                "Bluetooth Headset"
            outs.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES } ->
                "Wired Headphones"
            else -> "Device Speaker (gunakan headset untuk hindari feedback)"
        }
    }

    private fun requestAudioFocus() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = req
        audioManager.requestAudioFocus(req)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private external fun nativeStart(sampleRate: Int, framesPerBurst: Int): Boolean
    private external fun nativeStop()
    private external fun nativeIsRunning(): Boolean
    private external fun nativeUpdateParams(
        pitchSemitones: Float, formantShift: Float, bassDb: Float, trebleDb: Float,
        resonance: Float, brightness: Float, voiceDepth: Float, modulationIntensity: Float,
        noiseSuppression: Float, reverbMix: Float, distortionDrive: Float, robotModMix: Float,
        monitoringVolume: Float, vocalFilterMode: Int, processingMode: Int,
        muted: Boolean, bypassed: Boolean, spectralEnabled: Boolean, spectralBands: FloatArray
    )
    private external fun nativeGetInputRms(): Float
    private external fun nativeGetOutputRms(): Float
    private external fun nativeGetLatencyMs(): Float
    private external fun nativeGetUnderrunCount(): Int
    private external fun nativeGetWaveform(outArr: FloatArray)
    private external fun nativeProcessOffline(inputArr: FloatArray): FloatArray?

    companion object {
        private const val TAG = "NativeAudioBridge"
    }
}
