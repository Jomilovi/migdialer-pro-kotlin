package com.migdialer.pro

import android.net.Uri
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * ConnectionService como CONNECTION_MANAGER.
 *
 * Actúa como intermediario entre la app y el operador (SIM).
 * Delega la llamada real al operador pero retiene control del audio.
 */
class MigConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        // Crear nuestra Connection para control de audio
        val connection = MigConnection(request.address)
        MigConnection.current = connection

        // Delegar la llamada real al operador telefónico (SIM)
        val telecom = getSystemService(TelecomManager::class.java)
        val simAccounts = telecom?.callCapablePhoneAccounts

        if (!simAccounts.isNullOrEmpty()) {
            // Usar la primera SIM disponible para la llamada real
            val simHandle = simAccounts[0]
            val extras = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, simHandle)
            }
            try {
                telecom.placeCall(request.address, extras)
            } catch (e: Exception) {
                // Si falla, la connection manejará el estado
            }
        }

        return connection
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        MigConnection.current = null
    }
}
