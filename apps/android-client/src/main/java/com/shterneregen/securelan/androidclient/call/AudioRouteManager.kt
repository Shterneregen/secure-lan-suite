package com.shterneregen.securelan.androidclient.call

import android.content.Context
import android.media.*
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("DEPRECATION")
class AudioRouteManager(private val context: Context, private val onFocusLost: () -> Unit) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var previousMode = AudioManager.MODE_NORMAL
    private var previousSpeaker = false
    private var previousDevice: AudioDeviceInfo? = null
    private var previousSco = false
    private var started = false
    private val mutableSpeaker = MutableStateFlow(false)
    val speaker = mutableSpeaker.asStateFlow()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setOnAudioFocusChangeListener { if (it == AudioManager.AUDIOFOCUS_LOSS || it == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) onFocusLost() }
        .build()
    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) { if (started) route(mutableSpeaker.value) }
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) { if (started) route(mutableSpeaker.value) }
    }

    fun start(video: Boolean) {
        if (started) return
        previousMode = audio.mode; previousSpeaker = audio.isSpeakerphoneOn
        if (Build.VERSION.SDK_INT >= 31) previousDevice = audio.communicationDevice
        else previousSco = audio.isBluetoothScoOn
        check(audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "Audio focus unavailable" }
        started = true
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        audio.registerAudioDeviceCallback(devices, android.os.Handler(android.os.Looper.getMainLooper()))
        route(video)
    }
    fun toggleSpeaker() { if (started) route(!mutableSpeaker.value) }
    private fun route(speaker: Boolean) {
        if (Build.VERSION.SDK_INT >= 31) {
            val available = audio.availableCommunicationDevices
            val bluetoothAllowed = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val external = available.firstOrNull {
                it.type in setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET) ||
                    (bluetoothAllowed && it.type in setOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET))
            }
            val selected = if (speaker) available.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                else external ?: available.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            if (selected != null) {
                if (audio.setCommunicationDevice(selected)) mutableSpeaker.value = selected.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else { audio.clearCommunicationDevice(); mutableSpeaker.value = false }
        } else {
            if (!speaker && audio.isBluetoothScoAvailableOffCall && audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }) {
                audio.startBluetoothSco(); audio.isBluetoothScoOn = true
            } else { audio.stopBluetoothSco(); audio.isBluetoothScoOn = false }
            audio.isSpeakerphoneOn = speaker
            mutableSpeaker.value = speaker
        }
    }
    fun stop() {
        if (!started) return
        started = false
        audio.unregisterAudioDeviceCallback(devices)
        if (Build.VERSION.SDK_INT >= 31) {
            val previous = previousDevice
            if (previous == null || !audio.availableCommunicationDevices.any { it.id == previous.id } || !audio.setCommunicationDevice(previous)) {
                audio.clearCommunicationDevice()
            }
            previousDevice = null
        } else {
            if (previousSco) audio.startBluetoothSco() else audio.stopBluetoothSco()
            audio.isBluetoothScoOn = previousSco
        }
        audio.isSpeakerphoneOn = previousSpeaker
        audio.mode = previousMode
        audio.abandonAudioFocusRequest(focus)
        mutableSpeaker.value = false
    }
}
