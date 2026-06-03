package com.example.unipazar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LocalNotificationService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
    private val processedMessageKeys = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        listenForNewMessages()
        return START_STICKY
    }

    private fun listenForNewMessages() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUid = currentUser.uid

        val db = FirebaseFirestore.getInstance()
        
        listenerRegistration?.remove()
        
        // Listen to all conversations where current user is a participant
        listenerRegistration = db.collection("conversations")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                for (docChange in snapshot.documentChanges) {
                    val conv = docChange.document.toObject(Conversation::class.java).copy(id = docChange.document.id)
                    
                    // Check if there's unread messages for the current user
                    val unreadCount = conv.unreadCount[currentUid] ?: 0L
                    val lastSenderId = docChange.document.getString("lastSenderId") ?: ""
                    val lastMessage = docChange.document.getString("lastMessage") ?: ""
                    
                    // Ensure the message was sent by someone else and it's unread
                    if (unreadCount > 0 && lastSenderId != currentUid && lastSenderId.isNotEmpty()) {
                        val messageKey = "${conv.id}_${docChange.document.getLong("lastMessageTime")}"
                        
                        // Prevent showing notification for the same message multiple times
                        if (!processedMessageKeys.contains(messageKey)) {
                            processedMessageKeys.add(messageKey)
                            
                            val otherName = conv.participantNames[lastSenderId] ?: "Birisi"
                            showNotification(otherName, lastMessage, conv)
                        }
                    }
                }
            }
    }

    private fun showNotification(title: String, message: String, conv: Conversation) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("CONVERSATION", conv)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(this, conv.id.hashCode(), intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(this, "unipazar_messages")
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(conv.id.hashCode(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mesaj Bildirimleri"
            val descriptionText = "Yeni mesaj bildirimlerini gösterir"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("unipazar_messages", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
