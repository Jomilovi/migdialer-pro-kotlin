package com.migdialer.pro.ui.dialer

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.migdialer.pro.MigConnection
import com.migdialer.pro.MigInCallService
import com.migdialer.pro.R
import com.migdialer.pro.databinding.ActivityInCallBinding

class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInCallBinding
    private lateinit var audioManager: AudioManager

    private var isMuted   = false
    private var isSpeaker = false
    private var callConnected = false
    private var secondsElapsed = 0
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pendingSpeaker = false // speaker solicitado antes de que la llamada esté activa

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            val m = secondsElapsed / 60
            val s = secondsElapsed % 60
            binding.tvCallStatus.text = "%02d:%02d".format(m, s)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(AudioManager::class.java)

        val name     = intent.getStringExtra(MigInCallService.EXTRA_DISPLAY_NAME) ?: ""
        val photoUri = intent.getStringExtra(MigInCallService.EXTRA_PHOTO_URI)
        val initial  = name.firstOrNull()?.uppercase() ?: "#"

        binding.tvCallerName.text = name.ifBlank { getString(R.string.unknown_caller) }
        binding.tvCallStatus.text = getString(R.string.call_dialing)

        loadAvatar(photoUri, initial)
        setupButtons()

        // Pedir AudioFocus — el modo de audio lo gestiona el sistema via InCallService
        requestAudioFocus()

        MigInCallService.stateListener = { state -> runOnUiThread { handleCallState(state) } }
        MigInCallService.currentCall?.let { handleCallState(it.state) }
    }

    private fun loadAvatar(photoUri: String?, initial: String) {
        binding.tvAvatarInitial.text = initial
        if (!photoUri.isNullOrBlank()) {
            binding.ivAvatar.load(Uri.parse(photoUri)) {
                transformations(CircleCropTransformation())
                crossfade(false)
                listener(
                    onSuccess = { _, _ -> binding.tvAvatarInitial.visibility = View.GONE },
                    onError   = { _, _ -> binding.tvAvatarInitial.visibility = View.VISIBLE }
                )
            }
        }
    }

    private fun setupButtons() {
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            binding.icMute.setImageResource(
                if (isMuted) R.drawable.ic_mute_off else R.drawable.ic_mute
            )
            binding.icMute.alpha     = if (isMuted) 1f else 0.5f
            binding.tvMuteLabel.text = getString(
                if (isMuted) R.string.mute_on else R.string.mute_off
            )
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeaker = !isSpeaker
            if (callConnected) {
                // Llamada activa — aplicar inmediatamente
                toggleSpeaker(isSpeaker)
            } else {
                // Llamada aún no activa — guardar para aplicar cuando conecte
                pendingSpeaker = isSpeaker
            }
            binding.icSpeaker.alpha     = if (isSpeaker) 1f else 0.5f
            binding.tvSpeakerLabel.text = getString(
                if (isSpeaker) R.string.speaker_on else R.string.speaker_off
            )
        }

        binding.btnEndCall.setOnClickListener {
            MigInCallService.currentCall?.disconnect()
            finish()
        }
    }

    /**
     * Pide el foco de audio al sistema con AudioAttributes correctos.
     * Sin esto Android puede ignorar setCommunicationDevice durante una llamada.
     */
    private fun requestAudioFocus() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { /* Mantenemos el foco durante la llamada */ }
            .build()

        audioManager.requestAudioFocus(audioFocusRequest!!)
    }

    /**
     * Altavoz via MigConnection — tenemos control directo del audio
     * desde dentro del stack de telecom, Samsung no puede bloquearlo.
     */
    private fun toggleSpeaker(on: Boolean) {
        MigConnection.current?.setSpeaker(on) ?: run {
            // Fallback si ConnectionService no está activo
            handler.postDelayed({
                audioManager.mode = AudioManager.MODE_IN_CALL
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = on
            }, 300)
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            Call.STATE_CONNECTING,
            Call.STATE_DIALING -> {
                binding.tvCallStatus.text = getString(R.string.call_dialing)
            }
            Call.STATE_RINGING -> {
                binding.tvCallStatus.text = getString(R.string.call_ringing)
            }
            Call.STATE_ACTIVE -> {
                if (!callConnected) {
                    callConnected  = true
                    secondsElapsed = 0
                    handler.post(timerRunnable)
                    // Aplicar speaker si el usuario lo pidió antes de conectar
                    if (pendingSpeaker) toggleSpeaker(true)
                }
            }
            Call.STATE_HOLDING -> {
                handler.removeCallbacks(timerRunnable)
                binding.tvCallStatus.text = getString(R.string.call_on_hold)
            }
            Call.STATE_DISCONNECTING,
            Call.STATE_DISCONNECTED -> {
                handler.removeCallbacks(timerRunnable)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        MigInCallService.stateListener = null
        audioManager.clearCommunicationDevice()
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* Bloquear back durante llamada */ }
}
