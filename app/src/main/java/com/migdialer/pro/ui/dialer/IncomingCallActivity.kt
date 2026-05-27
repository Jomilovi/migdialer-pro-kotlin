package com.migdialer.pro.ui.dialer

import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.migdialer.pro.MigInCallService
import com.migdialer.pro.R
import com.migdialer.pro.databinding.ActivityIncomingCallBinding

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            runOnUiThread {
                when (state) {
                    Call.STATE_DISCONNECTED,
                    Call.STATE_DISCONNECTING -> {
                        stopRingtone()
                        finish()
                    }
                }
            }
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
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        binding.tvCallerName.text = name.ifBlank { getString(R.string.unknown_caller) }
        binding.tvCallStatus.text = getString(R.string.incoming_call)

        // Registrar callback para detectar si la llamada se cancela sola
        MigInCallService.currentCall?.registerCallback(callCallback)

        setupButtons()
        startRingtone()
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            stopRingtone()
            MigInCallService.currentCall?.answer(0)
            // Abrir pantalla de llamada activa
            val name = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
            startActivity(
                android.content.Intent(this, InCallActivity::class.java).apply {
                    putExtra(InCallActivity.EXTRA_DISPLAY_NAME, name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            finish()
        }

        binding.btnDecline.setOnClickListener {
            stopRingtone()
            MigInCallService.currentCall?.reject(false, null)
            finish()
        }
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.play()
        } catch (e: Exception) { /* Sin ringtone no es crítico */ }

        // Vibrar en patrón de llamada
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VibratorManager::class.java))?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) { /* Sin vibración no es crítico */ }
    }

    private fun stopRingtone() {
        try { ringtone?.stop() } catch (e: Exception) { }
        try { vibrator?.cancel() } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        MigInCallService.currentCall?.unregisterCallback(callCallback)
    }

    companion object {
        const val EXTRA_DISPLAY_NAME = "display_name"
    }
}
