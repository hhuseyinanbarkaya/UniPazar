package com.example.unipazar

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging

class MessagesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ConversationAdapter
    private var conversationsListener: ListenerRegistration? = null

    private var allConversations: List<Conversation> = emptyList()
    private var currentFilter = "all" // all | unread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Status bar insets
        val header = findViewById<View>(R.id.msgHeader)
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        findViewById<View>(R.id.btnMsgNotif).setOnClickListener {
            startActivity(android.content.Intent(this, NotificationsActivity::class.java))
        }

        // Save/refresh FCM token
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                db.collection("users").document(uid).update("fcmToken", token)
            }
        }

        // RecyclerView
        val rvConversations = findViewById<RecyclerView>(R.id.rvConversations)
        adapter = ConversationAdapter(emptyList()) { conv ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CONVERSATION_ID", conv.id)
            intent.putExtra("CONVERSATION", conv)
            startActivity(intent)
        }
        rvConversations.layoutManager = LinearLayoutManager(this)
        rvConversations.adapter = adapter

        // Load conversations
        loadConversations()

        // Filter tabs
        setupFilterTabs()

        // Search
        val etSearch = findViewById<EditText>(R.id.etMsgSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterConversations(s?.toString()?.lowercase() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Bottom nav
        setupBottomNav()

        // FAB
        findViewById<View>(R.id.cardMsgFab).setOnClickListener {
            startActivity(Intent(this, AddAdActivity::class.java))
        }

        // Check if opened from notification
        val openConvId = intent.getStringExtra("OPEN_CONVERSATION_ID")
        if (!openConvId.isNullOrEmpty()) {
            db.collection("conversations").document(openConvId).get()
                .addOnSuccessListener { doc ->
                    val conv = doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                    if (conv != null) {
                        val chatIntent = Intent(this, ChatActivity::class.java)
                        chatIntent.putExtra("CONVERSATION_ID", conv.id)
                        chatIntent.putExtra("CONVERSATION", conv)
                        startActivity(chatIntent)
                    }
                }
        }
    }

    private fun loadConversations() {
        val uid = auth.currentUser?.uid ?: return
        val llEmpty = findViewById<LinearLayout>(R.id.llEmptyState)
        val rv = findViewById<RecyclerView>(R.id.rvConversations)

        conversationsListener = db.collection("conversations")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                allConversations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()

                updateList()

                // Empty state
                if (allConversations.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    llEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                }
            }
    }

    private fun updateList(searchQuery: String = "") {
        val uid = auth.currentUser?.uid ?: ""
        var filtered = allConversations

        if (currentFilter == "unread") {
            filtered = filtered.filter { (it.unreadCount[uid] ?: 0) > 0 }
        }
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { conv ->
                val otherId = conv.participants.firstOrNull { it != uid } ?: ""
                val otherName = conv.participantNames[otherId] ?: ""
                otherName.lowercase().contains(searchQuery) ||
                conv.lastMessage.lowercase().contains(searchQuery) ||
                conv.adTitle.lowercase().contains(searchQuery)
            }
        }

        adapter.update(filtered)
    }

    private fun filterConversations(query: String) {
        updateList(query)
    }

    private fun setupFilterTabs() {
        val tabAll = findViewById<TextView>(R.id.tabFilterAll)
        val tabUnread = findViewById<TextView>(R.id.tabFilterUnread)
        val tabBought = findViewById<TextView>(R.id.tabFilterBought)

        tabAll.setOnClickListener {
            currentFilter = "all"
            tabAll.setBackgroundResource(R.drawable.bg_tab_active)
            tabAll.setTextColor(android.graphics.Color.WHITE)
            tabUnread.setBackgroundResource(R.drawable.bg_input_field)
            tabUnread.setTextColor(android.graphics.Color.parseColor("#6B7280"))
            updateList()
        }

        tabUnread.setOnClickListener {
            currentFilter = "unread"
            tabUnread.setBackgroundResource(R.drawable.bg_tab_active)
            tabUnread.setTextColor(android.graphics.Color.WHITE)
            tabAll.setBackgroundResource(R.drawable.bg_input_field)
            tabAll.setTextColor(android.graphics.Color.parseColor("#6B7280"))
            updateList()
        }
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.tabMsgHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.tabMsgSearch).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("FOCUS_SEARCH", true)
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.tabMsgProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationsListener?.remove()
    }
}
