package com.migdialer.pro

import android.app.Application
import android.content.ComponentName
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Application class — registra el PhoneAccount al arrancar la app.
 * El PhoneAccount es requerido por ConnectionService para manejar llamadas.
 */
class MigApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerPhoneAccount()
    }

    private fun registerPhoneAccount() {
        val telecom = getSystemService(TelecomManager::class.java) ?: return
        val handle  = getPhoneAccountHandle()

        val account = PhoneAccount.builder(handle, getString(R.string.app_name))
            .setCapabilities(
                PhoneAccount.CAPABILITY_CALL_PROVIDER or
                PhoneAccount.CAPABILITY_CONNECTION_MANAGER
            )
            .build()

        telecom.registerPhoneAccount(account)


    }

    companion object {
        lateinit var instance: MigApp
            private set

        fun getPhoneAccountHandle(app: android.content.Context? = null): PhoneAccountHandle {
            val ctx = app ?: instance
            return PhoneAccountHandle(
                ComponentName(ctx, MigConnectionService::class.java),
                "MigDialerAccount"
            )
        }
    }
}
