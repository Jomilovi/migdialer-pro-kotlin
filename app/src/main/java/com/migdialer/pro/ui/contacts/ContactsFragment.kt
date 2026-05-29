package com.migdialer.pro.ui.contacts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.migdialer.pro.MigApp
import com.migdialer.pro.databinding.FragmentContactsBinding
import com.migdialer.pro.utils.PhoneUtils

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactsViewModel by viewModels { ContactsViewModelFactory(requireContext()) }
    private lateinit var adapter: ContactsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ContactsAdapter { number -> placeCall(number) }
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.search(text?.toString() ?: "")
        }
        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
            binding.emptyState.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.search(binding.etSearch.text?.toString() ?: "")
    }

    private fun placeCall(number: String) {
        val clean   = PhoneUtils.cleanNumber(number)
        val telecom = requireContext().getSystemService(TelecomManager::class.java) ?: return
        val uri     = Uri.fromParts("tel", clean, null)
        val extras  = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, MigApp.getPhoneAccountHandle(requireContext()))
        }
        try {
            telecom.placeCall(uri, extras)
        } catch (e: SecurityException) {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean")))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
