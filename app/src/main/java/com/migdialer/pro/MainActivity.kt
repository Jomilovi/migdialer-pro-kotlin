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
import com.migdialer.pro.databinding.ActivityMainBinding
import com.migdialer.pro.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Fragments observe permissions individually */ }

    // Launcher para solicitar ser dialer predeterminado
    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* El usuario aceptó o rechazó — no forzamos nada */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupNavigation()
        requestPermissionsIfNeeded()
        requestDefaultDialerIfNeeded()
        handleDialIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDialIntent(intent)
    }

    private fun setupEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.statusBars())
                ctrl.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun requestPermissionsIfNeeded() {
        val missing = PermissionUtils.missing(this)
        if (missing.isNotEmpty()) permissionLauncher.launch(missing)
    }

    /**
     * Solicita al sistema ser el dialer predeterminado si aún no lo es.
     * Android muestra un diálogo nativo — el usuario decide.
     */
    private fun requestDefaultDialerIfNeeded() {
        val telecom = getSystemService(TelecomManager::class.java) ?: return
        if (telecom.defaultDialerPackage == packageName) return
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        }
        defaultDialerLauncher.launch(intent)
    }

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
