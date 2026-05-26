package com.migdialer.pro.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.migdialer.pro.R
import com.migdialer.pro.data.repository.SettingsRepository
import com.migdialer.pro.databinding.FragmentSettingsBinding
import com.migdialer.pro.utils.SimUtils

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(requireContext())
    }

    // ── Default dialer request ─────────────────────────────────────────────────
    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        updateDefaultDialerState()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSimSection()
        setupHapticsSection()
        setupFormatSection()
        setupAccentSection()
        setupDefaultDialerSection()
    }

    override fun onResume() {
        super.onResume()
        updateDefaultDialerState()
    }

    // ── SIM selection ──────────────────────────────────────────────────────────

    private fun setupSimSection() {
        val sims = viewModel.activeSims
        val isSingleSim = sims.size <= 1

        // Hide SIM row entirely on single-SIM devices
        binding.rowDefaultSim.visibility = if (isSingleSim) View.GONE else View.VISIBLE
        binding.dividerSim.visibility    = if (isSingleSim) View.GONE else View.VISIBLE

        if (isSingleSim) return

        val sim1Name = sims.getOrNull(0)?.displayName ?: "SIM 1"
        val sim2Name = sims.getOrNull(1)?.displayName ?: "SIM 2"

        viewModel.defaultSimSlot.observe(viewLifecycleOwner) { slot ->
            binding.tvSimValue.text = when (slot) {
                SettingsRepository.DEFAULT_SIM_ASK -> getString(R.string.sim_ask_every_time)
                0 -> sim1Name
                1 -> sim2Name
                else -> getString(R.string.sim_ask_every_time)
            }
        }

        binding.rowDefaultSim.setOnClickListener {
            val options = arrayOf(
                getString(R.string.sim_ask_every_time),
                sim1Name,
                sim2Name
            )
            val values = intArrayOf(SettingsRepository.DEFAULT_SIM_ASK, 0, 1)
            val current = viewModel.defaultSimSlot.value ?: SettingsRepository.DEFAULT_SIM_ASK
            val checkedIdx = values.indexOfFirst { it == current }.coerceAtLeast(0)

            showSingleChoiceDialog(
                title   = getString(R.string.settings_default_sim),
                options = options,
                checked = checkedIdx
            ) { idx -> viewModel.setDefaultSim(values[idx]) }
        }
    }

    // ── Vibration ──────────────────────────────────────────────────────────────

    private fun setupHapticsSection() {
        viewModel.vibrationEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.switchVibration.isChecked = enabled
        }
        binding.switchVibration.setOnCheckedChangeListener { _, checked ->
            viewModel.setVibration(checked)
        }
        binding.rowVibration.setOnClickListener {
            binding.switchVibration.toggle()
        }
    }

    // ── Number format ──────────────────────────────────────────────────────────

    private fun setupFormatSection() {
        viewModel.nationalFormat.observe(viewLifecycleOwner) { national ->
            binding.tvFormatValue.text = getString(
                if (national) R.string.format_national else R.string.format_international
            )
        }
        binding.rowFormat.setOnClickListener {
            val options = arrayOf(
                getString(R.string.format_national),
                getString(R.string.format_international)
            )
            val current   = if (viewModel.nationalFormat.value == true) 0 else 1
            showSingleChoiceDialog(
                title   = getString(R.string.settings_number_format),
                options = options,
                checked = current
            ) { idx -> viewModel.setNationalFormat(idx == 0) }
        }
    }

    // ── Accent colour ──────────────────────────────────────────────────────────

    private fun setupAccentSection() {
        val accentColors = listOf(
            Triple(SettingsRepository.ACCENT_WHITE, getString(R.string.accent_white), R.color.accent),
            Triple(SettingsRepository.ACCENT_GREEN, getString(R.string.accent_green), R.color.green),
            Triple(SettingsRepository.ACCENT_BLUE,  getString(R.string.accent_blue),  R.color.blue)
        )

        viewModel.accentIndex.observe(viewLifecycleOwner) { idx ->
            val entry = accentColors.firstOrNull { it.first == idx } ?: accentColors[0]
            binding.tvAccentValue.text = entry.second
            binding.vAccentPreview.setBackgroundColor(requireContext().getColor(entry.third))
        }

        binding.rowAccent.setOnClickListener {
            val options = accentColors.map { it.second }.toTypedArray()
            val current = viewModel.accentIndex.value ?: SettingsRepository.ACCENT_WHITE
            showSingleChoiceDialog(
                title   = getString(R.string.settings_accent_color),
                options = options,
                checked = current
            ) { idx -> viewModel.setAccentIndex(idx) }
        }
    }

    // ── Default dialer ─────────────────────────────────────────────────────────

    private fun setupDefaultDialerSection() {
        binding.btnSetDefault.setOnClickListener { requestDefaultDialer() }
    }

    private fun updateDefaultDialerState() {
        val telecom   = requireContext().getSystemService(TelecomManager::class.java)
        val isDefault = telecom?.defaultDialerPackage == requireContext().packageName
        binding.btnSetDefault.visibility = if (isDefault) View.GONE else View.VISIBLE
        binding.tvDefaultDialerStatus.text = getString(
            if (isDefault) R.string.dialer_is_default else R.string.dialer_not_default
        )
        binding.tvDefaultDialerStatus.setTextColor(
            requireContext().getColor(if (isDefault) R.color.green else R.color.text_secondary)
        )
    }

    private fun requestDefaultDialer() {
        val roleManager = requireContext().getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)
            && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            defaultDialerLauncher.launch(intent)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun showSingleChoiceDialog(
        title: String,
        options: Array<String>,
        checked: Int,
        onSelect: (Int) -> Unit
    ) {
        var selected = checked
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            requireContext(),
            R.style.MigDialerAlertDialog
        )
            .setTitle(title)
            .setSingleChoiceItems(options, checked) { _, which -> selected = which }
            .setPositiveButton(getString(R.string.ok)) { _, _ -> onSelect(selected) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
