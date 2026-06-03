package com.example.unipazar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddAdActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth

    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: SelectedImageAdapter

    private var userProfileName: String? = null
    private var userProfilePhone: String? = null
    private var userProfileUniversity: String? = null
    private var userAvatarUrl: String? = null

    // SALE by default
    private var isSaleType = true

    private val categories = listOf("Kitap", "Elektronik", "Eşya", "Giyim", "Diğer")
    private var selectedCategory = ""

    private val getMultipleContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.clear()
            selectedImages.addAll(uris.take(5))
            imageAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_ad)

        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()

        // Window insets
        val llHeader = findViewById<View>(R.id.llHeader)
        ViewCompat.setOnApplyWindowInsetsListener(llHeader) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // RecyclerView for selected images
        val rvSelectedImages = findViewById<RecyclerView>(R.id.rvSelectedImages)
        imageAdapter = SelectedImageAdapter(selectedImages)
        rvSelectedImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedImages.adapter = imageAdapter

        // Photo upload button
        findViewById<View>(R.id.btnSelectImages).setOnClickListener {
            getMultipleContent.launch("image/*")
        }

        // Satıyorum / Arıyorum toggle
        val tabSatiyorum = findViewById<LinearLayout>(R.id.tabSatiyorum)
        val tabAriyorum = findViewById<LinearLayout>(R.id.tabAriyorum)
        val tvSatiyorum = findViewById<TextView>(R.id.tvTabSatiyorum)
        val tvAriyorum = findViewById<TextView>(R.id.tvTabAriyorum)
        val ivSatiyorumIcon = findViewById<android.widget.ImageView>(R.id.ivTabSatiyorumIcon)
        val ivAriyorumIcon = findViewById<android.widget.ImageView>(R.id.ivTabAriyorumIcon)

        val orange = android.graphics.Color.parseColor("#FF5400")
        val gray = android.graphics.Color.parseColor("#6B7280")
        val white = android.graphics.Color.parseColor("#FFFFFF")

        tabSatiyorum.setOnClickListener {
            isSaleType = true
            tabSatiyorum.setBackgroundResource(R.drawable.bg_tab_active)
            tabAriyorum.setBackgroundResource(R.drawable.bg_tab_inactive)
            tvSatiyorum.setTextColor(white)
            tvAriyorum.setTextColor(gray)
            ivSatiyorumIcon.imageTintList = android.content.res.ColorStateList.valueOf(white)
            ivAriyorumIcon.imageTintList = android.content.res.ColorStateList.valueOf(gray)
        }

        tabAriyorum.setOnClickListener {
            isSaleType = false
            tabSatiyorum.setBackgroundResource(R.drawable.bg_tab_inactive)
            tabAriyorum.setBackgroundResource(R.drawable.bg_tab_active)
            tvSatiyorum.setTextColor(gray)
            tvAriyorum.setTextColor(white)
            ivSatiyorumIcon.imageTintList = android.content.res.ColorStateList.valueOf(gray)
            ivAriyorumIcon.imageTintList = android.content.res.ColorStateList.valueOf(white)
        }

        // Category dropdown
        val tvCategorySelected = findViewById<TextView>(R.id.tvCategorySelected)
        val categoryContainer = tvCategorySelected.parent as LinearLayout

        categoryContainer.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Kategori Seç")
            builder.setItems(categories.toTypedArray()) { _, which ->
                selectedCategory = categories[which]
                tvCategorySelected.text = selectedCategory
                tvCategorySelected.setTextColor(android.graphics.Color.parseColor("#111827"))
            }
            builder.show()
        }

        // Form fields
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etUniversity = findViewById<EditText>(R.id.etUniversity)
        val etCampus = findViewById<EditText>(R.id.etCampus)
        val etContactInfo = findViewById<EditText>(R.id.etContactInfo)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Prefill from Firestore profile
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userProfileName = doc.getString("name")
                        userProfilePhone = doc.getString("phone")
                        userProfileUniversity = doc.getString("university")
                        val campus = doc.getString("campus") ?: ""
                        userAvatarUrl = doc.getString("avatarUrl")

                        userProfilePhone?.let { if (it.isNotEmpty()) etContactInfo.setText(it) }
                        userProfileUniversity?.let { if (it.isNotEmpty()) etUniversity.setText(it) }
                        if (campus.isNotEmpty()) etCampus.setText(campus)
                    }
                }
        }

        // Bottom nav
        findViewById<View>(R.id.tabAddHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.tabAddSearch).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("FOCUS_SEARCH", true)
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.tabAddMessages).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.tabAddProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        // FAB center button stays on this page
        findViewById<View>(R.id.cardFabAddPage).setOnClickListener { /* already here */ }

        // Submit
        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val price = etPrice.text.toString().trim()
            val university = etUniversity.text.toString().trim()
            val campus = etCampus.text.toString().trim()
            val contactInfo = etContactInfo.text.toString().trim()
            val category = if (selectedCategory.isNotEmpty()) selectedCategory else "Diğer"

            if (title.isBlank() || price.isBlank() || university.isBlank()) {
                Toast.makeText(this, "Lütfen başlık, fiyat ve üniversite alanlarını doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val adId = UUID.randomUUID().toString()
            val imageUrls = mutableListOf<String>()

            if (selectedImages.isNotEmpty()) {
                var uploadedCount = 0
                for (uri in selectedImages) {
                    val ref = storage.reference.child("ad_images/${adId}_${UUID.randomUUID()}.jpg")
                    ref.putFile(uri)
                        .addOnSuccessListener {
                            ref.downloadUrl.addOnSuccessListener { downloadUri ->
                                imageUrls.add(downloadUri.toString())
                                uploadedCount++
                                if (uploadedCount == selectedImages.size) {
                                    saveAdToFirestore(adId, title, description, price, if (isSaleType) "SALE" else "WANTED", university, category, imageUrls, contactInfo, campus)
                                }
                            }
                        }
                        .addOnFailureListener {
                            uploadedCount++
                            if (uploadedCount == selectedImages.size) {
                                saveAdToFirestore(adId, title, description, price, if (isSaleType) "SALE" else "WANTED", university, category, imageUrls, contactInfo, campus)
                            }
                        }
                }
            } else {
                saveAdToFirestore(adId, title, description, price, if (isSaleType) "SALE" else "WANTED", university, category, emptyList(), contactInfo, campus)
            }
        }
    }

    private fun saveAdToFirestore(
        id: String, title: String, description: String, price: String,
        type: String, university: String, category: String, imageUrls: List<String>, contactInfo: String, campus: String = ""
    ) {
        val user = auth.currentUser
        val newAd = Ad(
            id = id,
            title = title,
            description = description,
            price = price,
            type = type,
            university = university,
            category = category,
            imageUrl = if (imageUrls.isNotEmpty()) imageUrls[0] else "",
            imageUrls = imageUrls,
            contactInfo = contactInfo,
            sellerName = if (!userProfileName.isNullOrBlank()) userProfileName!! else (user?.displayName ?: user?.email?.split("@")?.get(0) ?: "Anonim Satici"),
            sellerAvatarUrl = userAvatarUrl ?: "",
            sellerUid = user?.uid ?: "",
            timestamp = System.currentTimeMillis()
        )

        db.collection("ads").document(id).set(newAd)
            .addOnSuccessListener {
                Toast.makeText(this, "🎉 İlan başarıyla yayına alındı!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<MaterialButton>(R.id.btnSubmit).isEnabled = true
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            }
    }
}
