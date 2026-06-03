package com.example.unipazar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notifications: List<Notification>) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNotificationTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
        val tvNotificationMessage: TextView = view.findViewById(R.id.tvNotificationMessage)
        val tvNotificationTime: TextView = view.findViewById(R.id.tvNotificationTime)
        val ivNotificationIcon: ImageView = view.findViewById(R.id.ivNotificationIcon)
        val vUnreadIndicator: View = view.findViewById(R.id.vUnreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.tvNotificationTitle.text = notification.title
        holder.tvNotificationMessage.text = notification.message
        holder.tvNotificationTime.text = TimeUtils.getTimeAgo(notification.timestamp)

        if (notification.isRead) {
            holder.vUnreadIndicator.visibility = View.GONE
            holder.itemView.alpha = 0.6f
        } else {
            holder.vUnreadIndicator.visibility = View.VISIBLE
            holder.itemView.alpha = 1.0f
        }

        when (notification.type) {
            "FAVORITE" -> holder.ivNotificationIcon.setImageResource(R.drawable.ic_heart)
            "MESSAGE" -> holder.ivNotificationIcon.setImageResource(R.drawable.ic_chat)
            else -> holder.ivNotificationIcon.setImageResource(R.drawable.ic_bell)
        }
    }

    override fun getItemCount(): Int = notifications.size
}
