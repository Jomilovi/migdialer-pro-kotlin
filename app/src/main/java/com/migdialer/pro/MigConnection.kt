package com.migdialer.pro

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.DisconnectCause

/**
 * Connection individual que representa una llamada activa.
 * Aquí tenemos control total del audio — incluyendo altavoz en Samsung.
 */
class MigConnection(private val address: Uri) : Connection() {

    private val audioManager: AudioManager by lazy {
        MigApp.instance.getSystemService(AudioManager::class.java)
    }

    init {
        setAddress(address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
        setCallerDisplayName("", android.telecom.TelecomManager.PRESENTATION_ALLOWED)
        audioModeIsVoip = false   // GSM nativo — no VoIP
        setDialing()
    }

    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
        MigInCallService.stateListener?.invoke(state)
    }

    override fun onDisconnect() {
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
        current = null
    }

    override fun onAbort() {
        setDisconnected(DisconnectCause(DisconnectCause.UNKNOWN))
        destroy()
        current = null
    }

    /**
     * Control de altavoz con acceso directo al AudioManager desde Connection.
     * Aquí Samsung no bloquea porque estamos dentro del stack de telecom.
     */
    fun setSpeaker(on: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            audioManager.mode = if (on) AudioManager.MODE_IN_CALL else AudioManager.MODE_IN_CALL
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        }, 200)
    }

    companion object {
        @Volatile var current: MigConnection? = null
    }
}
