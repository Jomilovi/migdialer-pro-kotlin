package com.migdialer.pro.ui.recents

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.request.CachePolicy
import coil.transform.CircleCropTransformation
import com.migdialer.pro.R
import com.migdialer.pro.data.model.CallEntry
import com.migdialer.pro.data.model.CallType
import com.migdialer.pro.databinding.ItemCallBinding
import com.migdialer.pro.databinding.ItemDateHeaderBinding

sealed class CallListItem {
    data class Header(val dateLabel: String) : CallListItem()
    data class Entry(val call: CallEntry)    : CallListItem()
}

class RecentsAdapter(
    private val onCall: (String) -> Unit
) : ListAdapter<CallListItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY  = 1

        fun loadAvatar(
            photoUri: String?,
            initial: String,
            avatarIv: ImageView,
            initialTv: TextView
        ) {
            // Always reset first — prevents stale image from previous cell
            avatarIv.dispose()
            avatarIv.setImageResource(R.drawable.bg_avatar)
            initialTv.text = initial
            initialTv.visibility = View.VISIBLE

            if (!photoUri.isNullOrBlank()) {
                try {
                    avatarIv.load(Uri.parse(photoUri)) {
                        crossfade(false)
                        transformations(CircleCropTransformation())
                        memoryCacheKey(photoUri)
                        diskCacheKey(photoUri)
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)
                        placeholder(null)
                        error(R.drawable.bg_avatar)
                        listener(
                            onSuccess = { _, _ -> initialTv.visibility = View.GONE },
                            onError   = { _, _ ->
                                avatarIv.setImageResource(R.drawable.bg_avatar)
                                initialTv.visibility = View.VISIBLE
                                initialTv.text = initial
                            }
                        )
                    }
                } catch (e: Exception) {
                    avatarIv.setImageResource(R.drawable.bg_avatar)
                    initialTv.visibility = View.VISIBLE
                }
            }
        }

        val DIFF = object : DiffUtil.ItemCallback<CallListItem>() {
            override fun areItemsTheSame(a: CallListItem, b: CallListItem) = when {
                a is CallListItem.Header && b is CallListItem.Header -> a.dateLabel == b.dateLabel
                a is CallListItem.Entry  && b is CallListItem.Entry  -> a.call.id == b.call.id
                else -> false
            }
            override fun areContentsTheSame(a: CallListItem, b: CallListItem) = a == b
        }
    }

    class HeaderVH(val b: ItemDateHeaderBinding) : RecyclerView.ViewHolder(b.root)
    class EntryVH(val b: ItemCallBinding)        : RecyclerView.ViewHolder(b.root)

    override fun getItemViewType(pos: Int) = when (getItem(pos)) {
        is CallListItem.Header -> TYPE_HEADER
        is CallListItem.Entry  -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemDateHeaderBinding.inflate(inf, parent, false))
            else        -> EntryVH(ItemCallBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CallListItem.Header -> (holder as HeaderVH).b.tvDate.text = item.dateLabel
            is CallListItem.Entry  -> bindEntry(holder as EntryVH, item.call)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is EntryVH) {
            holder.b.ivAvatar.dispose()
            holder.b.ivAvatar.setImageResource(R.drawable.bg_avatar)
        }
    }

    private fun bindEntry(holder: EntryVH, call: CallEntry) {
        with(holder.b) {
            tvName.text = call.name
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(call.date))
            val dur = if (call.durationFormatted.isNotEmpty()) " · ${call.durationFormatted}" else ""
            tvMeta.text = "$time$dur · ${call.simName}"
            tvTime.visibility = View.GONE
            tvSimBadge.text = call.simName
            tvSimBadge.visibility = View.VISIBLE
            tvSimBadge.setTextColor(
                root.context.getColor(if (call.simSlot == 2) R.color.sim2 else R.color.sim1)
            )
            ivArrow.setImageResource(when (call.type) {
                CallType.INCOMING -> R.drawable.ic_call_incoming
                CallType.OUTGOING -> R.drawable.ic_call_outgoing
                CallType.MISSED, CallType.REJECTED -> R.drawable.ic_call_missed
            })
            tvName.setTextColor(root.context.getColor(
                if (call.type == CallType.MISSED || call.type == CallType.REJECTED)
                    R.color.red else R.color.text_primary
            ))
            loadAvatar(call.photoUri, call.initial, ivAvatar, tvInitial)
            btnCall.setOnClickListener { onCall(call.number) }
            root.setOnClickListener   { onCall(call.number) }
        }
    }
}
