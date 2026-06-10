package com.example.dacs3.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val content: String = "",
    val type: String = "TEXT", // TEXT, FILE
    val fileName: String? = null,
    val fileSize: String? = null,
    val timestamp: Timestamp? = null
)
