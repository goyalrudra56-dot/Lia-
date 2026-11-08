package com.lia.ai

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<Message>()

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    fun add(m: Message) {
        items.add(m)
        notifyItemInserted(items.lastIndex)
    }

    fun removeLast() {
        if (items.isNotEmpty()) {
            items.removeAt(items.lastIndex)
            notifyItemRemoved(items.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return VH(v.findViewById(R.id.tvMessage))
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = items[position]
        h.tv.text = m.text
        h.tv.setBackgroundResource(if (m.fromUser) R.drawable.bg_bubble_user else R.drawable.bg_bubble_lia)
        (h.tv.layoutParams as FrameLayout.LayoutParams).gravity =
            if (m.fromUser) Gravity.END else Gravity.START
    }

    override fun getItemCount() = items.size
}
