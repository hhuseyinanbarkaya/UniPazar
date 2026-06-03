package com.example.unipazar

import java.io.Serializable

data class Ad(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var price: String = "",
    var type: String = "SALE", 
    var status: String = "ACTIVE",
    var university: String = "",
    var category: String = "",
    var imageUrl: String = "",
    var imageUrls: List<String> = emptyList(),
    var contactInfo: String = "",
    var sellerName: String = "Anonim Satici",
    var sellerAvatarUrl: String = "",
    var sellerUid: String = "",
    var timestamp: Long = 0
): Serializable

object MockData {
    // Keep it empty now, we use Firestore
    val ads = mutableListOf<Ad>()
}
