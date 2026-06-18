package com.example.dacs3.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.AuthRepository
import com.example.dacs3.data.PreferenceManager
import com.example.dacs3.data.repository.HistoryRepository
import com.example.dacs3.data.repository.PresetRepository
import com.example.dacs3.data.repository.UserRepository
import com.example.dacs3.model.TimerPreset
import com.example.dacs3.service.TimerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val presetRepository = PresetRepository()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()
    private val preferenceManager = PreferenceManager(application)

    var userName by mutableStateOf("Loading...")
        private set

    var userEmail by mutableStateOf("")
        private set

    var userTitle by mutableStateOf("Member")
        private set

    var userAvatarUrl by mutableStateOf("")
        private set

    var userAvatarInitial by mutableStateOf("U")
        private set

    var totalDeepWorkHours by mutableIntStateOf(0)
        private set

    var currentStreak by mutableIntStateOf(0)
        private set

    var globalRank by mutableIntStateOf(0)
        private set

    var presetsList by mutableStateOf<List<TimerPreset>>(emptyList())
        private set

    var currentPreset by mutableStateOf<TimerPreset?>(null)
        private set

    // --- BIẾN ĐỒNG BỘ VỚI TIMER MANAGER ---
    var isFocusMode by mutableStateOf(true)
        private set

    var totalFocusSeconds by mutableIntStateOf(25 * 60)
        private set

    var totalFocusMinutes by mutableIntStateOf(0)
        private set

    var totalDeepWorkFormatted by mutableStateOf("00:00")
        private set

    var timeLeft by mutableIntStateOf(25 * 60)
        private set

    var isRunning by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isUploadingAvatar by mutableStateOf(false)
        private set

    var isNotificationSoundEnabled by mutableStateOf(preferenceManager.isNotificationSoundEnabled)
        private set

    var selectedSoundUri by mutableStateOf(preferenceManager.selectedSoundUri)
        private set

    // --- BỘ THU TÍN HIỆU TỪ TIMER SERVICE (ĐỂ REO CHUÔNG) ---
    private val timerFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.dacs3.TIMER_FINISHED") {
                playNotificationSound()
            }
        }
    }

    fun toggleNotificationSound(enabled: Boolean) {
        isNotificationSoundEnabled = enabled
        preferenceManager.isNotificationSoundEnabled = enabled
    }

    fun setSelectedSound(uri: String?) {
        selectedSoundUri = uri
        preferenceManager.selectedSoundUri = uri
        playNotificationSound()
    }

    private fun playNotificationSound() {
        if (!isNotificationSoundEnabled) return
        try {
            val uri = if (selectedSoundUri != null) {
                android.net.Uri.parse(selectedSoundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val r = RingtoneManager.getRingtone(getApplication(), uri)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        listenUser()
        listenPresets()

        // 1. ĐĂNG KÝ BỘ NGHE TÍN HIỆU HẾT GIỜ (Đã fix lỗi ContextCompat)
        val filter = IntentFilter("com.example.dacs3.TIMER_FINISHED")
        ContextCompat.registerReceiver(
            getApplication(),
            timerFinishedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // 2. LIÊN TỤC ĐỒNG BỘ GIAO DIỆN TỪ TIMER MANAGER (Cập nhật 10 lần/giây)
        viewModelScope.launch {
            while (true) {
                timeLeft = TimerManager.timeLeft.value
                isRunning = TimerManager.isRunning.value
                isFocusMode = TimerManager.isFocusMode.value
                totalFocusSeconds = TimerManager.totalFocusSeconds.value
                delay(100L)
            }
        }
    }

    private fun listenUser() {
        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        userRepository.listenUser(
            onResult = { userInfo ->
                userName = userInfo.fullName
                userTitle = userInfo.title
                userAvatarUrl = userInfo.avatarUrl
                userAvatarInitial = userInfo.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                currentStreak = userInfo.streak

                totalFocusMinutes = userInfo.totalFocusMinutes
                totalDeepWorkHours = userInfo.totalFocusMinutes / 60

                val hours = userInfo.totalFocusMinutes / 60
                val mins = userInfo.totalFocusMinutes % 60
                totalDeepWorkFormatted = String.format("%02d:%02d", hours, mins)

                viewModelScope.launch {
                    globalRank = userRepository.getGlobalRank()
                }
            },
            onError = { message -> errorMessage = message }
        )
    }

    private fun listenPresets() {
        presetRepository.listenPresets(
            onResult = { presets ->
                presetsList = presets
                if (currentPreset == null && presets.isNotEmpty() && !TimerManager.isRunning.value) {
                    setPresetWithoutStart(presets.first())
                }
            },
            onError = { message -> errorMessage = message }
        )
    }

    // ==========================================================
    // CÁC HÀM SAU ĐÂY ĐÃ ĐƯỢC CHUYỂN SANG RA LỆNH CHO TIMER SERVICE
    // ==========================================================

    fun toggleTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply { action = "TOGGLE" }
        // Khởi động Foreground Service để hiện thông báo chạy ngầm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !TimerManager.isRunning.value) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun restartTimer() {
        if (TimerManager.isRunning.value) {
            val intent = Intent(getApplication(), TimerService::class.java).apply { action = "TOGGLE" }
            getApplication<Application>().startService(intent)
        }
        TimerManager.isFocusMode.value = true
        val minutes = currentPreset?.focusMin ?: 25
        TimerManager.totalFocusSeconds.value = minutes * 60
        TimerManager.timeLeft.value = minutes * 60
    }

    fun skipTimer() {
        // Gửi lệnh Skip sang cho Service xử lý
        val intent = Intent(getApplication(), TimerService::class.java).apply { action = "SKIP" }
        getApplication<Application>().startService(intent)
    }

    fun activatePreset(preset: TimerPreset) {
        if (TimerManager.isRunning.value) {
            val intent = Intent(getApplication(), TimerService::class.java).apply { action = "TOGGLE" }
            getApplication<Application>().startService(intent)
        }
        currentPreset = preset
        TimerManager.currentPresetTitle.value = preset.title
        TimerManager.currentFocusMin.value = preset.focusMin
        TimerManager.currentBreakMin.value = preset.breakMin
        TimerManager.isFocusMode.value = true
        TimerManager.totalFocusSeconds.value = preset.focusMin * 60
        TimerManager.timeLeft.value = preset.focusMin * 60

        // Tự động bật luôn khi chọn Preset
        toggleTimer()
    }

    private fun setPresetWithoutStart(preset: TimerPreset) {
        currentPreset = preset
        TimerManager.currentPresetTitle.value = preset.title
        TimerManager.currentFocusMin.value = preset.focusMin
        TimerManager.currentBreakMin.value = preset.breakMin
        TimerManager.isFocusMode.value = true
        TimerManager.totalFocusSeconds.value = preset.focusMin * 60
        if (!TimerManager.isRunning.value) {
            TimerManager.timeLeft.value = preset.focusMin * 60
        }
    }

    // ==========================================================

    fun savePreset(preset: TimerPreset) {
        presetRepository.savePreset(preset) { errorMessage = it }
    }

    fun deletePreset(id: Long) {
        presetRepository.deletePreset(id) { errorMessage = it }
    }

    fun updateAvatar(file: File) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        isUploadingAvatar = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val url = authRepository.uploadUserAvatar(userId, file)
                authRepository.updateUserAvatar(userId, url)
            } catch (e: Throwable) {
                errorMessage = "Lỗi: ${e.message ?: "Không thể cập nhật ảnh đại diện"}"
                e.printStackTrace()
            } finally {
                isUploadingAvatar = false
            }
        }
    }

    fun createCustomTimer(minutes: Int) {
        val safeMinutes = if (minutes <= 0) 25 else minutes
        val customPreset = TimerPreset(System.currentTimeMillis(), "Focus $safeMinutes m", safeMinutes, 5)
        savePreset(customPreset)
        setPresetWithoutStart(customPreset)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(timerFinishedReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}