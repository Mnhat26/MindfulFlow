package com.example.dacs3.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow

// Dùng object (Singleton) để dữ liệu sống xuyên suốt toàn bộ app
object TimerManager {
    val timeLeft = MutableStateFlow(25 * 60)
    val totalFocusSeconds = MutableStateFlow(25 * 60)
    val isRunning = MutableStateFlow(false)
    val isFocusMode = MutableStateFlow(true)

    // Lưu tạm thông tin Preset để lúc hết giờ Service biết đường lưu Data
    val currentPresetTitle = MutableStateFlow("Focus Timer")
    val currentFocusMin = MutableStateFlow(25)
    val currentBreakMin = MutableStateFlow(5)
}