package com.example.unipazar

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import android.view.View
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

        findViewById<LinearLayout>(R.id.btnCampusSettings).setOnClickListener {
            showUniversitySelector()
        }
        findViewById<LinearLayout>(R.id.btnHelpSupport).setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }
    }

    private fun loadUserData(uid: String) {
        val ivProfilePic = findViewById<android.widget.ImageView>(R.id.ivProfileImage)
        val tvProfileVerifiedBadge = findViewById<TextView>(R.id.tvProfileVerifiedBadge)
        
        val email = auth.currentUser?.email ?: ""
        if (email.endsWith(".edu.tr")) {
            tvProfileVerifiedBadge.text = "Onaylı Öğrenci"
            tvProfileVerifiedBadge.setBackgroundResource(R.drawable.bg_badge_verified)
            tvProfileVerifiedBadge.setTextColor(android.graphics.Color.parseColor("#C2410C"))
            tvProfileVerifiedBadge.visibility = View.VISIBLE
        } else {
            tvProfileVerifiedBadge.text = "Doğrulanmamış Kullanıcı"
            tvProfileVerifiedBadge.setBackgroundResource(R.drawable.bg_input_field)
            tvProfileVerifiedBadge.setTextColor(android.graphics.Color.parseColor("#6B7280"))
            tvProfileVerifiedBadge.visibility = View.VISIBLE
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: auth.currentUser?.email?.split("@")?.get(0) ?: "Kullanıcı"
                    val university = doc.getString("university") ?: "Üniversite belirtilmemiş"
                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    tvProfileName.text = name
                    tvProfileUniversity.text = university
                    
                    if (avatarUrl.isNotEmpty()) {
                        com.bumptech.glide.Glide.with(this)
                            .load(avatarUrl)
                            .circleCrop()
                            .into(ivProfilePic)
                    }
                } else {
                    tvProfileName.text = auth.currentUser?.email?.split("@")?.get(0) ?: "Kullanıcı"
                    tvProfileUniversity.text = "Üniversite belirtilmemiş"
                }
            }
            .addOnFailureListener {
                tvProfileName.text = "Kullanıcı"
                tvProfileUniversity.text = "Bilgi alınamadı"
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

    private fun showUniversitySelector() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_university_bottom_sheet, null)

        val etSearchUniversity = view.findViewById<EditText>(R.id.etSearchUniversity)
        val rvUniversities = view.findViewById<RecyclerView>(R.id.rvUniversities)

        rvUniversities.layoutManager = LinearLayoutManager(this)
        
        val adapter = UniversityAdapter(UniversityData.universities) { selectedUni ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("users").document(currentUser.uid).update("university", selectedUni)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Üniversite güncellendi: $selectedUni", Toast.LENGTH_SHORT).show()
                        tvProfileUniversity.text = selectedUni
                    }
            }
            bottomSheetDialog.dismiss()
        }
        rvUniversities.adapter = adapter

        etSearchUniversity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                val filteredList = UniversityData.universities.filter {
                    it.lowercase().contains(query)
                }
                adapter.updateList(filteredList)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        bottomSheetDialog.setContentView(view)
        
        bottomSheetDialog.setOnShowListener { dialog ->
            val d = dialog as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetDialog.show()
    }
}
