package com.example.unipazar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class ConversationAdapter(
    private var conversations: List<Conversation>,
    private val onItemClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConvViewHolder>() {

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class ConvViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivConvAvatar)
        val tvName: TextView = view.findViewById(R.id.tvConvName)
        val tvTime: TextView = view.findViewById(R.id.tvConvTime)
        val tvLastMessage: TextView = view.findViewById(R.id.tvConvLastMessage)
        val tvUnread: TextView = view.findViewById(R.id.tvConvUnreadBadge)
        val tvAdTitle: TextView = view.findViewById(R.id.tvConvAdTitle)
        val ivAdThumb: ImageView = view.findViewById(R.id.ivConvAdThumbnail)
        val viewOnline: View = view.findViewById(R.id.viewOnlineDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConvViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConvViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConvViewHolder, position: Int) {
        val conv = conversations[position]

        // Other participant info
        val otherId = conv.participants.firstOrNull { it != currentUid } ?: ""
        val otherName = conv.participantNames[otherId] ?: "Kullanıcı"
        val otherAvatar = conv.participantAvatars[otherId] ?: ""

        holder.tvName.text = otherName
        holder.tvLastMessage.text = conv.lastMessage
        holder.tvAdTitle.text = conv.adTitle
        holder.tvTime.text = TimeUtils.getShortTimeAgo(conv.lastMessageTime)

        // Avatar
        if (otherAvatar.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(otherAvatar).circleCrop().into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        // Ad thumbnail
        if (conv.adImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(conv.adImageUrl).centerCrop().into(holder.ivAdThumb)
        } else {
            holder.ivAdThumb.setImageResource(R.drawable.placeholder_image)
        }

        // Unread badge
        val unread = conv.unreadCount[currentUid] ?: 0
        if (unread > 0) {
            holder.tvUnread.visibility = View.VISIBLE
            holder.tvUnread.text = if (unread > 9) "9+" else unread.toString()
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            holder.tvUnread.visibility = View.GONE
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener { onItemClick(conv) }
    }

    override fun getItemCount() = conversations.size

    fun update(newList: List<Conversation>) {
        conversations = newList
        notifyDataSetChanged()
    }
}
