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
        val fallbackUrl = when (ad.category) {
            "Kitap" -> "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=800&auto=format&fit=crop"
            "Elektronik" -> "https://images.unsplash.com/photo-1498049794561-7780e7231661?q=80&w=800&auto=format&fit=crop"
            "Ev Eşyası" -> "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?q=80&w=800&auto=format&fit=crop"
            "Özel Ders" -> "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?q=80&w=800&auto=format&fit=crop"
            "Diğer" -> "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=800&auto=format&fit=crop"
            else -> "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=800&auto=format&fit=crop"
        }

        val images = if (ad.imageUrls.isNotEmpty()) ad.imageUrls else listOf(ad.imageUrl).filter { it.isNotEmpty() }
        
        val firstImage = if (images.isNotEmpty()) images[0] else fallbackUrl
        val secondImage = if (images.size >= 2) images[1] else firstImage
        
        if (firstImage.startsWith("data:image")) {
            val bytes = android.util.Base64.decode(firstImage.substringAfter("base64,"), android.util.Base64.DEFAULT)
            Glide.with(this).load(bytes).transform(com.bumptech.glide.load.resource.bitmap.CenterCrop()).into(ivMain)
        } else {
            Glide.with(this).load(firstImage).error(Glide.with(this).load(fallbackUrl)).transform(com.bumptech.glide.load.resource.bitmap.CenterCrop()).into(ivMain)
        }
        
        if (secondImage.startsWith("data:image")) {
            val bytes = android.util.Base64.decode(secondImage.substringAfter("base64,"), android.util.Base64.DEFAULT)
            Glide.with(this).load(bytes).transform(com.bumptech.glide.load.resource.bitmap.CenterCrop()).into(ivSecondary)
        } else {
            Glide.with(this).load(secondImage).error(Glide.with(this).load(fallbackUrl)).transform(com.bumptech.glide.load.resource.bitmap.CenterCrop()).into(ivSecondary)
        }

        // Price
        val cleanPrice = ad.price.replace("\u20BA", "").trim()
        findViewById<TextView>(R.id.tvDetailPrice).text = "\u20BA" + PriceFormatter.format(cleanPrice)

        // Title, category, timestamp, location, description, seller
        findViewById<TextView>(R.id.tvDetailTitle).text = ad.title
        findViewById<TextView>(R.id.tvDetailCategory).text = "${ad.category.uppercase()} • ${ad.university.uppercase()}"
        findViewById<TextView>(R.id.tvDetailTimestamp).text = TimeUtils.getTimeAgo(ad.timestamp) + " yuklendi"
        findViewById<TextView>(R.id.tvDetailDescription).text =
            if (ad.description.isNotEmpty()) ad.description else "Aciklama eklenmemis."
        findViewById<TextView>(R.id.tvDetailSellerName).text = ad.sellerName
        val ivDetailVerifiedBadge = findViewById<ImageView>(R.id.ivDetailVerifiedBadge)
        ivDetailVerifiedBadge?.visibility = if (ad.isSellerVerified) View.VISIBLE else View.GONE

        val ivAvatar = findViewById<ImageView>(R.id.ivDetailSellerAvatar)
        val defaultAvatar = "https://ui-avatars.com/api/?name=${ad.sellerName.replace(" ", "+")}&background=random"
        val avatarUrl = if (ad.sellerAvatarUrl.isNotEmpty()) ad.sellerAvatarUrl else defaultAvatar
        if (avatarUrl.startsWith("data:image")) {
            val bytes = android.util.Base64.decode(avatarUrl.substringAfter("base64,"), android.util.Base64.DEFAULT)
            Glide.with(this).load(bytes).circleCrop().into(ivAvatar)
        } else {
            Glide.with(this).load(avatarUrl).error(Glide.with(this).load(defaultAvatar)).circleCrop().into(ivAvatar)
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

        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val favs = doc.get("favoriteAds") as? List<String> ?: emptyList()
                        isFavorited = favs.contains(ad.id)
                        if (isFavorited) {
                            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#EF4444")) // Kırmızı kalp
                        }
                        
                        
                        val purchases = doc.get("purchasedAds") as? List<String> ?: emptyList()
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
                btnFavorite.setColorFilter(android.graphics.Color.parseColor("#EF4444"))
                userRef.update("favoriteAds", com.google.firebase.firestore.FieldValue.arrayUnion(ad.id))
                
                // Bildirim Gönder
                if (ad.sellerUid.isNotEmpty() && ad.sellerUid != currentUser.uid) {
                    val notifId = java.util.UUID.randomUUID().toString()
                    val notif = Notification(
                        id = notifId,
                        receiverUid = ad.sellerUid,
                        senderUid = currentUser.uid,
                        senderName = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Bir kullanıcı",
                        title = "İlanınız Favorilere Eklendi",
                        message = "Bir kullanıcı '${ad.title}' başlıklı ilanınızı favorilerine ekledi.",
                        type = "FAVORITE",
                        relatedId = ad.id,
                        timestamp = System.currentTimeMillis()
                    )
                    db.collection("users").document(ad.sellerUid).collection("notifications").document(notifId).set(notif)
                }
                Toast.makeText(this, "Favorilere eklendi", Toast.LENGTH_SHORT).show()
            } else {
                btnFavorite.setColorFilter(android.graphics.Color.parseColor("#374151"))
                userRef.update("favoriteAds", com.google.firebase.firestore.FieldValue.arrayRemove(ad.id))
                Toast.makeText(this, "Favorilerden cikarildi", Toast.LENGTH_SHORT).show()
            }
        }

        val reportClickListener = View.OnClickListener {
            if (currentUser == null) {
                Toast.makeText(this, "Şikayet etmek için giriş yapmalısınız", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            showReportDialog(ad, currentUser.uid)
        }
        
        findViewById<View>(R.id.btnReport)?.setOnClickListener(reportClickListener)
        findViewById<View>(R.id.btnReportBottom)?.setOnClickListener(reportClickListener)

        // Removed btnBuy listener

        // Teklif Ver (Bottom Sheet Dialog)
        findViewById<MaterialButton>(R.id.btnCall).setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Teklif vermek için giriş yapın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ad.sellerUid == currentUser.uid) {
                Toast.makeText(this, "Kendi ilanınıza teklif veremezsiniz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOfferDialog(ad, auth, db)
        }

        // Mesaj Gonder - in-app chat using sellerUid
        val btnMessage = findViewById<MaterialButton>(R.id.btnWhatsApp)
        val btnDeleteAd = findViewById<MaterialButton>(R.id.btnDeleteAd)
        val btnMarkSold = findViewById<MaterialButton>(R.id.btnMarkSold)
        val btnEditAd = findViewById<MaterialButton>(R.id.btnEditAd)
        val llReportBlock = findViewById<android.widget.LinearLayout>(R.id.llReportBlock)

        if (currentUser != null) {
            if (ad.sellerUid == currentUser.uid) {
                btnEditAd.visibility = View.VISIBLE
                btnMarkSold.visibility = if (ad.status != "SOLD") View.VISIBLE else View.GONE
                btnDeleteAd.visibility = View.VISIBLE
                btnMessage.visibility = View.GONE
                llReportBlock.visibility = View.GONE
            } else {
                btnEditAd.visibility = View.GONE
                btnMarkSold.visibility = View.GONE
                btnDeleteAd.visibility = View.GONE
                btnMessage.visibility = View.VISIBLE
                llReportBlock.visibility = View.VISIBLE
            }

            // --- Handle Clicks ---
            findViewById<View>(R.id.btnBlockUser).setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Kullanıcıyı Engelle")
                    .setMessage("Bu kullanıcının ilanlarını ve mesajlarını artık görmeyeceksiniz. Emin misiniz?")
                    .setPositiveButton("Engelle") { _, _ ->
                        val userRef = db.collection("users").document(currentUser.uid)
                        userRef.update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(ad.sellerUid))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Kullanıcı engellendi.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
            
            btnMessage.setOnClickListener {
                openChatWithSeller(ad, auth, db, null)
            }

            btnEditAd.setOnClickListener {
                startActivity(Intent(this, AddAdActivity::class.java).putExtra("EDIT_AD_ID", ad.id))
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


    }
    private fun parsePrice(priceStr: String): Double {
        val clean = priceStr.replace("[^\\d,.]".toRegex(), "")
        return if (clean.contains(",") && clean.contains(".")) {
            clean.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        } else if (clean.contains(",")) {
            clean.replace(",", ".").toDoubleOrNull() ?: 0.0
        } else {
            clean.toDoubleOrNull() ?: 0.0
        }
    }

    private fun showOfferDialog(ad: Ad, auth: FirebaseAuth, db: FirebaseFirestore) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dialog_offer, null)
        
        val etOfferPrice = view.findViewById<android.widget.EditText>(R.id.etOfferPrice)
        val btnSubmitOffer = view.findViewById<MaterialButton>(R.id.btnSubmitOffer)

        btnSubmitOffer.setOnClickListener {
            val priceStr = etOfferPrice.text.toString().trim()
            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Lütfen bir teklif tutarı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val offerPriceDouble = priceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val adPriceDouble = parsePrice(ad.price)

            if (adPriceDouble > 0 && offerPriceDouble >= adPriceDouble) {
                Toast.makeText(this, "Teklifiniz ürün fiyatından düşük olmalıdır!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val message = "Merhaba, ilanınız için teklifim: ₺$priceStr"
            openChatWithSeller(ad, auth, db, message)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    private fun openChatWithSeller(ad: Ad, auth: FirebaseAuth, db: FirebaseFirestore, prefillMessage: String?) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "Mesaj göndermek için giriş yapın", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUid = currentUser.uid
        val currentName = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Kullanici"
        val sellerId = ad.sellerUid

        if (sellerId == currentUid) {
            Toast.makeText(this, "Kendi ilanınıza mesaj gönderemezsiniz", Toast.LENGTH_SHORT).show()
            return
        }

        if (sellerId.isEmpty()) {
            findSellerAndOpenChat(db, ad, currentUid, currentName, prefillMessage)
            return
        }

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
                        val intent = Intent(this, ChatActivity::class.java)
                            .putExtra("CONVERSATION_ID", conv.id)
                            .putExtra("CONVERSATION", conv)
                        if (prefillMessage != null) {
                            intent.putExtra("PREFILL_MESSAGE", prefillMessage)
                        }
                        startActivity(intent)
                    }
                } else {
                    createConversation(db, ad, currentUid, currentName, sellerId, prefillMessage)
                }
            }
    }

    private fun findSellerAndOpenChat(db: FirebaseFirestore, ad: Ad, currentUid: String, currentName: String, prefillMessage: String?) {
        // Fallback: try by name, then by email prefix
        db.collection("users").whereEqualTo("name", ad.sellerName).limit(1).get()
            .addOnSuccessListener { userSnap ->
                val sellerDoc = userSnap.documents.firstOrNull()
                if (sellerDoc != null) {
                    val sellerId = sellerDoc.id
                    if (sellerId == currentUid) {
                        Toast.makeText(this, "Kendi ilanınıza mesaj gönderemezsiniz", Toast.LENGTH_SHORT).show()
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
                                    val intent = Intent(this, ChatActivity::class.java)
                                        .putExtra("CONVERSATION_ID", conv.id)
                                        .putExtra("CONVERSATION", conv)
                                    if (prefillMessage != null) {
                                        intent.putExtra("PREFILL_MESSAGE", prefillMessage)
                                    }
                                    startActivity(intent)
                                }
                            } else {
                                createConversation(db, ad, currentUid, currentName, sellerId, prefillMessage)
                            }
                        }
                } else {
                    Toast.makeText(this, "Satici bulunamadi. Ilan sahibi henuz kayitli degil.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createConversation(db: FirebaseFirestore, ad: Ad, currentUid: String, currentName: String, sellerId: String, prefillMessage: String?) {
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
                val intent = Intent(this, ChatActivity::class.java)
                    .putExtra("CONVERSATION_ID", docRef.id)
                    .putExtra("CONVERSATION", newConv)
                if (prefillMessage != null) {
                    intent.putExtra("PREFILL_MESSAGE", prefillMessage)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReportDialog(ad: Ad, currentUid: String) {
        val reasons = arrayOf("Sahte İlan", "Yanlış Kategori", "Uygunsuz İçerik", "Diğer")
        var selectedReason = reasons[0]

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("İlanı Şikayet Et")
            .setSingleChoiceItems(reasons, 0) { _, which ->
                selectedReason = reasons[which]
            }
            .setPositiveButton("Şikayet Et") { _, _ ->
                val reportData = hashMapOf(
                    "adId" to ad.id,
                    "adTitle" to ad.title,
                    "reporterUid" to currentUid,
                    "reason" to selectedReason,
                    "timestamp" to System.currentTimeMillis()
                )
                FirebaseFirestore.getInstance().collection("reports").add(reportData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Şikayetiniz alındı, teşekkürler.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Bir hata oluştu.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
