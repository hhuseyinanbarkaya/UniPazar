package com.example.unipazar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var messages: MutableList<Message>,
    private val currentUid: String
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvMsgText)
        val tvTime: TextView = view.findViewById(R.id.tvMsgTime)
        val ivImage: ImageView? = view.findViewById(R.id.ivMsgImage)
        val ivReadReceipt: ImageView? = view.findViewById(R.id.ivReadReceipt)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) R.layout.item_message_sent
                       else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        
        if (msg.text.isNotEmpty()) {
            holder.tvText.visibility = View.VISIBLE
            holder.tvText.text = msg.text
        } else {
            holder.tvText.visibility = View.GONE
        }

        if (msg.imageUrl.isNotEmpty()) {
            holder.ivImage?.visibility = View.VISIBLE
            holder.ivImage?.let { iv ->
                if (msg.imageUrl.startsWith("data:image")) {
                    val bytes = android.util.Base64.decode(msg.imageUrl.substringAfter("base64,"), android.util.Base64.DEFAULT)
                    Glide.with(iv.context).load(bytes).into(iv)
                } else {
                    Glide.with(iv.context).load(msg.imageUrl).into(iv)
                }
            }
        } else {
            holder.ivImage?.visibility = View.GONE
        }

        holder.tvTime.text = formatTime(msg.timestamp)

        holder.ivReadReceipt?.let {
            if (msg.read) {
                it.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3B82F6")) // Blue ticks
            } else {
                it.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFDDD0")) // Gray ticks
            }
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(msg: Message) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale("tr"))
        return sdf.format(Date(timestamp))
    }
}
