package com.example.unipazar

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileUniversity: TextView
    private lateinit var tvActiveAdsCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupBottomNavigation()
        setupMenuListeners()

        loadUserData(currentUser.uid)
        loadUserStats(currentUser.uid)
    }

    private fun initViews() {
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileUniversity = findViewById(R.id.tvProfileUniversity)
        tvActiveAdsCount = findViewById(R.id.tvActiveAdsCount)
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.tabHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        
        findViewById<LinearLayout>(R.id.tabSearch).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("FOCUS_SEARCH", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        
        findViewById<CardView>(R.id.cardFabAdd).setOnClickListener {
            startActivity(Intent(this, AddAdActivity::class.java))
        }
        
        findViewById<LinearLayout>(R.id.tabMessages).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        
        // tabProfile is already active
    }

    private fun setupMenuListeners() {
        findViewById<LinearLayout>(R.id.btnAccountSettings).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<LinearLayout>(R.id.btnMyAds).setOnClickListener {
            startActivity(Intent(this, AdListActivity::class.java).putExtra("LIST_TYPE", "MY_ADS"))
        }
        findViewById<LinearLayout>(R.id.btnFavorites).setOnClickListener {
            startActivity(Intent(this, AdListActivity::class.java).putExtra("LIST_TYPE", "FAVORITES"))
        }
        findViewById<LinearLayout>(R.id.btnPurchases).setOnClickListener {
            startActivity(Intent(this, AdListActivity::class.java).putExtra("LIST_TYPE", "PURCHASES"))
        }
        findViewById<LinearLayout>(R.id.btnCampusSettings).setOnClickListener {
            startActivity(Intent(this, CampusSettingsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnHelpSupport).setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }
    }

    private fun loadUserData(uid: String) {
        val ivProfilePic = findViewById<android.widget.ImageView>(R.id.ivProfileImage)
        val tvRating = findViewById<TextView>(R.id.tvRating)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: auth.currentUser?.email?.split("@")?.get(0) ?: "Kullanıcı"
                    val university = doc.getString("university") ?: "Üniversite belirtilmemiş"
                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    val rating = doc.getDouble("rating") ?: 0.0
                    
                    tvProfileName.text = name
                    tvProfileUniversity.text = university
                    tvRating.text = String.format("%.1f ★", rating)
                    
                    if (avatarUrl.isNotEmpty()) {
                        com.bumptech.glide.Glide.with(this)
                            .load(avatarUrl)
                            .circleCrop()
                            .into(ivProfilePic)
                    }
                } else {
                    tvProfileName.text = auth.currentUser?.email?.split("@")?.get(0) ?: "Kullanıcı"
                    tvProfileUniversity.text = "Üniversite belirtilmemiş"
                    tvRating.text = "0.0 ★"
                }
            }
            .addOnFailureListener {
                tvProfileName.text = "Kullanıcı"
                tvProfileUniversity.text = "Bilgi alınamadı"
                tvRating.text = "0.0 ★"
            }
    }

    private fun loadUserStats(uid: String) {
        db.collection("ads").whereEqualTo("sellerUid", uid).get()
            .addOnSuccessListener { snapshot ->
                tvActiveAdsCount.text = snapshot.size().toString()
            }
            .addOnFailureListener {
                tvActiveAdsCount.text = "0"
            }
    }
}
