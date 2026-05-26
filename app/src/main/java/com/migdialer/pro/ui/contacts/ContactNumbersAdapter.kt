package com.migdialer.pro.ui.contacts

import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.migdialer.pro.data.model.PhoneNumber
import com.migdialer.pro.databinding.ItemContactNumberBinding

class ContactNumbersAdapter(
    private val onCall: (String) -> Unit,
    private val onCopy: (String) -> Unit
) : ListAdapter<PhoneNumber, ContactNumbersAdapter.VH>(DIFF) {

    inner class VH(val b: ItemContactNumberBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemContactNumberBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val phone = getItem(position)
        with(holder.b) {
            tvNumber.text = phone.number
            tvLabel.text  = resolveLabel(phone)
            btnCall.setOnClickListener { onCall(phone.number) }
            btnCopy.setOnClickListener { onCopy(phone.number) }
        }
    }

    private fun resolveLabel(phone: PhoneNumber): String {
        if (phone.label.isNotBlank()) return phone.label
        return when (phone.type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE  -> "Móvil"
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME    -> "Casa"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK    -> "Trabajo"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER   -> "Otro"
            else -> "Teléfono"
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PhoneNumber>() {
            override fun areItemsTheSame(a: PhoneNumber, b: PhoneNumber) = a.number == b.number
            override fun areContentsTheSame(a: PhoneNumber, b: PhoneNumber) = a == b
        }
    }
}
