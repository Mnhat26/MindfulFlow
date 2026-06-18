package com.example.dacs3.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val totalDeepWorkHours: Int = 0,
    val totalFocusMinutes: Int = 0,
    val title: String = "Member",
    val currentStreak: Int = 0,
    val lastOpenTimestamp: Long = 0L
)
