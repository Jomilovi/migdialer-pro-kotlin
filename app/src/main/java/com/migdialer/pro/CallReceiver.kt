package com.migdialer.pro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * BroadcastReceiver para cambios de estado del teléfono.
 * Requerido por Android/Samsung para reconocer la app como dialer válido.
 */
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Manejo básico de estado — suficiente para calificar como dialer
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                // Estado de llamada cambiado — en fases futuras se puede
                // usar para mostrar UI de llamada activa
            }
        }
    }
}
