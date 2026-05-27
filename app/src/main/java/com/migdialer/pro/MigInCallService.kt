package com.migdialer.pro

import android.telecom.Call
import android.telecom.InCallService

/**
 * InCallService requerido por Android para que la app pueda ser
 * establecida como dialer predeterminado del sistema.
 *
 * Sin este servicio registrado en el Manifest, TelecomManager.placeCall()
 * lanzará SecurityException y Android no ofrecerá la app como opción predeterminada.
 */
class MigInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
    }
}
