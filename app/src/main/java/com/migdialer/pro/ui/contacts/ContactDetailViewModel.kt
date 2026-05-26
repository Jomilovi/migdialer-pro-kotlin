package com.migdialer.pro.ui.contacts

import android.content.Context
import androidx.lifecycle.*
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.data.repository.ContactRepository
import kotlinx.coroutines.launch

class ContactDetailViewModel(private val repo: ContactRepository) : ViewModel() {

    private val _contact = MutableLiveData<Contact?>()
    val contact: LiveData<Contact?> = _contact

    fun load(contactId: Long) {
        viewModelScope.launch {
            _contact.value = repo.getContactById(contactId)
        }
    }
}

class ContactDetailViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ContactDetailViewModel(ContactRepository(context)) as T
    }
}
