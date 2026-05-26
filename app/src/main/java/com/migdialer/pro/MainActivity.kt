package com.migdialer.pro

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.migdialer.pro.data.repository.SettingsRepository
import com.migdialer.pro.databinding.ActivityMainBinding
import com.migdialer.pro.ui.onboarding.DefaultDialerActivity
import com.migdialer.pro.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepo: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Fragments observe permissions individually */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepo = SettingsRepository(applicationContext)

        setupEdgeToEdge()
        setupNavigation()
        requestPermissionsIfNeeded()
        handleDialIntent(intent)

        // Show onboarding only once if not default dialer
        if (savedInstanceState == null) checkDefaultDialer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDialIntent(intent)
    }

    // ── Default dialer onboarding ──────────────────────────────────────────

    private fun checkDefaultDialer() {
        if (settingsRepo.onboardingShown) return
        val telecom = getSystemService(TelecomManager::class.java)
        val isDefault = telecom?.defaultDialerPackage == packageName
        if (!isDefault) {
            startActivity(Intent(this, DefaultDialerActivity::class.java))
        } else {
            settingsRepo.onboardingShown = true
        }
    }

    // ── Edge to edge ──────────────────────────────────────────────────────

    private fun setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.statusBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    // ── Permissions ───────────────────────────────────────────────────────

    private fun requestPermissionsIfNeeded() {
        val missing = PermissionUtils.missing(this)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing)
    }

    // ── tel: intent ───────────────────────────────────────────────────────

    private fun handleDialIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == "tel") {
            val number = data.schemeSpecificPart?.replace(Regex("[^0-9+*#]"), "") ?: return
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            navHost?.navController?.navigate(
                R.id.dialerFragment,
                Bundle().apply { putString("prefill_number", number) }
            )
        }
    }
}
