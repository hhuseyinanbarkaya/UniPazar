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
import android.text.TextWatcher
import android.text.Editable
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
    
    private var editAdId: String? = null
    private var existingImageUrls: List<String> = emptyList()
    private var existingTimestamp: Long = 0L

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
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        etUniversity.setOnClickListener {
            showUniversitySelector(etUniversity)
        }

        // Prefill from Firestore profile
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        userProfileName = doc.getString("name")
                        userProfilePhone = doc.getString("phone")
                        userProfileUniversity = doc.getString("university")
                        userAvatarUrl = doc.getString("avatarUrl")

                        userProfileUniversity?.let { if (it.isNotEmpty() && etUniversity.text.isEmpty()) etUniversity.setText(it) }
                    }
                }
        }

        // Check if we are in Edit Mode
        editAdId = intent.getStringExtra("EDIT_AD_ID")
        if (editAdId != null) {
            findViewById<TextView>(R.id.tvAddAdHeaderTitle).text = "İlanı Düzenle"
            btnSubmit.text = "Değişiklikleri Kaydet →"
            
            db.collection("ads").document(editAdId!!).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etTitle.setText(doc.getString("title"))
                    etDescription.setText(doc.getString("description"))
                    etPrice.setText(doc.getString("price"))
                    etUniversity.setText(doc.getString("university"))
                    
                    val cat = doc.getString("category") ?: "Diğer"
                    selectedCategory = cat
                    tvCategorySelected.text = cat
                    tvCategorySelected.setTextColor(android.graphics.Color.parseColor("#111827"))
                    
                    val type = doc.getString("type")
                    if (type == "WANTED") tabAriyorum.performClick() else tabSatiyorum.performClick()
                    
                    existingImageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    val timestamp = doc.getLong("timestamp")
                    if (timestamp != null) existingTimestamp = timestamp
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
            val category = if (selectedCategory.isNotEmpty()) selectedCategory else "Diğer"

            if (title.isBlank() || price.isBlank() || university.isBlank()) {
                Toast.makeText(this, "Lütfen başlık, fiyat ve üniversite alanlarını doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            progressBar.visibility = View.VISIBLE

            val adId = editAdId ?: UUID.randomUUID().toString()
            val imageUrls = mutableListOf<String>()

            if (selectedImages.isNotEmpty()) {
                var uploadedCount = 0
                for (uri in selectedImages) {
                    try {
                        val bmp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                            android.graphics.ImageDecoder.decodeBitmap(source)
                        } else {
                            android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        }
                        
                        // Scale down heavily for Base64 (max 600px width) to fit in Firestore 1MB limit
                        val ratio = 600.0f / bmp.width
                        val height = (bmp.height * ratio).toInt()
                        val scaledBmp = android.graphics.Bitmap.createScaledBitmap(bmp, 600, height, true)
                        
                        val baos = java.io.ByteArrayOutputStream()
                        scaledBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
                        val data = baos.toByteArray()
                        
                        // Create Base64 URI string for Glide to load
                        val base64Image = "data:image/jpeg;base64," + android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT).replace("\n", "")
                        
                        imageUrls.add(base64Image)
                        uploadedCount++
                        if (uploadedCount == selectedImages.size) {
                            saveAdToFirestore(adId, title, description, price, if (isSaleType) "SALE" else "WANTED", university, category, imageUrls, "")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        uploadedCount++
                        if (uploadedCount == selectedImages.size) {
                            saveAdToFirestore(adId, title, description, price, if (isSaleType) "SALE" else "WANTED", university, category, imageUrls, "")
                        }
                    }
                }
            } else {
                saveAdToFirestore(adId, title, description, price, if (isSaleType) "SALE" else "WANTED", university, category, existingImageUrls, "")
            }
        }
    }

    private fun saveAdToFirestore(
        id: String, title: String, description: String, price: String,
        type: String, university: String, category: String, imageUrls: List<String>, contactInfo: String
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
            isSellerVerified = user?.email?.endsWith(".edu.tr") == true,
            timestamp = if (existingTimestamp > 0L) existingTimestamp else System.currentTimeMillis()
        )

        val successMessage = if (editAdId != null) "🎉 İlan başarıyla güncellendi!" else "🎉 İlan başarıyla yayına alındı!"

        db.collection("ads").document(id).set(newAd)
            .addOnSuccessListener {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<MaterialButton>(R.id.btnSubmit).isEnabled = true
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            }
    }

    private fun showUniversitySelector(targetEditText: EditText) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_university_bottom_sheet, null)

        val etSearchUniversity = view.findViewById<EditText>(R.id.etSearchUniversity)
        val rvUniversities = view.findViewById<RecyclerView>(R.id.rvUniversities)

        rvUniversities.layoutManager = LinearLayoutManager(this)
        
        val adapter = UniversityAdapter(UniversityData.universities) { selectedUni ->
            targetEditText.setText(selectedUni)
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
