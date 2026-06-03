package com.example.unipazar

import java.io.Serializable

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
) : Serializable
