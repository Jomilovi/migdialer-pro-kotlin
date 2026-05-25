package com.migdialer.pro.telecom

import android.telecom.Call
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object CallStateManager {

    @Volatile
    var activeCall: Call? = null
        private set

    private val _callState = MutableLiveData(MigCallState.IDLE)
    val callState: LiveData<MigCallState> = _callState

    private val _callerInfo = MutableLiveData(CallerInfo())
    val callerInfo: LiveData<CallerInfo> = _callerInfo

    fun onCallAdded(call: Call) {
        activeCall = call
        _callerInfo.postValue(CallerInfo.from(call))
        _callState.postValue(MigCallState.from(call.state))

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                _callState.postValue(MigCallState.from(state))
                if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                    onCallRemoved(c)
                }
            }
            override fun onDetailsChanged(c: Call, details: Call.Details) {
                _callerInfo.postValue(CallerInfo.from(c))
            }
        })
    }

    fun onCallRemoved(call: Call) {
        if (activeCall == call) {
            activeCall = null
            _callState.postValue(MigCallState.IDLE)
        }
    }

    fun answer()   { activeCall?.answer(0) }
    fun reject()   { activeCall?.reject(false, null) }
    fun hangUp()   { activeCall?.disconnect() }

    fun toggleHold() {
        activeCall?.let { call ->
            if (call.state == Call.STATE_ACTIVE) call.hold()
            else if (call.state == Call.STATE_HOLDING) call.unhold()
        }
    }

    fun sendDtmf(digit: Char) { activeCall?.playDtmfTone(digit) }
    fun stopDtmf()            { activeCall?.stopDtmfTone() }
}

data class CallerInfo(
    val number: String = "",
    val displayName: String = "",
    val simSubscriptionId: Int = -1
) {
    companion object {
        fun from(call: Call): CallerInfo {
            val handle = call.details?.handle
            val number = handle?.schemeSpecificPart ?: ""
            val name   = call.details?.callerDisplayName
                ?.takeIf { it.isNotBlank() } ?: number
            val subId  = call.details?.accountHandle?.id?.toIntOrNull() ?: -1
            return CallerInfo(number = number, displayName = name, simSubscriptionId = subId)
        }
    }
}

enum class MigCallState {
    IDLE, RINGING, DIALING, ACTIVE, HOLDING, DISCONNECTING, DISCONNECTED;

    companion object {
        fun from(telecomState: Int): MigCallState = when (telecomState) {
            Call.STATE_RINGING                   -> RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> DIALING
            Call.STATE_ACTIVE                    -> ACTIVE
            Call.STATE_HOLDING                   -> HOLDING
            Call.STATE_DISCONNECTING             -> DISCONNECTING
            Call.STATE_DISCONNECTED              -> DISCONNECTED
            else                                 -> IDLE
        }
    }
}
