package com.migdialer.pro.ui.dialer

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.data.repository.ContactRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DialerViewModel(
    private val contactRepo: ContactRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _digits = MutableLiveData("")
    val digits: LiveData<String> = _digits

    private val _suggestions = MutableLiveData<List<Contact>>(emptyList())
    val suggestions: LiveData<List<Contact>> = _suggestions

    private var searchJob: Job? = null

    var lastNumber: String?
        get() = prefs.getString("last_number", null)
        set(v) = prefs.edit().putString("last_number", v).apply()

    fun appendDigit(d: String) {
        val current = _digits.value ?: ""
        if (current.length >= 16) return
        val new = current + d
        _digits.value = new
        // Search using T9 when digits are entered (via ContactRepository)
        searchContacts(new)
    }

    fun deleteLastDigit() {
        val current = _digits.value ?: ""
        if (current.isEmpty()) return
        val new = current.dropLast(1)
        _digits.value = new
        searchContacts(new)
    }

    fun clearDigits() {
        _digits.value = ""
        _suggestions.value = emptyList()
    }

    fun setDigits(number: String) {
        _digits.value = number
        searchContacts(number)
    }

    fun saveLastNumber(number: String) { lastNumber = number }

    fun loadSuggestions() {
        val current = _digits.value ?: ""
        if (current.length >= 2) searchContacts(current)
        else _suggestions.value = emptyList()
    }

    private fun searchContacts(query: String) {
        // Need at least 2 chars/digits to start searching
        if (query.length < 2) {
            _suggestions.value = emptyList()
            return
        }
        // Cancel previous search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                // ContactRepository now handles T9 digit→letter mapping internally
                val results = contactRepo.getContacts(query).take(6)
                _suggestions.value = results
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            }
        }
    }
}

class DialerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DialerViewModel(
            ContactRepository(context),
            context.getSharedPreferences("migdialer_prefs", Context.MODE_PRIVATE)
        ) as T
    }
}
