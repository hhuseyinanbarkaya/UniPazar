package com.example.unipazar

import java.io.Serializable

data class Notification(
    var id: String = "",
    var receiverUid: String = "",
    var senderUid: String = "",
    var senderName: String = "",
    var title: String = "",
    var message: String = "",
    var type: String = "FAVORITE", // e.g., FAVORITE, MESSAGE, SYSTEM
    var relatedId: String = "", // e.g., Ad ID or Chat ID
    var isRead: Boolean = false,
    var timestamp: Long = 0
) : Serializable
