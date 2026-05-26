package com.migdialer.pro.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.migdialer.pro.R
import com.migdialer.pro.databinding.FragmentContactsBinding

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

        adapter = ContactsAdapter(
            onCall = { number ->
                // Llamada directa desde el botón de teléfono en la lista
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_CALL,
                    android.net.Uri.parse("tel:$number")
                )
                startActivity(intent)
            },
            onContactClick = { contact ->
                // Tocar la fila → abrir ContactDetail
                findNavController().navigate(
                    R.id.action_contacts_to_detail,
                    ContactDetailFragment.args(contact.id)
                )
            }
        )

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

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
