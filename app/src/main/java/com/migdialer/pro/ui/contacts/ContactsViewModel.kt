package com.migdialer.pro.ui.contacts

import android.content.Context
import androidx.lifecycle.*
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.data.repository.ContactRepository
import kotlinx.coroutines.launch

class ContactsViewModel(private val repo: ContactRepository) : ViewModel() {
    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts

    fun search(query: String) {
        viewModelScope.launch {
            _contacts.value = repo.getContacts(query)
        }
    }
}

class ContactsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ContactsViewModel(ContactRepository(context)) as T
    }
}
