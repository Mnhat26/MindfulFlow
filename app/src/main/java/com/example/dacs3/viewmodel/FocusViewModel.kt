package com.example.dacs3.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.AuthRepository
import com.example.dacs3.data.repository.HistoryRepository
import com.example.dacs3.data.repository.PresetRepository
import com.example.dacs3.data.repository.UserRepository
import com.example.dacs3.model.TimerPreset
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class FocusViewModel : ViewModel() {

    private val presetRepository = PresetRepository()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private var timerJob: Job? = null
    private var secondsTracked = 0 

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

    var presetsList by mutableStateOf<List<TimerPreset>>(emptyList())
        private set

    var currentPreset by mutableStateOf<TimerPreset?>(null)
        private set

    var isFocusMode by mutableStateOf(true)
        private set

    var totalFocusSeconds by mutableIntStateOf(25 * 60)
        private set

    var timeLeft by mutableIntStateOf(25 * 60)
        private set

    var isRunning by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isUploadingAvatar by mutableStateOf(false)
        private set

    init {
        listenUser()
        listenPresets()
    }

    private fun listenUser() {
        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        userRepository.listenUser(
            onResult = { userInfo ->
                userName = userInfo.fullName
                userTitle = userInfo.title
                userAvatarUrl = userInfo.avatarUrl
                userAvatarInitial = userInfo.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            },
            onError = { message -> errorMessage = message }
        )
    }

    private fun listenPresets() {
        presetRepository.listenPresets(
            onResult = { presets ->
                presetsList = presets
                // Chỉ thiết lập preset mặc định nếu chưa có và KHÔNG đang chạy
                if (currentPreset == null && presets.isNotEmpty() && !isRunning) {
                    setPresetWithoutStart(presets.first())
                }
            },
            onError = { message -> errorMessage = message }
        )
    }

    fun toggleTimer() {
        if (isRunning) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        isRunning = true
        timerJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isRunning && timeLeft > 0) {
                delay(200L) // Kiểm tra thường xuyên hơn để bù trừ sai lệch thời gian
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTime >= 1000L) {
                    timeLeft--
                    lastTime += 1000L // Đảm bảo đếm chính xác từng giây

                    if (isFocusMode) {
                        secondsTracked++
                        if (secondsTracked >= 60) {
                            userRepository.incrementFocusMinutes(1)
                            secondsTracked = 0
                        }
                    }
                }
            }
            if (isRunning && timeLeft <= 0) finishCurrentTimer()
        }
    }

    private fun pauseTimer() {
        isRunning = false
        timerJob?.cancel()
        timerJob = null
        secondsTracked = 0 
    }

    private fun finishCurrentTimer() {
        timerJob?.cancel()
        timerJob = null
        secondsTracked = 0

        if (isFocusMode) {
            val presetName = currentPreset?.title ?: "Focus Timer"
            val duration = currentPreset?.focusMin ?: 25
            historyRepository.saveFocusHistory(presetName, duration, onError = { errorMessage = it })
        }

        isFocusMode = !isFocusMode
        val nextMinutes = if (isFocusMode) currentPreset?.focusMin ?: 25 else currentPreset?.breakMin ?: 5
        totalFocusSeconds = nextMinutes * 60
        timeLeft = totalFocusSeconds
        startTimer()
    }

    fun restartTimer() {
        pauseTimer()
        isFocusMode = true
        val minutes = currentPreset?.focusMin ?: 25
        totalFocusSeconds = minutes * 60
        timeLeft = totalFocusSeconds
    }

    fun skipTimer() {
        timeLeft = 0
        finishCurrentTimer()
    }

    fun activatePreset(preset: TimerPreset) {
        pauseTimer()
        currentPreset = preset
        isFocusMode = true
        totalFocusSeconds = preset.focusMin * 60
        timeLeft = totalFocusSeconds
        startTimer()
    }

    private fun setPresetWithoutStart(preset: TimerPreset) {
        currentPreset = preset
        isFocusMode = true
        totalFocusSeconds = preset.focusMin * 60
        // Tránh reset thời gian nếu timer đang chạy hoặc đã có tiến trình
        if (!isRunning && timeLeft == 25 * 60) {
            timeLeft = totalFocusSeconds
        }
    }

    fun savePreset(preset: TimerPreset) {
        presetRepository.savePreset(preset) { errorMessage = it }
    }

    fun deletePreset(id: Long) {
        presetRepository.deletePreset(id) { errorMessage = it }
    }

    fun updateAvatar(file: File) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        isUploadingAvatar = true
        errorMessage = null // Reset lỗi cũ
        viewModelScope.launch {
            try {
                // 1. Upload lên Supabase
                val url = authRepository.uploadUserAvatar(userId, file)
                // 2. Cập nhật link vào Firestore
                authRepository.updateUserAvatar(userId, url)
//                errorMessage = "Cập nhật ảnh đại diện thành công!"
            } catch (e: Exception) {
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
        timerJob?.cancel()
    }
}
