package com.migdialer.pro.ui.dialer

import android.content.Intent
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.TelecomManager
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.migdialer.pro.MigApp
import com.migdialer.pro.R
import com.migdialer.pro.databinding.FragmentDialerBinding
import com.migdialer.pro.utils.PermissionUtils
import com.migdialer.pro.utils.PhoneUtils

class DialerFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentDialerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DialerViewModel by viewModels {
        DialerViewModelFactory(requireContext())
    }

    private lateinit var suggestionsAdapter: SuggestionsAdapter
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null

    private val keyRows = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSuggestions()
        buildKeypad()
        setupActionButtons()
        observeViewModel()
        setupSensor()
        arguments?.getString("prefill_number")?.let { viewModel.setDigits(it) }
    }

    private fun setupSuggestions() {
        suggestionsAdapter = SuggestionsAdapter { contact ->
            val number = contact.phoneNumbers.firstOrNull()?.number ?: return@SuggestionsAdapter
            placeCall(number)
        }
        binding.rvSuggestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = suggestionsAdapter
        }
    }

    private fun buildKeypad() {
        val container = binding.keypadContainer
        container.removeAllViews()
        val density  = resources.displayMetrics.density
        val keySize  = (72 * density).toInt()
        val margin   = (5 * density).toInt()
        val boldFont = try { ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold) } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        val monoFont = try { ResourcesCompat.getFont(requireContext(), R.font.mono) } catch (e: Exception) { Typeface.MONOSPACE }

        keyRows.forEach { row ->
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.forEach { (digit, letters) ->
                val frame = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(R.drawable.bg_key_circle)
                    layoutParams = LinearLayout.LayoutParams(0, keySize, 1f).apply { setMargins(margin, margin, margin, margin) }
                }
                val digitTv = TextView(requireContext()).apply {
                    text = digit
                    setTextColor(requireContext().getColor(R.color.text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    gravity = Gravity.CENTER
                    typeface = boldFont
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val lettersTv = TextView(requireContext()).apply {
                    text = letters
                    setTextColor(requireContext().getColor(R.color.text_tertiary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                    gravity = Gravity.CENTER
                    typeface = monoFont
                    letterSpacing = 0.15f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                frame.addView(digitTv)
                frame.addView(lettersTv)
                frame.setOnClickListener { vibrate(); viewModel.appendDigit(digit) }
                if (digit == "0") frame.setOnLongClickListener { vibrate(); viewModel.appendDigit("+"); true }
                rowLayout.addView(frame)
            }
            container.addView(rowLayout)
        }
    }

    private fun setupActionButtons() {
        binding.btnBackspace.setOnClickListener { viewModel.deleteLastDigit() }
        binding.btnBackspace.setOnLongClickListener { viewModel.clearDigits(); true }
        binding.btnDial.setOnClickListener {
            val digits = viewModel.digits.value ?: ""
            if (digits.isBlank()) viewModel.lastNumber?.let { placeCall(it) }
            else placeCall(digits)
        }
    }

    private fun observeViewModel() {
        viewModel.digits.observe(viewLifecycleOwner) { digits ->
            binding.tvNumber.text = PhoneUtils.formatDisplay(digits)
            binding.btnBackspace.visibility = if (digits.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.suggestions.observe(viewLifecycleOwner) { contacts ->
            suggestionsAdapter.submitList(contacts)
            binding.rvSuggestions.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun placeCall(number: String) {
        if (!PermissionUtils.hasAll(requireContext())) {
            requestPermissions(PermissionUtils.REQUIRED, 100)
            return
        }
        val clean = PhoneUtils.cleanNumber(number)
        viewModel.saveLastNumber(clean)

        val telecom = requireContext().getSystemService(TelecomManager::class.java) ?: return
        val uri     = Uri.fromParts("tel", clean, null)

        val extras = Bundle().apply {
            putParcelable(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                MigApp.getPhoneAccountHandle(requireContext())
            )
        }

        try {
            telecom.placeCall(uri, extras)
        } catch (e: SecurityException) {
            // Fallback si no tenemos permiso CALL_PHONE
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean")))
        }
    }

    private fun setupSensor() {
        sensorManager   = requireContext().getSystemService(SensorManager::class.java)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        viewModel.loadSuggestions()
    }

    override fun onPause() { super.onPause(); sensorManager?.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent) { }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun vibrate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
                requireContext().getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) { }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
