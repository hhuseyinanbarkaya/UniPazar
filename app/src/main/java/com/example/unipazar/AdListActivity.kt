package com.example.unipazar

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var adapter: AdAdapter
    private val adList = mutableListOf<Ad>()
    
    private lateinit var shimmerLayout: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyStateDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_list)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val listType = intent.getStringExtra("LIST_TYPE") ?: "MY_ADS"

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        shimmerLayout = findViewById(R.id.shimmerLayout)
        recyclerView = findViewById(R.id.recyclerView)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvEmptyStateDesc = findViewById(R.id.tvEmptyStateDesc)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdAdapter(adList)
        recyclerView.adapter = adapter

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        when (listType) {
            "MY_ADS" -> {
                supportActionBar?.title = "İlanlarım"
                tvEmptyStateDesc.text = "Henüz bir ilan paylaşmadınız."
                fetchMyAds(currentUser.uid)
            }
            "FAVORITES" -> {
                supportActionBar?.title = "Favorilerim"
                tvEmptyStateDesc.text = "Henüz favoriye eklediğiniz bir ilan yok."
                fetchUserArray(currentUser.uid, "favoriteAds")
            }
            "PURCHASES" -> {
                supportActionBar?.title = "Satın Aldıklarım"
                tvEmptyStateDesc.text = "Henüz bir ürün satın almadınız."
                fetchUserArray(currentUser.uid, "purchasedAds")
            }
        }
    }

    private fun showLoading() {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        recyclerView.visibility = View.GONE
        llEmptyState.visibility = View.GONE
    }

    private fun showContent() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        llEmptyState.visibility = View.GONE
        adapter.updateData(adList)
    }

    private fun showEmpty() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
    }

    private fun fetchMyAds(uid: String) {
        showLoading()
        db.collection("ads").whereEqualTo("sellerUid", uid).get()
            .addOnSuccessListener { snapshot ->
                adList.clear()
                for (doc in snapshot.documents) {
                    val ad = doc.toObject(Ad::class.java)?.copy(id = doc.id)
                    if (ad != null) adList.add(ad)
                }
                if (adList.isEmpty()) showEmpty() else showContent()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmpty()
            }
    }

    private fun fetchUserArray(uid: String, arrayName: String) {
        showLoading()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val ids = doc.get(arrayName) as? List<String> ?: emptyList()
                    if (ids.isEmpty()) {
                        showEmpty()
                    } else {
                        fetchAdsByIds(ids)
                    }
                } else {
                    showEmpty()
                }
            }
            .addOnFailureListener {
                showEmpty()
            }
    }

    private fun fetchAdsByIds(ids: List<String>) {
        adList.clear()
        var fetchedCount = 0
        var hasError = false

        for (id in ids) {
            db.collection("ads").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val ad = doc.toObject(Ad::class.java)?.copy(id = doc.id)
                        if (ad != null) adList.add(ad)
                    }
                    fetchedCount++
                    checkIfDone(fetchedCount, ids.size)
                }
                .addOnFailureListener {
                    fetchedCount++
                    hasError = true
                    checkIfDone(fetchedCount, ids.size)
                }
        }
    }

    private fun checkIfDone(current: Int, total: Int) {
        if (current == total) {
            if (adList.isEmpty()) {
                showEmpty()
            } else {
                showContent()
            }
        }
    }
}
