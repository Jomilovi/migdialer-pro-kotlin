package com.migdialer.pro.ui.onboarding

import android.app.Activity
import android.app.role.RoleManager
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.migdialer.pro.data.repository.SettingsRepository
import com.migdialer.pro.databinding.ActivityDefaultDialerBinding

/**
 * Single-screen onboarding to request the default dialer role.
 * Shown once on first launch if MigDialer is not already the default dialer.
 * Transparent-finish: returns to MainActivity immediately after decision.
 */
class DefaultDialerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDefaultDialerBinding
    private lateinit var settingsRepo: SettingsRepository

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // User has decided (granted or denied) — mark onboarding done and exit
        settingsRepo.onboardingShown = true
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDefaultDialerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepo = SettingsRepository(applicationContext)

        binding.btnSetDefault.setOnClickListener { requestRole() }
        binding.btnSkip.setOnClickListener {
            settingsRepo.onboardingShown = true
            finish()
        }
    }

    private fun requestRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)
            && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } else {
            // Already default — just close
            settingsRepo.onboardingShown = true
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
