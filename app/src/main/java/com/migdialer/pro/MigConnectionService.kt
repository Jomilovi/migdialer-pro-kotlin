package com.migdialer.pro

import android.net.Uri
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * ConnectionService propio — reemplaza TelecomManager.placeCall().
 *
 * Con ConnectionService tenemos control total del audio incluyendo altavoz,
 * a diferencia de placeCall() donde Samsung RIL bloquea el enrutamiento.
 */
class MigConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        val connection = MigConnection(request.address)
        MigConnection.current = connection
        return connection
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ) {
        // Fallback: si falla el ConnectionService, la llamada no se inicia
    }
}
