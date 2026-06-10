package com.example.dacs3.model

import com.google.firebase.Timestamp

data class ChatGroup(
    val groupId: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val goal: String = "",
    val leaderId: String = "",
    val members: List<String> = emptyList(),
    // Hệ thống Timer & Gamification
    val timerStatus: String = "WAITING", // WAITING, START, BREAK
    val timerSeconds: Int = 1500,        // Giờ tập trung
    val breakSeconds: Int = 300,         // Giờ nghỉ
    val startTime: Timestamp? = null,
    val totalFocusMinutes: Int = 0,      // Điểm nhóm tích lũy
    val currentPresetTitle: String = ""
)
