package com.example.unipazar

import java.io.Serializable

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val adId: String = "",
    val adTitle: String = "",
    val adImageUrl: String = "",
    val unreadCount: Map<String, Long> = emptyMap()
) : Serializable
