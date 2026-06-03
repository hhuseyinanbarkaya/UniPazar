package com.example.unipazar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class NotificationsActivity : AppCompatActivity() {
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val notifications = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter
    private lateinit var rvNotifications: androidx.recyclerview.widget.RecyclerView
    private lateinit var llEmptyState: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvNotifications = findViewById(R.id.rvNotifications)
        llEmptyState = findViewById(R.id.llEmptyState)

        adapter = NotificationAdapter(notifications)
        rvNotifications.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvNotifications.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        db.collection("users").document(currentUser.uid).collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    notifications.clear()
                    for (doc in snapshot) {
                        val notif = doc.toObject(Notification::class.java)
                        notifications.add(notif)
                        
                        // Mark as read after fetching
                        if (!notif.isRead) {
                            doc.reference.update("isRead", true)
                        }
                    }
                    adapter.notifyDataSetChanged()

                    if (notifications.isEmpty()) {
                        rvNotifications.visibility = android.view.View.GONE
                        llEmptyState.visibility = android.view.View.VISIBLE
                    } else {
                        rvNotifications.visibility = android.view.View.VISIBLE
                        llEmptyState.visibility = android.view.View.GONE
                    }
                }
            }
    }
}
