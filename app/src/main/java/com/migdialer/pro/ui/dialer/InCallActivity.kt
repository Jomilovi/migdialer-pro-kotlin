package com.migdialer.pro.ui.dialer

import android.media.AudioManager
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

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            runOnUiThread { handleCallState(state) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(AudioManager::class.java)

        val name = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        binding.tvCallerName.text = name.ifBlank { getString(R.string.unknown_caller) }
        binding.tvCallStatus.text = getString(R.string.call_dialing)

        setupButtons()
        attachToCall()
    }

    private fun setupButtons() {
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            binding.icMute.setImageResource(
                if (isMuted) R.drawable.ic_mute_off else R.drawable.ic_mute
            )
            binding.icMute.alpha = if (isMuted) 1f else 0.5f
            binding.tvMuteLabel.text = if (isMuted) "Activado" else "Silenciar"
        }

        binding.btnSpeaker.setOnClickListener {
            isSpeaker = !isSpeaker
            audioManager.isSpeakerphoneOn = isSpeaker
            binding.icSpeaker.alpha = if (isSpeaker) 1f else 0.5f
            binding.tvSpeakerLabel.text = if (isSpeaker) "Activado" else "Altavoz"
        }

        binding.btnEndCall.setOnClickListener {
            MigInCallService.currentCall?.disconnect()
        }
    }

    private fun attachToCall() {
        val call = MigInCallService.currentCall
        if (call != null) {
            call.registerCallback(callCallback)
            handleCallState(call.state)
        } else {
            MigInCallService.onCallReady = { call ->
                runOnUiThread {
                    call.registerCallback(callCallback)
                    handleCallState(call.state)
                }
            }
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
                    callConnected = true
                    secondsElapsed = 0
                    handler.post(timerRunnable)
                }
            }
            Call.STATE_HOLDING -> {
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
        MigInCallService.currentCall?.unregisterCallback(callCallback)
        MigInCallService.onCallReady = null
        // Restaurar audio
        audioManager.isMicrophoneMute = false
        audioManager.isSpeakerphoneOn = false
    }

    override fun onBackPressed() {
        // No permitir salir con back mientras hay llamada activa
    }

    companion object {
        const val EXTRA_DISPLAY_NAME = "display_name"
    }
}
