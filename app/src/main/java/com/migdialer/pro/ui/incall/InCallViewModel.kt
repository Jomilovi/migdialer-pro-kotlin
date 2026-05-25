package com.migdialer.pro.ui.incall

import android.app.Application
import android.media.AudioManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.migdialer.pro.telecom.CallStateManager
import com.migdialer.pro.telecom.CallerInfo
import com.migdialer.pro.telecom.MigCallState
import com.migdialer.pro.utils.SimUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InCallViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager: AudioManager =
        application.getSystemService(AudioManager::class.java)

    val callState: LiveData<MigCallState> = CallStateManager.callState
    val callerInfo: LiveData<CallerInfo>  = CallStateManager.callerInfo

    private val _elapsedSeconds = MutableLiveData(0L)
    val elapsedSeconds: LiveData<Long> = _elapsedSeconds

    private val _isMuted = MutableLiveData(false)
    val isMuted: LiveData<Boolean> = _isMuted

    private val _isSpeaker = MutableLiveData(false)
    val isSpeaker: LiveData<Boolean> = _isSpeaker

    private val _isHolding = MutableLiveData(false)
    val isHolding: LiveData<Boolean> = _isHolding

    private val _simDisplayName = MutableLiveData("")
    val simDisplayName: LiveData<String> = _simDisplayName

    private val _isDtmfVisible = MutableLiveData(false)
    val isDtmfVisible: LiveData<Boolean> = _isDtmfVisible

    private var timerJob: Job? = null
    private var timerStartEpoch = 0L

    init { loadSimName() }

    private fun loadSimName() {
        val subId  = callerInfo.value?.simSubscriptionId ?: -1
        val simMap = SimUtils.getSimMap(getApplication())
        _simDisplayName.value = simMap[subId]?.displayName ?: ""
    }

    fun startTimer() {
        if (timerJob?.isActive == true) return
        timerStartEpoch = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (SystemClock.elapsedRealtime() - timerStartEpoch) / 1000
                _elapsedSeconds.postValue(elapsed)
                delay(1_000)
            }
        }
    }

    fun stopTimer() { timerJob?.cancel(); timerJob = null }

    fun answer()  = CallStateManager.answer()
    fun reject()  = CallStateManager.reject()

    fun hangUp() {
        stopTimer()
        CallStateManager.hangUp()
    }

    fun toggleMute() {
        val next = !(_isMuted.value ?: false)
        audioManager.isMicrophoneMute = next
        _isMuted.value = next
    }

    fun toggleSpeaker() {
        val next = !(_isSpeaker.value ?: false)
        audioManager.isSpeakerphoneOn = next
        _isSpeaker.value = next
    }

    fun toggleHold() {
        CallStateManager.toggleHold()
        _isHolding.value = !(_isHolding.value ?: false)
    }

    fun toggleDtmf() {
        _isDtmfVisible.value = !(_isDtmfVisible.value ?: false)
    }

    fun sendDtmf(digit: Char) = CallStateManager.sendDtmf(digit)
    fun stopDtmf()            = CallStateManager.stopDtmf()

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute  = false
    }
}
