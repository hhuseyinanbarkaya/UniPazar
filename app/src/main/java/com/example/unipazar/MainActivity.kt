package com.example.unipazar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AdAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private var allAds = mutableListOf<Ad>()
    private var searchQuery: String = ""
    private var selectedCategory: String = ""
    private var profileName: String = ""
    private var sortOption: Int = 0 // 0: Newest, 1: Lowest, 2: Highest
    private var minPriceFilter: Double? = null
    private var maxPriceFilter: Double? = null
    private var selectedUniversityFilter: String = "Tüm Üniversiteler"
    private var adTypeFilter: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        // Set status bar inset padding to header
        val llHeader = findViewById<View>(R.id.llHeader)
        ViewCompat.setOnApplyWindowInsetsListener(llHeader) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Initialize RecyclerView with Grid Layout Manager (2 Columns)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = AdAdapter(allAds)
        adapter.isGridView = true
        recyclerView.adapter = adapter

        // Setup Grid/List toggles
        val ivToggleGrid = findViewById<ImageView>(R.id.ivToggleGrid)
        val ivToggleList = findViewById<ImageView>(R.id.ivToggleList)
        
        ivToggleGrid.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5400"))
        ivToggleList.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6B7280"))

        ivToggleGrid.setOnClickListener {
            recyclerView.layoutManager = GridLayoutManager(this, 2)
            adapter.isGridView = true
            ivToggleGrid.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5400"))
            ivToggleList.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6B7280"))
        }

        ivToggleList.setOnClickListener {
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter.isGridView = false
            ivToggleGrid.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#6B7280"))
            ivToggleList.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5400"))
        }

        // Category items
        val catElectronics = findViewById<View>(R.id.catElectronics)
        val catBooks = findViewById<View>(R.id.catBooks)
        val catHome = findViewById<View>(R.id.catHome)
        val catWanted = findViewById<View>(R.id.catWanted)
        val catFashion = findViewById<View>(R.id.catFashion)

        val categoriesMap = mapOf(
            catElectronics to "Elektronik",
            catBooks to "Kitap",
            catHome to "Ev Eşyası",
            catWanted to "Diğer", // Using Diğer for Aranıyor since "Aranıyor" isn't a strict DB category, or maybe map it to nothing.
            catFashion to "Giyim"
        )

        categoriesMap.forEach { (view, categoryName) ->
            view.setOnClickListener {
                if (selectedCategory == categoryName) {
                    selectedCategory = ""
                } else {
                    selectedCategory = categoryName
                }
                updateCategoryUI(categoriesMap.keys, view)
                filterAds()
            }
        }

        // See All Categories click listener
        findViewById<View>(R.id.tvSeeAllCategories).setOnClickListener {
            selectedCategory = ""
            updateCategoryUI(categoriesMap.keys, null)
            filterAds()
            Toast.makeText(this, "Tüm kategoriler gösteriliyor", Toast.LENGTH_SHORT).show()
        }

        // University Selector Card click -> opens bottom sheet
        findViewById<View>(R.id.cardUniversity).setOnClickListener {
            showUniversitySelector()
        }

        // Notifications click listener
        findViewById<View>(R.id.ivNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Search edit text
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                filterAds()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Bottom Nav click listeners
        findViewById<View>(R.id.tabHome).setOnClickListener {
            updateBottomNavSelection(R.id.tabHome)
            // Reset search & category filters
            selectedCategory = ""
            updateCategoryUI(categoriesMap.keys, null)
            etSearch.setText("")
            searchQuery = ""
            filterAds()
            // Smooth scroll to top
            findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollContainer).smoothScrollTo(0, 0)
        }

        findViewById<View>(R.id.tabSearch).setOnClickListener {
            updateBottomNavSelection(R.id.tabSearch)
            etSearch.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        findViewById<View>(R.id.cardFabAdd).setOnClickListener {
            startActivity(Intent(this, AddAdActivity::class.java))
        }

        findViewById<View>(R.id.tabMessages).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        findViewById<View>(R.id.tabProfile).setOnClickListener {
            updateBottomNavSelection(R.id.tabProfile)
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Filter button click
        findViewById<View>(R.id.btnFilter).setOnClickListener {
            showFilterBottomSheet()
        }

        // Initial fetch
        fetchAds()

        // Start local notification service
        val serviceIntent = Intent(this, LocalNotificationService::class.java)
        startService(serviceIntent)

        // Request POST_NOTIFICATIONS for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Highlight tabHome by default when returning or starting
        updateBottomNavSelection(R.id.tabHome)
        fetchUserProfile()
        fetchAds()
        
        if (intent.getBooleanExtra("FOCUS_SEARCH", false)) {
            intent.removeExtra("FOCUS_SEARCH")
            val etSearch = findViewById<android.widget.EditText>(R.id.etSearch)
            etSearch.requestFocus()
            updateBottomNavSelection(R.id.tabSearch)
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun updateBottomNavSelection(activeTabId: Int) {
        val orange = Color.parseColor("#FF5400")
        val gray = Color.parseColor("#6B7280")

        // Home
        val isHome = activeTabId == R.id.tabHome
        findViewById<ImageView>(R.id.ivTabHomeIcon).imageTintList = android.content.res.ColorStateList.valueOf(if (isHome) orange else gray)
        findViewById<TextView>(R.id.tvTabHomeText).setTextColor(if (isHome) orange else gray)
        findViewById<TextView>(R.id.tvTabHomeText).setTypeface(null, if (isHome) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Search
        val isSearch = activeTabId == R.id.tabSearch
        findViewById<ImageView>(R.id.ivTabSearchIcon).imageTintList = android.content.res.ColorStateList.valueOf(if (isSearch) orange else gray)
        findViewById<TextView>(R.id.tvTabSearchText).setTextColor(if (isSearch) orange else gray)
        findViewById<TextView>(R.id.tvTabSearchText).setTypeface(null, if (isSearch) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Messages
        val isMessages = activeTabId == R.id.tabMessages
        findViewById<ImageView>(R.id.ivTabMessagesIcon).imageTintList = android.content.res.ColorStateList.valueOf(if (isMessages) orange else gray)
        findViewById<TextView>(R.id.tvTabMessagesText).setTextColor(if (isMessages) orange else gray)
        findViewById<TextView>(R.id.tvTabMessagesText).setTypeface(null, if (isMessages) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        // Profile
        val isProfile = activeTabId == R.id.tabProfile
        findViewById<ImageView>(R.id.ivTabProfileIcon).imageTintList = android.content.res.ColorStateList.valueOf(if (isProfile) orange else gray)
        findViewById<TextView>(R.id.tvTabProfileText).setTextColor(if (isProfile) orange else gray)
        findViewById<TextView>(R.id.tvTabProfileText).setTypeface(null, if (isProfile) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun updateCategoryUI(categoryViews: Collection<View>, selectedView: View?) {
        val orange = Color.parseColor("#FF5400")
        val gray = Color.parseColor("#4B5563")

        categoryViews.forEach { view ->
            val tv = (view as? android.widget.LinearLayout)?.getChildAt(1) as? TextView
            if (view == selectedView && selectedCategory.isNotEmpty()) {
                tv?.setTextColor(orange)
                tv?.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tv?.setTextColor(gray)
                tv?.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private fun fetchUserProfile() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val tvSelectedUniversity = findViewById<TextView>(R.id.tvSelectedUniversity)
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        profileName = doc.getString("name") ?: ""
                        val university = doc.getString("university") ?: ""
                        if (university.isNotEmpty()) {
                            // Removing auto-set logic based on user request. 
                            // Default will remain "Tüm Üniversiteler".
                        } else {
                            if (tvSelectedUniversity.text.toString() == "Üniversite Seç") {
                                tvSelectedUniversity.text = "Tüm Üniversiteler"
                            }
                        }
                    }
                }
        } else {
            tvSelectedUniversity.text = "Tüm Üniversiteler"
        }
    }

    private fun fetchAds() {
        val shimmerLayout = findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmerLayout)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val llEmptyState = findViewById<android.widget.LinearLayout>(R.id.llEmptyState)
        
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        recyclerView.visibility = View.GONE
        llEmptyState.visibility = View.GONE
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get().addOnSuccessListener { userDoc ->
                val blockedUsers = userDoc.get("blockedUsers") as? List<String> ?: emptyList()
                
                db.collection("ads")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshots, e ->
                        shimmerLayout.stopShimmer()
                        shimmerLayout.visibility = View.GONE

                        if (e != null) {
                            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                            llEmptyState.visibility = View.VISIBLE
                            return@addSnapshotListener
                        }

                        if (snapshots != null) {
                            allAds.clear()
                            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
                            val currentTime = System.currentTimeMillis()
                            
                            for (doc in snapshots) {
                                val ad = doc.toObject(Ad::class.java)
                                // Engellenen kullanıcıları filtrele ve 30 günden eski ilanları gösterme
                                if (!blockedUsers.contains(ad.sellerUid) && (currentTime - ad.timestamp <= thirtyDaysInMillis)) {
                                    allAds.add(ad)
                                }
                            }
                        }

                        filterAds()
                    }
            }
        } else {
            db.collection("ads")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE

                    if (e != null) {
                        Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                        llEmptyState.visibility = View.VISIBLE
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        allAds.clear()
                        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
                        val currentTime = System.currentTimeMillis()
                        
                        for (doc in snapshots) {
                            val ad = doc.toObject(Ad::class.java)
                            if (currentTime - ad.timestamp <= thirtyDaysInMillis) {
                                allAds.add(ad)
                            }
                        }
                    }

                    filterAds()
                }
        }
    }

    private fun filterAds() {
        var filtered = allAds.toList()

        // Apply Category filter
        if (selectedCategory.isNotEmpty()) {
            if (selectedCategory.equals("Wanted", ignoreCase = true)) {
                filtered = filtered.filter { 
                    it.type.equals("WANTED", ignoreCase = true) || 
                    it.category.equals("Wanted", ignoreCase = true) 
                }
            } else {
                filtered = filtered.filter { ad ->
                    val cat = ad.category.lowercase()
                    when (selectedCategory.lowercase()) {
                        "electronics" -> cat.contains("elektronik") || cat.contains("electronics") || cat.contains("tech")
                        "books" -> cat.contains("kitap") || cat.contains("book")
                        "home" -> cat.contains("esya") || cat.contains("eşya") || cat.contains("home") || cat.contains("ev")
                        "fashion" -> cat.contains("giyim") || cat.contains("fashion") || cat.contains("elbise") || cat.contains("moda")
                        else -> cat.contains(selectedCategory.lowercase())
                    }
                }
            }
        }

        // Apply Search query filter (matches title, description, category, university)
        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase().trim()
            filtered = filtered.filter { 
                it.title.lowercase().contains(query) || 
                it.description.lowercase().contains(query) || 
                it.university.lowercase().contains(query) ||
                it.category.lowercase().contains(query)
            }
        }

        // Apply University Filter
        if (selectedUniversityFilter != "Tüm Üniversiteler") {
            filtered = filtered.filter { 
                it.university.equals(selectedUniversityFilter, ignoreCase = true)
            }
        }

        if (adTypeFilter != null && adTypeFilter != "Tümü") {
            filtered = filtered.filter {
                it.type.equals(adTypeFilter, ignoreCase = true)
            }
        }

        // Apply Price filter
        filtered = filtered.filter { ad ->
            val priceVal = parsePrice(ad.price)
            val minOk = minPriceFilter?.let { priceVal >= it } ?: true
            val maxOk = maxPriceFilter?.let { priceVal <= it } ?: true
            minOk && maxOk
        }

        // Apply Sort
        filtered = when (sortOption) {
            1 -> filtered.sortedBy { parsePrice(it.price) }
            2 -> filtered.sortedByDescending { parsePrice(it.price) }
            else -> filtered.sortedByDescending { it.timestamp } // 0 or default is Newest
        }

        adapter.updateData(filtered)
        
        val llEmptyState = findViewById<android.widget.LinearLayout>(R.id.llEmptyState)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        if (filtered.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun parsePrice(priceStr: String): Double {
        val clean = priceStr.replace(Regex("[^0-9]"), "")
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun showFilterBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_filter_bottom_sheet, null)

        val rgSortOptions = view.findViewById<android.widget.RadioGroup>(R.id.rgSortOptions)
        val rbSortNewest = view.findViewById<android.widget.RadioButton>(R.id.rbSortNewest)
        val rbSortLowestPrice = view.findViewById<android.widget.RadioButton>(R.id.rbSortLowestPrice)
        val rbSortHighestPrice = view.findViewById<android.widget.RadioButton>(R.id.rbSortHighestPrice)
        
        val rgAdTypeOptions = view.findViewById<android.widget.RadioGroup>(R.id.rgAdTypeOptions)
        val rbAdTypeAll = view.findViewById<android.widget.RadioButton>(R.id.rbAdTypeAll)
        val rbAdTypeSelling = view.findViewById<android.widget.RadioButton>(R.id.rbAdTypeSelling)
        val rbAdTypeBuying = view.findViewById<android.widget.RadioButton>(R.id.rbAdTypeBuying)
        
        val etMinPrice = view.findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice = view.findViewById<EditText>(R.id.etMaxPrice)
        val btnApplyFilter = view.findViewById<android.widget.Button>(R.id.btnApplyFilter)
        val btnClearFilter = view.findViewById<TextView>(R.id.btnClearFilter)

        // Pre-fill
        when (sortOption) {
            1 -> rbSortLowestPrice.isChecked = true
            2 -> rbSortHighestPrice.isChecked = true
            else -> rbSortNewest.isChecked = true
        }
        
        when (adTypeFilter) {
            "Satıyorum" -> rbAdTypeSelling.isChecked = true
            "Arıyorum" -> rbAdTypeBuying.isChecked = true
            else -> rbAdTypeAll.isChecked = true
        }
        
        etMinPrice.setText(minPriceFilter?.toInt()?.toString() ?: "")
        etMaxPrice.setText(maxPriceFilter?.toInt()?.toString() ?: "")

        btnApplyFilter.setOnClickListener {
            sortOption = when (rgSortOptions.checkedRadioButtonId) {
                R.id.rbSortLowestPrice -> 1
                R.id.rbSortHighestPrice -> 2
                else -> 0
            }
            
            adTypeFilter = when (rgAdTypeOptions.checkedRadioButtonId) {
                R.id.rbAdTypeSelling -> "Satıyorum"
                R.id.rbAdTypeBuying -> "Arıyorum"
                else -> "Tümü"
            }
            
            val minText = etMinPrice.text.toString()
            val maxText = etMaxPrice.text.toString()
            minPriceFilter = minText.toDoubleOrNull()
            maxPriceFilter = maxText.toDoubleOrNull()

            bottomSheetDialog.dismiss()
            filterAds()
            Toast.makeText(this, "Filtreler uygulandı", Toast.LENGTH_SHORT).show()
        }

        btnClearFilter.setOnClickListener {
            sortOption = 0
            adTypeFilter = null
            minPriceFilter = null
            maxPriceFilter = null
            bottomSheetDialog.dismiss()
            filterAds()
            Toast.makeText(this, "Filtreler temizlendi", Toast.LENGTH_SHORT).show()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun showUniversitySelector() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_university_bottom_sheet, null)

        val etSearchUniversity = view.findViewById<EditText>(R.id.etSearchUniversity)
        val rvUniversities = view.findViewById<RecyclerView>(R.id.rvUniversities)

        rvUniversities.layoutManager = LinearLayoutManager(this)
        
        val adapter = UniversityAdapter(UniversityData.universities) { selectedUni ->
            selectedUniversityFilter = selectedUni
            findViewById<TextView>(R.id.tvSelectedUniversity).text = selectedUni
            filterAds()
            bottomSheetDialog.dismiss()
        }
        rvUniversities.adapter = adapter

        etSearchUniversity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                val filteredList = UniversityData.universities.filter {
                    it.lowercase().contains(query) || it == "Tüm Üniversiteler"
                }
                adapter.updateList(filteredList)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        bottomSheetDialog.setContentView(view)
        
        // Expand the bottom sheet
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