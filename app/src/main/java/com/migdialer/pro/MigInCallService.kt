package com.migdialer.pro

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.migdialer.pro.ui.dialer.InCallActivity
import com.migdialer.pro.ui.dialer.IncomingCallActivity

class MigInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callStateCallback)

        val displayName = getDisplayName(call)

        when (call.state) {
            Call.STATE_RINGING -> {
                // Llamada entrante — mostrar pantalla para contestar/rechazar
                startActivity(Intent(this, IncomingCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(IncomingCallActivity.EXTRA_DISPLAY_NAME, displayName)
                })
            }
            else -> {
                // Llamada saliente — mostrar pantalla de llamada activa
                startActivity(Intent(this, InCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(InCallActivity.EXTRA_DISPLAY_NAME, displayName)
                })
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callStateCallback)
        if (currentCall == call) currentCall = null
    }

    private val callStateCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
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
        @Volatile var currentCall: Call? = null
        var stateListener: ((Int) -> Unit)? = null
    }
}
