package com.example.unipazar

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var msgAdapter: MessageAdapter
    private var messagesListener: ListenerRegistration? = null

    private lateinit var conversationId: String
    private lateinit var conversation: Conversation
    private var otherId: String = ""
    private var otherName: String = ""

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadImageAndSend(uri, auth.currentUser?.uid ?: return@registerForActivityResult)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUid = auth.currentUser?.uid ?: run { finish(); return }

        conversation = intent.getSerializableExtra("CONVERSATION") as? Conversation
            ?: run { finish(); return }
        conversationId = conversation.id

        otherId = conversation.participants.firstOrNull { it != currentUid } ?: ""
        otherName = conversation.participantNames[otherId] ?: "Kullanici"

        // Insets
        val header = findViewById<View>(R.id.chatHeader)
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // Header setup
        findViewById<TextView>(R.id.tvChatOtherName).text = otherName
        findViewById<TextView>(R.id.tvChatAdTitle).text = conversation.adTitle

        val ivAvatar = findViewById<ImageView>(R.id.ivChatAvatar)
        val ivAdThumb = findViewById<ImageView>(R.id.ivChatAdThumb)

        val otherAvatar = conversation.participantAvatars[otherId] ?: ""
        if (otherAvatar.isNotEmpty()) {
            Glide.with(this).load(otherAvatar).circleCrop().into(ivAvatar)
        }
        if (conversation.adImageUrl.isNotEmpty()) {
            Glide.with(this).load(conversation.adImageUrl).centerCrop().into(ivAdThumb)
        }

        // Back button
        findViewById<View>(R.id.chatBtnBack).setOnClickListener { finish() }

        // RecyclerView
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val llManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.layoutManager = llManager

        msgAdapter = MessageAdapter(mutableListOf(), currentUid)
        rvMessages.adapter = msgAdapter

        // Load messages realtime
        loadMessages(rvMessages)

        // Mark messages as read
        markAsRead(currentUid)

        // Send button
        val etInput = findViewById<EditText>(R.id.etChatInput)
        val btnSend = findViewById<View>(R.id.btnSendMessage)
        val btnAttachImage = findViewById<View>(R.id.btnAttachImage)

        // Prefill message if available (e.g. from offer dialog)
        val prefillMsg = intent.getStringExtra("PREFILL_MESSAGE")
        if (!prefillMsg.isNullOrEmpty()) {
            etInput.setText(prefillMsg)
        }

        btnAttachImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            etInput.setText("")
            sendMessage(currentUid, text, null, rvMessages)
        }

        etInput.setOnEditorActionListener { _, _, _ ->
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                etInput.setText("")
                sendMessage(currentUid, text, null, rvMessages)
            }
            true
        }
    }

    private fun loadMessages(rv: RecyclerView) {
        messagesListener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Mark unread incoming messages as read
                val currentUid = auth.currentUser?.uid ?: ""
                var changed = false
                snapshot?.documents?.forEach { doc ->
                    val msg = doc.toObject(Message::class.java)
                    if (msg != null && msg.senderId != currentUid && !msg.read) {
                        doc.reference.update("read", true)
                        changed = true
                    }
                }

                if (!changed) {
                    msgAdapter.setMessages(msgs)
                    if (msgs.isNotEmpty()) {
                        rv.smoothScrollToPosition(msgs.size - 1)
                    }
                }
            }
    }

    private fun uploadImageAndSend(uri: Uri, senderId: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("chat_images/${UUID.randomUUID()}.jpg")
        Toast.makeText(this, "Fotoğraf yükleniyor...", Toast.LENGTH_SHORT).show()
        
        storageRef.putFile(uri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
                sendMessage(senderId, "📷 Fotoğraf", downloadUrl.toString(), rvMessages)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Fotoğraf yüklenemedi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(senderId: String, text: String, imageUrl: String?, rv: RecyclerView) {
        val timestamp = System.currentTimeMillis()
        val currentName = auth.currentUser?.displayName
            ?: auth.currentUser?.email?.split("@")?.get(0)
            ?: "Kullanici"

        val msg = hashMapOf(
            "senderId" to senderId,
            "senderName" to currentName,
            "text" to text,
            "imageUrl" to (imageUrl ?: ""),
            "timestamp" to timestamp,
            "read" to false
        )

        // Add message to subcollection
        db.collection("conversations").document(conversationId)
            .collection("messages")
            .add(msg)
            .addOnSuccessListener {
                // Update conversation's lastMessage + increment unread for other user
                val convUpdate = hashMapOf<String, Any>(
                    "lastMessage" to text,
                    "lastMessageTime" to timestamp,
                    "lastSenderId" to senderId,
                    "unreadCount.$otherId" to FieldValue.increment(1)
                )
                db.collection("conversations").document(conversationId).update(convUpdate)
                
                // Bildirim Gonder
                val notifId = java.util.UUID.randomUUID().toString()
                val notif = Notification(
                    id = notifId,
                    receiverUid = otherId,
                    senderUid = senderId,
                    senderName = currentName,
                    title = "Yeni Mesaj",
                    message = if (imageUrl != null) "📷 Fotoğraf gönderdi" else text,
                    type = "MESSAGE",
                    relatedId = conversationId,
                    timestamp = timestamp
                )
                db.collection("users").document(otherId).collection("notifications").document(notifId).set(notif)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Mesaj gonderilemedi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markAsRead(uid: String) {
        db.collection("conversations").document(conversationId)
            .update("unreadCount.$uid", 0)
    }

    override fun onResume() {
        super.onResume()
        markAsRead(auth.currentUser?.uid ?: "")
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }
}
