package com.migdialer.pro.ui.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.migdialer.pro.data.repository.SettingsRepository
import com.migdialer.pro.utils.SimUtils

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val context: Context
) : ViewModel() {

    // Loaded once — SIM list is static during app lifetime
    val activeSims: List<SimUtils.SimInfo> by lazy { SimUtils.getActiveSimCards(context) }

    // ── Exposed state ─────────────────────────────────────────────────────────

    private val _defaultSimSlot = MutableLiveData(repo.defaultSimSlot)
    val defaultSimSlot: LiveData<Int> = _defaultSimSlot

    private val _vibrationEnabled = MutableLiveData(repo.vibrationEnabled)
    val vibrationEnabled: LiveData<Boolean> = _vibrationEnabled

    private val _nationalFormat = MutableLiveData(repo.nationalFormat)
    val nationalFormat: LiveData<Boolean> = _nationalFormat

    private val _accentIndex = MutableLiveData(repo.accentColorIndex)
    val accentIndex: LiveData<Int> = _accentIndex

    // ── Write-through setters ─────────────────────────────────────────────────

    fun setDefaultSim(slotIndex: Int) {
        repo.defaultSimSlot = slotIndex
        _defaultSimSlot.value = slotIndex
    }

    fun setVibration(enabled: Boolean) {
        repo.vibrationEnabled = enabled
        _vibrationEnabled.value = enabled
    }

    fun setNationalFormat(national: Boolean) {
        repo.nationalFormat = national
        _nationalFormat.value = national
    }

    fun setAccentIndex(index: Int) {
        repo.accentColorIndex = index
        _accentIndex.value = index
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(
            SettingsRepository(context.applicationContext),
            context.applicationContext
        ) as T
    }
}
