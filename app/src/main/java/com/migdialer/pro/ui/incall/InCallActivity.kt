package com.migdialer.pro.ui.incall

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.migdialer.pro.databinding.ActivityInCallBinding
import com.migdialer.pro.telecom.MigCallState

class InCallActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityInCallBinding
    private val viewModel: InCallViewModel by viewModels()

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupProximitySensor()
        setupClickListeners()
        observeViewModel()
        requestAudioFocus()
    }

    private fun observeViewModel() {
        viewModel.callState.observe(this) { state -> updateCallStateUI(state) }

        viewModel.callerInfo.observe(this) { info ->
            binding.tvCallerName.text = info.displayName.ifBlank { info.number }
            binding.tvCallerNumber.text = if (info.displayName.isNotBlank()) info.number else ""
            binding.tvCallerNumber.isVisible = info.displayName.isNotBlank()
        }

        viewModel.elapsedSeconds.observe(this) { secs ->
            binding.tvTimer.text = formatDuration(secs)
        }

        viewModel.isMuted.observe(this) { muted ->
            binding.btnMute.alpha = if (muted) 1f else 0.5f
            binding.tvMuteLabel.text = if (muted) "Sin micro" else "Silencio"
        }

        viewModel.isSpeaker.observe(this) { speaker ->
            binding.btnSpeaker.alpha = if (speaker) 1f else 0.5f
            binding.tvSpeakerLabel.text = if (speaker) "Altavoz on" else "Altavoz"
        }

        viewModel.isHolding.observe(this) { holding ->
            binding.btnHold.alpha = if (holding) 1f else 0.5f
            binding.tvHoldLabel.text = if (holding) "En espera" else "Espera"
            binding.tvCallStatus.text = if (holding) "En espera" else ""
            binding.tvCallStatus.isVisible = holding
        }

        viewModel.simDisplayName.observe(this) { name ->
            binding.tvSimName.text = name
            binding.tvSimName.isVisible = name.isNotBlank()
        }

        viewModel.isDtmfVisible.observe(this) { visible ->
            binding.dtmfPanel.isVisible = visible
        }
    }

    private fun updateCallStateUI(state: MigCallState) {
        when (state) {
            MigCallState.RINGING -> {
                binding.tvCallStatus.text = "Llamada entrante"
                binding.tvCallStatus.isVisible = true
                binding.tvTimer.isVisible = false
                binding.answerGroup.isVisible = true
                binding.btnHangUp.isVisible = false
                showPulse()
            }
            MigCallState.DIALING -> {
                binding.tvCallStatus.text = "Marcando…"
                binding.tvCallStatus.isVisible = true
                binding.tvTimer.isVisible = false
                binding.answerGroup.isVisible = false
                binding.btnHangUp.isVisible = true
            }
            MigCallState.ACTIVE -> {
                binding.tvCallStatus.isVisible = false
                binding.tvTimer.isVisible = true
                binding.answerGroup.isVisible = false
                binding.btnHangUp.isVisible = true
                stopPulse()
                viewModel.startTimer()
            }
            MigCallState.DISCONNECTED,
            MigCallState.IDLE -> {
                viewModel.stopTimer()
                stopPulse()
                finish()
            }
            else -> {}
        }
    }

    private fun setupClickListeners() {
        binding.btnAnswer.setOnClickListener  { animateBtn(it); viewModel.answer() }
        binding.btnReject.setOnClickListener  { animateBtn(it); viewModel.reject() }
        binding.btnHangUp.setOnClickListener  { animateBtn(it); viewModel.hangUp() }
        binding.btnMute.setOnClickListener    { animateBtn(it); viewModel.toggleMute() }
        binding.btnSpeaker.setOnClickListener { animateBtn(it); viewModel.toggleSpeaker() }
        binding.btnHold.setOnClickListener    { animateBtn(it); viewModel.toggleHold() }
        binding.btnDtmf.setOnClickListener    { animateBtn(it); viewModel.toggleDtmf() }
        setupDtmfKeys()
    }

    private fun setupDtmfKeys() {
        val keys   = listOf(binding.dtmf1, binding.dtmf2, binding.dtmf3,
                            binding.dtmf4, binding.dtmf5, binding.dtmf6,
                            binding.dtmf7, binding.dtmf8, binding.dtmf9,
                            binding.dtmfStar, binding.dtmf0, binding.dtmfHash)
        val digits = listOf('1','2','3','4','5','6','7','8','9','*','0','#')
        keys.forEachIndexed { i, btn ->
            btn.setOnClickListener { viewModel.sendDtmf(digits[i]) }
            btn.setOnLongClickListener { viewModel.stopDtmf(); true }
        }
    }

    private var pulseAnimator: ObjectAnimator? = null

    private fun showPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofFloat(binding.callerAvatar, View.SCALE_X, 1f, 1.08f).apply {
            duration = 700; repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator(); start()
        }
        ObjectAnimator.ofFloat(binding.callerAvatar, View.SCALE_Y, 1f, 1.08f).apply {
            duration = 700; repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator(); start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        binding.callerAvatar.scaleX = 1f
        binding.callerAvatar.scaleY = 1f
    }

    private fun animateBtn(view: View) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(80).start() }
            .start()
    }

    private fun setupProximitySensor() {
        sensorManager  = getSystemService(SensorManager::class.java)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val nearEar = event.values[0] < (proximitySensor?.maximumRange ?: 5f) * 0.5f
        val am = getSystemService(AudioManager::class.java)
        if (nearEar) {
            window.attributes = window.attributes.also { it.screenBrightness = 0.01f }
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_IN_CALL
        } else {
            window.attributes = window.attributes.also {
                it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun requestAudioFocus() {
        val am = getSystemService(AudioManager::class.java)
        am.mode = AudioManager.MODE_IN_CALL
        am.isSpeakerphoneOn = false
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPulse()
        getSystemService(AudioManager::class.java).mode = AudioManager.MODE_NORMAL
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { moveTaskToBack(true) }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }
}
