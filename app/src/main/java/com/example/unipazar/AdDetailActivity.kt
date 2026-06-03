package com.example.unipazar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad_detail)

        val ad = intent.getSerializableExtra("AD_ITEM") as? Ad ?: return

        val detailHeader = findViewById<View>(R.id.detailHeader)
        ViewCompat.setOnApplyWindowInsetsListener(detailHeader) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBar.top, 0, 0)
            insets
        }

        // Back
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Images
        val ivMain = findViewById<ImageView>(R.id.ivDetailImageMain)
        val ivSecondary = findViewById<ImageView>(R.id.ivDetailImageSecondary)
        val images = if (ad.imageUrls.isNotEmpty()) ad.imageUrls
                     else listOf(ad.imageUrl).filter { it.isNotEmpty() }
        if (images.isNotEmpty()) {
            Glide.with(this).load(images[0]).transform(CenterCrop()).into(ivMain)
            Glide.with(this).load(if (images.size >= 2) images[1] else images[0]).transform(CenterCrop()).into(ivSecondary)
        } else {
            ivMain.setImageResource(R.drawable.placeholder_image)
            ivSecondary.setImageResource(R.drawable.placeholder_image)
        }

        // Price
        val priceText = ad.price.trim()
        findViewById<TextView>(R.id.tvDetailPrice).text =
            if (priceText.startsWith("\u20BA")) priceText else "\u20BA${priceText}"

        // Title, category, timestamp, location, description, seller
        findViewById<TextView>(R.id.tvDetailTitle).text = ad.title
        findViewById<TextView>(R.id.tvDetailCategory).text = ad.category.uppercase()
        findViewById<TextView>(R.id.tvDetailTimestamp).text = TimeUtils.getTimeAgo(ad.timestamp) + " yuklendi"
        findViewById<TextView>(R.id.tvDetailUniversity).text = ad.university
        findViewById<TextView>(R.id.tvDetailLocationSub).text = "\u2022 Elden Teslim"
        findViewById<TextView>(R.id.tvDetailDescription).text =
            if (ad.description.isNotEmpty()) ad.description else "Aciklama eklenmemis."
        findViewById<TextView>(R.id.tvDetailSellerName).text = ad.sellerName
        findViewById<TextView>(R.id.tvDetailContactInfo).text = ad.contactInfo

        val ivAvatar = findViewById<ImageView>(R.id.ivDetailSellerAvatar)
        if (ad.sellerAvatarUrl.isNotEmpty()) {
            Glide.with(this).load(ad.sellerAvatarUrl).circleCrop().into(ivAvatar)
        }

        // Type badge
        val tvType = findViewById<TextView>(R.id.tvDetailType)
        if (ad.status == "SOLD") {
            tvType.text = "Satıldı"
            tvType.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))
        } else if (ad.type == "SALE") {
            tvType.text = "Satılık"
            tvType.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3B82F6"))
        } else {
            tvType.text = "Aranıyor"
            tvType.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F59E0B"))
        }

        // Share
        findViewById<View>(R.id.btnShare).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "UniPazar'da '${ad.title}' ilanina goz at! Fiyat: ${ad.price}")
            }
            startActivity(Intent.createChooser(shareIntent, "Paylas"))
        }

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // Favorite
        var isFavorited = false
        val btnFavorite = findViewById<ImageView>(R.id.btnFavorite)
        val btnBuy = findViewById<MaterialButton>(R.id.btnBuy)

        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val favs = doc.get("favoriteAds") as? List<String> ?: emptyList()
                        isFavorited = favs.contains(ad.id)
                        if (isFavorited) {
                            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FF5400"))
                        }
                        
                        val purchases = doc.get("purchasedAds") as? List<String> ?: emptyList()
                        if (purchases.contains(ad.id)) {
                            btnBuy.text = "Satın Alındı"
                            btnBuy.isEnabled = false
                        }
                    }
                }
        }

        btnFavorite.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(this, "Favoriye eklemek için giriş yapın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isFavorited = !isFavorited
            val userRef = db.collection("users").document(currentUser.uid)
            if (isFavorited) {
                btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FF5400"))
                userRef.update("favoriteAds", com.google.firebase.firestore.FieldValue.arrayUnion(ad.id))
                Toast.makeText(this, "Favorilere eklendi", Toast.LENGTH_SHORT).show()
            } else {
                btnFavorite.setColorFilter(android.graphics.Color.parseColor("#374151"))
                userRef.update("favoriteAds", com.google.firebase.firestore.FieldValue.arrayRemove(ad.id))
                Toast.makeText(this, "Favorilerden cikarildi", Toast.LENGTH_SHORT).show()
            }
        }

        btnBuy.setOnClickListener {
            if (currentUser == null) {
                Toast.makeText(this, "Satın almak için giriş yapın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ad.sellerUid == currentUser.uid) {
                Toast.makeText(this, "Kendi ilanınızı satın alamazsınız", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userRef = db.collection("users").document(currentUser.uid)
            userRef.update("purchasedAds", com.google.firebase.firestore.FieldValue.arrayUnion(ad.id))
            btnBuy.text = "Satın Alındı"
            btnBuy.isEnabled = false
            Toast.makeText(this, "İlan satın alınanlara eklendi!", Toast.LENGTH_SHORT).show()
        }

        // Teklif Ver (call)
        val cleanNumber = ad.contactInfo.filter { it.isDigit() }
        findViewById<MaterialButton>(R.id.btnCall).setOnClickListener {
            if (cleanNumber.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")))
            } else {
                Toast.makeText(this, "Iletisim bilgisi bulunamadi", Toast.LENGTH_SHORT).show()
            }
        }

        // Mesaj Gonder - in-app chat using sellerUid
        val btnMessage = findViewById<MaterialButton>(R.id.btnWhatsApp)

        btnMessage.setOnClickListener {
            val currentUser = auth.currentUser ?: run {
                Toast.makeText(this, "Mesaj gondermek icin giris yapin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUid = currentUser.uid
            val currentName = currentUser.displayName
                ?: currentUser.email?.split("@")?.get(0) ?: "Kullanici"

            // Determine seller UID
            val sellerId = ad.sellerUid

            // Don't message yourself
            if (sellerId == currentUid) {
                Toast.makeText(this, "Kendi ilaniniza mesaj gonderemezsiniz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sellerId.isEmpty()) {
                // Fallback: try finding seller by name in users collection
                findSellerAndOpenChat(db, ad, currentUid, currentName)
                return@setOnClickListener
            }

            // Check if conversation already exists for this ad between these two users
            db.collection("conversations")
                .whereArrayContains("participants", currentUid)
                .whereEqualTo("adId", ad.id)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        // Existing conversation found
                        val doc = snapshot.documents[0]
                        val conv = doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                        if (conv != null) {
                            startActivity(Intent(this, ChatActivity::class.java)
                                .putExtra("CONVERSATION_ID", conv.id)
                                .putExtra("CONVERSATION", conv))
                        }
                    } else {
                        // Create new conversation
                        createConversation(db, ad, currentUid, currentName, sellerId)
                    }
                }
        }

        // Owner vs Buyer actions
        val btnDeleteAd = findViewById<MaterialButton>(R.id.btnDeleteAd)
        val btnMarkSold = findViewById<MaterialButton>(R.id.btnMarkSold)
        val btnRateSeller = findViewById<MaterialButton>(R.id.btnRateSeller)

        fun setupOwnerButtons() {
            btnDeleteAd.visibility = View.VISIBLE
            if (ad.status != "SOLD") {
                btnMarkSold.visibility = View.VISIBLE
            }
            
            btnDeleteAd.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("İlanı Sil")
                    .setMessage("Bu ilanı silmek istediğinize emin misiniz?")
                    .setPositiveButton("Evet") { _, _ ->
                        db.collection("ads").document(ad.id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "İlan silindi", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Hayır", null)
                    .show()
            }

            btnMarkSold.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Satıldı İşaretle")
                    .setMessage("Bu ilanı satıldı olarak işaretlemek istiyor musunuz?")
                    .setPositiveButton("Evet") { _, _ ->
                        db.collection("ads").document(ad.id).update("status", "SOLD")
                            .addOnSuccessListener {
                                Toast.makeText(this, "İlan satıldı olarak işaretlendi", Toast.LENGTH_SHORT).show()
                                btnMarkSold.visibility = View.GONE
                                tvType.text = "Satıldı"
                                tvType.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#10B981"))
                            }
                    }
                    .setNegativeButton("Hayır", null)
                    .show()
            }
        }

        fun setupBuyerButtons() {
            btnRateSeller.visibility = View.VISIBLE
            btnRateSeller.setOnClickListener {
                showReviewDialog(ad.sellerUid)
            }
        }

        if (currentUser != null) {
            // Check by sellerUid first
            if (ad.sellerUid == currentUser.uid) {
                setupOwnerButtons()
            } else {
                setupBuyerButtons()
            }
        }
    }

    private fun showReviewDialog(sellerUid: String) {
        if (sellerUid.isEmpty()) {
            Toast.makeText(this, "Satıcı bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dialog_review, null)
        val ratingBar = view.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val etReviewComment = view.findViewById<android.widget.EditText>(R.id.etReviewComment)
        val btnSubmitReview = view.findViewById<MaterialButton>(R.id.btnSubmitReview)

        btnSubmitReview.setOnClickListener {
            val rating = ratingBar.rating
            val comment = etReviewComment.text.toString()

            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(sellerUid)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentRating = snapshot.getDouble("rating") ?: 0.0
                val currentCount = snapshot.getLong("reviewCount") ?: 0L

                val newCount = currentCount + 1
                val newRating = ((currentRating * currentCount) + rating) / newCount

                transaction.update(userRef, "rating", newRating)
                transaction.update(userRef, "reviewCount", newCount)
            }.addOnSuccessListener {
                Toast.makeText(this, "Değerlendirmeniz için teşekkürler!", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }.addOnFailureListener {
                Toast.makeText(this, "Bir hata oluştu", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun findSellerAndOpenChat(db: FirebaseFirestore, ad: Ad, currentUid: String, currentName: String) {
        // Fallback: try by name, then by email prefix
        db.collection("users").whereEqualTo("name", ad.sellerName).limit(1).get()
            .addOnSuccessListener { userSnap ->
                val sellerDoc = userSnap.documents.firstOrNull()
                if (sellerDoc != null) {
                    val sellerId = sellerDoc.id
                    if (sellerId == currentUid) {
                        Toast.makeText(this, "Kendi ilaniniza mesaj gonderemezsiniz", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    // Check existing conversation first
                    db.collection("conversations")
                        .whereArrayContains("participants", currentUid)
                        .whereEqualTo("adId", ad.id)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            if (!snapshot.isEmpty) {
                                val doc = snapshot.documents[0]
                                val conv = doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                                if (conv != null) {
                                    startActivity(Intent(this, ChatActivity::class.java)
                                        .putExtra("CONVERSATION_ID", conv.id)
                                        .putExtra("CONVERSATION", conv))
                                }
                            } else {
                                createConversation(db, ad, currentUid, currentName, sellerId)
                            }
                        }
                } else {
                    Toast.makeText(this, "Satici bulunamadi. Ilan sahibi henuz kayitli degil.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createConversation(db: FirebaseFirestore, ad: Ad, currentUid: String, currentName: String, sellerId: String) {
        val convData = hashMapOf(
            "participants" to listOf(currentUid, sellerId),
            "participantNames" to mapOf(currentUid to currentName, sellerId to ad.sellerName),
            "participantAvatars" to mapOf(currentUid to "", sellerId to ad.sellerAvatarUrl),
            "lastMessage" to "",
            "lastMessageTime" to System.currentTimeMillis(),
            "lastSenderId" to currentUid,
            "adId" to ad.id,
            "adTitle" to ad.title,
            "adImageUrl" to ad.imageUrl,
            "unreadCount" to mapOf(currentUid to 0L, sellerId to 0L)
        )
        db.collection("conversations").add(convData)
            .addOnSuccessListener { docRef ->
                val newConv = Conversation(
                    id = docRef.id,
                    participants = listOf(currentUid, sellerId),
                    participantNames = mapOf(currentUid to currentName, sellerId to ad.sellerName),
                    participantAvatars = mapOf(currentUid to "", sellerId to ad.sellerAvatarUrl),
                    adId = ad.id, adTitle = ad.title, adImageUrl = ad.imageUrl,
                    lastMessageTime = System.currentTimeMillis()
                )
                startActivity(Intent(this, ChatActivity::class.java)
                    .putExtra("CONVERSATION_ID", docRef.id)
                    .putExtra("CONVERSATION", newConv))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
