package com.migdialer.pro

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.migdialer.pro.ui.dialer.InCallActivity

class MigInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callStateCallback)

        // Lanzar pantalla de llamada activa
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(InCallActivity.EXTRA_DISPLAY_NAME, getDisplayName(call))
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callStateCallback)
        if (currentCall == call) currentCall = null
    }

    private val callStateCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            // Notificar a la Activity si está activa
            stateListener?.invoke(state)
        }
    }

    private fun getDisplayName(call: Call): String {
        val details = call.details ?: return ""
        return details.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: details.handle?.schemeSpecificPart
            ?: ""
    }

    companion object {
        // La llamada activa actual — accesible desde InCallActivity
        @Volatile var currentCall: Call? = null

        // Listener de estado — InCallActivity lo asigna al crearse
        var stateListener: ((Int) -> Unit)? = null
    }
}
