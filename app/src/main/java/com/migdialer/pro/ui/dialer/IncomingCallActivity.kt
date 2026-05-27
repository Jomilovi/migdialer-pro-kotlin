package com.migdialer.pro.ui.dialer

import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
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
                    Call.STATE_ACTIVE -> {
                        // Contestada desde otro lugar (auriculares, etc.)
                        stopRingtone()
                        val name = intent.getStringExtra(MigInCallService.EXTRA_DISPLAY_NAME) ?: ""
                        val photo = intent.getStringExtra(MigInCallService.EXTRA_PHOTO_URI)
                        startActivity(
                            android.content.Intent(this@IncomingCallActivity, InCallActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(MigInCallService.EXTRA_DISPLAY_NAME, name)
                                putExtra(MigInCallService.EXTRA_PHOTO_URI, photo)
                            }
                        )
                        finish()
                    }
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

        val name     = intent.getStringExtra(MigInCallService.EXTRA_DISPLAY_NAME) ?: ""
        val photoUri = intent.getStringExtra(MigInCallService.EXTRA_PHOTO_URI)

        binding.tvCallerName.text  = name.ifBlank { getString(R.string.unknown_caller) }
        binding.tvCallStatus.text  = getString(R.string.incoming_call)

        val initial = name.firstOrNull()?.uppercase() ?: "#"
        loadAvatar(photoUri, initial)

        MigInCallService.currentCall?.registerCallback(callCallback)

        setupButtons()
        startRingtone()
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
        binding.btnAccept.setOnClickListener {
            stopRingtone()
            MigInCallService.currentCall?.answer(0)
            val name     = intent.getStringExtra(MigInCallService.EXTRA_DISPLAY_NAME) ?: ""
            val photoUri = intent.getStringExtra(MigInCallService.EXTRA_PHOTO_URI)
            startActivity(
                android.content.Intent(this, InCallActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(MigInCallService.EXTRA_DISPLAY_NAME, name)
                    putExtra(MigInCallService.EXTRA_PHOTO_URI, photoUri)
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
        } catch (e: Exception) { }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VibratorManager::class.java))?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
        } catch (e: Exception) { }
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* No salir durante llamada entrante */ }
}
