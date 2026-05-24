package com.migdialer.pro.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import com.migdialer.pro.R
import com.migdialer.pro.data.model.Contact
import com.migdialer.pro.databinding.ItemContactBinding
import com.migdialer.pro.ui.recents.RecentsAdapter

class ContactsAdapter(
    private val onCall: (String) -> Unit
) : ListAdapter<Contact, ContactsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

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
            btnCall.setOnClickListener { contact.phoneNumbers.firstOrNull()?.number?.let { onCall(it) } }
            root.setOnClickListener   { contact.phoneNumbers.firstOrNull()?.number?.let { onCall(it) } }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Contact>() {
            override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
            override fun areContentsTheSame(a: Contact, b: Contact) = a == b
        }
    }
}
