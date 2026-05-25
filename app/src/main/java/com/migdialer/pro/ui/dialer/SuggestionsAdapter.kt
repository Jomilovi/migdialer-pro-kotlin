package com.migdialer.pro.ui.dialer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.migdialer.pro.R
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.databinding.ItemSuggestionBinding
import com.migdialer.pro.ui.recents.RecentsAdapter

class SuggestionsAdapter(
    private val onCall: (Contact) -> Unit
) : ListAdapter<Contact, SuggestionsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemSuggestionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.b.ivAvatar.dispose()
        holder.b.ivAvatar.setImageResource(R.drawable.bg_avatar)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val contact = getItem(position)
        with(holder.b) {
            tvName.text   = contact.name
            tvNumber.text = contact.phoneNumbers.firstOrNull()?.number ?: ""
            RecentsAdapter.loadAvatar(contact.photoUri, contact.initial, ivAvatar, tvInitial)
            root.setOnClickListener { onCall(contact) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
            override fun areContentsTheSame(a: Contact, b: Contact) = a == b
        }
    }
}
