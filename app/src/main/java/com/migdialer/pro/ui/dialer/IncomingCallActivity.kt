package com.migdialer.pro.ui.dialer

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.migdialer.pro.databinding.ActivityIncomingCallBinding
import com.migdialer.pro.telecom.MigCallState
import com.migdialer.pro.ui.incall.InCallViewModel

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private val viewModel: InCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeViewModel()
        setupClickListeners()
        startPulseAnimation()
    }

    private fun observeViewModel() {
        viewModel.callerInfo.observe(this) { info ->
            binding.tvCallerName.text = info.displayName.ifBlank { info.number }
            binding.tvCallerNumber.text = if (info.displayName.isNotBlank()) info.number else ""
            binding.tvCallerNumber.isVisible = info.displayName.isNotBlank()
            val initial = info.displayName.firstOrNull()?.uppercaseChar()
                ?: info.number.firstOrNull() ?: '?'
            binding.tvAvatarInitial.text = initial.toString()
        }

        viewModel.simDisplayName.observe(this) { name ->
            binding.tvSimBadge.text = name
            binding.tvSimBadge.isVisible = name.isNotBlank()
        }

        viewModel.callState.observe(this) { state ->
            when (state) {
                MigCallState.ACTIVE,
                MigCallState.DISCONNECTED,
                MigCallState.IDLE -> finish()
                else -> {}
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAnswer.setOnClickListener {
            stopPulseAnimation()
            viewModel.answer()
            finish()
        }
        binding.btnDecline.setOnClickListener {
            stopPulseAnimation()
            viewModel.reject()
            finish()
        }
    }

    private var pulseX: ObjectAnimator? = null
    private var pulseY: ObjectAnimator? = null

    private fun startPulseAnimation() {
        pulseX = ObjectAnimator.ofFloat(binding.avatarContainer, View.SCALE_X, 1f, 1.06f).apply {
            duration = 800; repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
        }
        pulseY = ObjectAnimator.ofFloat(binding.avatarContainer, View.SCALE_Y, 1f, 1.06f).apply {
            duration = 800; repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
        }
        pulseX?.start(); pulseY?.start()
    }

    private fun stopPulseAnimation() {
        pulseX?.cancel(); pulseY?.cancel()
        binding.avatarContainer.scaleX = 1f
        binding.avatarContainer.scaleY = 1f
    }

    override fun onDestroy() { super.onDestroy(); stopPulseAnimation() }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {}
}
