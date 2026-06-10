package com.example.dacs3.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val totalDeepWorkHours: Int = 0,
    val totalFocusMinutes: Int = 0, // Thêm điểm tích lũy phút
    val title: String = "Member"
)
