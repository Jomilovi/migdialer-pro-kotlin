package com.migdialer.pro.ui.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.migdialer.pro.R
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.databinding.FragmentContactDetailBinding
import com.migdialer.pro.utils.SimUtils

class ContactDetailFragment : Fragment() {

    private var _binding: FragmentContactDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactDetailViewModel by viewModels {
        ContactDetailViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contactId = arguments?.getLong(ARG_CONTACT_ID) ?: run {
            findNavController().popBackStack(); return
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.load(contactId)
        viewModel.contact.observe(viewLifecycleOwner) { contact ->
            if (contact != null) render(contact) else findNavController().popBackStack()
        }
    }

    private fun render(contact: Contact) {
        // Avatar
        com.migdialer.pro.ui.recents.RecentsAdapter.loadAvatar(
            contact.photoUri,
            contact.initial,
            binding.ivAvatar,
            binding.tvInitial
        )

        binding.tvName.text = contact.name

        // Phone numbers list
        val adapter = ContactNumbersAdapter(
            onCall  = { number -> initiateCall(number) },
            onCopy  = { number -> copyNumber(number) }
        )
        binding.rvNumbers.adapter = adapter
        binding.rvNumbers.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        adapter.submitList(contact.phoneNumbers)

        // Primary call button — uses first number
        val primaryNumber = contact.phoneNumbers.firstOrNull()?.number
        binding.btnCall.setOnClickListener {
            if (primaryNumber != null) initiateCall(primaryNumber)
        }
        binding.btnCall.isEnabled = primaryNumber != null
    }

    private fun initiateCall(number: String) {
        val sims = SimUtils.getActiveSimCards(requireContext())
        when {
            sims.size <= 1 -> {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            }
            else -> showSimPickerAndCall(number, sims)
        }
    }

    private fun showSimPickerAndCall(number: String, sims: List<SimUtils.SimInfo>) {
        val names = sims.map { it.displayName }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext(), R.style.MigDialerAlertDialog)
            .setTitle(getString(R.string.choose_sim))
            .setItems(names) { _, idx ->
                val sim = sims[idx]
                val extras = Bundle().apply {
                    putInt("android.telecom.extra.PHONE_ACCOUNT_HANDLE_ID", sim.subscriptionId)
                }
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, extras)
                }
                startActivity(intent)
            }
            .show()
    }

    private fun copyNumber(number: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("phone", number))
        Toast.makeText(requireContext(), getString(R.string.number_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_CONTACT_ID = "contact_id"
        fun args(contactId: Long) = bundleOf(ARG_CONTACT_ID to contactId)
    }
}
