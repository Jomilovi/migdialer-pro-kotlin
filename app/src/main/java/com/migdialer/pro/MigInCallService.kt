package com.migdialer.pro

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.migdialer.pro.ui.dialer.InCallActivity

class MigInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call

        // Notificar al callback si InCallActivity ya está esperando
        onCallReady?.invoke(call)

        // Lanzar pantalla de llamada activa
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(InCallActivity.EXTRA_DISPLAY_NAME, getDisplayName(call))
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (currentCall == call) {
            currentCall = null
            onCallReady = null
        }
    }

    private fun getDisplayName(call: Call): String {
        val details = call.details ?: return ""
        return details.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: details.handle?.schemeSpecificPart
            ?: ""
    }

    companion object {
        var currentCall: Call? = null
        var onCallReady: ((Call) -> Unit)? = null
    }
}
