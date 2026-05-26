package com.migdialer.pro.ui.recents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.migdialer.pro.R
import com.migdialer.pro.data.model.CallEntry
import com.migdialer.pro.databinding.BottomSheetRecentActionsBinding
import com.migdialer.pro.databinding.FragmentRecentsBinding

class RecentsFragment : Fragment() {

    private var _binding: FragmentRecentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecentsViewModel by viewModels {
        RecentsViewModelFactory(requireContext())
    }

    private lateinit var adapter: RecentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecentsAdapter(
            onCall = { number -> call(number) },
            onItemLongClick = { entry -> showActionsSheet(entry) }
        )

        binding.rvRecents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecentsFragment.adapter
            setHasFixedSize(false)
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    // ── Acciones rápidas ──────────────────────────────────────────────────

    private fun showActionsSheet(entry: CallEntry) {
        val sheet = BottomSheetDialog(requireContext(), R.style.MigBottomSheet)
        val sheetBinding = BottomSheetRecentActionsBinding.inflate(layoutInflater)
        sheet.setContentView(sheetBinding.root)

        sheetBinding.tvSheetNumber.text = entry.number
        sheetBinding.tvSheetName.text   = if (entry.name != entry.number) entry.name else ""
        sheetBinding.tvSheetName.visibility =
            if (sheetBinding.tvSheetName.text.isBlank()) View.GONE else View.VISIBLE

        sheetBinding.rowCall.setOnClickListener {
            sheet.dismiss()
            call(entry.number)
        }
        sheetBinding.rowCopy.setOnClickListener {
            sheet.dismiss()
            copyNumber(entry.number)
        }
        sheetBinding.rowAddContact.setOnClickListener {
            sheet.dismiss()
            addToContacts(entry.number, entry.name)
        }

        sheet.show()
    }

    private fun call(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
        } catch (e: SecurityException) { /* Permission not granted */ }
    }

    private fun copyNumber(number: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("phone", number))
        Toast.makeText(requireContext(), getString(R.string.number_copied), Toast.LENGTH_SHORT).show()
    }

    private fun addToContacts(number: String, name: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
            if (name != number) putExtra(ContactsContract.Intents.Insert.NAME, name)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
