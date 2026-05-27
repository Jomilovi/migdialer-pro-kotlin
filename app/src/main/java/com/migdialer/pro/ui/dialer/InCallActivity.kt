package com.migdialer.pro.ui.dialer

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.migdialer.pro.MigInCallService
import com.migdialer.pro.R
import com.migdialer.pro.databinding.ActivityInCallBinding

class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInCallBinding
    private lateinit var audioManager: AudioManager

    private var isMuted = false
    private var isSpeaker = false
    private var callConnected = false
    private var secondsElapsed = 0

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

        val name = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        binding.tvCallerName.text = name.ifBlank { getString(R.string.unknown_caller) }
        binding.tvCallStatus.text = getString(R.string.call_dialing)

        setupButtons()

        MigInCallService.stateListener = { state -> runOnUiThread { handleCallState(state) } }
        MigInCallService.currentCall?.let { handleCallState(it.state) }
    }

    private fun setupButtons() {
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            binding.icMute.setImageResource(
                if (isMuted) R.drawable.ic_mute_off else R.drawable.ic_mute
            )
            binding.icMute.alpha = if (isMuted) 1f else 0.5f
            binding.tvMuteLabel.text =
                if (isMuted) getString(R.string.mute_on) else getString(R.string.mute_off)
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeaker = !isSpeaker
            setSpeakerphone(isSpeaker)
            binding.icSpeaker.alpha = if (isSpeaker) 1f else 0.5f
            binding.tvSpeakerLabel.text =
                if (isSpeaker) getString(R.string.speaker_on) else getString(R.string.speaker_off)
        }

        binding.btnEndCall.setOnClickListener {
            MigInCallService.currentCall?.disconnect()
            finish()
        }
    }

    /**
     * Activar/desactivar altavoz correctamente durante una llamada.
     * Requiere primero poner el modo de audio en MODE_IN_CALL.
     */
    private fun setSpeakerphone(on: Boolean) {
        if (on) {
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = true
        } else {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_IN_CALL
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            Call.STATE_CONNECTING,
            Call.STATE_DIALING -> {
                binding.tvCallStatus.text = getString(R.string.call_dialing)
                // Poner audio en modo llamada desde el inicio
                audioManager.mode = AudioManager.MODE_IN_CALL
            }
            Call.STATE_RINGING -> {
                binding.tvCallStatus.text = getString(R.string.call_ringing)
            }
            Call.STATE_ACTIVE -> {
                audioManager.mode = AudioManager.MODE_IN_CALL
                if (!callConnected) {
                    callConnected = true
                    secondsElapsed = 0
                    handler.post(timerRunnable)
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
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* Bloquear back durante llamada */ }

    companion object {
        const val EXTRA_DISPLAY_NAME = "display_name"
    }
}
