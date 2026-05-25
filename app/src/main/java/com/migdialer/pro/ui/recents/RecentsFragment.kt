package com.migdialer.pro.ui.recents

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
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

        adapter = RecentsAdapter { number ->
            try {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
